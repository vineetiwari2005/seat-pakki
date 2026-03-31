package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Dtos.RequestDtos.SeatConfigDto;
import com.driver.bookMyShow.Dtos.RequestDtos.ShowEntryDto;
import com.driver.bookMyShow.Enums.PaymentStatus;
import com.driver.bookMyShow.Enums.SeatType;
import com.driver.bookMyShow.Enums.TicketStatus;
import com.driver.bookMyShow.Enums.UserRole;
import com.driver.bookMyShow.Models.*;
import com.driver.bookMyShow.Repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TheatreAdminService - Business logic for Theatre Admin operations.
 * 
 * Theatre Admin can only manage the theatre assigned to them.
 * Features:
 * - View assigned theatre details
 * - View and respond to movie recommendations from Main Admin
 * - Schedule shows for recommended/available movies
 * - Manage seats and show timings
 * - View theatre-specific analytics (bookings, revenue, occupancy)
 */
@Service
public class TheatreAdminService {

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private TheaterSeatRepository theaterSeatRepository;

    @Autowired
    private MovieRecommendationRepository movieRecommendationRepository;

    @Autowired(required = false)
    private HttpServletRequest request;

    // =====================================================
    // THEATRE ACCESS
    // =====================================================

    /**
     * Get the theatre for the current request.
     * If the frontend sends an X-Theatre-Id header (from city/theatre selection on login),
     * use that theatre. Otherwise fall back to the admin's assigned theatre.
     */
    public Theater getAssignedTheatre(Integer adminUserId) throws Exception {
        // Check for override theatreId from request header
        if (request != null) {
            try {
                String theatreIdStr = request.getHeader("X-Theatre-Id");
                if (theatreIdStr != null && !theatreIdStr.isEmpty()) {
                    Integer theatreId = Integer.parseInt(theatreIdStr);
                    if (theatreId > 0) {
                        return theaterRepository.findById(theatreId)
                                .orElseThrow(() -> new Exception("Theatre not found with id: " + theatreId));
                    }
                }
            } catch (NumberFormatException e) {
                // Ignore, fall through to default lookup
            }
        }

        // Default: find theatre assigned to this admin
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new Exception("User not found"));

        if (admin.getRole() != UserRole.THEATER_OWNER) {
            throw new Exception("User is not a Theatre Admin");
        }

