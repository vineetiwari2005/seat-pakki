package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Enums.TicketStatus;
import com.driver.bookMyShow.Models.Payment;
import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.Repositories.PaymentRepository;
import com.driver.bookMyShow.Repositories.TicketRepository;
import com.driver.bookMyShow.Repositories.UserRepository;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * BookingService - Business logic for user bookings
 * 
 * Handles:
 * - Fetching user bookings (all, upcoming, past, paginated)
 * - Converting Ticket entities to BookingDTOs
 * - Booking count
 * 
 * Layered: Controller → Service → Repository → Database
 */
@Slf4j
@Service
public class BookingService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    /**
     * Get all bookings for a user (latest first)
     */
    public List<BookingDTO> getUserBookings(Integer userId) throws Exception {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new Exception("User not found with id: " + userId));

        List<Ticket> tickets = ticketRepository.findByUserOrderByBookedAtDesc(user);
        log.info("Found {} tickets for user ID: {}", tickets.size(), userId);

        return tickets.stream()
            .map(this::toDTO)
            .toList();
    }

    /**
     * Get upcoming bookings (active only, show in future)
     */
    public List<BookingDTO> getUpcomingBookings(Integer userId) throws Exception {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new Exception("User not found"));

        List<Ticket> tickets = ticketRepository.findUpcomingTicketsByUser(user);
        return tickets.stream().map(this::toDTO).toList();
    }

    /**
     * Get past bookings (show already happened)
     */
    public List<BookingDTO> getPastBookings(Integer userId) throws Exception {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new Exception("User not found"));

        List<Ticket> tickets = ticketRepository.findPastTicketsByUser(user);
        return tickets.stream().map(this::toDTO).toList();
    }

    /**
     * Get paginated bookings
     */
    public Page<BookingDTO> getPaginatedBookings(Integer userId, int page, int size) throws Exception {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new Exception("User not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("bookedAt").descending());
        Page<Ticket> tickets = ticketRepository.findByUser(user, pageable);
        return tickets.map(this::toDTO);
    }

    /**
     * Get booking count
     */
    public long getBookingCount(Integer userId) throws Exception {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new Exception("User not found"));
        return ticketRepository.countByUser(user);
    }

    /**
     * Convert Ticket entity to BookingDTO
     * Enriches with payment details from database
     */
    private BookingDTO toDTO(Ticket ticket) {
        // Determine if booking is still active (not cancelled AND show hasn't started)
        LocalDate showDate = ticket.getShow().getDate().toLocalDate();
        LocalTime showTime = ticket.getShow().getTime().toLocalTime();
        LocalDateTime showDateTime = LocalDateTime.of(showDate, showTime);
        boolean isUpcoming = LocalDateTime.now().isBefore(showDateTime);
        boolean isCancelled = ticket.getStatus() == TicketStatus.CANCELLED;
        boolean isActive = isUpcoming && !isCancelled;
        
        // Get payment details from database
        Double totalPrice = ticket.getTotalTicketsPrice().doubleValue();
        String paymentMethodDisplay = "N/A";
        Double walletAmount = 0.0;
        Double cardAmount = 0.0;
        
        try {
            List<Payment> payments = paymentRepository.findByTicket(ticket);
            if (!payments.isEmpty()) {
                Optional<Payment> successfulPayment = payments.stream()
                    .filter(p -> "SUCCESS".equals(p.getStatus().toString()))
                    .findFirst();
                
                if (successfulPayment.isPresent()) {
                    Payment payment = successfulPayment.get();
                    totalPrice = payment.getTotalAmount();
                    walletAmount = payment.getWalletAmount() != null ? payment.getWalletAmount() : 0.0;
                    cardAmount = payment.getCardAmount() != null ? payment.getCardAmount() : 0.0;
                    
                    if (walletAmount > 0 && cardAmount > 0) {
                        paymentMethodDisplay = payment.getPaymentMethod() + "+WALLET";
                    } else if (walletAmount > 0 && cardAmount == 0) {
                        paymentMethodDisplay = "WALLET";
                    } else {
                        paymentMethodDisplay = payment.getPaymentMethod().toString();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error fetching payment for ticket {}: {}", ticket.getId(), e.getMessage());
        }
        
        return BookingDTO.builder()
            .ticketId(ticket.getId())
            .movieName(ticket.getShow().getMovie().getMovieName())
            .theaterName(ticket.getShow().getTheater().getName())
            .showDate(ticket.getShow().getDate().toString())
            .showTime(ticket.getShow().getTime().toString())
            .seats(ticket.getBookedSeats())
            .totalPrice(totalPrice)
            .paymentMethod(paymentMethodDisplay)
            .walletAmount(walletAmount)
            .cardAmount(cardAmount)
            .bookedAt(ticket.getBookedAt())
            .qrCodeAvailable(ticket.getQrCodeData() != null && !ticket.getQrCodeData().isEmpty())
            .isActive(isActive)
            .status(ticket.getStatus().name())
            .refundAmount(ticket.getRefundAmount())
            .cancelledAt(ticket.getCancelledAt())
            .build();
    }

    // ==================== DTOs ====================

    @Data
    @Builder
    public static class BookingDTO {
        private Integer ticketId;
        private String movieName;
        private String theaterName;
        private String showDate;
        private String showTime;
        private String seats;
        private Double totalPrice;
        private String paymentMethod;
        private Double walletAmount;
        private Double cardAmount;
        private LocalDateTime bookedAt;
        private boolean qrCodeAvailable;
        private boolean isActive;
        private String status;          // BOOKED or CANCELLED
        private Double refundAmount;    // null if not cancelled
        private LocalDateTime cancelledAt; // null if not cancelled
    }
}
