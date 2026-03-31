package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Enums.PaymentMethod;
import com.driver.bookMyShow.Enums.PaymentStatus;
import com.driver.bookMyShow.Gateway.MockPaymentGateway;
import com.driver.bookMyShow.Models.*;
import com.driver.bookMyShow.Repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * PaymentService - Handles payment processing and lifecycle
 * 
 * Features:
 * - Idempotent payment initiation
 * - Integration with payment gateway
 * - Ticket creation after successful payment
 * - Wallet transaction recording
 * - Refund processing
 * - Price calculation with fees and taxes
 */
@Slf4j
@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockPaymentGateway paymentGateway;

    @Autowired
    private UserWalletService userWalletService; // New DB-driven wallet service
    
    @Autowired
    private SeatLockService seatLockService;
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private TemporaryWalletRepository temporaryWalletRepository;

    @Autowired
    private TemporaryWalletUsageRepository temporaryWalletUsageRepository;

    // Pricing constants
    private static final double CONVENIENCE_FEE_PERCENTAGE = 2.5; // 2.5%
    private static final double TAX_PERCENTAGE = 18.0; // 18% GST
    private static final double MIN_CONVENIENCE_FEE = 20.0;

    /**
     * Initiate payment (idempotent)
     * Creates or retrieves existing payment record
     */
    @Transactional
    public Payment initiatePayment(String sessionId, Integer userId, Double baseAmount,
                                   PaymentMethod paymentMethod, String promoCode,
                                   Boolean useTemporaryWallet) throws Exception {
        
        // Check for existing payment with same session (idempotency)
        Optional<Payment> existingPayment = paymentRepository.findBySessionId(sessionId);
        if (existingPayment.isPresent()) {
            return existingPayment.get();
        }

        // Verify seat locks are still active
        Long remainingTime = seatLockService.getRemainingTime(sessionId);
        if (remainingTime <= 0) {
            throw new Exception("Seat locks have expired. Please select seats again.");
        }
        
        // Auto-extend locks if less than 3 minutes remaining
        // This prevents locks from expiring during payment processing
        if (remainingTime < 180) { // 3 minutes
            try {
                seatLockService.extendLockTime(sessionId, 5); // Extend by 5 minutes
                log.info("🔄 Auto-extended seat locks for session {} (was {} seconds remaining)", 
                        sessionId, remainingTime);
            } catch (Exception e) {
                log.warn("⚠️ Failed to auto-extend seat locks: {}", e.getMessage());
                // Continue anyway - will fail later if locks actually expired
            }
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found"));

        // Calculate fees and taxes
        double convenienceFee = Math.max(
            baseAmount * CONVENIENCE_FEE_PERCENTAGE / 100,
            MIN_CONVENIENCE_FEE
        );
        double tax = (baseAmount + convenienceFee) * TAX_PERCENTAGE / 100;

        // Apply promo code discount if any
        double discountAmount = 0.0;
        if (promoCode != null && !promoCode.isEmpty()) {
            discountAmount = calculateDiscount(promoCode, baseAmount);
        }

        // Create payment record
        Payment payment = Payment.builder()
                .transactionId("TXN_" + UUID.randomUUID().toString().replace("-", ""))
                .sessionId(sessionId)
                .user(user)
                .baseAmount(baseAmount)
                .convenienceFee(convenienceFee)
                .tax(tax)
                .discountAmount(discountAmount)
                .promoCode(promoCode)
                .paymentMethod(paymentMethod)
                .status(PaymentStatus.PENDING)
                .build();

        payment.calculateTotal();
        boolean shouldUseTemporaryWallet = useTemporaryWallet == null || useTemporaryWallet;
        Double appliedTempCredit = shouldUseTemporaryWallet
            ? getApplicableTemporaryWalletAmount(userId, payment.getTotalAmount())
            : 0.0;
        payment.setTemporaryWalletAmount(appliedTempCredit);
        payment.setPayableAmount(round2(Math.max(0.0, payment.getTotalAmount() - appliedTempCredit)));
        return paymentRepository.save(payment);
    }

    /**
     * Process payment through gateway
     */
    @Transactional
    public Payment processPayment(String transactionId) throws Exception {
        Payment payment = paymentRepository.findByTransactionIdForUpdate(transactionId)
                .orElseThrow(() -> new Exception("Payment not found"));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            // Already processed (idempotency)
            return payment;
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new Exception("Payment cannot be processed in current status: " + 
                              payment.getStatus());
        }

        if (payment.getPaymentMethod() == PaymentMethod.STRIPE ||
                payment.getPaymentMethod() == PaymentMethod.WALLET_CARD_SPLIT) {
            throw new Exception("Use Stripe confirmation endpoint to complete this payment");
        }

        // Verify seats are still locked
        Long remainingTime = seatLockService.getRemainingTime(payment.getSessionId());
        if (remainingTime <= 0) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse("Seat locks expired");
            paymentRepository.save(payment);
            throw new Exception("Seat locks have expired");
        }

        // Update status to processing
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        try {
            // Handle WALLET payment method
            if (payment.getPaymentMethod() == PaymentMethod.WALLET) {
                Double payableAmount = payment.getPayableAmount() != null
                        ? payment.getPayableAmount()
                        : payment.getTotalAmount();

                // Use UserWalletService to get current balance from DB
                Double currentBalance = userWalletService.getWalletBalance(payment.getUser().getId());
                
                // Check wallet balance BEFORE attempting debit
                if (payableAmount > 0 && currentBalance < payableAmount) {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setGatewayResponse("Insufficient wallet balance. Available: ₹" + currentBalance + ", Required: ₹" + payableAmount);
                    paymentRepository.save(payment);
                    
                    // Release seat locks
                    seatLockService.releaseLocks(payment.getSessionId());
                    
                    throw new Exception("Insufficient wallet balance. Available: ₹" + currentBalance + ", Required: ₹" + payableAmount);
                }
                
                // Debit from wallet using UserWalletService (DB-driven)
                String transactionRef = "PAY_" + transactionId + "_" + System.currentTimeMillis();
                String description = "Payment for booking";
                
                try {
                    if (payableAmount > 0) {
                        userWalletService.debitWallet(
                            payment.getUser().getId(),
                            payableAmount,
                            transactionRef,
                            description
                        );
                    }

                    if (payment.getTemporaryWalletAmount() != null && payment.getTemporaryWalletAmount() > 0) {
                        consumeTemporaryWalletGreedy(
                            payment.getUser().getId(),
                            payment.getTemporaryWalletAmount(),
                            payment.getTransactionId()
                        );
                    }
                    
                    // Store wallet-only payment amount
                    payment.setWalletAmount(payableAmount);
                    payment.setCardAmount(0.0);
                    
                    // Wallet payment successful
                    payment.setStatus(PaymentStatus.SUCCESS);
                    payment.setCompletedAt(LocalDateTime.now());
                    payment.setGatewayTransactionId(transactionRef);
                    payment.setGatewayResponse("Wallet payment successful");
                    
                    Payment savedPayment = paymentRepository.save(payment);
                    
                    // Create ticket after successful payment
                    createTicketFromPayment(savedPayment);
                    
                    // Confirm seat locks
                    seatLockService.confirmLocks(payment.getSessionId());
                    
                    return savedPayment;
                    
                } catch (Exception walletException) {
                    // Wallet payment failed (insufficient balance or other error)
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setGatewayResponse("Wallet payment failed: " + walletException.getMessage());
                    paymentRepository.save(payment);
                    
                    // Release seat locks
                    seatLockService.releaseLocks(payment.getSessionId());
                    
                    throw new Exception("Wallet payment failed: " + walletException.getMessage());
                }
            }
            
            // Call payment gateway for other payment methods
            MockPaymentGateway.PaymentGatewayResponse response = paymentGateway.processPayment(
                payment.getPayableAmount() != null ? payment.getPayableAmount() : payment.getTotalAmount(),
                payment.getPaymentMethod().name(),
                payment.getUser().getEmailId()
            );

            payment.setGatewayTransactionId(response.getTransactionId());
            payment.setGatewayResponse(response.getMessage());

            if (response.isSuccess()) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setCompletedAt(LocalDateTime.now());
                
                // Track payment amounts for analytics
                // Non-wallet payments: full amount goes to card/gateway
                payment.setWalletAmount(0.0);
                payment.setCardAmount(payment.getPayableAmount() != null ? payment.getPayableAmount() : payment.getTotalAmount());

                if (payment.getTemporaryWalletAmount() != null && payment.getTemporaryWalletAmount() > 0) {
                    consumeTemporaryWalletGreedy(
                        payment.getUser().getId(),
                        payment.getTemporaryWalletAmount(),
                        payment.getTransactionId()
                    );
                }
                
                Payment savedPayment = paymentRepository.save(payment);
                
                // Create ticket after successful payment
                createTicketFromPayment(savedPayment);
                
                // Confirm seat locks
                seatLockService.confirmLocks(payment.getSessionId());
                
                return savedPayment;
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                
                // Release seat locks
                seatLockService.releaseLocks(payment.getSessionId());
            }

            return paymentRepository.save(payment);

        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse("Error: " + e.getMessage());
            paymentRepository.save(payment);
            
            // Release seat locks
            seatLockService.releaseLocks(payment.getSessionId());
            
            throw new Exception("Payment processing failed: " + e.getMessage());
        }
    }

    /**
     * Process refund
     * Updated to use WalletService for proper transaction tracking
     */
    @Transactional
    public Payment processRefund(Integer paymentId, String reason) throws Exception {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new Exception("Payment not found"));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new Exception("Only successful payments can be refunded");
        }

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            // Already refunded (idempotency)
            return payment;
        }

        // Process refund through gateway
        MockPaymentGateway.RefundResponse refundResponse = paymentGateway.processRefund(
            payment.getGatewayTransactionId(),
            payment.getTotalAmount()
        );

        if (refundResponse.isSuccess()) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundAmount(payment.getTotalAmount());
            payment.setRefundedAt(LocalDateTime.now());
            payment.setRefundReason(reason);
            paymentRepository.save(payment);

            // Credit to wallet using UserWalletService (DB-driven, consistent with payment debit)
            String transactionRef = "REFUND_PAY_" + paymentId + "_" + System.currentTimeMillis();
            String description = "Refund for payment #" + paymentId + " - " + reason;
            
            userWalletService.creditWallet(
                payment.getUser().getId(),
                payment.getTotalAmount(),
                transactionRef,
                description
            );

            return payment;
        } else {
            throw new Exception("Refund processing failed");
        }
    }

    /**
     * Calculate discount based on promo code
     * TODO: Implement promo code validation with database
     */
    private double calculateDiscount(String promoCode, double baseAmount) {
        // Mock implementation - replace with actual promo code logic
        if ("SAVE10".equalsIgnoreCase(promoCode)) {
            return baseAmount * 0.10; // 10% discount
        } else if ("SAVE20".equalsIgnoreCase(promoCode)) {
            return baseAmount * 0.20; // 20% discount
        } else if ("FIRSTBOOKING".equalsIgnoreCase(promoCode)) {
            return Math.min(baseAmount * 0.15, 100.0); // 15% up to ₹100
        }
        return 0.0;
    }

    /**
     * Get payment by transaction ID
     */
    public Payment getPaymentByTransactionId(String transactionId) throws Exception {
        return paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new Exception("Payment not found"));
    }

    /**
     * Get payment by session ID
     */
    public Payment getPaymentBySessionId(String sessionId) throws Exception {
        return paymentRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new Exception("No payment found for this session"));
    }

    /**
     * Link ticket to payment after booking confirmation
     */
    @Transactional
    public void linkTicketToPayment(String transactionId, Ticket ticket) throws Exception {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new Exception("Payment not found"));

        payment.setTicket(ticket);
        paymentRepository.save(payment);
    }

    /**
     * Create ticket from payment after successful transaction
     * This is called automatically after payment success
     */
    @Transactional
    private void createTicketFromPayment(Payment payment) {
        try {
            log.info("🎫 Creating ticket for payment: {}", payment.getTransactionId());
            
            // Get locked seats from session
            List<String> lockedSeats = seatLockService.getLockedSeats(payment.getSessionId());
            if (lockedSeats == null || lockedSeats.isEmpty()) {
                log.error("❌ No locked seats found for session: {}", payment.getSessionId());
                return;
            }
            
            // Get show from seat locks
            Show show = seatLockService.getShowFromSession(payment.getSessionId());
            if (show == null) {
                log.error("❌ Cannot find show for session: {}", payment.getSessionId());
                return;
            }
            
            // Convert seat list to comma-separated string
            String bookedSeatsStr = String.join(",", lockedSeats);
            
            // Create ticket
            // Note: bookedAt is automatically set by @CreationTimestamp to match database transaction time
            // This ensures ticket timestamp matches wallet transaction timestamp
            // Store TOTAL AMOUNT (ticket + parking + food) not just base amount
            Ticket ticket = Ticket.builder()
                .user(payment.getUser())
                .show(show)
                .bookedSeats(bookedSeatsStr)
                .totalTicketsPrice(BigDecimal.valueOf(payment.getTotalAmount()).intValue())
                .qrCodeData(generateQRCode(payment, show))
                .build();
            
            ticket = ticketRepository.save(ticket);
            log.info("[TICKET_SAVED] Ticket saved to DB: id={}, bookedAt={}, seats={}", 
                ticket.getId(), ticket.getBookedAt(), bookedSeatsStr);
            
            // Link ticket to payment
            payment.setTicket(ticket);
            paymentRepository.save(payment);
            
            // ATOMIC UPDATE - Mark seats as unavailable to prevent double booking
            // This uses database-level locking to prevent race conditions
            int seatsUpdated = showSeatRepository.markSeatsAsUnavailable(
                show.getId(), 
                lockedSeats
            );
            
            if (seatsUpdated != lockedSeats.size()) {
                log.error("⚠️ WARNING: Expected to update {} seats but only updated {}. " +
                         "Possible race condition or seats already booked!", 
                         lockedSeats.size(), seatsUpdated);
                // Payment succeeded but some seats couldn't be marked - log for manual review
            } else {
                log.info("🎫 Atomically marked {} seats as unavailable", seatsUpdated);
            }
            
            // Wallet transaction is already created by WalletService for WALLET payments
            // For non-wallet payments, no wallet transaction needed
            log.info("✅ Ticket created successfully: ID={}, Seats={}", 
                    ticket.getId(), bookedSeatsStr);
            
            log.info("✅ Ticket created successfully: ID={}, Seats={}, User={}", 
                ticket.getId(), bookedSeatsStr, payment.getUser().getName());
            
        } catch (Exception e) {
            log.error("❌ Error creating ticket from payment: {}", e.getMessage(), e);
            // Don't throw exception - payment was already successful
        }
    }

    /**
     * Generate QR code data for ticket
     */
    private String generateQRCode(Payment payment, Show show) {
        return String.format("BMS_%s_%s_%s_%s",
            payment.getTransactionId(),
            show.getMovie().getMovieName().replaceAll(" ", "_"),
            show.getTheater().getName().replaceAll(" ", "_"),
            System.currentTimeMillis()
        );
    }

    /**
     * Get payment history for a user
     * STRICT FLOW: Controller → Service → Repository → Database
     * 
     * @param userId User ID
     * @return List of payments
     * @throws Exception if user not found
     */
    public List<Payment> getPaymentHistoryByUser(Integer userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found with id: " + userId));
        
        // Use Repository layer: Service → Repository → Database
        return paymentRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Get user bookings with complete details (tickets, payments, wallet transactions)
     * 
     * @param userId User ID
     * @return List of booking details with tickets and payments
     * @throws Exception if user not found
     */
    public List<Map<String, Object>> getUserBookingsWithDetails(Integer userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found with id: " + userId));
        
        // Get all successful payments for user
        List<Payment> payments = paymentRepository.findByUserAndStatusOrderByCreatedAtDesc(
            user, PaymentStatus.SUCCESS
        );
        
        List<Map<String, Object>> bookings = new ArrayList<>();
        
        for (Payment payment : payments) {
            Map<String, Object> booking = new HashMap<>();
            booking.put("transactionId", payment.getTransactionId());
            booking.put("paymentMethod", payment.getPaymentMethod());
            booking.put("amount", payment.getTotalAmount());
            booking.put("status", payment.getStatus());
            booking.put("createdAt", payment.getCreatedAt());
            booking.put("completedAt", payment.getCompletedAt());
            
            // Add ticket details if available
            if (payment.getTicket() != null) {
                Ticket ticket = payment.getTicket();
                Map<String, Object> ticketDetails = new HashMap<>();
                ticketDetails.put("ticketId", ticket.getId());
                ticketDetails.put("seats", ticket.getBookedSeats());
                ticketDetails.put("bookedAt", ticket.getBookedAt());
                
                if (ticket.getShow() != null) {
                    Show show = ticket.getShow();
                    Map<String, Object> showDetails = new HashMap<>();
                    showDetails.put("showId", show.getId());
                    showDetails.put("showDate", show.getDate());
                    showDetails.put("showTime", show.getTime());
                    
                    if (show.getMovie() != null) {
                        showDetails.put("movieName", show.getMovie().getMovieName());
                        showDetails.put("duration", show.getMovie().getDuration());
                    }
                    
                    if (show.getTheater() != null) {
                        showDetails.put("theaterName", show.getTheater().getName());
                        showDetails.put("city", show.getTheater().getCity());
                    }
                    
                    ticketDetails.put("show", showDetails);
                }
                
                booking.put("ticket", ticketDetails);
            }
            
            bookings.add(booking);
        }
        
        return bookings;
    }

    /**
     * Process split payment - Deduct from wallet first, then create Stripe intent for remaining
     * 
     * @param transactionId Transaction ID
     * @param walletAmount Amount to deduct from wallet
     * @return Map with Stripe client secret and split payment details
     * @throws Exception if payment fails
     */
    @Transactional
    public Map<String, Object> processSplitPayment(String transactionId, Double walletAmount) throws Exception {
        Payment payment = paymentRepository.findByTransactionIdForUpdate(transactionId)
                .orElseThrow(() -> new Exception("Payment not found"));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new Exception("Payment cannot be processed in current status: " + payment.getStatus());
        }

        // Get current wallet balance from DB using UserWalletService
        Double currentBalance = userWalletService.getWalletBalance(payment.getUser().getId());
        
        // Validate wallet amount
        if (walletAmount > currentBalance) {
            throw new Exception("Insufficient wallet balance. Available: ₹" + currentBalance + ", Requested: ₹" + walletAmount);
        }
        
        Double payableAmount = payment.getPayableAmount() != null ? payment.getPayableAmount() : payment.getTotalAmount();

        if (walletAmount > payableAmount) {
            throw new Exception("Wallet amount cannot exceed total amount");
        }
        
        if (walletAmount < 0) {
            throw new Exception("Wallet amount must be positive");
        }

        // Calculate card amount
        Double cardAmount = payableAmount - walletAmount;
        
        if (cardAmount < 0) {
            throw new Exception("Invalid payment split");
        }

        // Step 1: Debit from wallet using UserWalletService (DB-driven)
        String walletTransactionRef = "SPLIT_WALLET_" + transactionId + "_" + System.currentTimeMillis();
        String walletDescription = "Wallet portion of split payment for booking";
        
        try {
            userWalletService.debitWallet(
                payment.getUser().getId(),
                walletAmount,
                walletTransactionRef,
                walletDescription
            );
            
            // Store split payment amounts in payment record
            payment.setWalletAmount(walletAmount);
            payment.setCardAmount(cardAmount);
            paymentRepository.save(payment);
            
        } catch (Exception e) {
            throw new Exception("Failed to deduct wallet amount: " + e.getMessage());
        }

        // Step 2: Create Stripe payment intent for card amount
        Long cardAmountInPaise = Math.round(cardAmount * 100);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("transactionId", transactionId);
        metadata.put("userId", payment.getUser().getId().toString());
        metadata.put("paymentType", "SPLIT_PAYMENT_CARD_PORTION");
        metadata.put("walletAmount", walletAmount.toString());
        metadata.put("cardAmount", cardAmount.toString());
        metadata.put("walletTransactionRef", walletTransactionRef);

        try {
            com.stripe.model.PaymentIntent paymentIntent = com.stripe.Stripe.apiKey != null 
                ? com.stripe.model.PaymentIntent.create(
                    Map.of(
                        "amount", cardAmountInPaise,
                        "currency", "inr",
                        "metadata", metadata,
                        "description", "Card portion of split payment"
                    )
                )
                : null;

            if (paymentIntent == null) {
                throw new Exception("Failed to create Stripe payment intent");
            }

            // Store split payment info in payment record
            payment.setGatewayResponse("Split payment initiated: Wallet ₹" + walletAmount + " + Card ₹" + cardAmount);
            payment.setStatus(PaymentStatus.PROCESSING);
            paymentRepository.save(payment);

            // Return response
            Map<String, Object> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());
            response.put("paymentIntentId", paymentIntent.getId());
            response.put("cardAmount", cardAmount);
            response.put("walletAmount", walletAmount);
            response.put("walletTransactionId", walletTransactionRef);
            response.put("totalAmount", payment.getTotalAmount());
            response.put("payableAmount", payableAmount);
            response.put("temporaryWalletAmount", payment.getTemporaryWalletAmount());

            return response;
        } catch (Exception e) {
            // Rollback wallet transaction if Stripe payment intent creation fails
            try {
                userWalletService.creditWallet(
                    payment.getUser().getId(),
                    walletAmount,
                    "REFUND_" + walletTransactionRef,
                    "Refund due to card payment setup failure"
                );
            } catch (Exception refundException) {
                log.error("Failed to refund wallet after split payment failure: {}", refundException.getMessage());
            }
            throw new Exception("Failed to create card payment: " + e.getMessage());
        }
    }

    /**
     * Complete split payment after Stripe confirms card payment
     * 
     * @param transactionId Transaction ID
     * @param paymentIntentId Stripe payment intent ID
     * @return Completed payment
     * @throws Exception if completion fails
     */
    @Transactional
    public Payment completeSplitPayment(String transactionId, String paymentIntentId) throws Exception {
        Payment payment = paymentRepository.findByTransactionIdForUpdate(transactionId)
                .orElseThrow(() -> new Exception("Payment not found"));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return payment; // Already completed
        }

        if (payment.getStatus() != PaymentStatus.PROCESSING) {
            throw new Exception("Payment is not in processing state");
        }

        // Update payment status to success
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setCompletedAt(LocalDateTime.now());
        payment.setGatewayTransactionId(paymentIntentId);
        payment.setGatewayResponse(payment.getGatewayResponse() + " | Card payment confirmed: " + paymentIntentId);

        if (payment.getTemporaryWalletAmount() != null && payment.getTemporaryWalletAmount() > 0) {
            consumeTemporaryWalletGreedy(
                payment.getUser().getId(),
                payment.getTemporaryWalletAmount(),
                payment.getTransactionId()
            );
        }
        
        Payment savedPayment = paymentRepository.save(payment);

        // Create ticket and confirm locks
        try {
            createTicketFromPayment(savedPayment);
            seatLockService.confirmLocks(savedPayment.getSessionId());
        } catch (Exception e) {
            log.error("Failed to create ticket for split payment: {}", e.getMessage());
            throw new Exception("Payment successful but ticket creation failed: " + e.getMessage());
        }

        return savedPayment;
    }

    /**
     * Complete Stripe payment after Stripe confirms card payment
     *
     * @param transactionId Internal transaction ID
     * @param paymentIntentId Stripe payment intent ID
     * @return Completed payment
     * @throws Exception if completion fails
     */
    @Transactional
    public Payment completeStripePayment(String transactionId, String paymentIntentId) throws Exception {
        Payment payment = paymentRepository.findByTransactionIdForUpdate(transactionId)
                .orElseThrow(() -> new Exception("Payment not found"));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return payment;
        }

        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.PROCESSING) {
            throw new Exception("Payment cannot be completed in current status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setCompletedAt(LocalDateTime.now());
        payment.setGatewayTransactionId(paymentIntentId);
        payment.setGatewayResponse("Stripe payment confirmed: " + paymentIntentId);
        payment.setWalletAmount(0.0);
        payment.setCardAmount(payment.getPayableAmount() != null ? payment.getPayableAmount() : payment.getTotalAmount());

        if (payment.getTemporaryWalletAmount() != null && payment.getTemporaryWalletAmount() > 0) {
            consumeTemporaryWalletGreedy(
                payment.getUser().getId(),
                payment.getTemporaryWalletAmount(),
                payment.getTransactionId()
            );
        }

        Payment savedPayment = paymentRepository.save(payment);

        try {
            createTicketFromPayment(savedPayment);
            seatLockService.confirmLocks(savedPayment.getSessionId());
        } catch (Exception e) {
            log.error("Failed to create ticket for Stripe payment: {}", e.getMessage());
            throw new Exception("Payment successful but ticket creation failed: " + e.getMessage());
        }

        return savedPayment;
    }

    private Double round2(Double value) {
        if (value == null) {
            return 0.0;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    private Double getApplicableTemporaryWalletAmount(Integer userId, Double billAmount) {
        if (billAmount == null || billAmount <= 0) {
            return 0.0;
        }

        List<TemporaryWallet> activeFunds = temporaryWalletRepository
            .findActiveByUserIdOrderByExpiresAtAsc(userId, LocalDateTime.now());

        double remaining = billAmount;
        double applicable = 0.0;

        for (TemporaryWallet fund : activeFunds) {
            if (remaining <= 0) {
                break;
            }

            double available = fund.getAmount() == null ? 0.0 : fund.getAmount().doubleValue();
            if (available <= 0) {
                continue;
            }

            double used = Math.min(available, remaining);
            applicable += used;
            remaining -= used;
        }

        return round2(applicable);
    }

    private void consumeTemporaryWalletGreedy(Integer userId, Double amountToConsume, String paymentTransactionId) throws Exception {
        if (amountToConsume == null || amountToConsume <= 0) {
            return;
        }

        BigDecimal remaining = BigDecimal.valueOf(round2(amountToConsume));
        List<TemporaryWallet> activeFunds = temporaryWalletRepository
            .findActiveByUserIdOrderByExpiresAtAsc(userId, LocalDateTime.now());

        for (TemporaryWallet fund : activeFunds) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal available = fund.getAmount() == null ? BigDecimal.ZERO : fund.getAmount();
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal deduction = remaining.min(available);
            BigDecimal newAvailable = available.subtract(deduction);

            if (newAvailable.compareTo(BigDecimal.ZERO) <= 0) {
                fund.setAmount(BigDecimal.ZERO);
                fund.setIsUsed(true);
                fund.setUsedAt(LocalDateTime.now());
            } else {
                fund.setAmount(newAvailable);
            }
            temporaryWalletRepository.save(fund);

            TemporaryWalletUsage usage = TemporaryWalletUsage.builder()
                .userId(userId.longValue())
                .temporaryWalletId(fund.getId())
                .paymentTransactionId(paymentTransactionId)
                .amount(deduction)
                .sourceType("TEMP_WALLET_PAYMENT_DEBIT")
                .build();
            temporaryWalletUsageRepository.save(usage);

            remaining = remaining.subtract(deduction);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new Exception("Insufficient temporary wallet balance to consume: pending amount ₹" + remaining);
        }
    }
}

