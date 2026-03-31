package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Dtos.RequestDtos.CityEntryDto;
import com.driver.bookMyShow.Dtos.RequestDtos.MovieEntryDto;
import com.driver.bookMyShow.Dtos.RequestDtos.ShowEntryDto;
import com.driver.bookMyShow.Dtos.RequestDtos.TheaterEntryDto;
import com.driver.bookMyShow.Enums.PaymentStatus;
import com.driver.bookMyShow.Enums.SeatType;
import com.driver.bookMyShow.Enums.UserRole;
import com.driver.bookMyShow.Models.City;
import com.driver.bookMyShow.Models.Movie;
import com.driver.bookMyShow.Models.MovieRecommendation;
import com.driver.bookMyShow.Models.Show;
import com.driver.bookMyShow.Models.ShowSeat;
import com.driver.bookMyShow.Models.Theater;
import com.driver.bookMyShow.Models.TheaterSeat;
import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.Models.Payment;
import com.driver.bookMyShow.Models.WalletTransaction;
import com.driver.bookMyShow.Models.UserWallet;
import com.driver.bookMyShow.Repositories.*;
import com.driver.bookMyShow.modules.parking.entity.ParkingLot;
import com.driver.bookMyShow.modules.parking.entity.ParkingSlot;
import com.driver.bookMyShow.modules.parking.enums.VehicleType;
import com.driver.bookMyShow.modules.parking.repository.ParkingLotRepository;
import com.driver.bookMyShow.modules.parking.repository.ParkingSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AdminService - Provides administrative operations
 * 
 * Features:
 * - Movie management (CRUD)
 * - Theater management
 * - Show management
 * - Analytics and reporting
 * - User management
 */
@Service
public class AdminService {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private UserWalletRepository userWalletRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private TheaterSeatRepository theaterSeatRepository;

    @Autowired
    private MovieRecommendationRepository movieRecommendationRepository;

    @Autowired
    private ParkingSlotRepository parkingSlotRepository;

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    /**
     * Get comprehensive analytics dashboard data
     */
    public Map<String, Object> getDashboardAnalytics() {
        Map<String, Object> analytics = new HashMap<>();

        // User statistics
        long totalUsers = userRepository.count();
        analytics.put("totalUsers", totalUsers);

        // Movie statistics
        long totalMovies = movieRepository.count();
        analytics.put("totalMovies", totalMovies);
        analytics.put("activeMovies", totalMovies); // For now, all movies are active

        // Theater statistics
        long totalTheaters = theaterRepository.count();
        analytics.put("totalTheaters", totalTheaters);

        // Show statistics
        long totalShows = showRepository.count();
        analytics.put("totalShows", totalShows);

        // Ticket/Booking statistics
        long totalTickets = ticketRepository.count();
        analytics.put("totalTicketsBooked", totalTickets);
        analytics.put("totalBookings", totalTickets); // Same as tickets

        // Revenue statistics - TOTAL revenue from all successful payments
        List<Payment> successfulPayments = paymentRepository.findByStatus(PaymentStatus.SUCCESS);
        Double totalRevenue = successfulPayments.stream()
                .mapToDouble(Payment::getTotalAmount)
                .sum();
        analytics.put("totalRevenue", totalRevenue);
        
        // Monthly revenue
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime monthEnd = LocalDateTime.now();
        Double monthlyRevenue = paymentRepository.getTotalRevenue(monthStart, monthEnd);
        analytics.put("monthlyRevenue", monthlyRevenue != null ? monthlyRevenue : 0.0);

        // Payment statistics
        long successfulPaymentsCount = successfulPayments.size();
        long failedPayments = paymentRepository.findByStatus(PaymentStatus.FAILED).size();
        
        analytics.put("successfulPayments", successfulPaymentsCount);
        analytics.put("failedPayments", failedPayments);

        return analytics;
    }

    /**
     * Get revenue report for date range
     */
    public Map<String, Object> getRevenueReport(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> report = new HashMap<>();

        Double totalRevenue = paymentRepository.getTotalRevenue(startDate, endDate);
        report.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
        report.put("startDate", startDate);
        report.put("endDate", endDate);

        List<com.driver.bookMyShow.Models.Payment> successfulPayments = 
            paymentRepository.findByStatus(PaymentStatus.SUCCESS);
        
        report.put("transactionCount", successfulPayments.size());

        return report;
    }

    /**
     * Get all users (paginated in real implementation)
     */
    public List<com.driver.bookMyShow.Models.User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Get all movies
     */
    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    /**
     * Get all theaters
     */
    public List<Theater> getAllTheaters() {
        return theaterRepository.findAll();
    }