        return theaterRepository.findByAdminId(adminUserId)
                .orElseThrow(() -> new Exception("No theatre assigned to this admin. Contact Main Admin."));
    }

    /**
     * Get theatre details with summary info
     */
    public Map<String, Object> getTheatreDashboard(Integer adminUserId) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);
        Map<String, Object> dashboard = new LinkedHashMap<>();

        // Theatre info
        Map<String, Object> theatreInfo = new LinkedHashMap<>();
        theatreInfo.put("id", theatre.getId());
        theatreInfo.put("name", theatre.getName());
        theatreInfo.put("address", theatre.getAddress());
        theatreInfo.put("cityName", theatre.getCityName());
        if (theatre.getCity() != null) {
            theatreInfo.put("city", theatre.getCity().getName());
        }
        theatreInfo.put("seatCount", theatre.getTheaterSeatList() != null ? theatre.getTheaterSeatList().size() : 0);
        dashboard.put("theatre", theatreInfo);

        // Show statistics
        List<Show> shows = showRepository.findByTheaterId(theatre.getId());
        dashboard.put("totalShows", shows.size());

        // Today's shows
        Date today = Date.valueOf(LocalDate.now());
        long todaysShows = shows.stream()
                .filter(s -> s.getDate().equals(today))
                .count();
        dashboard.put("todaysShows", todaysShows);

        // Total bookings for this theatre
        long totalBookings = shows.stream()
                .mapToLong(s -> s.getTicketList() != null ? s.getTicketList().size() : 0)
                .sum();
        dashboard.put("totalBookings", totalBookings);

        // Revenue calculation from tickets
        double totalRevenue = shows.stream()
                .flatMap(s -> s.getTicketList() != null ? s.getTicketList().stream() : java.util.stream.Stream.empty())
                .mapToDouble(t -> t.getTotalTicketsPrice() != null ? t.getTotalTicketsPrice().doubleValue() : 0.0)
                .sum();
        dashboard.put("totalRevenue", totalRevenue);

        // Pending recommendations
        long pendingRecommendations = movieRecommendationRepository
                .countByTheaterIdAndStatus(theatre.getId(), "PENDING");
        dashboard.put("pendingRecommendations", pendingRecommendations);

        // Seat occupancy rate
        long totalShowSeats = shows.stream()
                .mapToLong(s -> s.getShowSeatList() != null ? s.getShowSeatList().size() : 0)
                .sum();
        long bookedSeats = shows.stream()
                .flatMap(s -> s.getShowSeatList() != null ? s.getShowSeatList().stream() : java.util.stream.Stream.empty())
                .filter(ss -> !ss.getIsAvailable())
                .count();
        double occupancyRate = totalShowSeats > 0 ? (double) bookedSeats / totalShowSeats * 100 : 0;
        dashboard.put("occupancyRate", Math.round(occupancyRate * 100.0) / 100.0);

        return dashboard;
    }

    // =====================================================
    // MOVIE RECOMMENDATIONS
    // =====================================================

    /**
     * Get all movie recommendations for the theatre admin's theatre
     */
    public List<Map<String, Object>> getRecommendations(Integer adminUserId) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);
        List<MovieRecommendation> recommendations = movieRecommendationRepository
                .findByTheaterId(theatre.getId());

        return recommendations.stream().map(rec -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", rec.getId());
            map.put("status", rec.getStatus());
            map.put("adminMessage", rec.getAdminMessage());
            map.put("theatreAdminResponse", rec.getTheatreAdminResponse());
            map.put("createdAt", rec.getCreatedAt());

            // Movie info
            Map<String, Object> movieMap = new LinkedHashMap<>();
            movieMap.put("id", rec.getMovie().getId());
            movieMap.put("movieName", rec.getMovie().getMovieName());
            movieMap.put("genre", rec.getMovie().getGenre());
            movieMap.put("language", rec.getMovie().getLanguage());
            movieMap.put("duration", rec.getMovie().getDuration());
            movieMap.put("rating", rec.getMovie().getRating());
            movieMap.put("posterUrl", rec.getMovie().getPosterUrl());
            map.put("movie", movieMap);

            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Get only pending recommendations
     */
    public List<Map<String, Object>> getPendingRecommendations(Integer adminUserId) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);
        List<MovieRecommendation> recommendations = movieRecommendationRepository
                .findByTheaterIdAndStatusOrderByCreatedAtDesc(theatre.getId(), "PENDING");

        return recommendations.stream().map(rec -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", rec.getId());
            map.put("status", rec.getStatus());
            map.put("adminMessage", rec.getAdminMessage());
            map.put("createdAt", rec.getCreatedAt());

            Map<String, Object> movieMap = new LinkedHashMap<>();
            movieMap.put("id", rec.getMovie().getId());
            movieMap.put("movieName", rec.getMovie().getMovieName());
            movieMap.put("genre", rec.getMovie().getGenre());
            movieMap.put("language", rec.getMovie().getLanguage());
            movieMap.put("duration", rec.getMovie().getDuration());
            movieMap.put("rating", rec.getMovie().getRating());
            movieMap.put("posterUrl", rec.getMovie().getPosterUrl());
            map.put("movie", movieMap);

            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Accept a movie recommendation
     */
    @Transactional
    public String acceptRecommendation(Integer adminUserId, Integer recommendationId, String responseMessage) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);
        MovieRecommendation rec = movieRecommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new Exception("Recommendation not found"));

        if (!rec.getTheater().getId().equals(theatre.getId())) {
            throw new Exception("This recommendation does not belong to your theatre");
        }

        if (!"PENDING".equals(rec.getStatus())) {
            throw new Exception("This recommendation has already been " + rec.getStatus().toLowerCase());
        }

        rec.setStatus("ACCEPTED");
        rec.setTheatreAdminResponse(responseMessage);
        movieRecommendationRepository.save(rec);

        return "Recommendation accepted. You can now schedule shows for " + rec.getMovie().getMovieName();
    }

    /**
     * Reject a movie recommendation
     */
    @Transactional
    public String rejectRecommendation(Integer adminUserId, Integer recommendationId, String reason) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);
        MovieRecommendation rec = movieRecommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new Exception("Recommendation not found"));

        if (!rec.getTheater().getId().equals(theatre.getId())) {
            throw new Exception("This recommendation does not belong to your theatre");
        }

        if (!"PENDING".equals(rec.getStatus())) {
            throw new Exception("This recommendation has already been " + rec.getStatus().toLowerCase());
        }

        rec.setStatus("REJECTED");
        rec.setTheatreAdminResponse(reason);
        movieRecommendationRepository.save(rec);

        return "Recommendation rejected for " + rec.getMovie().getMovieName();
    }

    // =====================================================
    // SHOW MANAGEMENT
    // =====================================================

    /**
     * Get all shows for the theatre admin's theatre
     */
    public List<Map<String, Object>> getShows(Integer adminUserId) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);
        List<Show> shows = showRepository.findByTheaterId(theatre.getId());

        return shows.stream().map(show -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", show.getId());
            map.put("date", show.getDate());
            map.put("time", show.getTime());
            map.put("createdAt", show.getCreatedAt());

            // Movie info
            if (show.getMovie() != null) {
                Map<String, Object> movieMap = new LinkedHashMap<>();
                movieMap.put("id", show.getMovie().getId());
                movieMap.put("movieName", show.getMovie().getMovieName());
                movieMap.put("genre", show.getMovie().getGenre());
                movieMap.put("language", show.getMovie().getLanguage());
                movieMap.put("duration", show.getMovie().getDuration());
                movieMap.put("posterUrl", show.getMovie().getPosterUrl());
                map.put("movie", movieMap);
            }

            // Seat info
            int totalSeats = show.getShowSeatList() != null ? show.getShowSeatList().size() : 0;
            long bookedSeats = show.getShowSeatList() != null ?
                    show.getShowSeatList().stream().filter(ss -> !ss.getIsAvailable()).count() : 0;
            map.put("totalSeats", totalSeats);
            map.put("bookedSeats", bookedSeats);
            map.put("availableSeats", totalSeats - bookedSeats);

            // Ticket count
            map.put("ticketCount", show.getTicketList() != null ? show.getTicketList().size() : 0);

            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Schedule a new show for the theatre admin's theatre.
     * Supports two modes:
     * 1. Custom seat config: Theatre admin specifies seat types, counts, rows, prices
     * 2. Auto-generate: Copy from theatre seat layout with default/custom prices
     */
    @Transactional
    public String addShow(Integer adminUserId, ShowEntryDto showEntryDto) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);

        // Verify the movie exists
        Movie movie = movieRepository.findById(showEntryDto.getMovieId())
                .orElseThrow(() -> new Exception("Movie not found"));

        // Create the show
        Show show = Show.builder()
                .date(showEntryDto.getShowDate())
                .time(showEntryDto.getShowStartTime())
                .movie(movie)
                .theater(theatre)
                .build();

        show = showRepository.save(show);

        int totalSeatsCreated = 0;

        // Check if custom seat configuration is provided
        if (Boolean.TRUE.equals(showEntryDto.getUseCustomSeats()) 
                && showEntryDto.getSeatConfigs() != null 
                && !showEntryDto.getSeatConfigs().isEmpty()) {
            
            // Use custom seat configuration from theatre admin
            List<ShowSeat> showSeats = new ArrayList<>();
            for (SeatConfigDto config : showEntryDto.getSeatConfigs()) {
                String rowPrefix = config.getRowPrefix();
                SeatType seatType = config.getSeatType();
                int count = config.getCount();
                int price = config.getPrice();

                // Couple seats must always be in even numbers (pairs)
                if (seatType == SeatType.COUPLE && count % 2 != 0) {
                    throw new Exception("Couple seats must be added in even numbers (pairs). Requested: " + count + " for row " + rowPrefix);
                }

                for (int i = 1; i <= count; i++) {
                    ShowSeat ss = ShowSeat.builder()
                            .seatNo(rowPrefix + i)
                            .seatType(seatType)
                            .price(price)
                            .isAvailable(true)
                            .isFoodContains(false)
                            .show(show)
                            .build();
                    showSeats.add(ss);
                }
                totalSeatsCreated += count;
            }
            showSeatRepository.saveAll(showSeats);

        } else {
            // Auto-generate show seats from theatre seats with default pricing
            List<TheaterSeat> theaterSeats = theaterSeatRepository.findByTheaterId(theatre.getId());
            if (!theaterSeats.isEmpty()) {
                List<ShowSeat> showSeats = new ArrayList<>();
                for (TheaterSeat ts : theaterSeats) {
                    ShowSeat ss = ShowSeat.builder()
                            .seatNo(ts.getSeatNo())
                            .seatType(ts.getSeatType())
                            .price(getDefaultPrice(ts.getSeatType()))
                            .isAvailable(true)
                            .isFoodContains(false)
                            .show(show)
                            .build();
                    showSeats.add(ss);
                }
                showSeatRepository.saveAll(showSeats);
                totalSeatsCreated = showSeats.size();
            }
        }

        return "Show added successfully for " + movie.getMovieName() + " at " + theatre.getName()
                + " on " + showEntryDto.getShowDate() + " at " + showEntryDto.getShowStartTime()
                + " (" + totalSeatsCreated + " seats configured)";
    }

    /**
     * Delete a show (only if no bookings)
     */
    @Transactional
    public void deleteShow(Integer adminUserId, Integer showId) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new Exception("Show not found"));

        if (!show.getTheater().getId().equals(theatre.getId())) {
            throw new Exception("This show does not belong to your theatre");
        }

        if (show.getTicketList() != null && !show.getTicketList().isEmpty()) {
            throw new Exception("Cannot delete show with existing bookings");
        }

        showRepository.delete(show);
    }

    // =====================================================
    // SEAT MANAGEMENT
    // =====================================================

    /**
     * Get theatre seats
     */
    public List<TheaterSeat> getTheatreSeats(Integer adminUserId) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);
        return theaterSeatRepository.findByTheaterId(theatre.getId());
    }

    /**
     * Get show seats for a specific show
     */
    public List<Map<String, Object>> getShowSeats(Integer adminUserId, Integer showId) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new Exception("Show not found"));

        if (!show.getTheater().getId().equals(theatre.getId())) {
            throw new Exception("This show does not belong to your theatre");
        }

        List<ShowSeat> showSeats = showSeatRepository.findByShowId(showId);
        return showSeats.stream().map(ss -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", ss.getId());
            map.put("seatNo", ss.getSeatNo());
            map.put("seatType", ss.getSeatType());
            map.put("price", ss.getPrice());
            map.put("isAvailable", ss.getIsAvailable());
            return map;
        }).collect(Collectors.toList());
    }

    // =====================================================
    // ANALYTICS
    // =====================================================

    /**
     * Get theatre-specific analytics
     */
    public Map<String, Object> getTheatreAnalytics(Integer adminUserId) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);
        Map<String, Object> analytics = new LinkedHashMap<>();

        List<Show> allShows = showRepository.findByTheaterId(theatre.getId());

        // Total shows
        analytics.put("totalShows", allShows.size());

        // Today's shows
        Date today = Date.valueOf(LocalDate.now());
        List<Show> todaysShows = allShows.stream()
                .filter(s -> s.getDate().equals(today))
                .collect(Collectors.toList());
        analytics.put("todaysShows", todaysShows.size());

        // Upcoming shows (next 7 days)
        Date weekFromNow = Date.valueOf(LocalDate.now().plusDays(7));
        long upcomingShows = allShows.stream()
                .filter(s -> !s.getDate().before(today) && !s.getDate().after(weekFromNow))
                .count();
        analytics.put("upcomingShows", upcomingShows);

        // Total bookings
        long totalBookings = allShows.stream()
                .mapToLong(s -> s.getTicketList() != null ? s.getTicketList().size() : 0)
                .sum();
        analytics.put("totalBookings", totalBookings);

        // Total revenue
        double totalRevenue = allShows.stream()
                .flatMap(s -> s.getTicketList() != null ? s.getTicketList().stream() : java.util.stream.Stream.empty())
                .mapToDouble(t -> t.getTotalTicketsPrice() != null ? t.getTotalTicketsPrice().doubleValue() : 0.0)
                .sum();
        analytics.put("totalRevenue", totalRevenue);

        // Today's revenue
        double todaysRevenue = todaysShows.stream()
                .flatMap(s -> s.getTicketList() != null ? s.getTicketList().stream() : java.util.stream.Stream.empty())
                .mapToDouble(t -> t.getTotalTicketsPrice() != null ? t.getTotalTicketsPrice().doubleValue() : 0.0)
                .sum();
        analytics.put("todaysRevenue", todaysRevenue);

        // Seat occupancy
        long totalSeats = allShows.stream()
                .mapToLong(s -> s.getShowSeatList() != null ? s.getShowSeatList().size() : 0)
                .sum();
        long bookedSeats = allShows.stream()
                .flatMap(s -> s.getShowSeatList() != null ? s.getShowSeatList().stream() : java.util.stream.Stream.empty())
                .filter(ss -> !ss.getIsAvailable())
                .count();
        double occupancyRate = totalSeats > 0 ? (double) bookedSeats / totalSeats * 100 : 0;
        analytics.put("totalSeats", totalSeats);
        analytics.put("bookedSeats", bookedSeats);
        analytics.put("occupancyRate", Math.round(occupancyRate * 100.0) / 100.0);

        // Movie-wise performance
        Map<String, Map<String, Object>> moviePerformance = new LinkedHashMap<>();
        for (Show show : allShows) {
            String movieName = show.getMovie().getMovieName();
            Map<String, Object> perf = moviePerformance.getOrDefault(movieName, new LinkedHashMap<>());
            int showCount = (int) perf.getOrDefault("showCount", 0) + 1;
            long bookings = (long) perf.getOrDefault("bookings", 0L) + 
                    (show.getTicketList() != null ? show.getTicketList().size() : 0);
            double revenue = (double) perf.getOrDefault("revenue", 0.0) +
                    (show.getTicketList() != null ? show.getTicketList().stream()
                            .mapToDouble(t -> t.getTotalTicketsPrice() != null ? t.getTotalTicketsPrice().doubleValue() : 0.0)
                            .sum() : 0.0);

            perf.put("movieName", movieName);
            perf.put("movieId", show.getMovie().getId());
            perf.put("showCount", showCount);
            perf.put("bookings", bookings);
            perf.put("revenue", revenue);
            moviePerformance.put(movieName, perf);
        }
        analytics.put("moviePerformance", new ArrayList<>(moviePerformance.values()));

        // Daily booking trend (last 7 days) - aggregate by ticket booking date
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        // Collect ALL tickets from all shows
        List<com.driver.bookMyShow.Models.Ticket> allTickets = allShows.stream()
                .flatMap(s -> s.getTicketList() != null ? s.getTicketList().stream() : java.util.stream.Stream.empty())
                .toList();
        for (int i = 6; i >= 0; i--) {
            LocalDate targetDate = LocalDate.now().minusDays(i);
            long dayBookings = allTickets.stream()
                    .filter(t -> t.getBookedAt() != null && t.getBookedAt().toLocalDate().equals(targetDate))
                    .count();
            double dayRevenue = allTickets.stream()
                    .filter(t -> t.getBookedAt() != null && t.getBookedAt().toLocalDate().equals(targetDate))
                    .mapToDouble(t -> t.getTotalTicketsPrice() != null ? t.getTotalTicketsPrice().doubleValue() : 0.0)
                    .sum();

            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", targetDate.toString());
            day.put("bookings", dayBookings);
            day.put("revenue", dayRevenue);
            dailyTrend.add(day);
        }
        analytics.put("dailyTrend", dailyTrend);

        return analytics;
    }

    /**
     * Get booking details for a specific show in the theatre admin's theatre
     */
    public List<Map<String, Object>> getShowBookings(Integer adminUserId, Integer showId) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new Exception("Show not found"));

        if (!show.getTheater().getId().equals(theatre.getId())) {
            throw new Exception("This show does not belong to your theatre");
        }

        List<Ticket> tickets = show.getTicketList();
        if (tickets == null) return new ArrayList<>();

        return tickets.stream().map(ticket -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("ticketId", ticket.getId());
            map.put("bookedSeats", ticket.getBookedSeats());
            map.put("totalPrice", ticket.getTotalTicketsPrice());
            map.put("bookedAt", ticket.getBookedAt());
            if (ticket.getUser() != null) {
                map.put("userName", ticket.getUser().getName());
                map.put("userEmail", ticket.getUser().getEmailId());
            }
            return map;
        }).collect(Collectors.toList());
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    private int getDefaultPrice(SeatType seatType) {
        return switch (seatType) {
            case SILVER -> 150;
            case GOLD -> 250;
            case PREMIUM -> 400;
            case COUPLE -> 600;
            case CLASSIC -> 150;
        };
    }

    // =====================================================
    // SEAT MANAGEMENT (Theatre Admin can manage their own seats)
    // =====================================================

    /**
     * Add a row of seats to the theatre
     */
    @Transactional
    public String addTheatreSeatsRow(Integer adminUserId, String rowPrefix, SeatType seatType, int count) throws Exception {
        // Couple seats must always be added in even numbers (they come in pairs)
        if (seatType == SeatType.COUPLE && count % 2 != 0) {
            throw new Exception("Couple seats must be added in even numbers (pairs). Requested: " + count);
        }
        Theater theatre = getAssignedTheatre(adminUserId);
        List<TheaterSeat> existingSeats = theaterSeatRepository.findByTheaterId(theatre.getId());

        int added = 0;
        for (int i = 1; i <= count; i++) {
            String seatNo = rowPrefix + i;
            boolean exists = existingSeats.stream().anyMatch(s -> s.getSeatNo().equals(seatNo));
            if (!exists) {
                TheaterSeat seat = TheaterSeat.builder()
                        .seatNo(seatNo)
                        .seatType(seatType)
                        .theater(theatre)
                        .build();
                theaterSeatRepository.save(seat);
                added++;
            }
        }
        return "Added " + added + " " + seatType + " seats (row " + rowPrefix + ") to " + theatre.getName();
    }

    /**
     * Delete a theatre seat (only own theatre)
     */
    @Transactional
    public void deleteTheatreSeat(Integer adminUserId, Integer seatId) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);
        TheaterSeat seat = theaterSeatRepository.findById(seatId)
                .orElseThrow(() -> new Exception("Seat not found"));
        if (!seat.getTheater().getId().equals(theatre.getId())) {
            throw new Exception("Seat does not belong to your theatre");
        }
        theaterSeatRepository.delete(seat);
    }

    /**
     * Get seat type summary for the theatre
     */
    public Map<String, Object> getSeatTypeSummary(Integer adminUserId) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);
        List<TheaterSeat> seats = theaterSeatRepository.findByTheaterId(theatre.getId());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("theatreName", theatre.getName());
        summary.put("totalSeats", seats.size());

        // Group by seat type
        Map<SeatType, Long> typeCounts = seats.stream()
                .collect(java.util.stream.Collectors.groupingBy(TheaterSeat::getSeatType, java.util.stream.Collectors.counting()));

        List<Map<String, Object>> typeBreakdown = new ArrayList<>();
        for (SeatType type : SeatType.values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("seatType", type.name());
            entry.put("count", typeCounts.getOrDefault(type, 0L));
            entry.put("defaultPrice", getDefaultPrice(type));
            typeBreakdown.add(entry);
        }
        summary.put("seatTypeBreakdown", typeBreakdown);

        return summary;
    }

    // =====================================================
    // ENHANCED ANALYTICS
    // =====================================================

    /**
     * Get seat type revenue breakdown for the theatre
     */
    public Map<String, Object> getSeatTypeRevenue(Integer adminUserId) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);
        List<Show> shows = showRepository.findByTheaterId(theatre.getId());
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("theatreName", theatre.getName());

        // Aggregate revenue and bookings per seat type across all shows
        Map<SeatType, Double> revenueByType = new LinkedHashMap<>();
        Map<SeatType, Long> bookingsByType = new LinkedHashMap<>();
        Map<SeatType, Long> totalByType = new LinkedHashMap<>();
        
        for (SeatType type : SeatType.values()) {
            revenueByType.put(type, 0.0);
            bookingsByType.put(type, 0L);
            totalByType.put(type, 0L);
        }

        for (Show show : shows) {
            if (show.getShowSeatList() != null) {
                for (ShowSeat ss : show.getShowSeatList()) {
                    SeatType type = ss.getSeatType();
                    totalByType.merge(type, 1L, Long::sum);
                    if (!ss.getIsAvailable()) {
                        bookingsByType.merge(type, 1L, Long::sum);
                        revenueByType.merge(type, (double) ss.getPrice(), Double::sum);
                    }
                }
            }
        }

        List<Map<String, Object>> breakdown = new ArrayList<>();
        double totalRevenue = revenueByType.values().stream().mapToDouble(d -> d).sum();
        
        for (SeatType type : SeatType.values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("seatType", type.name());
            entry.put("totalSeats", totalByType.get(type));
            entry.put("bookedSeats", bookingsByType.get(type));
            entry.put("revenue", revenueByType.get(type));
            double occupancy = totalByType.get(type) > 0 
                    ? ((double) bookingsByType.get(type) / totalByType.get(type)) * 100 : 0;
            entry.put("occupancyRate", Math.round(occupancy * 100.0) / 100.0);
            double share = totalRevenue > 0 ? (revenueByType.get(type) / totalRevenue) * 100 : 0;
            entry.put("revenueShare", Math.round(share * 100.0) / 100.0);
            breakdown.add(entry);
        }

        result.put("seatTypeBreakdown", breakdown);
        result.put("totalRevenue", totalRevenue);

        return result;
    }

    /**
     * Get recommendation conversion analytics
     */
    public Map<String, Object> getRecommendationStats(Integer adminUserId) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);
        List<MovieRecommendation> all = movieRecommendationRepository.findByTheaterId(theatre.getId());

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRecommendations", all.size());

        long pending = all.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
        long accepted = all.stream().filter(r -> "ACCEPTED".equals(r.getStatus())).count();
        long rejected = all.stream().filter(r -> "REJECTED".equals(r.getStatus())).count();

        stats.put("pending", pending);
        stats.put("accepted", accepted);
        stats.put("rejected", rejected);

        double acceptRate = all.size() > 0 ? ((double) accepted / all.size()) * 100 : 0;
        stats.put("acceptanceRate", Math.round(acceptRate * 100.0) / 100.0);

        // For accepted recommendations, check if shows were actually scheduled
        long acceptedWithShows = 0;
        List<Show> shows = showRepository.findByTheaterId(theatre.getId());
        Set<Integer> movieIdsWithShows = shows.stream()
                .map(s -> s.getMovie().getId())
                .collect(java.util.stream.Collectors.toSet());

        for (MovieRecommendation rec : all) {
            if ("ACCEPTED".equals(rec.getStatus()) && movieIdsWithShows.contains(rec.getMovie().getId())) {
                acceptedWithShows++;
            }
        }
        stats.put("acceptedWithShows", acceptedWithShows);
        double conversionRate = accepted > 0 ? ((double) acceptedWithShows / accepted) * 100 : 0;
        stats.put("conversionRate", Math.round(conversionRate * 100.0) / 100.0);

        return stats;
    }

    /**
     * Get time slot performance analytics
     */
    public List<Map<String, Object>> getTimeSlotAnalytics(Integer adminUserId) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);
        List<Show> shows = showRepository.findByTheaterId(theatre.getId());

        // Group by time slots: Morning (6-12), Afternoon (12-17), Evening (17-21), Night (21+)
        Map<String, List<Show>> slots = new LinkedHashMap<>();
        slots.put("Morning (6AM-12PM)", new ArrayList<>());
        slots.put("Afternoon (12PM-5PM)", new ArrayList<>());
        slots.put("Evening (5PM-9PM)", new ArrayList<>());
        slots.put("Night (9PM+)", new ArrayList<>());

        for (Show show : shows) {
            if (show.getTime() != null) {
                int hour = show.getTime().toLocalTime().getHour();
                if (hour >= 6 && hour < 12) slots.get("Morning (6AM-12PM)").add(show);
                else if (hour >= 12 && hour < 17) slots.get("Afternoon (12PM-5PM)").add(show);
                else if (hour >= 17 && hour < 21) slots.get("Evening (5PM-9PM)").add(show);
                else slots.get("Night (9PM+)").add(show);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Show>> entry : slots.entrySet()) {
            Map<String, Object> slotData = new LinkedHashMap<>();
            slotData.put("timeSlot", entry.getKey());
            slotData.put("showCount", entry.getValue().size());

            long totalSeats = entry.getValue().stream()
                    .mapToLong(s -> s.getShowSeatList() != null ? s.getShowSeatList().size() : 0).sum();
            long bookedSeats = entry.getValue().stream()
                    .flatMap(s -> s.getShowSeatList() != null ? s.getShowSeatList().stream() : java.util.stream.Stream.empty())
                    .filter(ss -> !ss.getIsAvailable()).count();
            long totalBookings = entry.getValue().stream()
                    .mapToLong(s -> s.getTicketList() != null ? s.getTicketList().size() : 0).sum();
            double revenue = entry.getValue().stream()
                    .flatMap(s -> s.getTicketList() != null ? s.getTicketList().stream() : java.util.stream.Stream.empty())
                    .mapToDouble(t -> t.getTotalTicketsPrice() != null ? t.getTotalTicketsPrice().doubleValue() : 0.0).sum();
            double occupancy = totalSeats > 0 ? ((double) bookedSeats / totalSeats) * 100 : 0;

            slotData.put("totalSeats", totalSeats);
            slotData.put("bookedSeats", bookedSeats);
            slotData.put("totalBookings", totalBookings);
            slotData.put("revenue", revenue);
            slotData.put("occupancyRate", Math.round(occupancy * 100.0) / 100.0);
            result.add(slotData);
        }

        return result;
    }

    /**
     * Get cancellation analytics for the theatre
     */
    public Map<String, Object> getCancellationStats(Integer adminUserId) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);
        List<Show> shows = showRepository.findByTheaterId(theatre.getId());

        Map<String, Object> stats = new LinkedHashMap<>();

        List<Ticket> allTickets = shows.stream()
                .flatMap(s -> s.getTicketList() != null ? s.getTicketList().stream() : java.util.stream.Stream.empty())
                .toList();

        long totalTickets = allTickets.size();
        long cancelledTickets = allTickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.CANCELLED).count();
        long activeTickets = allTickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.BOOKED).count();

        stats.put("totalTickets", totalTickets);
        stats.put("activeTickets", activeTickets);
        stats.put("cancelledTickets", cancelledTickets);

        double cancelRate = totalTickets > 0 ? ((double) cancelledTickets / totalTickets) * 100 : 0;
        stats.put("cancellationRate", Math.round(cancelRate * 100.0) / 100.0);

        double totalRefunds = allTickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.CANCELLED && t.getRefundAmount() != null)
                .mapToDouble(Ticket::getRefundAmount).sum();
        stats.put("totalRefunds", totalRefunds);

        return stats;
    }

    /**
     * Get weekly revenue trend (last 4 weeks)
     */
    public List<Map<String, Object>> getWeeklyRevenueTrend(Integer adminUserId) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);
        List<Show> shows = showRepository.findByTheaterId(theatre.getId());

        // Collect ALL tickets from all shows
        List<com.driver.bookMyShow.Models.Ticket> allTickets = shows.stream()
                .flatMap(s -> s.getTicketList() != null ? s.getTicketList().stream() : java.util.stream.Stream.empty())
                .toList();

        List<Map<String, Object>> weeklyTrend = new ArrayList<>();
        for (int w = 3; w >= 0; w--) {
            LocalDate weekStart = LocalDate.now().minusWeeks(w).with(java.time.DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);

            // Aggregate by ticket bookedAt date, not show date
            long bookings = allTickets.stream()
                    .filter(t -> t.getBookedAt() != null) 
                    .filter(t -> {
                        LocalDate bd = t.getBookedAt().toLocalDate();
                        return !bd.isBefore(weekStart) && !bd.isAfter(weekEnd);
                    })
                    .count();
            double revenue = allTickets.stream()
                    .filter(t -> t.getBookedAt() != null)
                    .filter(t -> {
                        LocalDate bd = t.getBookedAt().toLocalDate();
                        return !bd.isBefore(weekStart) && !bd.isAfter(weekEnd);
                    })
                    .mapToDouble(t -> t.getTotalTicketsPrice() != null ? t.getTotalTicketsPrice().doubleValue() : 0.0).sum();

            // Count shows scheduled in this week (for reference)
            Date sqlStart = Date.valueOf(weekStart);
            Date sqlEnd = Date.valueOf(weekEnd);
            long weekShowCount = shows.stream()
                    .filter(s -> !s.getDate().before(sqlStart) && !s.getDate().after(sqlEnd))
                    .count();

            Map<String, Object> week = new LinkedHashMap<>();
            week.put("weekStart", weekStart.toString());
            week.put("weekEnd", weekEnd.toString());
            week.put("label", "Week " + (4 - w));
            week.put("showCount", weekShowCount);
            week.put("bookings", bookings);
            week.put("revenue", revenue);
            weeklyTrend.add(week);
        }

        return weeklyTrend;
    }

    /**
     * Get genre performance analytics for the theatre
     */
    public List<Map<String, Object>> getGenrePerformance(Integer adminUserId) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);
        List<Show> shows = showRepository.findByTheaterId(theatre.getId());

        Map<String, List<Show>> genreShows = new LinkedHashMap<>();
        for (Show show : shows) {
            String genre = show.getMovie().getGenre() != null ? show.getMovie().getGenre().name() : "UNKNOWN";
            genreShows.computeIfAbsent(genre, k -> new ArrayList<>()).add(show);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Show>> entry : genreShows.entrySet()) {
            Map<String, Object> genreData = new LinkedHashMap<>();
            genreData.put("genre", entry.getKey());
            genreData.put("showCount", entry.getValue().size());

            long bookings = entry.getValue().stream()
                    .mapToLong(s -> s.getTicketList() != null ? s.getTicketList().size() : 0).sum();
            double revenue = entry.getValue().stream()
                    .flatMap(s -> s.getTicketList() != null ? s.getTicketList().stream() : java.util.stream.Stream.empty())
                    .mapToDouble(t -> t.getTotalTicketsPrice() != null ? t.getTotalTicketsPrice().doubleValue() : 0.0).sum();
            long totalSeats = entry.getValue().stream()
                    .mapToLong(s -> s.getShowSeatList() != null ? s.getShowSeatList().size() : 0).sum();
            long bookedSeats = entry.getValue().stream()
                    .flatMap(s -> s.getShowSeatList() != null ? s.getShowSeatList().stream() : java.util.stream.Stream.empty())
                    .filter(ss -> !ss.getIsAvailable()).count();
            double occupancy = totalSeats > 0 ? ((double) bookedSeats / totalSeats) * 100 : 0;

            genreData.put("bookings", bookings);
            genreData.put("revenue", revenue);
            genreData.put("occupancyRate", Math.round(occupancy * 100.0) / 100.0);
            result.add(genreData);
        }

        // Sort by revenue desc
        result.sort((a, b) -> Double.compare((double) b.get("revenue"), (double) a.get("revenue")));
        return result;
    }

    // =====================================================
    // Payment Analytics
    // =====================================================
    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentAnalytics(Integer adminUserId) throws Exception {
        Theater theatre = getAssignedTheatre(adminUserId);
        Map<String, Object> analytics = new LinkedHashMap<>();

        // Get all shows for this theatre
        List<Show> allShows = showRepository.findByTheaterId(theatre.getId());

        // Get all tickets for these shows
        List<Ticket> allTickets = allShows.stream()
                .flatMap(s -> s.getTicketList() != null ? s.getTicketList().stream() : java.util.stream.Stream.empty())
                .toList();
        Set<Integer> ticketIds = allTickets.stream().map(Ticket::getId).collect(Collectors.toSet());

        // Get all payments linked to these tickets (proper DB query with eager fetch)
        List<Payment> allPayments = ticketIds.isEmpty()
                ? List.of()
                : paymentRepository.findByTicketIdIn(ticketIds);

        List<Payment> successPayments = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .toList();

        // ---- Overall Payment Summary ----
        analytics.put("totalTransactions", allPayments.size());
        analytics.put("successfulTransactions", successPayments.size());
        analytics.put("failedTransactions", allPayments.stream().filter(p -> p.getStatus() == com.driver.bookMyShow.Enums.PaymentStatus.FAILED).count());
        analytics.put("pendingTransactions", allPayments.stream().filter(p -> p.getStatus() == com.driver.bookMyShow.Enums.PaymentStatus.PENDING).count());
        analytics.put("refundedTransactions", allPayments.stream().filter(p -> p.getStatus() == com.driver.bookMyShow.Enums.PaymentStatus.REFUNDED).count());

        double totalRevenue = successPayments.stream().mapToDouble(p -> p.getTotalAmount() != null ? p.getTotalAmount() : 0).sum();
        double totalBaseAmount = successPayments.stream().mapToDouble(p -> p.getBaseAmount() != null ? p.getBaseAmount() : 0).sum();
        double totalConvenienceFee = successPayments.stream().mapToDouble(p -> p.getConvenienceFee() != null ? p.getConvenienceFee() : 0).sum();
        double totalTax = successPayments.stream().mapToDouble(p -> p.getTax() != null ? p.getTax() : 0).sum();
        double totalDiscounts = successPayments.stream().mapToDouble(p -> p.getDiscountAmount() != null ? p.getDiscountAmount() : 0).sum();
        double totalRefunds = allPayments.stream().mapToDouble(p -> p.getRefundAmount() != null ? p.getRefundAmount() : 0).sum();
        double totalWalletAmount = successPayments.stream().mapToDouble(p -> {
            if (p.getWalletAmount() != null && p.getWalletAmount() > 0) return p.getWalletAmount();
            // Derive from payment method for payments where walletAmount wasn't tracked
            if (p.getPaymentMethod() == com.driver.bookMyShow.Enums.PaymentMethod.WALLET) return p.getTotalAmount() != null ? p.getTotalAmount() : 0;
            return 0;
        }).sum();
        double totalCardAmount = successPayments.stream().mapToDouble(p -> {
            if (p.getCardAmount() != null && p.getCardAmount() > 0) return p.getCardAmount();
            // Derive from payment method for payments where cardAmount wasn't tracked
            if (p.getPaymentMethod() != null && p.getPaymentMethod() != com.driver.bookMyShow.Enums.PaymentMethod.WALLET) return p.getTotalAmount() != null ? p.getTotalAmount() : 0;
            return 0;
        }).sum();

        analytics.put("totalRevenue", Math.round(totalRevenue * 100.0) / 100.0);
        analytics.put("totalBaseAmount", Math.round(totalBaseAmount * 100.0) / 100.0);
        analytics.put("totalConvenienceFee", Math.round(totalConvenienceFee * 100.0) / 100.0);
        analytics.put("totalTax", Math.round(totalTax * 100.0) / 100.0);
        analytics.put("totalDiscounts", Math.round(totalDiscounts * 100.0) / 100.0);
        analytics.put("totalRefunds", Math.round(totalRefunds * 100.0) / 100.0);
        analytics.put("totalWalletAmount", Math.round(totalWalletAmount * 100.0) / 100.0);
        analytics.put("totalCardAmount", Math.round(totalCardAmount * 100.0) / 100.0);
        analytics.put("avgTransactionValue", successPayments.size() > 0 ? Math.round((totalRevenue / successPayments.size()) * 100.0) / 100.0 : 0);

        // ---- Payment Method Breakdown ----
        Map<String, Long> methodCounts = successPayments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "UNKNOWN",
                        Collectors.counting()));
        Map<String, Double> methodRevenue = successPayments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "UNKNOWN",
                        Collectors.summingDouble(p -> p.getTotalAmount() != null ? p.getTotalAmount() : 0)));

        List<Map<String, Object>> paymentMethodBreakdown = new ArrayList<>();
        for (String method : methodCounts.keySet()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("method", method);
            entry.put("count", methodCounts.get(method));
            entry.put("revenue", Math.round(methodRevenue.getOrDefault(method, 0.0) * 100.0) / 100.0);
            entry.put("percentage", successPayments.size() > 0
                    ? Math.round((double) methodCounts.get(method) / successPayments.size() * 10000.0) / 100.0
                    : 0);
            paymentMethodBreakdown.add(entry);
        }
        paymentMethodBreakdown.sort((a, b) -> Double.compare((double) b.get("revenue"), (double) a.get("revenue")));
        analytics.put("paymentMethodBreakdown", paymentMethodBreakdown);

        // ---- Show-wise Payment Breakdown ----
        Map<Integer, List<Payment>> paymentsByShow = new LinkedHashMap<>();
        for (Payment p : successPayments) {
            if (p.getTicket() != null && p.getTicket().getShow() != null) {
                int showId = p.getTicket().getShow().getId();
                paymentsByShow.computeIfAbsent(showId, k -> new ArrayList<>()).add(p);
            }
        }

        List<Map<String, Object>> showPayments = new ArrayList<>();
        for (Show show : allShows) {
            List<Payment> showPaymentList = paymentsByShow.getOrDefault(show.getId(), Collections.emptyList());
            if (showPaymentList.isEmpty()) continue;

            Map<String, Object> sp = new LinkedHashMap<>();
            sp.put("showId", show.getId());
            sp.put("movieName", show.getMovie() != null ? show.getMovie().getMovieName() : "N/A");
            sp.put("showDate", show.getDate().toString());
            sp.put("showTime", show.getTime().toString());
            sp.put("transactionCount", showPaymentList.size());
            double showRevenue = showPaymentList.stream().mapToDouble(p -> p.getTotalAmount() != null ? p.getTotalAmount() : 0).sum();
            sp.put("revenue", Math.round(showRevenue * 100.0) / 100.0);
            double showWallet = showPaymentList.stream().mapToDouble(p -> {
                if (p.getWalletAmount() != null && p.getWalletAmount() > 0) return p.getWalletAmount();
                if (p.getPaymentMethod() == com.driver.bookMyShow.Enums.PaymentMethod.WALLET) return p.getTotalAmount() != null ? p.getTotalAmount() : 0;
                return 0;
            }).sum();
            double showCard = showPaymentList.stream().mapToDouble(p -> {
                if (p.getCardAmount() != null && p.getCardAmount() > 0) return p.getCardAmount();
                if (p.getPaymentMethod() != null && p.getPaymentMethod() != com.driver.bookMyShow.Enums.PaymentMethod.WALLET) return p.getTotalAmount() != null ? p.getTotalAmount() : 0;
                return 0;
            }).sum();
            sp.put("walletAmount", Math.round(showWallet * 100.0) / 100.0);
            sp.put("cardAmount", Math.round(showCard * 100.0) / 100.0);

            // Payment method distribution for this show
            Map<String, Long> showMethodCounts = showPaymentList.stream()
                    .collect(Collectors.groupingBy(
                            p -> p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "UNKNOWN",
                            Collectors.counting()));
            sp.put("methodBreakdown", showMethodCounts);
            showPayments.add(sp);
        }
        showPayments.sort((a, b) -> Double.compare((double) b.get("revenue"), (double) a.get("revenue")));
        analytics.put("showPayments", showPayments);

        // ---- Daily Payment Trend (last 14 days) ----
        List<Map<String, Object>> dailyPaymentTrend = new ArrayList<>();
        for (int i = 13; i >= 0; i--) {
            LocalDate targetDate = LocalDate.now().minusDays(i);
            List<Payment> dayPayments = successPayments.stream()
                    .filter(p -> p.getCompletedAt() != null && p.getCompletedAt().toLocalDate().equals(targetDate))
                    .toList();

            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", targetDate.toString());
            day.put("transactions", dayPayments.size());
            day.put("revenue", Math.round(dayPayments.stream().mapToDouble(p -> p.getTotalAmount() != null ? p.getTotalAmount() : 0).sum() * 100.0) / 100.0);
            day.put("walletAmount", Math.round(dayPayments.stream().mapToDouble(p -> {
                if (p.getWalletAmount() != null && p.getWalletAmount() > 0) return p.getWalletAmount();
                if (p.getPaymentMethod() == com.driver.bookMyShow.Enums.PaymentMethod.WALLET) return p.getTotalAmount() != null ? p.getTotalAmount() : 0;
                return 0;
            }).sum() * 100.0) / 100.0);
            day.put("cardAmount", Math.round(dayPayments.stream().mapToDouble(p -> {
                if (p.getCardAmount() != null && p.getCardAmount() > 0) return p.getCardAmount();
                if (p.getPaymentMethod() != null && p.getPaymentMethod() != com.driver.bookMyShow.Enums.PaymentMethod.WALLET) return p.getTotalAmount() != null ? p.getTotalAmount() : 0;
                return 0;
            }).sum() * 100.0) / 100.0);
            dailyPaymentTrend.add(day);
        }
        analytics.put("dailyPaymentTrend", dailyPaymentTrend);

        // ---- Promo Code Usage ----
        List<Payment> promoPayments = successPayments.stream().filter(p -> p.getPromoCode() != null && !p.getPromoCode().isEmpty()).toList();
        Map<String, Object> promoAnalytics = new LinkedHashMap<>();
        promoAnalytics.put("totalPromoUsed", promoPayments.size());
        promoAnalytics.put("totalDiscount", Math.round(promoPayments.stream().mapToDouble(p -> p.getDiscountAmount() != null ? p.getDiscountAmount() : 0).sum() * 100.0) / 100.0);

        Map<String, Long> promoCounts = promoPayments.stream()
                .collect(Collectors.groupingBy(Payment::getPromoCode, Collectors.counting()));
        Map<String, Double> promoDiscounts = promoPayments.stream()
                .collect(Collectors.groupingBy(Payment::getPromoCode,
                        Collectors.summingDouble(p -> p.getDiscountAmount() != null ? p.getDiscountAmount() : 0)));

        List<Map<String, Object>> promoBreakdown = new ArrayList<>();
        for (String code : promoCounts.keySet()) {
            Map<String, Object> promo = new LinkedHashMap<>();
            promo.put("code", code);
            promo.put("usageCount", promoCounts.get(code));
            promo.put("totalDiscount", Math.round(promoDiscounts.getOrDefault(code, 0.0) * 100.0) / 100.0);
            promoBreakdown.add(promo);
        }
        promoBreakdown.sort((a, b) -> Long.compare((long) b.get("usageCount"), (long) a.get("usageCount")));
        promoAnalytics.put("promoBreakdown", promoBreakdown);
        analytics.put("promoAnalytics", promoAnalytics);

        // ---- Hourly Payment Distribution ----
        Map<Integer, Long> hourlyDist = successPayments.stream()
                .filter(p -> p.getCompletedAt() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getCompletedAt().getHour(),
                        Collectors.counting()));
        Map<Integer, Double> hourlyRevenue = successPayments.stream()
                .filter(p -> p.getCompletedAt() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getCompletedAt().getHour(),
                        Collectors.summingDouble(p -> p.getTotalAmount() != null ? p.getTotalAmount() : 0)));

        List<Map<String, Object>> hourlyPayments = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            if (hourlyDist.containsKey(h)) {
                Map<String, Object> hourData = new LinkedHashMap<>();
                hourData.put("hour", h);
                hourData.put("label", String.format("%02d:00", h));
                hourData.put("transactions", hourlyDist.get(h));
                hourData.put("revenue", Math.round(hourlyRevenue.getOrDefault(h, 0.0) * 100.0) / 100.0);
                hourlyPayments.add(hourData);
            }
        }
        analytics.put("hourlyPayments", hourlyPayments);

        // ---- Payment Status Distribution ----
        List<Map<String, Object>> statusDist = new ArrayList<>();
        Map<String, Long> statusCounts = allPayments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getStatus() != null ? p.getStatus().name() : "UNKNOWN",
                        Collectors.counting()));
        for (Map.Entry<String, Long> e : statusCounts.entrySet()) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("status", e.getKey());
            s.put("count", e.getValue());
            s.put("percentage", allPayments.size() > 0
                    ? Math.round((double) e.getValue() / allPayments.size() * 10000.0) / 100.0 : 0);
            statusDist.add(s);
        }
        analytics.put("paymentStatusDistribution", statusDist);

        // ---- Refund Analytics ----
        List<Payment> refundedPayments = allPayments.stream()
                .filter(p -> p.getRefundAmount() != null && p.getRefundAmount() > 0)
                .toList();
        Map<String, Object> refundAnalytics = new LinkedHashMap<>();
        refundAnalytics.put("totalRefundCount", refundedPayments.size());
        refundAnalytics.put("totalRefundAmount", Math.round(refundedPayments.stream().mapToDouble(p -> p.getRefundAmount()).sum() * 100.0) / 100.0);
        refundAnalytics.put("avgRefundAmount", refundedPayments.size() > 0
                ? Math.round(refundedPayments.stream().mapToDouble(p -> p.getRefundAmount()).average().orElse(0) * 100.0) / 100.0 : 0);
        analytics.put("refundAnalytics", refundAnalytics);

        return analytics;
    }
}
