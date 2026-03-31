package com.driver.bookMyShow.Controllers;

import com.driver.bookMyShow.Dtos.RequestDtos.PaymentInitiationDto;
import com.driver.bookMyShow.Dtos.ResponseDtos.PaymentResponseDto;
import com.driver.bookMyShow.Enums.PaymentMethod;
import com.driver.bookMyShow.Models.Payment;
import com.driver.bookMyShow.Services.OtpService;
import com.driver.bookMyShow.Services.PaymentService;
import com.driver.bookMyShow.Services.StripePaymentService;
import com.driver.bookMyShow.common.dto.ApiResponse;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PaymentController - Handles payment operations
 * 
 * NEW ENDPOINTS for payment processing:
 * - Initiate payment
 * - Process payment
 * - Check payment status
 * - Process refund
 * - Stripe payment intent (NEW)
 * - Get payment history by user (NEW)
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private StripePaymentService stripePaymentService;

    @Autowired
    private OtpService otpService;

    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;

    /**
     * Initiate payment
     * POST /api/payment/initiate
     * 
     * Creates payment record with price breakdown
     * Returns transaction ID for payment processing
     */
    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@Valid @RequestBody PaymentInitiationDto request) {
        try {
            Payment payment = paymentService.initiatePayment(
                    request.getSessionId(),
                    request.getUserId(),
                    request.getBaseAmount(),
                    request.getPaymentMethod(),
                    request.getPromoCode(),
                    request.getUseTemporaryWallet()
            );

            PaymentResponseDto response = buildPaymentResponse(payment);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Process payment
     * POST /api/payment/process/{transactionId}
     * 
     * Processes payment through gateway
     * Confirms or releases seat locks based on result
     */
        @PostMapping("/process/{transactionId}")
        public ResponseEntity<?> processPayment(
            @PathVariable String transactionId,
            @RequestBody(required = false) Map<String, String> requestBody) {
        try {
            Map<String, String> otpData = requestBody == null ? new HashMap<>() : requestBody;
            Payment existingPayment = paymentService.getPaymentByTransactionId(transactionId);
                Double payableAmount = existingPayment.getPayableAmount() == null
                        ? existingPayment.getTotalAmount()
                        : existingPayment.getPayableAmount();

                if (payableAmount == null || payableAmount > 0.0) {
                    otpService.verifyOtp(
                        existingPayment.getUser().getId(),
                        otpData.get("otpRequestId"),
                        otpData.get("otpCode"),
                        "PAYMENT",
                        transactionId
                    );
                }

            Payment payment = paymentService.processPayment(transactionId);
            PaymentResponseDto response = buildPaymentResponse(payment);
            
            if (payment.getStatus().name().equals("SUCCESS")) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get payment status
     * GET /api/payment/status/{transactionId}
     * 
     * Retrieves payment details and status
     */
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String transactionId) {
        try {
            Payment payment = paymentService.getPaymentByTransactionId(transactionId);
            PaymentResponseDto response = buildPaymentResponse(payment);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Process refund
     * POST /api/payment/refund/{paymentId}
     * 
     * Initiates refund for a successful payment
     * Credits amount to user wallet
     */
    @PostMapping("/refund/{paymentId}")
    public ResponseEntity<?> processRefund(
            @PathVariable Integer paymentId,
            @RequestParam String reason) {
        try {
            Payment payment = paymentService.processRefund(paymentId, reason);
            PaymentResponseDto response = buildPaymentResponse(payment);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Build payment response DTO
     */
    private PaymentResponseDto buildPaymentResponse(Payment payment) {
        String message;
        switch (payment.getStatus()) {
            case SUCCESS:
                message = "Payment completed successfully";
                break;
            case FAILED:
                message = "Payment failed: " + payment.getGatewayResponse();
                break;
            case PENDING:
                message = "Payment initiated. Please proceed to complete payment.";
                break;
            case PROCESSING:
                message = "Payment is being processed...";
                break;
            case REFUNDED:
                message = "Payment refunded successfully";
                break;
            default:
                message = "Payment status: " + payment.getStatus();
        }

        return PaymentResponseDto.builder()
                .paymentId(payment.getId())
                .transactionId(payment.getTransactionId())
                .sessionId(payment.getSessionId())
                .baseAmount(payment.getBaseAmount())
                .convenienceFee(payment.getConvenienceFee())
                .tax(payment.getTax())
                .discountAmount(payment.getDiscountAmount())
                .totalAmount(payment.getTotalAmount())
            .temporaryWalletAmount(payment.getTemporaryWalletAmount())
            .payableAmount(payment.getPayableAmount())
                .walletAmount(payment.getWalletAmount())
                .cardAmount(payment.getCardAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .message(message)
                .createdAt(payment.getCreatedAt())
                .completedAt(payment.getCompletedAt())
                .build();
    }

    /**
     * Get Stripe publishable key
     * GET /api/payment/stripe-config
     * 
     * Returns publishable key for frontend Stripe integration
     */
    @GetMapping("/stripe-config")
    public ResponseEntity<Map<String, String>> getStripeConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("publishableKey", stripePublishableKey);
        return ResponseEntity.ok(config);
    }

    /**
     * Create Stripe payment intent for split payment (Wallet + Card)
     * POST /api/payment/create-split-payment-intent
     * 
     * Deducts wallet amount first, then creates Stripe intent for remaining
     */
    @PostMapping("/create-split-payment-intent")
    public ResponseEntity<?> createSplitPaymentIntent(@RequestBody Map<String, Object> data) {
        try {
            String sessionId = (String) data.get("sessionId");
            Integer userId = Integer.valueOf(data.get("userId").toString());
            Double baseAmount = Double.valueOf(data.get("baseAmount").toString());
            Double walletAmount = Double.valueOf(data.get("walletAmount").toString());
            String promoCode = (String) data.get("promoCode");
                Boolean useTemporaryWallet = data.get("useTemporaryWallet") == null
                    ? true
                    : Boolean.valueOf(data.get("useTemporaryWallet").toString());
            
            if (sessionId == null || walletAmount == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "sessionId and walletAmount are required"));
            }
            
            // Step 1: Initiate payment to get total amount with fees
            Payment payment = paymentService.initiatePayment(
                sessionId, 
                userId, 
                baseAmount, 
                PaymentMethod.WALLET_CARD_SPLIT,
                promoCode,
                useTemporaryWallet
            );
            
            // Step 2: Process split payment (deduct wallet, create card payment)
            Map<String, Object> splitResult = paymentService.processSplitPayment(
                payment.getTransactionId(),
                walletAmount
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("clientSecret", splitResult.get("clientSecret"));
            response.put("paymentIntentId", splitResult.get("paymentIntentId"));
            response.put("transactionId", payment.getTransactionId());
            response.put("totalAmount", payment.getTotalAmount());
            response.put("temporaryWalletAmount", payment.getTemporaryWalletAmount());
            response.put("payableAmount", payment.getPayableAmount());
            response.put("walletAmount", walletAmount);
            response.put("cardAmount", splitResult.get("cardAmount"));
            response.put("walletTransactionId", splitResult.get("walletTransactionId"));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Confirm split payment (Wallet + Card)
     * POST /api/payment/confirm-split-payment
     * 
     * Verifies Stripe payment and completes the booking
     */
    @PostMapping("/confirm-split-payment")
    public ResponseEntity<?> confirmSplitPayment(@RequestBody Map<String, String> data) {
        try {
            String paymentIntentId = data.get("paymentIntentId");
            String transactionId = data.get("transactionId");
            String otpCode = data.get("otpCode");
            String otpRequestId = data.get("otpRequestId");
            
            if (transactionId == null || paymentIntentId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "transactionId and paymentIntentId are required"));
            }

            Payment existingPayment = paymentService.getPaymentByTransactionId(transactionId);
            otpService.verifyOtp(
                existingPayment.getUser().getId(),
                otpRequestId,
                otpCode,
                "PAYMENT",
                transactionId
            );
            
            // Verify Stripe payment was successful
            PaymentIntent paymentIntent = stripePaymentService.retrievePaymentIntent(paymentIntentId);
            if (!"succeeded".equals(paymentIntent.getStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Card payment not successful: " + paymentIntent.getStatus()));
            }
            
            // Complete the split payment (both wallet and card confirmed)
            Payment payment = paymentService.completeSplitPayment(transactionId, paymentIntentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", payment.getStatus().toString());
            response.put("transactionId", payment.getTransactionId());
            response.put("paymentIntentId", paymentIntentId);
            response.put("amount", payment.getTotalAmount());
            response.put("paymentMethod", "WALLET_CARD_SPLIT");
            
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Create Stripe payment intent
     * POST /api/payment/create-stripe-intent
     * 
     * Creates a Stripe payment intent for the booking
     * NOW VALIDATES SEAT LOCKS BEFORE CREATING PAYMENT INTENT
     */
    @PostMapping("/create-stripe-intent")
    public ResponseEntity<?> createStripePaymentIntent(@RequestBody Map<String, Object> data) {
        try {
            String sessionId = (String) data.get("sessionId");
            Integer userId = Integer.valueOf(data.get("userId").toString());
            Double baseAmount = Double.valueOf(data.get("baseAmount").toString());
            String promoCode = (String) data.get("promoCode");
                Boolean useTemporaryWallet = data.get("useTemporaryWallet") == null
                    ? true
                    : Boolean.valueOf(data.get("useTemporaryWallet").toString());
            
            if (sessionId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "sessionId is required"));
            }
            
            // CRITICAL: Initiate payment first to validate seat locks (same as wallet)
            Payment payment = paymentService.initiatePayment(
                sessionId, 
                userId, 
                baseAmount, 
                PaymentMethod.STRIPE,
                promoCode,
                useTemporaryWallet
            );
            
            // Create Stripe payment intent with actual total amount (including fees)
            Long amountInPaise = Math.round(payment.getPayableAmount() * 100);
            Map<String, String> metadata = new HashMap<>();
            metadata.put("sessionId", sessionId);
            metadata.put("transactionId", payment.getTransactionId());
            metadata.put("userId", userId.toString());
            
            PaymentIntent paymentIntent = stripePaymentService.createPaymentIntent(
                amountInPaise, 
                "inr", 
                metadata
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());
            response.put("paymentIntentId", paymentIntent.getId());
            response.put("transactionId", payment.getTransactionId());
            response.put("totalAmount", payment.getTotalAmount());
            response.put("temporaryWalletAmount", payment.getTemporaryWalletAmount());
            response.put("payableAmount", payment.getPayableAmount());
            response.put("cardAmount", payment.getPayableAmount());
            
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Confirm Stripe payment and complete booking
     * POST /api/payment/confirm-stripe
     * 
     * Called after Stripe confirms payment on frontend
     * Processes the payment in our system (creates ticket, marks seats unavailable)
     */
    @PostMapping("/confirm-stripe")
    public ResponseEntity<?> confirmStripePayment(@RequestBody Map<String, String> data) {
        try {
            String paymentIntentId = data.get("paymentIntentId");
            String transactionId = data.get("transactionId");
            String otpCode = data.get("otpCode");
            String otpRequestId = data.get("otpRequestId");
            
            if (transactionId == null || paymentIntentId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "transactionId and paymentIntentId are required"));
            }

            Payment existingPayment = paymentService.getPaymentByTransactionId(transactionId);
            otpService.verifyOtp(
                existingPayment.getUser().getId(),
                otpRequestId,
                otpCode,
                "PAYMENT",
                transactionId
            );
            
            // Verify Stripe payment was successful
            PaymentIntent paymentIntent = stripePaymentService.retrievePaymentIntent(paymentIntentId);
            if (!"succeeded".equals(paymentIntent.getStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Payment not successful: " + paymentIntent.getStatus()));
            }
            
            // Complete Stripe payment in our system
            // This creates ticket and marks seats as unavailable
            Payment payment = paymentService.completeStripePayment(transactionId, paymentIntentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", payment.getStatus().toString());
            response.put("transactionId", payment.getTransactionId());
            response.put("paymentIntentId", paymentIntentId);
            response.put("amount", payment.getTotalAmount());
            
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get booking history with tickets and transactions
     * GET /api/payment/user/{userId}/bookings
     * Returns complete booking details with tickets and payments
     */
    @GetMapping("/user/{userId}/bookings")
    public ResponseEntity<?> getUserBookings(@PathVariable Integer userId) {
        try {
            List<Map<String, Object>> bookings = paymentService.getUserBookingsWithDetails(userId);
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get payment history for a user
     * GET /api/payment/user/{userId}/history
     * Latest payments first
     * 
     * STRICT FLOW: Controller → Service → Repository → Database
     */
    @GetMapping("/user/{userId}/history")
    public ResponseEntity<ApiResponse<List<PaymentHistoryDTO>>> getPaymentHistory(
            @PathVariable Integer userId) {
        
        try {
            log.info("💳 Fetching payment history for userId: {}", userId);

            // Call service (Service → Repository → Database)
            List<Payment> payments = paymentService.getPaymentHistoryByUser(userId);

            log.info("💰 Database returned {} payments for user ID: {}", payments.size(), userId);

            // Convert to DTOs
            List<PaymentHistoryDTO> response = payments.stream()
                .map(this::toPaymentHistoryDTO)
                .toList();

            log.info("✅ Returning {} payments for user: {}", response.size(), userId);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error fetching payment history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Convert Payment to PaymentHistoryDTO
     */
    private PaymentHistoryDTO toPaymentHistoryDTO(Payment payment) {
        PaymentHistoryDTO dto = new PaymentHistoryDTO();
        dto.setId(payment.getId());
        dto.setTransactionId(payment.getTransactionId());
        dto.setAmount(payment.getTotalAmount());
        dto.setBaseAmount(payment.getBaseAmount());
        dto.setConvenienceFee(payment.getConvenienceFee());
        dto.setTax(payment.getTax());
        dto.setDiscountAmount(payment.getDiscountAmount());
        dto.setPaymentMethod(payment.getPaymentMethod().name());
        dto.setStatus(payment.getStatus().name());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setCompletedAt(payment.getCompletedAt());
        
        // Add ticket details if available
        if (payment.getTicket() != null) {
            dto.setTicketId(payment.getTicket().getId());
            dto.setBookedSeats(payment.getTicket().getBookedSeats());
            
            if (payment.getTicket().getShow() != null) {
                dto.setMovieName(payment.getTicket().getShow().getMovie().getMovieName());
                dto.setTheaterName(payment.getTicket().getShow().getTheater().getName());
                dto.setShowDate(payment.getTicket().getShow().getDate().toString());
                dto.setShowTime(payment.getTicket().getShow().getTime().toString());
            }
        }
        
        return dto;
    }

    /**
     * PaymentHistoryDTO - Response DTO for payment history
     */
    @Data
    public static class PaymentHistoryDTO {
        private Integer id;
        private String transactionId;
        private Double amount;
        private Double baseAmount;
        private Double convenienceFee;
        private Double tax;
        private Double discountAmount;
        private String paymentMethod;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
        
        // Ticket details
        private Integer ticketId;
        private String bookedSeats;
        private String movieName;
        private String theaterName;
        private String showDate;
        private String showTime;
    }
}