    public Theater getTheaterById(Integer theaterId) throws Exception {
        return theaterRepository.findById(theaterId)
                .orElseThrow(() -> new Exception("Theater not found with id: " + theaterId));
    }

    /**
     * Get all shows
     */
    public List<Show> getAllShows() {
        return showRepository.findAll();
    }

    /**
     * Delete movie (admin only)
     */
    @Transactional
    public void deleteMovie(Integer movieId) throws Exception {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new Exception("Movie not found"));
        
        // Check if movie has active shows
        if (!movie.getShows().isEmpty()) {
            throw new Exception("Cannot delete movie with active shows. Please remove shows first.");
        }

        movieRepository.delete(movie);
    }

    /**
     * Delete theater (admin only)
     */
    @Transactional
    public void deleteTheater(Integer theaterId) throws Exception {
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new Exception("Theater not found"));
        
        // Check if theater has active shows
        if (!theater.getShowList().isEmpty()) {
            throw new Exception("Cannot delete theater with active shows. Please remove shows first.");
        }

        theaterRepository.delete(theater);
    }

    /**
     * Delete show (admin only)
     */
    @Transactional
    public void deleteShow(Integer showId) throws Exception {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new Exception("Show not found"));
        
        // Check if show has bookings
        if (!show.getTicketList().isEmpty()) {
            throw new Exception("Cannot delete show with existing bookings.");
        }

        showRepository.delete(show);
    }

    /**
     * Update user status (activate/deactivate)
     */
    @Transactional
    public void updateUserStatus(Integer userId, boolean isActive) throws Exception {
        com.driver.bookMyShow.Models.User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found"));
        
        user.setIsActive(isActive);
        userRepository.save(user);
    }

    /**
     * Get popular movies (by booking count)
     */
    public List<Movie> getPopularMovies(int limit) {
        // In real implementation, use a query to sort by ticket count
        List<Movie> allMovies = movieRepository.findAll();
        allMovies.sort((m1, m2) -> {
            int count1 = m1.getShows().stream()
                    .mapToInt(show -> show.getTicketList().size())
                    .sum();
            int count2 = m2.getShows().stream()
                    .mapToInt(show -> show.getTicketList().size())
                    .sum();
            return Integer.compare(count2, count1);
        });
        
        return allMovies.stream()
                .limit(limit)
                .toList();
    }

    /**
     * Update movie (admin only)
     */
    @Transactional
    public String updateMovie(Integer movieId, MovieEntryDto movieEntryDto) throws Exception {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new Exception("Movie not found"));

        // Update fields
        if (movieEntryDto.getMovieName() != null) {
            movie.setMovieName(movieEntryDto.getMovieName());
        }
        if (movieEntryDto.getDuration() != null) {
            movie.setDuration(movieEntryDto.getDuration());
        }
        if (movieEntryDto.getGenre() != null) {
            movie.setGenre(movieEntryDto.getGenre());
        }
        if (movieEntryDto.getLanguage() != null) {
            movie.setLanguage(movieEntryDto.getLanguage());
        }
        if (movieEntryDto.getReleaseDate() != null) {
            movie.setReleaseDate(movieEntryDto.getReleaseDate());
        }
        if (movieEntryDto.getRating() != null) {
            movie.setRating(movieEntryDto.getRating());
        }

        movieRepository.save(movie);
        return "Movie updated successfully";
    }

    /**
     * Update theater (admin only)
     */
    @Transactional
    public String updateTheater(Integer theaterId, TheaterEntryDto theaterEntryDto) throws Exception {
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new Exception("Theater not found"));

        // Update fields
        if (theaterEntryDto.getName() != null) {
            theater.setName(theaterEntryDto.getName());
        }
        if (theaterEntryDto.getAddress() != null) {
            theater.setAddress(theaterEntryDto.getAddress());
        }

        theaterRepository.save(theater);
        return "Theater updated successfully";
    }

    /**
     * Update show (admin only)
     */
    @Transactional
    public String updateShow(Integer showId, ShowEntryDto showEntryDto) throws Exception {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new Exception("Show not found"));

        // Update fields
        if (showEntryDto.getShowDate() != null) {
            show.setDate(showEntryDto.getShowDate());
        }
        if (showEntryDto.getShowStartTime() != null) {
            show.setTime(showEntryDto.getShowStartTime());
        }

        showRepository.save(show);
        return "Show updated successfully";
    }

    /**
     * Get all bookings (admin view)
     */
    public List<Ticket> getAllBookings() {
        return ticketRepository.findAll();
    }

    /**
     * Get all payments (admin view)
     */
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    /**
     * Get payments by status (admin view)
     */
    public List<Payment> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }

    /**
     * Get all wallet transactions (admin view)
     */
    public List<WalletTransaction> getAllWalletTransactions() {
        return walletTransactionRepository.findAll();
    }

    /**
     * Adjust user wallet balance (admin only)
     */
    @Transactional
    public String adjustUserWallet(Integer userId, Double amount, String reason) throws Exception {
        com.driver.bookMyShow.Models.User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found"));

        // Use UserWallet for DB-centric wallet management
        UserWallet userWallet = userWalletRepository.findByUserId(userId)
                .orElseThrow(() -> new Exception("User wallet not found"));

        Double currentBalance = userWallet.getBalance();
        Double newBalance = currentBalance + amount;

        if (newBalance < 0) {
            throw new Exception("Insufficient wallet balance");
        }

        // Update wallet balance in DB
        if (amount > 0) {
            userWallet.credit(amount);
        } else {
            userWallet.debit(Math.abs(amount));
        }
        userWalletRepository.save(userWallet);

        // Also update User entity for backward compatibility
        user.setWalletBalance(newBalance);
        userRepository.save(user);

        return String.format("Wallet adjusted: %s ₹%.2f. New balance: ₹%.2f", 
            amount > 0 ? "Added" : "Deducted", Math.abs(amount), newBalance);
    }

    // =====================================================
    // CITY MANAGEMENT
    // =====================================================

    /**
     * Get all cities
     */
    public List<City> getAllCities() {
        return cityRepository.findAll();
    }

    /**
     * Add new city
     */
    @Transactional
    public String addCity(CityEntryDto cityEntryDto) throws Exception {
        // Check if city already exists
        if (cityRepository.findByName(cityEntryDto.getName()).isPresent()) {
            throw new Exception("City with this name already exists");
        }

        City city = City.builder()
                .name(cityEntryDto.getName())
                .state(cityEntryDto.getState())
                .country(cityEntryDto.getCountry())
                .build();

        cityRepository.save(city);
        return "City added successfully";
    }

    /**
     * Update city
     */
    @Transactional
    public String updateCity(Integer cityId, CityEntryDto cityEntryDto) throws Exception {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new Exception("City not found"));

        if (cityEntryDto.getName() != null) {
            city.setName(cityEntryDto.getName());
        }
        if (cityEntryDto.getState() != null) {
            city.setState(cityEntryDto.getState());
        }
        if (cityEntryDto.getCountry() != null) {
            city.setCountry(cityEntryDto.getCountry());
        }

        cityRepository.save(city);
        return "City updated successfully";
    }

    /**
     * Delete city
     */
    @Transactional
    public void deleteCity(Integer cityId) throws Exception {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new Exception("City not found"));

        // Check if city has theaters
        if (!city.getTheaters().isEmpty()) {
            throw new Exception("Cannot delete city with associated theaters. Please remove theaters first.");
        }

        cityRepository.delete(city);
    }

    /**
     * Delete user (admin only)
     */
    @Transactional
    public void deleteUser(Integer userId) throws Exception {
        com.driver.bookMyShow.Models.User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found"));

        // Check if user is admin
        if (user.getRole().toString().equals("ADMIN")) {
            throw new Exception("Cannot delete admin users");
        }

        userRepository.delete(user);
    }

    // =====================================================
    // THEATER SEAT MANAGEMENT (Admin dynamic seat management)
    // =====================================================

    /**
     * Get all seats for a theater
     */
    public List<TheaterSeat> getTheaterSeats(Integer theaterId) throws Exception {
        theaterRepository.findById(theaterId)
                .orElseThrow(() -> new Exception("Theater not found"));
        return theaterSeatRepository.findByTheaterId(theaterId);
    }

    /**
     * Add seats to a theater in bulk (by row)
     * @param theaterId Theater ID
     * @param rowPrefix Row letter prefix (e.g. "A", "B")
     * @param seatType Seat type (GOLD, SILVER, PREMIUM, COUPLE, CLASSIC)
     * @param count Number of seats in this row
     */
    @Transactional
    public String addTheaterSeatsRow(Integer theaterId, String rowPrefix, SeatType seatType, int count) throws Exception {
        // Couple seats must always be added in even numbers (they come in pairs)
        if (seatType == SeatType.COUPLE && count % 2 != 0) {
            throw new Exception("Couple seats must be added in even numbers (pairs). Requested: " + count);
        }
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new Exception("Theater not found"));

        List<TheaterSeat> existingSeats = theaterSeatRepository.findByTheaterId(theaterId);

        int added = 0;
        for (int i = 1; i <= count; i++) {
            String seatNo = rowPrefix + i;
            // Skip if a seat with this number already exists
            boolean exists = existingSeats.stream().anyMatch(s -> s.getSeatNo().equals(seatNo));
            if (!exists) {
                TheaterSeat seat = TheaterSeat.builder()
                        .seatNo(seatNo)
                        .seatType(seatType)
                        .theater(theater)
                        .build();
                theaterSeatRepository.save(seat);
                added++;
            }
        }

        return "Added " + added + " " + seatType + " seats (row " + rowPrefix + ") to theater: " + theater.getName();
    }

    /**
     * Delete a theater seat
     */
    @Transactional
    public void deleteTheaterSeat(Integer theaterId, Integer seatId) throws Exception {
        theaterRepository.findById(theaterId)
                .orElseThrow(() -> new Exception("Theater not found"));

        TheaterSeat seat = theaterSeatRepository.findById(seatId)
                .orElseThrow(() -> new Exception("Theater seat not found"));

        if (!seat.getTheater().getId().equals(theaterId)) {
            throw new Exception("Seat does not belong to this theater");
        }

        theaterSeatRepository.delete(seat);
    }

    // =====================================================
    // SHOW SEAT MANAGEMENT (Admin dynamic seat management)
    // =====================================================

    /**
     * Get all seats for a show
     */
    public List<ShowSeat> getShowSeats(Integer showId) throws Exception {
        showRepository.findById(showId)
                .orElseThrow(() -> new Exception("Show not found"));
        return showSeatRepository.findByShowId(showId);
    }

    /**
     * Add a single seat to a show
     */
    @Transactional
    public ShowSeat addShowSeat(Integer showId, String seatNo, SeatType seatType, Integer price) throws Exception {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new Exception("Show not found"));

        // Check for duplicate seat number in this show
        List<ShowSeat> existing = showSeatRepository.findBySeatNoAndShow(seatNo, show);
        if (!existing.isEmpty()) {
            throw new Exception("Seat " + seatNo + " already exists for this show");
        }

        ShowSeat showSeat = ShowSeat.builder()
                .seatNo(seatNo)
                .seatType(seatType)
                .price(price)
                .isAvailable(true)
                .isFoodContains(false)
                .show(show)
                .build();

        return showSeatRepository.save(showSeat);
    }

    /**
     * Add seats to a show in bulk (by row)
     */
    @Transactional
    public String addShowSeatsRow(Integer showId, String rowPrefix, SeatType seatType, int count, int price) throws Exception {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new Exception("Show not found"));

        List<ShowSeat> existingSeats = showSeatRepository.findByShowId(showId);
        int added = 0;

        for (int i = 1; i <= count; i++) {
            String seatNo = rowPrefix + i;
            boolean exists = existingSeats.stream().anyMatch(s -> s.getSeatNo().equals(seatNo));
            if (!exists) {
                ShowSeat seat = ShowSeat.builder()
                        .seatNo(seatNo)
                        .seatType(seatType)
                        .price(price)
                        .isAvailable(true)
                        .isFoodContains(false)
                        .show(show)
                        .build();
                showSeatRepository.save(seat);
                added++;
            }
        }

        return "Added " + added + " " + seatType + " seats (row " + rowPrefix + ", ₹" + price + " each) to show #" + showId;
    }

    /**
     * Generate show seats from theater seats (copies theater layout to show with pricing)
     */
    @Transactional
    public String generateShowSeatsFromTheater(Integer showId) throws Exception {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new Exception("Show not found"));

        // Check if show already has seats
        long existingCount = showSeatRepository.countByShow(show);
        if (existingCount > 0) {
            throw new Exception("Show already has " + existingCount + " seats. Delete existing seats first or add individually.");
        }

        List<TheaterSeat> theaterSeats = theaterSeatRepository.findByTheaterId(show.getTheater().getId());
        if (theaterSeats.isEmpty()) {
            throw new Exception("Theater has no seats configured. Add theater seats first via the admin panel.");
        }

        int totalAdded = 0;
        for (TheaterSeat ts : theaterSeats) {
            int price = getDefaultPrice(ts.getSeatType());

            ShowSeat showSeat = ShowSeat.builder()
                    .seatNo(ts.getSeatNo())
                    .seatType(ts.getSeatType())
                    .price(price)
                    .isAvailable(true)
                    .isFoodContains(false)
                    .show(show)
                    .build();
            showSeatRepository.save(showSeat);
            totalAdded++;
        }

        return "Generated " + totalAdded + " show seats from theater layout (Theater: " + show.getTheater().getName() + ")";
    }

    /**
     * Delete a show seat (only if it hasn't been booked)
     */
    @Transactional
    public void deleteShowSeat(Integer showId, Integer seatId) throws Exception {
        showRepository.findById(showId)
                .orElseThrow(() -> new Exception("Show not found"));

        ShowSeat seat = showSeatRepository.findById(seatId)
                .orElseThrow(() -> new Exception("Show seat not found"));

        if (!seat.getShow().getId().equals(showId)) {
            throw new Exception("Seat does not belong to this show");
        }

        if (!seat.getIsAvailable()) {
            throw new Exception("Cannot delete a booked seat. Cancel the booking first.");
        }

        showSeatRepository.delete(seat);
    }

    /**
     * Toggle show seat availability (admin override)
     */
    @Transactional
    public ShowSeat toggleShowSeatAvailability(Integer showId, Integer seatId) throws Exception {
        showRepository.findById(showId)
                .orElseThrow(() -> new Exception("Show not found"));

        ShowSeat seat = showSeatRepository.findById(seatId)
                .orElseThrow(() -> new Exception("Show seat not found"));

        if (!seat.getShow().getId().equals(showId)) {
            throw new Exception("Seat does not belong to this show");
        }

        seat.setIsAvailable(!seat.getIsAvailable());
        return showSeatRepository.save(seat);
    }

    /**
     * Update show seat price
     */
    @Transactional
    public ShowSeat updateShowSeatPrice(Integer showId, Integer seatId, Integer newPrice) throws Exception {
        showRepository.findById(showId)
                .orElseThrow(() -> new Exception("Show not found"));

        ShowSeat seat = showSeatRepository.findById(seatId)
                .orElseThrow(() -> new Exception("Show seat not found"));

        if (!seat.getShow().getId().equals(showId)) {
            throw new Exception("Seat does not belong to this show");
        }

        seat.setPrice(newPrice);
        return showSeatRepository.save(seat);
    }

    /**
     * Default pricing by seat type
     */
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
    // THEATRE ADMIN MANAGEMENT
    // =====================================================

    /**
     * Get all users with THEATER_OWNER role along with their assigned theatres
     */
    public List<Map<String, Object>> getAllTheatreAdmins() {
        List<User> theatreAdmins = userRepository.findByRole(UserRole.THEATER_OWNER);
        List<Map<String, Object>> result = new ArrayList<>();

        for (User admin : theatreAdmins) {
            Map<String, Object> adminInfo = new HashMap<>();
            adminInfo.put("id", admin.getId());
            adminInfo.put("name", admin.getName());
            adminInfo.put("email", admin.getEmailId());
            adminInfo.put("mobile", admin.getMobileNo());

            // Find theatre(s) assigned to this admin
            List<Theater> assignedTheatres = theaterRepository.findAllByAdminId(admin.getId());
            if (!assignedTheatres.isEmpty()) {
                Theater t = assignedTheatres.get(0);
                Map<String, Object> theatreInfo = new HashMap<>();
                theatreInfo.put("id", t.getId());
                theatreInfo.put("name", t.getName());
                theatreInfo.put("address", t.getAddress());
                theatreInfo.put("cityName", t.getCityName());
                adminInfo.put("assignedTheatre", theatreInfo);
            } else {
                adminInfo.put("assignedTheatre", null);
            }

            result.add(adminInfo);
        }
        return result;
    }

    /**
     * Assign a THEATER_OWNER user as admin of a specific theatre
     */
    @Transactional
    public String assignTheatreAdmin(Integer theaterId, Integer adminUserId) throws Exception {
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new Exception("Theater not found with id: " + theaterId));

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new Exception("User not found with id: " + adminUserId));

        if (adminUser.getRole() != UserRole.THEATER_OWNER) {
            throw new Exception("User '" + adminUser.getName() + "' does not have THEATER_OWNER role. Current role: " + adminUser.getRole());
        }

        // Check if this user is already assigned to another theatre
        List<Theater> existingAssignments = theaterRepository.findAllByAdminId(adminUserId);
        for (Theater existing : existingAssignments) {
            if (!existing.getId().equals(theaterId)) {
                throw new Exception("User '" + adminUser.getName() + "' is already assigned to theatre: " + existing.getName());
            }
        }

        theater.setAdmin(adminUser);
        theaterRepository.save(theater);

        return "Successfully assigned '" + adminUser.getName() + "' as admin of theatre: " + theater.getName();
    }

    /**
     * Remove the admin from a theatre
     */
    @Transactional
    public String removeTheatreAdmin(Integer theaterId) throws Exception {
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new Exception("Theater not found with id: " + theaterId));

        if (theater.getAdmin() == null) {
            throw new Exception("Theatre '" + theater.getName() + "' does not have an assigned admin");
        }

        String adminName = theater.getAdmin().getName();
        theater.setAdmin(null);
        theaterRepository.save(theater);

        return "Removed admin '" + adminName + "' from theatre: " + theater.getName();
    }

    // =====================================================
    // MOVIE RECOMMENDATIONS
    // =====================================================

    /**
     * Recommend a movie to a specific theatre
     */
    @Transactional
    public String recommendMovieToTheatre(Integer movieId, Integer theaterId, Integer adminUserId, String message) throws Exception {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new Exception("Movie not found with id: " + movieId));

        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new Exception("Theater not found with id: " + theaterId));

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new Exception("Admin user not found"));

        // Check if recommendation already exists
        Optional<MovieRecommendation> existing = movieRecommendationRepository.findByMovieIdAndTheaterId(movieId, theaterId);
        if (existing.isPresent()) {
            MovieRecommendation rec = existing.get();
            if ("PENDING".equals(rec.getStatus())) {
                throw new Exception("A pending recommendation for '" + movie.getMovieName() + "' already exists for theatre: " + theater.getName());
            }
            // If previously rejected/accepted, allow a new recommendation by updating
            rec.setStatus("PENDING");
            rec.setAdminMessage(message);
            rec.setTheatreAdminResponse(null);
            rec.setRecommendedBy(admin);
            movieRecommendationRepository.save(rec);
            return "Re-recommended '" + movie.getMovieName() + "' to theatre: " + theater.getName();
        }

        MovieRecommendation recommendation = MovieRecommendation.builder()
                .movie(movie)
                .theater(theater)
                .recommendedBy(admin)
                .adminMessage(message)
                .status("PENDING")
                .build();

        movieRecommendationRepository.save(recommendation);
        return "Recommended '" + movie.getMovieName() + "' to theatre: " + theater.getName();
    }

    /**
     * Get all movie recommendations with their statuses
     */
    public List<Map<String, Object>> getAllRecommendations() {
        List<MovieRecommendation> recommendations = movieRecommendationRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (MovieRecommendation rec : recommendations) {
            Map<String, Object> recInfo = new HashMap<>();
            recInfo.put("id", rec.getId());
            recInfo.put("status", rec.getStatus());
            recInfo.put("adminMessage", rec.getAdminMessage());
            recInfo.put("theatreAdminResponse", rec.getTheatreAdminResponse());
            recInfo.put("createdAt", rec.getCreatedAt());
            recInfo.put("updatedAt", rec.getUpdatedAt());

            // Movie info
            Map<String, Object> movieInfo = new HashMap<>();
            movieInfo.put("id", rec.getMovie().getId());
            movieInfo.put("name", rec.getMovie().getMovieName());
            movieInfo.put("genre", rec.getMovie().getGenre());
            recInfo.put("movie", movieInfo);

            // Theater info
            Map<String, Object> theaterInfo = new HashMap<>();
            theaterInfo.put("id", rec.getTheater().getId());
            theaterInfo.put("name", rec.getTheater().getName());
            theaterInfo.put("cityName", rec.getTheater().getCityName());
            recInfo.put("theater", theaterInfo);

            // Recommended by info
            if (rec.getRecommendedBy() != null) {
                Map<String, Object> byInfo = new HashMap<>();
                byInfo.put("id", rec.getRecommendedBy().getId());
                byInfo.put("name", rec.getRecommendedBy().getName());
                recInfo.put("recommendedBy", byInfo);
            }

            result.add(recInfo);
        }
        return result;
    }

    /**
     * Recommend a movie to all theatres in a city
     */
    @Transactional
    public String recommendMovieToCity(Integer movieId, Integer cityId, Integer adminUserId, String message) throws Exception {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new Exception("Movie not found with id: " + movieId));

        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new Exception("City not found with id: " + cityId));

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new Exception("Admin user not found"));

        List<Theater> theatersInCity = theaterRepository.findByCityId(cityId);
        if (theatersInCity.isEmpty()) {
            throw new Exception("No theatres found in city: " + city.getName());
        }

        int recommended = 0;
        int skipped = 0;
        for (Theater theater : theatersInCity) {
            Optional<MovieRecommendation> existing = movieRecommendationRepository.findByMovieIdAndTheaterId(movieId, theater.getId());
            if (existing.isPresent() && "PENDING".equals(existing.get().getStatus())) {
                skipped++;
                continue;
            }

            if (existing.isPresent()) {
                // Update existing
                MovieRecommendation rec = existing.get();
                rec.setStatus("PENDING");
                rec.setAdminMessage(message);
                rec.setTheatreAdminResponse(null);
                rec.setRecommendedBy(admin);
                movieRecommendationRepository.save(rec);
            } else {
                MovieRecommendation recommendation = MovieRecommendation.builder()
                        .movie(movie)
                        .theater(theater)
                        .recommendedBy(admin)
                        .adminMessage(message)
                        .status("PENDING")
                        .build();
                movieRecommendationRepository.save(recommendation);
            }
            recommended++;
        }

        return "Recommended '" + movie.getMovieName() + "' to " + recommended + " theatres in " + city.getName()
                + (skipped > 0 ? " (" + skipped + " already had pending recommendations)" : "");
    }

    // =====================================================
    // NEW: User Detail Analytics
    // =====================================================
    @Transactional(readOnly = true)
    public Map<String, Object> getUserDetailAnalytics(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("userId", user.getId());
        analytics.put("name", user.getName());
        analytics.put("email", user.getEmailId());
        analytics.put("role", user.getRole().name());
        analytics.put("walletBalance", user.getWalletBalance());
        analytics.put("isActive", user.getIsActive());
        analytics.put("createdAt", user.getCreatedAt());

        List<Ticket> tickets = ticketRepository.findByUserOrderByBookedAtDesc(user);
        analytics.put("totalBookings", tickets.size());
        long activeBookings = tickets.stream().filter(t -> t.getCancelledAt() == null).count();
        long cancelledBookings = tickets.size() - activeBookings;
        analytics.put("activeBookings", activeBookings);
        analytics.put("cancelledBookings", cancelledBookings);

        double totalSpend = tickets.stream()
                .mapToDouble(t -> t.getTotalTicketsPrice() != null ? t.getTotalTicketsPrice() : 0)
                .sum();
        analytics.put("totalSpending", totalSpend);

        List<Payment> userPayments = paymentRepository.findAll().stream()
                .filter(p -> p.getUser() != null && p.getUser().getId().equals(userId))
                .collect(java.util.stream.Collectors.toList());

        double walletPaid = userPayments.stream()
                .mapToDouble(p -> p.getWalletAmount() != null ? p.getWalletAmount() : 0).sum();
        double cardPaid = userPayments.stream()
                .mapToDouble(p -> p.getCardAmount() != null ? p.getCardAmount() : 0).sum();
        analytics.put("walletPaid", walletPaid);
        analytics.put("cardPaid", cardPaid);

        // Recent bookings (last 10)
        List<Map<String, Object>> recentBookings = new ArrayList<>();
        tickets.stream()
                .sorted((a, b) -> {
                    if (a.getBookedAt() == null && b.getBookedAt() == null) return 0;
                    if (a.getBookedAt() == null) return 1;
                    if (b.getBookedAt() == null) return -1;
                    return b.getBookedAt().compareTo(a.getBookedAt());
                })
                .limit(10)
                .forEach(t -> {
                    Map<String, Object> bm = new HashMap<>();
                    bm.put("ticketId", t.getId());
                    bm.put("movieName", t.getShow() != null && t.getShow().getMovie() != null ? t.getShow().getMovie().getMovieName() : "N/A");
                    bm.put("theaterName", t.getShow() != null && t.getShow().getTheater() != null ? t.getShow().getTheater().getName() : "N/A");
                    bm.put("amount", t.getTotalTicketsPrice());
                    bm.put("bookedAt", t.getBookedAt());
                    recentBookings.add(bm);
                });
        analytics.put("recentBookings", recentBookings);

        return analytics;
    }

    // =====================================================
    // NEW: Parking Price by Theatre (bulk)
    // =====================================================
    @Transactional
    public String updateParkingPriceByTheater(Integer theaterId, String vehicleType, Integer hourlyRate) {
        // Auto-create ParkingLot and default slots if none exist for this theatre
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new RuntimeException("Theatre not found with ID: " + theaterId));
        
        Optional<ParkingLot> existingLot = parkingLotRepository.findByTheaterId(theaterId);
        if (existingLot.isEmpty()) {
            // Create a parking lot with default slots for this theatre
            ParkingLot lot = ParkingLot.builder()
                    .name(theater.getName() + " Parking")
                    .totalSlots(30)
                    .availableSlots(30)
                    .theater(theater)
                    .build();
            lot = parkingLotRepository.save(lot);
            
            // Create default slots: 10 TWO_WHEELER, 15 FOUR_WHEELER, 5 SUV
            List<ParkingSlot> defaultSlots = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                defaultSlots.add(ParkingSlot.builder()
                        .slotNumber("TW-" + i)
                        .vehicleType(VehicleType.TWO_WHEELER)
                        .hourlyRate(hourlyRate)
                        .isOccupied(false)
                        .parkingLot(lot)
                        .build());
            }
            for (int i = 1; i <= 15; i++) {
                defaultSlots.add(ParkingSlot.builder()
                        .slotNumber("FW-" + i)
                        .vehicleType(VehicleType.FOUR_WHEELER)
                        .hourlyRate(hourlyRate)
                        .isOccupied(false)
                        .parkingLot(lot)
                        .build());
            }
            for (int i = 1; i <= 5; i++) {
                defaultSlots.add(ParkingSlot.builder()
                        .slotNumber("EV-" + i)
                        .vehicleType(VehicleType.EV)
                        .hourlyRate(hourlyRate)
                        .isOccupied(false)
                        .parkingLot(lot)
                        .build());
            }
            parkingSlotRepository.saveAll(defaultSlots);
            return "Created parking lot with 30 slots for " + theater.getName() + " and set hourly rate to ₹" + hourlyRate;
        }
        
        List<ParkingSlot> slots;
        if ("ALL".equalsIgnoreCase(vehicleType)) {
            slots = parkingSlotRepository.findByTheaterId(theaterId);
        } else {
            VehicleType vt = VehicleType.valueOf(vehicleType.toUpperCase());
            slots = parkingSlotRepository.findByTheaterIdAndVehicleType(theaterId, vt);
        }
        if (slots.isEmpty()) {
            return "No parking slots found for this theatre. Please add parking lots and slots for this theatre first.";
        }
        for (ParkingSlot slot : slots) {
            slot.setHourlyRate(hourlyRate);
        }
        parkingSlotRepository.saveAll(slots);
        return "Updated hourly rate to ₹" + hourlyRate + " for " + slots.size() + " slots";
    }

    // =====================================================
    // NEW: Detailed Bookings
    // =====================================================
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllBookingsDetailed() {
        List<Ticket> tickets = ticketRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        tickets.stream()
                .sorted((a, b) -> {
                    if (a.getBookedAt() == null && b.getBookedAt() == null) return 0;
                    if (a.getBookedAt() == null) return 1;
                    if (b.getBookedAt() == null) return -1;
                    return b.getBookedAt().compareTo(a.getBookedAt());
                })
                .forEach(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", t.getId());
                    m.put("userName", t.getUser() != null ? t.getUser().getName() : "N/A");
                    m.put("userEmail", t.getUser() != null ? t.getUser().getEmailId() : "N/A");
                    m.put("movieName", t.getShow() != null && t.getShow().getMovie() != null ? t.getShow().getMovie().getMovieName() : "N/A");
                    m.put("theaterName", t.getShow() != null && t.getShow().getTheater() != null ? t.getShow().getTheater().getName() : "N/A");
                    m.put("cityName", t.getShow() != null && t.getShow().getTheater() != null && t.getShow().getTheater().getCity() != null ? t.getShow().getTheater().getCity().getName() : "N/A");
                    m.put("showDate", t.getShow() != null ? t.getShow().getDate() : null);
                    m.put("showTime", t.getShow() != null ? t.getShow().getTime() : null);
                    m.put("totalAmount", t.getTotalTicketsPrice());
                    m.put("status", t.getCancelledAt() == null ? "CONFIRMED" : "CANCELLED");
                    m.put("bookedAt", t.getBookedAt());
                    m.put("seatCount", t.getBookedSeats() != null ? t.getBookedSeats().split(",").length : 0);
                    result.add(m);
                });
        return result;
    }

    // =====================================================
    // NEW: Detailed Payments with split info
    // =====================================================
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllPaymentsDetailed(String statusFilter) {
        List<Payment> allPayments = paymentRepository.findAll();
        if (statusFilter != null && !statusFilter.isEmpty()) {
            try {
                PaymentStatus ps = PaymentStatus.valueOf(statusFilter.toUpperCase());
                allPayments = allPayments.stream()
                        .filter(p -> p.getStatus() == ps)
                        .collect(java.util.stream.Collectors.toList());
            } catch (Exception ignored) {}
        }
        List<Map<String, Object>> result = new ArrayList<>();
        allPayments.stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .forEach(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", p.getId());
                    m.put("userName", p.getUser() != null ? p.getUser().getName() : "N/A");
                    m.put("amount", p.getTotalAmount());
                    m.put("baseAmount", p.getBaseAmount());
                    m.put("tax", p.getTax());
                    m.put("convenienceFee", p.getConvenienceFee());
                    m.put("walletAmount", p.getWalletAmount());
                    m.put("cardAmount", p.getCardAmount());
                    m.put("discountAmount", p.getDiscountAmount());
                    m.put("promoCode", p.getPromoCode());
                    m.put("paymentMethod", p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "N/A");
                    m.put("status", p.getStatus() != null ? p.getStatus().name() : "PENDING");
                    m.put("createdAt", p.getCreatedAt());
                    result.add(m);
                });
        return result;
    }
}
