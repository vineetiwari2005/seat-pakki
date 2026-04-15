package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Enums.TicketStatus;
import com.driver.bookMyShow.Models.Show;
import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.Models.Payment;
import com.driver.bookMyShow.Repositories.TicketRepository;
import com.driver.bookMyShow.Repositories.ShowSeatRepository;
import com.driver.bookMyShow.Repositories.PaymentRepository;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BookingCancellationService - Handles ticket cancellations and refunds
 * 
 * Mathematical Time-Decay Cancellation Formula:
 *   Refund = P × (T_remaining - 30) / (T_total - 30)
 * 
 * Where:
 *   P = Original ticket price paid
 *   T_remaining = Minutes remaining until show starts
 *   T_total = Total minutes from booking time to show time
 * 
 * Constraints:
 *   - If T_remaining ≤ 30 → Refund = 0 (cancellation blocked)
 *   - Refund cannot be negative
 *   - Refund cannot exceed P
 *   - Ticket status set to CANCELLED (not deleted)
 *   - Seats released atomically via DB UPDATE
 *   - Refund credited to UserWallet (DB-driven)
 */
@Slf4j
@Service
public class BookingCancellationService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private UserWalletService userWalletService;

    @Autowired
    private EmailService emailService;

    // Minimum minutes before show to allow cancellation
    private static final long CANCELLATION_CUTOFF_MINUTES = 20;

    /**
     * Cancel ticket and process refund using time-decay formula.
     * 
     * Formula: Refund = P × (T_remaining - 30) / (T_total - 30)
     * 
     * @param ticketId Ticket to cancel
     * @param userId User requesting cancellation
     * @return CancellationResult with refund details
     * @throws Exception if validation fails
     */
    @Transactional
    public CancellationResult cancelTicket(Integer ticketId, Integer userId) throws Exception {
        // Step 1: Validate ticket exists
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new Exception("Ticket not found with ID: " + ticketId));

        // Step 2: Validate user ownership
        if (!ticket.getUser().getId().equals(userId)) {
            throw new Exception("Unauthorized: You can only cancel your own bookings");
        }

        // Step 3: Check ticket is not already cancelled
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new Exception("This booking has already been cancelled");
        }

        User user = ticket.getUser();
        Show show = ticket.getShow();
        
        // Step 4: Calculate time metrics
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime showDateTime = LocalDateTime.of(
            show.getDate().toLocalDate(),
            show.getTime().toLocalTime()
        );

        long minutesRemaining = ChronoUnit.MINUTES.between(now, showDateTime);

        // Step 5: Enforce cutoff
        if (minutesRemaining <= CANCELLATION_CUTOFF_MINUTES) {
            throw new Exception("Cancellation not allowed. Must cancel at least 20 minutes before show time.");
        }

        // Step 6: Calculate refund using requested formula
        double ticketAmount = ticket.getTotalTicketsPrice().doubleValue();
        Payment payment = paymentRepository.findByTicket(ticket).stream().findFirst().orElse(null);
        double baseAmount = payment != null && payment.getBaseAmount() != null
                ? payment.getBaseAmount()
                : ticketAmount;
        double feeAmount = Math.max(ticketAmount - baseAmount, 0.0);

        double refundAmount = calculateRefund(ticketAmount, baseAmount, feeAmount, minutesRemaining);

        // Step 7: Update ticket status to CANCELLED (never delete)
        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setCancelledAt(now);
        ticket.setRefundAmount(refundAmount);
        ticketRepository.save(ticket);

        // Step 8: ATOMIC UPDATE - Release seats back to inventory
        List<String> seatNumbers = Arrays.stream(ticket.getBookedSeats().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        
        int seatsReleased = showSeatRepository.markSeatsAsAvailable(
            show.getId(), 
            seatNumbers
        );
        
        log.info("Released {} seats back to inventory: {}", seatsReleased, seatNumbers);

        // Step 9: Credit refund to wallet (using DB-driven UserWalletService)
        if (refundAmount > 0) {
            String transactionRef = "REFUND_TKT_" + ticketId + "_" + System.currentTimeMillis();
            String description = String.format(
                "Refund for cancelled booking #%d - %s at %s",
                ticketId,
                show.getMovie().getMovieName(),
                show.getTheater().getName()
            );

            userWalletService.creditWallet(userId, refundAmount, transactionRef, description);
            log.info("Refund of {} credited to wallet for user {}", refundAmount, userId);
        }

        // Step 10: Send cancellation email (async, non-blocking)
        try {
            emailService.sendCancellationEmail(user, ticket, refundAmount);
        } catch (Exception e) {
            log.error("Failed to send cancellation email for ticket: {}. Error: {}", 
                     ticketId, e.getMessage());
        }

        // Step 11: Calculate refund percentage for display
        int refundPercentage = ticketAmount > 0 
            ? (int) Math.round((refundAmount / ticketAmount) * 100) 
            : 0;

        return CancellationResult.builder()
                .success(true)
                .ticketId(ticketId)
                .ticketAmount(ticketAmount)
                .refundAmount(refundAmount)
                .refundPercentage(refundPercentage)
                .minutesRemaining(minutesRemaining)
                .seatsReleased(seatsReleased)
                .message(String.format(
                    "Booking cancelled successfully. Cancellation amount credited: ₹%.2f. %d seats released.",
                    refundAmount, seatsReleased))
                .build();
    }

    /**
     * Calculate refund using formula:
     * R = [t≥20](0.50T + F) + [t≥120](0.25T)
     *
     * @param totalPaid total paid amount (upper cap)
     * @param baseAmount T
     * @param feeAmount F
     * @param minutesRemaining t
     * @return Refund amount
     */
    public double calculateRefund(double totalPaid, double baseAmount, double feeAmount, long minutesRemaining) {
        if (minutesRemaining < CANCELLATION_CUTOFF_MINUTES) {
            return 0.0;
        }

        double refund = (0.50 * baseAmount) + feeAmount;
        if (minutesRemaining >= 120) {
            refund += 0.25 * baseAmount;
        }

        refund = Math.max(0.0, Math.min(refund, totalPaid));
        return Math.round(refund * 100.0) / 100.0;
    }

    /**
     * Get refund estimate for a ticket (read-only, no modifications)
     * Uses the same time-decay formula.
     * 
     * @param ticketId Ticket ID
     * @return CancellationPolicy with eligibility and estimated refund
     * @throws Exception if ticket not found
     */
    public CancellationPolicy getRefundEstimate(Integer ticketId) throws Exception {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new Exception("Ticket not found with ID: " + ticketId));

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            return CancellationPolicy.builder()
                    .ticketId(ticketId)
                    .ticketAmount(ticket.getTotalTicketsPrice().doubleValue())
                    .canCancel(false)
                    .refundAmount(0.0)
                    .refundPercentage(0)
                    .policyMessage("This booking has already been cancelled.")
                    .build();
        }

        Show show = ticket.getShow();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime showDateTime = LocalDateTime.of(
            show.getDate().toLocalDate(),
            show.getTime().toLocalTime()
        );

        long minutesRemaining = ChronoUnit.MINUTES.between(now, showDateTime);

        double ticketAmount = ticket.getTotalTicketsPrice().doubleValue();
        Payment payment = paymentRepository.findByTicket(ticket).stream().findFirst().orElse(null);
        double baseAmount = payment != null && payment.getBaseAmount() != null
            ? payment.getBaseAmount()
            : ticketAmount;
        double feeAmount = Math.max(ticketAmount - baseAmount, 0.0);
        double refundAmount = calculateRefund(ticketAmount, baseAmount, feeAmount, minutesRemaining);

        boolean canCancel = minutesRemaining > CANCELLATION_CUTOFF_MINUTES;

        int refundPercentage = ticketAmount > 0 
            ? (int) Math.round((refundAmount / ticketAmount) * 100) 
            : 0;

        String policyMessage;
        if (minutesRemaining <= 0) {
            policyMessage = "Cannot cancel. Show has already started.";
        } else if (minutesRemaining <= CANCELLATION_CUTOFF_MINUTES) {
            policyMessage = String.format(
                "Cancellation blocked. Less than %d minutes before show time.", 
                CANCELLATION_CUTOFF_MINUTES);
        } else {
            long hoursRemaining = minutesRemaining / 60;
            long minsRemaining = minutesRemaining % 60;
            policyMessage = String.format(
                "Estimated cancellation amount: ₹%.2f. Show in %dh %dm.",
                refundAmount, hoursRemaining, minsRemaining);
        }

        return CancellationPolicy.builder()
                .ticketId(ticketId)
                .ticketAmount(ticketAmount)
                .minutesRemaining(minutesRemaining)
                .canCancel(canCancel)
                .refundAmount(refundAmount)
                .refundPercentage(refundPercentage)
                .policyMessage(policyMessage)
                .build();
    }

    // ==================== DTOs ====================

    @Data
    @Builder
    public static class CancellationResult {
        private boolean success;
        private Integer ticketId;
        private double ticketAmount;
        private double refundAmount;
        private int refundPercentage;
        private long minutesRemaining;
        private int seatsReleased;
        private String message;
    }

    @Data
    @Builder
    public static class CancellationPolicy {
        private Integer ticketId;
        private double ticketAmount;
        private long minutesRemaining;
        private boolean canCancel;
        private double refundAmount;
        private int refundPercentage;
        private String policyMessage;
    }
}
