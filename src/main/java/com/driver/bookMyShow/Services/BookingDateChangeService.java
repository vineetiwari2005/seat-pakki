package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Enums.TicketStatus;
import com.driver.bookMyShow.Models.Show;
import com.driver.bookMyShow.Models.ShowSeat;
import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.Repositories.ShowRepository;
import com.driver.bookMyShow.Repositories.ShowSeatRepository;
import com.driver.bookMyShow.Repositories.TicketRepository;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BookingDateChangeService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private TicketService ticketService;

    private static final long CHANGE_CUTOFF_MINUTES = 30;

    public List<DateChangeOption> getProfitableDateOptions(Integer ticketId, Integer userId) throws Exception {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new Exception("Ticket not found with ID: " + ticketId));

        validateTicketOwnershipAndStatus(ticket, userId);

        Show currentShow = ticket.getShow();
        ensureShowCanBeChanged(currentShow);

        List<String> seatNumbers = parseSeatNumbers(ticket.getBookedSeats());
        int seatCount = seatNumbers.size();

        List<Show> candidateShows = showRepository.findByMovieIdAndTheaterId(
                currentShow.getMovie().getId(),
                currentShow.getTheater().getId()
        );

        return candidateShows.stream()
                .filter(show -> !show.getId().equals(currentShow.getId()))
                .filter(this::isFutureShowEligible)
                .map(show -> buildOptionIfEligible(ticket, currentShow, show, seatNumbers, seatCount))
                .filter(option -> option != null)
                .collect(Collectors.toList());
    }

    @Transactional
    public DateChangeResult changeTicketDate(Integer ticketId, Integer userId) throws Exception {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new Exception("Ticket not found with ID: " + ticketId));

        validateTicketOwnershipAndStatus(ticket, userId);

        Show currentShow = ticket.getShow();
        ensureShowCanBeChanged(currentShow);

        List<String> seatNumbers = parseSeatNumbers(ticket.getBookedSeats());
        showSeatRepository.findByShowAndSeatNoInForUpdate(currentShow, seatNumbers);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime showDateTime = LocalDateTime.of(currentShow.getDate().toLocalDate(), currentShow.getTime().toLocalTime());
        long minutesRemaining = ChronoUnit.MINUTES.between(now, showDateTime);

        double ticketAmount = ticket.getTotalTicketsPrice() != null
            ? ticket.getTotalTicketsPrice().doubleValue()
            : 0.0;

        double refundAmount = calculateDateChangeRefund(ticketAmount, ticket.getBookedAt(), showDateTime, minutesRemaining);
        double cancellationCharge = round2(Math.max(0.0, ticketAmount - refundAmount));
        int refundPercentage = ticketAmount > 0
            ? (int) Math.round((refundAmount / ticketAmount) * 100)
            : 0;

        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setCancelledAt(now);
        ticket.setRefundAmount(refundAmount);
        ticketRepository.save(ticket);

        int releasedSeats = showSeatRepository.markSeatsAsAvailable(currentShow.getId(), seatNumbers);

        // ✅ FIX: Use TicketService to store refund in temporary_wallet table (not TemporaryWalletCredit)
        // This ensures refunds are stored in the same DB table used by balance calculations
        com.driver.bookMyShow.Models.TemporaryWallet savedWallet = ticketService.addRefundToTemporaryWallet(
            userId,
            BigDecimal.valueOf(refundAmount),
                "TICKET_CHANGE_REFUND"
        );

        log.info("Ticket {} converted to temporary wallet credit for user {}. Show {} seats released {}",
                ticketId, userId, currentShow.getId(), releasedSeats);

        return DateChangeResult.builder()
                .success(true)
                .ticketId(ticketId)
                .oldShowId(currentShow.getId())
                .newShowId(null)
                .oldShowDate(currentShow.getDate().toString())
                .oldShowTime(currentShow.getTime().toString())
                .newShowDate(null)
                .newShowTime(null)
                .seatsMoved(null)
                .seatsReleased(releasedSeats)
                .seatsBooked(0)
                .originalAmount(round2(ticketAmount))
                .refundAmount(refundAmount)
                .cancellationCharge(cancellationCharge)
                .refundPercentage(refundPercentage)
                .temporaryWalletAmount(savedWallet != null ? savedWallet.getAmount().doubleValue() : 0.0)
                .temporaryWalletExpiresAt(savedWallet != null ? savedWallet.getExpiresAt() : null)
                .message(String.format(
                        "Date change completed. Refund ₹%.2f credited to temporary wallet for 15 days after deducting cancellation charge ₹%.2f.",
                        refundAmount,
                        cancellationCharge
                ))
                .build();
    }

    private double calculateDateChangeRefund(
            double ticketAmount,
            LocalDateTime bookedAt,
            LocalDateTime showDateTime,
            long minutesRemaining
    ) {
        if (ticketAmount <= 0) {
            return 0.0;
        }

        LocalDateTime effectiveBookedAt = bookedAt != null ? bookedAt : LocalDateTime.now().minusDays(1);
        long totalWindowMinutes = ChronoUnit.MINUTES.between(effectiveBookedAt, showDateTime);

        if (totalWindowMinutes <= CHANGE_CUTOFF_MINUTES) {
            return round2(ticketAmount);
        }

        double linearFactor = (double) (minutesRemaining - CHANGE_CUTOFF_MINUTES)
                / (double) (totalWindowMinutes - CHANGE_CUTOFF_MINUTES);

        double clampedFactor = Math.max(0.0, Math.min(1.0, linearFactor));
        double refundAmount = ticketAmount * clampedFactor;

        return round2(Math.max(0.0, Math.min(ticketAmount, refundAmount)));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void validateTicketOwnershipAndStatus(Ticket ticket, Integer userId) throws Exception {
        if (!ticket.getUser().getId().equals(userId)) {
            throw new Exception("Unauthorized: You can only modify your own bookings");
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new Exception("Cancelled tickets cannot be changed");
        }
    }

    private void ensureShowCanBeChanged(Show show) throws Exception {
        LocalDateTime showDateTime = LocalDateTime.of(show.getDate().toLocalDate(), show.getTime().toLocalTime());
        long minutesRemaining = java.time.temporal.ChronoUnit.MINUTES.between(LocalDateTime.now(), showDateTime);
        if (minutesRemaining <= CHANGE_CUTOFF_MINUTES) {
            throw new Exception("Ticket date change is allowed only before 30 minutes of show time");
        }
    }

    private boolean isFutureShowEligible(Show show) {
        LocalDateTime showDateTime = LocalDateTime.of(show.getDate().toLocalDate(), show.getTime().toLocalTime());
        long minutesRemaining = java.time.temporal.ChronoUnit.MINUTES.between(LocalDateTime.now(), showDateTime);
        return minutesRemaining > CHANGE_CUTOFF_MINUTES;
    }

    private DateChangeOption buildOptionIfEligible(
            Ticket ticket,
            Show currentShow,
            Show candidateShow,
            List<String> seatNumbers,
            int seatCount
    ) {
        List<ShowSeat> candidateSeats = showSeatRepository.findByShowAndSeatNoIn(candidateShow, seatNumbers);
        if (candidateSeats.size() != seatCount || candidateSeats.stream().anyMatch(seat -> !Boolean.TRUE.equals(seat.getIsAvailable()))) {
            return null;
        }

        long oldTotal = showSeatRepository.countByShow(currentShow);
        long oldAvailable = showSeatRepository.countAvailableSeats(currentShow);
        long oldBooked = Math.max(0, oldTotal - oldAvailable);

        long newTotal = showSeatRepository.countByShow(candidateShow);
        long newAvailable = showSeatRepository.countAvailableSeats(candidateShow);
        long newBooked = Math.max(0, newTotal - newAvailable);

        double rhoOld = oldTotal > 0 ? (double) oldBooked / oldTotal : 0.0;
        double rhoNew = newTotal > 0 ? (double) newBooked / newTotal : 0.0;

        double ticketPrice = ticket.getTotalTicketsPrice() != null ? ticket.getTotalTicketsPrice() : 0;
        double expectedNetGain = ticketPrice * (rhoOld - rhoNew);
        boolean theaterProfitable = expectedNetGain >= 0;

        if (!theaterProfitable) {
            return null;
        }

        return DateChangeOption.builder()
                .showId(candidateShow.getId())
                .showDate(candidateShow.getDate().toString())
                .showTime(candidateShow.getTime().toString())
                .availableSeats((int) newAvailable)
                .totalSeats((int) newTotal)
                .seatNumbers(ticket.getBookedSeats())
                .theaterProfitable(true)
                .expectedNetGain(Math.round(expectedNetGain * 100.0) / 100.0)
                .build();
    }

    private List<String> parseSeatNumbers(String bookedSeatsCsv) {
        return Arrays.stream(bookedSeatsCsv.split(","))
                .map(String::trim)
                .filter(seat -> !seat.isEmpty())
                .collect(Collectors.toList());
    }

    @Data
    @Builder
    public static class DateChangeOption {
        private Integer showId;
        private String showDate;
        private String showTime;
        private Integer availableSeats;
        private Integer totalSeats;
        private String seatNumbers;
        private Boolean theaterProfitable;
        private Double expectedNetGain;
    }

    @Data
    @Builder
    public static class DateChangeResult {
        private boolean success;
        private Integer ticketId;
        private Integer oldShowId;
        private Integer newShowId;
        private String oldShowDate;
        private String oldShowTime;
        private String newShowDate;
        private String newShowTime;
        private String seatsMoved;
        private int seatsReleased;
        private int seatsBooked;
        private Double originalAmount;
        private Double refundAmount;
        private Double cancellationCharge;
        private Integer refundPercentage;
        private Double temporaryWalletAmount;
        private LocalDateTime temporaryWalletExpiresAt;
        private String message;
    }
}