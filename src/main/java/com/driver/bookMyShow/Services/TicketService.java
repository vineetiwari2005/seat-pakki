package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Dtos.RequestDtos.TicketEntryDto;
import com.driver.bookMyShow.Dtos.ResponseDtos.TicketResponseDto;
import com.driver.bookMyShow.Enums.TicketStatus;
import com.driver.bookMyShow.Exceptions.RequestedSeatAreNotAvailable;
import com.driver.bookMyShow.Exceptions.ShowDoesNotExists;
import com.driver.bookMyShow.Exceptions.UserDoesNotExists;
import com.driver.bookMyShow.Models.Show;
import com.driver.bookMyShow.Models.ShowSeat;
import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.Models.TemporaryWallet;
import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.Repositories.*;
import com.driver.bookMyShow.Transformers.TicketTransformer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UnifiedQrCodeService unifiedQrCodeService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TemporaryWalletRepository temporaryWalletRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public TicketResponseDto ticketBooking(TicketEntryDto ticketEntryDto) throws RequestedSeatAreNotAvailable, UserDoesNotExists, ShowDoesNotExists{
        // check user present
        Optional<Show> showOpt = showRepository.findById(ticketEntryDto.getShowId());
        if(showOpt.isEmpty()) {
            throw new ShowDoesNotExists();
        }

        //check show present
        Optional<User> userOpt = userRepository.findById(ticketEntryDto.getUserId());
        if(userOpt.isEmpty()) {
            throw new UserDoesNotExists();
        }

        User user = userOpt.get();
        Show show = showOpt.get();

        // CRITICAL: Validate show time hasn't passed
        LocalDateTime showDateTime = LocalDateTime.of(
            show.getDate().toLocalDate(),
            show.getTime().toLocalTime()
        );
        
        if (LocalDateTime.now().isAfter(showDateTime)) {
            throw new ShowDoesNotExists(); // Using existing exception for consistency
        }

        //check requested seat available
        Boolean isSeatAvailable = isSeatAvailable(show.getShowSeatList(), ticketEntryDto.getRequestSeats());
        if(!isSeatAvailable) {
            throw new RequestedSeatAreNotAvailable();
        }

        // count price
        Integer getPriceAndAssignSeats = getPriceAndAssignSeats(show.getShowSeatList(),ticketEntryDto.getRequestSeats());

        // change list to string
        String seats = listToString(ticketEntryDto.getRequestSeats());

        // create ticket entity and set all attribute
        Ticket ticket = new Ticket();
        ticket.setTotalTicketsPrice(getPriceAndAssignSeats);
        ticket.setBookedSeats(seats);

        // setting foreign key variables
        ticket.setUser(user);
        ticket.setShow(show);

        ticket = ticketRepository.save(ticket);

        user.getTicketList().add(ticket);
        show.getTicketList().add(ticket);
        userRepository.save(user);
        showRepository.save(show);

        // Send email notification using EmailService (async)
        try {
            emailService.sendBookingConfirmationEmail(user, ticket);
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email: {}", e.getMessage());
            // Continue - email failure doesn't affect booking
        }

        // build Ticket Response Dto
        return TicketTransformer.returnTicket(show, ticket);
    }

    private void sendMailToUser(User user, Show show, String seats) {
        String body = "Dear"+user.getName()+",\n\nI hope this email finds you well. \n" +
                "I am writing to inform you that your ticket has been successfully booked. \n" +
                "We are pleased to confirm that your preferred date and time and more details have been secured.\n \n" +
                "Ticket Details:\n\n" +
                "Booked seat No's: "+seats+"\n" +
                "Movie Name: "+show.getMovie().getMovieName()+"\n" +
                "Date: "+show.getDate()+"\n" +
                "Time: "+show.getTime()+"\n" +
                "Location: "+show.getTheater().getAddress()+"\n\n"+
                "Enjoy the show !!";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setText(body);
        message.setFrom("khanking001qwerty@gmail.com");
        message.setTo(user.getEmailId());
        message.setSubject("Ticket Successfully Booked!");
        mailSender.send(message);
    }

    private Boolean isSeatAvailable(List<ShowSeat> showSeatList, List<String> requestSeats) {
        for(ShowSeat showSeat : showSeatList) {
            String seatNo = showSeat.getSeatNo();
            if(requestSeats.contains(seatNo)) {
                if(!showSeat.getIsAvailable()) {
                    return false;
                }
            }
        }
        return true;
    }

    private Integer getPriceAndAssignSeats(List<ShowSeat> showSeatList, List<String> requestSeats) {
        Integer totalAmount = 0;
        for(ShowSeat showSeat : showSeatList) {
            if(requestSeats.contains(showSeat.getSeatNo())) {
                totalAmount += showSeat.getPrice();
                showSeat.setIsAvailable(Boolean.FALSE);
            }
        }
        return totalAmount;
    }

    private String listToString(List<String> requestSeats) {
        StringBuilder sb = new StringBuilder();
        for(String s : requestSeats) {
            sb.append(s).append(",");
        }
        return sb.toString();
    }

    /**
     * Get all bookings for a user
     * STRICT FLOW: Controller → Service → Repository → Database
     * 
     * @param userId User ID
     * @return List of tickets
     * @throws UserDoesNotExists if user not found
     */
    public List<Ticket> getUserBookings(Integer userId) throws UserDoesNotExists {
        Optional<User> userOpt = userRepository.findById(userId);
        if(userOpt.isEmpty()) {
            throw new UserDoesNotExists();
        }
        User user = userOpt.get();
        
        // Use Repository layer instead of entity relationship
        // This follows: Service → Repository → Database
        return ticketRepository.findByUserOrderByBookedAtDesc(user);
    }

    @Transactional
    public void cancelBooking(Integer ticketId) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        if(ticketOpt.isEmpty()) {
            throw new RuntimeException("Ticket not found");
        }
        Ticket ticket = ticketOpt.get();
        
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new RuntimeException("Ticket is already cancelled");
        }
        
        Show show = ticket.getShow();
        
        // Release the seats atomically via repository
        String[] seats = ticket.getBookedSeats().split(",");
        List<String> seatList = new java.util.ArrayList<>();
        for (String seat : seats) {
            String trimmed = seat.trim();
            if (!trimmed.isEmpty()) {
                seatList.add(trimmed);
            }
        }
        
        // Mark ticket as CANCELLED instead of deleting
        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setCancelledAt(LocalDateTime.now());
        ticketRepository.save(ticket);
        
        // Release seats via repository (in-memory fallback for legacy endpoint)
        for(ShowSeat showSeat : show.getShowSeatList()) {
            if (seatList.contains(showSeat.getSeatNo())) {
                showSeat.setIsAvailable(Boolean.TRUE);
            }
        }
        showRepository.save(show);
        
        // ✅ ADD REFUND TO TEMPORARY WALLET (15-day expiry)
        addRefundToTemporaryWallet(ticket.getUser().getId(), ticket.getTotalTicketsPrice(), "TICKET_CANCELLATION");
    }
    
    /**
     * Change booking seats and handle refund/charge difference
     * PUT /ticket/change/{ticketId}
     */
    @Transactional
    public TicketResponseDto changeBooking(Integer ticketId, List<String> newSeats) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        if(ticketOpt.isEmpty()) {
            throw new RuntimeException("Ticket not found");
        }
        
        Ticket ticket = ticketOpt.get();
        if (ticket.getStatus() != TicketStatus.BOOKED) {
            throw new RuntimeException("Cannot change cancelled or expired tickets");
        }
        
        Show show = ticket.getShow();
        
        // Get old booked seats
        String[] oldSeats = ticket.getBookedSeats().split(",");
        List<String> oldSeatList = new java.util.ArrayList<>();
        for (String seat : oldSeats) {
            String trimmed = seat.trim();
            if (!trimmed.isEmpty()) {
                oldSeatList.add(trimmed);
            }
        }
        
        // Check if new seats are available
        Boolean isSeatAvailable = isSeatAvailable(show.getShowSeatList(), newSeats);
        if(!isSeatAvailable) {
            throw new RuntimeException("Requested seats are not available");
        }
        
        // Calculate old price and new price
        Integer oldPrice = ticket.getTotalTicketsPrice();
        Integer newPrice = getPriceAndAssignSeats(show.getShowSeatList(), newSeats);
        Integer priceDifference = newPrice - oldPrice;
        
        // Release old seats
        for(ShowSeat showSeat : show.getShowSeatList()) {
            if (oldSeatList.contains(showSeat.getSeatNo())) {
                showSeat.setIsAvailable(Boolean.TRUE);
            }
        }
        
        // Assign new seats (getPriceAndAssignSeats marks them as unavailable)
        getPriceAndAssignSeats(show.getShowSeatList(), newSeats);
        showRepository.save(show);
        
        // Update ticket with new seats and price
        String newSeatsString = listToString(newSeats);
        ticket.setBookedSeats(newSeatsString);
        ticket.setTotalTicketsPrice(newPrice);
        ticketRepository.save(ticket);
        
        // Handle price difference
        if (priceDifference > 0) {
            // User owes money - can be charged to main wallet or card (not implemented here)
            log.warn("Ticket change: User owes additional ₹{} for ticket {}", priceDifference, ticketId);
        } else if (priceDifference < 0) {
            // User gets refund - add to temporary wallet (15-day expiry)
            Integer refundAmount = Math.abs(priceDifference);
            addRefundToTemporaryWallet(ticket.getUser().getId(), refundAmount, "TICKET_CHANGE_REFUND");
        }
        
        return TicketTransformer.returnTicket(ticket.getShow(), ticket);
    }
    
    /**
     * Add refund to temporary wallet with 15-day expiry
     * DB-CENTRIC: Money stored in DB with expiry, cleaned up automatically
     * Returns the saved TemporaryWallet with expiry date for API responses
     */
    public TemporaryWallet addRefundToTemporaryWallet(Integer userId, Integer refundAmount, String refundReason) {
        if (refundAmount == null) {
            return null;
        }

        return addRefundToTemporaryWallet(userId, BigDecimal.valueOf(refundAmount), refundReason);
    }

    public TemporaryWallet addRefundToTemporaryWallet(Integer userId, BigDecimal refundAmount, String refundReason) {
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        
        TemporaryWallet wallet = new TemporaryWallet();
        wallet.setUserId(userId.longValue());
        wallet.setAmount(refundAmount);
        wallet.setSourceType(refundReason); // Store the reason as source type: TICKET_CANCELLATION, TICKET_CHANGE_REFUND
        wallet.setEarnedAt(LocalDateTime.now());
        // @PrePersist will set expires_at = earned_at + 15 days
        wallet.setIsExpired(false);
        wallet.setIsUsed(false);
        
        TemporaryWallet savedWallet = temporaryWalletRepository.save(wallet);
        
        log.info("✅ Refund of ₹{} added to temporary wallet for user {} (Source: {} | Expires: {})", 
            refundAmount, userId, refundReason, savedWallet.getExpiresAt());
        
        return savedWallet;
    }

    /**
     * Generate unified QR code for ticket (with optional parking & food)
     * 
     * Called AFTER payment success to add QR to ticket
     * 
     * @param ticketId Movie ticket ID
     * @param parkingTicketId Parking ticket ID (nullable)
     * @param vehicleType Vehicle type (nullable)
     * @param vehicleNumber Vehicle number (nullable)
     * @param parkingFee Parking fee (nullable)
     * @param foodOrderId Food order ID (nullable)
     * @param foodItems Food items JSON (nullable)
     * @param foodTotal Food total (nullable)
     * @return Updated ticket with QR code
     */
    @Transactional
    public Ticket generateUnifiedQrForTicket(
            Integer ticketId,
            Integer parkingTicketId,
            String vehicleType,
            String vehicleNumber,
            Double parkingFee,
            Integer foodOrderId,
            String foodItems,
            Double foodTotal
    ) {
        try {
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

            // Generate unified QR code
            String qrCodeBase64 = unifiedQrCodeService.generateUnifiedQrCode(
                    ticket, parkingTicketId, vehicleType, vehicleNumber, parkingFee,
                    foodOrderId, foodItems, foodTotal
            );

            if (qrCodeBase64 != null) {
                ticket.setQrCodeData(qrCodeBase64);
                
                // Also store the QR payload as JSON for validation
                String qrPayload = buildQrPayloadJson(ticket, parkingTicketId, foodOrderId);
                ticket.setQrPayload(qrPayload);
                
                // Set expiry time (show time + 2 hours)
                LocalDateTime showTime = ticket.getShow().getDate().toLocalDate()
                        .atTime(ticket.getShow().getTime().toLocalTime());
                ticket.setQrExpiryTime(showTime.plusHours(2));

                ticket = ticketRepository.save(ticket);
                log.info("QR code generated for ticket: {}, hasParking: {}, hasFood: {}",
                        ticketId, parkingTicketId != null, foodOrderId != null);
            } else {
                log.warn("QR generation failed for ticket: {}, continuing without QR", ticketId);
            }

            return ticket;

        } catch (Exception e) {
            log.error("Failed to generate QR for ticket: {}", ticketId, e);
            // Fail-safe: Return ticket without QR
            return ticketRepository.findById(ticketId).orElse(null);
        }
    }

    /**
     * Build QR payload JSON string
     */
    private String buildQrPayloadJson(Ticket ticket, Integer parkingTicketId, Integer foodOrderId) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of(
                    "ticketId", ticket.getId(),
                    "userId", ticket.getUser().getId(),
                    "movieName", ticket.getShow().getMovie().getMovieName(),
                    "theaterName", ticket.getShow().getTheater().getName(),
                    "seats", ticket.getBookedSeats(),
                    "hasParking", parkingTicketId != null,
                    "hasFood", foodOrderId != null,
                    "generatedAt", java.time.LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            log.error("Failed to build QR payload JSON", e);
            return "{}";
        }
    }

}
