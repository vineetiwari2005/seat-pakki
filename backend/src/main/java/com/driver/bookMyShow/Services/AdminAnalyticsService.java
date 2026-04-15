package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Enums.PaymentStatus;
import com.driver.bookMyShow.Enums.TicketStatus;
import com.driver.bookMyShow.Models.*;
import com.driver.bookMyShow.Repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AdminAnalyticsService - Provides aggregation-based analytics
 * 
 * All data is derived from existing DB tables using aggregation queries.
 * No schema changes, no business logic changes.
 */
@Service
public class AdminAnalyticsService {

    @Autowired private MovieRepository movieRepository;
    @Autowired private TheaterRepository theaterRepository;
    @Autowired private ShowRepository showRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private ShowSeatRepository showSeatRepository;
    @Autowired private MovieRecommendationRepository movieRecommendationRepository;

    // =====================================================
    // CITY ANALYTICS
    // =====================================================

    /**
     * Get analytics for all cities: theatre count, show count, booking count, revenue
     */
    public List<Map<String, Object>> getCityAnalytics() {
        List<City> cities = cityRepository.findAll();
        List<Theater> allTheaters = theaterRepository.findAll();
        List<Show> allShows = showRepository.findAll();
        List<Ticket> allTickets = ticketRepository.findAll();
        List<Payment> successPayments = paymentRepository.findByStatus(PaymentStatus.SUCCESS);

        List<Map<String, Object>> result = new ArrayList<>();

        for (City city : cities) {
            Map<String, Object> cityData = new LinkedHashMap<>();
            cityData.put("cityId", city.getId());
            cityData.put("cityName", city.getName());
            cityData.put("state", city.getState());
            cityData.put("country", city.getCountry());

            // Theatres in this city  
            List<Theater> cityTheaters = allTheaters.stream()
                    .filter(t -> t.getCity() != null && t.getCity().getId().equals(city.getId()))
                    .toList();
            cityData.put("theaterCount", cityTheaters.size());

            // Shows in this city's theatres
            Set<Integer> cityTheaterIds = cityTheaters.stream()
                    .map(Theater::getId).collect(Collectors.toSet());
            List<Show> cityShows = allShows.stream()
                    .filter(s -> s.getTheater() != null && cityTheaterIds.contains(s.getTheater().getId()))
                    .toList();
            cityData.put("showCount", cityShows.size());

            // Bookings in this city
            Set<Integer> cityShowIds = cityShows.stream()
                    .map(Show::getId).collect(Collectors.toSet());
            long cityBookings = allTickets.stream()
                    .filter(t -> t.getShow() != null && cityShowIds.contains(t.getShow().getId()))
                    .count();
            cityData.put("bookingCount", cityBookings);

            // Revenue from this city
            double cityRevenue = successPayments.stream()
                    .filter(p -> p.getTicket() != null && p.getTicket().getShow() != null 
                            && cityShowIds.contains(p.getTicket().getShow().getId()))
                    .mapToDouble(p -> p.getTotalAmount() != null ? p.getTotalAmount() : 0)
                    .sum();
            cityData.put("totalRevenue", cityRevenue);

            // Active users (users who booked in this city)
            long activeUsers = allTickets.stream()
                    .filter(t -> t.getShow() != null && cityShowIds.contains(t.getShow().getId()))
                    .map(t -> t.getUser() != null ? t.getUser().getId() : -1)
                    .distinct().count();
            cityData.put("activeUsers", activeUsers);

            result.add(cityData);
        }

        // Also add theatres with cityName string that aren't linked to City entity
        Set<Integer> linkedTheaterIds = allTheaters.stream()
                .filter(t -> t.getCity() != null)
                .map(Theater::getId)
                .collect(Collectors.toSet());
        
        Map<String, List<Theater>> unlinkedByCity = allTheaters.stream()
                .filter(t -> t.getCity() == null && t.getCityName() != null)
                .collect(Collectors.groupingBy(Theater::getCityName));

        for (Map.Entry<String, List<Theater>> entry : unlinkedByCity.entrySet()) {
            // Check if this city name already exists in our results
            boolean alreadyExists = result.stream()
                    .anyMatch(r -> entry.getKey().equalsIgnoreCase((String) r.get("cityName")));
            if (!alreadyExists) {
                Map<String, Object> cityData = new LinkedHashMap<>();
                cityData.put("cityId", null);
                cityData.put("cityName", entry.getKey());
                cityData.put("state", "");
                cityData.put("country", "");
                cityData.put("theaterCount", entry.getValue().size());

                Set<Integer> theaterIds = entry.getValue().stream()
                        .map(Theater::getId).collect(Collectors.toSet());
                long showCount = allShows.stream()
                        .filter(s -> s.getTheater() != null && theaterIds.contains(s.getTheater().getId()))
                        .count();
                cityData.put("showCount", showCount);

                Set<Integer> showIds = allShows.stream()
                        .filter(s -> s.getTheater() != null && theaterIds.contains(s.getTheater().getId()))
                        .map(Show::getId).collect(Collectors.toSet());
                long bookings = allTickets.stream()
                        .filter(t -> t.getShow() != null && showIds.contains(t.getShow().getId()))
                        .count();
                cityData.put("bookingCount", bookings);

                double revenue = successPayments.stream()
                        .filter(p -> p.getTicket() != null && p.getTicket().getShow() != null
                                && showIds.contains(p.getTicket().getShow().getId()))
                        .mapToDouble(p -> p.getTotalAmount() != null ? p.getTotalAmount() : 0)
                        .sum();
                cityData.put("totalRevenue", revenue);
                cityData.put("activeUsers", 0);
                result.add(cityData);
            }
        }

        return result;
    }

    // =====================================================
    // MOVIE ANALYTICS
    // =====================================================

    /**
     * Get analytics for all movies with optional filters
     */
    public List<Map<String, Object>> getMovieAnalytics(String genre, String language, String dateFrom, String dateTo) {
        List<Movie> movies = movieRepository.findAll();
        List<Ticket> allTickets = ticketRepository.findAll();
        List<Payment> successPayments = paymentRepository.findByStatus(PaymentStatus.SUCCESS);

        // Apply filters
        if (genre != null && !genre.isEmpty()) {
            movies = movies.stream()
                    .filter(m -> m.getGenre() != null && m.getGenre().name().equalsIgnoreCase(genre))
                    .toList();
        }
        if (language != null && !language.isEmpty()) {
            movies = movies.stream()
                    .filter(m -> m.getLanguage() != null && m.getLanguage().name().equalsIgnoreCase(language))
                    .toList();
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Movie movie : movies) {
            Map<String, Object> movieData = new LinkedHashMap<>();
            movieData.put("movieId", movie.getId());
            movieData.put("movieName", movie.getMovieName());
            movieData.put("genre", movie.getGenre() != null ? movie.getGenre().name() : null);
            movieData.put("language", movie.getLanguage() != null ? movie.getLanguage().name() : null);
            movieData.put("rating", movie.getRating());
            movieData.put("releaseDate", movie.getReleaseDate());
            movieData.put("posterUrl", movie.getPosterUrl());
            movieData.put("nowShowing", movie.getNowShowing());

            // Shows for this movie
            List<Show> movieShows = movie.getShows() != null ? movie.getShows() : Collections.emptyList();

            // Apply date filter on shows
            if (dateFrom != null && !dateFrom.isEmpty()) {
                LocalDate from = LocalDate.parse(dateFrom);
                movieShows = movieShows.stream()
                        .filter(s -> s.getDate() != null && !s.getDate().toLocalDate().isBefore(from))
                        .toList();
            }
            if (dateTo != null && !dateTo.isEmpty()) {
                LocalDate to = LocalDate.parse(dateTo);
                movieShows = movieShows.stream()
                        .filter(s -> s.getDate() != null && !s.getDate().toLocalDate().isAfter(to))
                        .toList();
            }

            movieData.put("totalShows", movieShows.size());

            // Tickets for this movie's shows
            Set<Integer> showIds = movieShows.stream().map(Show::getId).collect(Collectors.toSet());
            List<Ticket> movieTickets = allTickets.stream()
                    .filter(t -> t.getShow() != null && showIds.contains(t.getShow().getId()))
                    .toList();

            long bookedCount = movieTickets.stream()
                    .filter(t -> t.getStatus() == TicketStatus.BOOKED).count();
            long cancelledCount = movieTickets.stream()
                    .filter(t -> t.getStatus() == TicketStatus.CANCELLED).count();
            movieData.put("totalBookings", movieTickets.size());
            movieData.put("bookedCount", bookedCount);
            movieData.put("cancelledCount", cancelledCount);

            // Total seats for this movie's shows and occupancy
            long totalSeats = 0;
            long bookedSeats = 0;
            for (Show show : movieShows) {
                List<ShowSeat> seats = show.getShowSeatList();
                if (seats != null) {
                    totalSeats += seats.size();
                    bookedSeats += seats.stream().filter(s -> !s.getIsAvailable()).count();
                }
            }
            movieData.put("totalSeats", totalSeats);
            movieData.put("bookedSeats", bookedSeats);
            movieData.put("occupancyRate", totalSeats > 0 ? Math.round((double) bookedSeats / totalSeats * 100.0 * 10) / 10.0 : 0);

            // Revenue
            double movieRevenue = successPayments.stream()
                    .filter(p -> p.getTicket() != null && p.getTicket().getShow() != null
                            && showIds.contains(p.getTicket().getShow().getId()))
                    .mapToDouble(p -> p.getTotalAmount() != null ? p.getTotalAmount() : 0)
                    .sum();
            movieData.put("totalRevenue", movieRevenue);

            // Average ticket price
            double avgPrice = movieTickets.stream()
                    .filter(t -> t.getTotalTicketsPrice() != null)
                    .mapToInt(Ticket::getTotalTicketsPrice)
                    .average().orElse(0);
            movieData.put("avgTicketPrice", Math.round(avgPrice * 100.0) / 100.0);

            // Theatres showing this movie
            long theatreCount = movieShows.stream()
                    .map(s -> s.getTheater() != null ? s.getTheater().getId() : -1)
                    .distinct().count();
            movieData.put("theaterCount", theatreCount);

            result.add(movieData);
        }

        // Sort by revenue desc
        result.sort((a, b) -> Double.compare(
                (double) b.getOrDefault("totalRevenue", 0.0),
                (double) a.getOrDefault("totalRevenue", 0.0)));

        return result;
    }

    /**
     * Compare specific movies side by side
     */
    public List<Map<String, Object>> compareMovies(List<Integer> movieIds) {
        if (movieIds == null || movieIds.isEmpty()) {
            return getMovieAnalytics(null, null, null, null);
        }
        List<Map<String, Object>> allAnalytics = getMovieAnalytics(null, null, null, null);
        return allAnalytics.stream()
                .filter(m -> movieIds.contains((Integer) m.get("movieId")))
                .toList();
    }

    // =====================================================
    // THEATRE PERFORMANCE RANKING
    // =====================================================

    /**
     * Get theatre rankings based on revenue, occupancy, bookings
     */
    public List<Map<String, Object>> getTheaterRankings(String sortBy) {
        List<Theater> theaters = theaterRepository.findAll();
        List<Show> allShows = showRepository.findAll();
        List<Ticket> allTickets = ticketRepository.findAll();
        List<Payment> successPayments = paymentRepository.findByStatus(PaymentStatus.SUCCESS);

        List<Map<String, Object>> result = new ArrayList<>();

        for (Theater theater : theaters) {
            Map<String, Object> theaterData = new LinkedHashMap<>();
            theaterData.put("theaterId", theater.getId());
            theaterData.put("theaterName", theater.getName());
            theaterData.put("address", theater.getAddress());
            theaterData.put("cityName", theater.getCity() != null ? theater.getCity().getName() 
                    : (theater.getCityName() != null ? theater.getCityName() : "Unknown"));

            // Shows in this theater
            List<Show> theaterShows = allShows.stream()
                    .filter(s -> s.getTheater() != null && s.getTheater().getId().equals(theater.getId()))
                    .toList();
            theaterData.put("totalShows", theaterShows.size());

            // Tickets for this theater
            Set<Integer> showIds = theaterShows.stream().map(Show::getId).collect(Collectors.toSet());
            List<Ticket> theaterTickets = allTickets.stream()
                    .filter(t -> t.getShow() != null && showIds.contains(t.getShow().getId()))
                    .toList();
            theaterData.put("totalBookings", theaterTickets.size());

            // Occupancy
            long totalSeats = 0;
            long bookedSeats = 0;
            for (Show show : theaterShows) {
                if (show.getShowSeatList() != null) {
                    totalSeats += show.getShowSeatList().size();
                    bookedSeats += show.getShowSeatList().stream()
                            .filter(s -> !s.getIsAvailable()).count();
                }
            }
            theaterData.put("totalSeats", totalSeats);
            theaterData.put("bookedSeats", bookedSeats);
            theaterData.put("occupancyRate", totalSeats > 0 
                    ? Math.round((double) bookedSeats / totalSeats * 100.0 * 10) / 10.0 : 0);

            // Revenue
            double theaterRevenue = successPayments.stream()
                    .filter(p -> p.getTicket() != null && p.getTicket().getShow() != null
                            && showIds.contains(p.getTicket().getShow().getId()))
                    .mapToDouble(p -> p.getTotalAmount() != null ? p.getTotalAmount() : 0)
                    .sum();
            theaterData.put("totalRevenue", theaterRevenue);

            // Movies shown
            long moviesShown = theaterShows.stream()
                    .map(s -> s.getMovie() != null ? s.getMovie().getId() : -1)
                    .distinct().count();
            theaterData.put("moviesShown", moviesShown);

            // Cancellation rate
            long cancelled = theaterTickets.stream()
                    .filter(t -> t.getStatus() == TicketStatus.CANCELLED).count();
            theaterData.put("cancellationRate", theaterTickets.size() > 0 
                    ? Math.round((double) cancelled / theaterTickets.size() * 100.0 * 10) / 10.0 : 0);

            result.add(theaterData);
        }

        // Sort
        String sort = sortBy != null ? sortBy.toLowerCase() : "revenue";
        switch (sort) {
            case "occupancy":
                result.sort((a, b) -> Double.compare(
                        (double) b.getOrDefault("occupancyRate", 0.0),
                        (double) a.getOrDefault("occupancyRate", 0.0)));
                break;
            case "bookings":
                result.sort((a, b) -> Integer.compare(
                        (int) b.getOrDefault("totalBookings", 0),
                        (int) a.getOrDefault("totalBookings", 0)));
                break;
            default: // revenue
                result.sort((a, b) -> Double.compare(
                        (double) b.getOrDefault("totalRevenue", 0.0),
                        (double) a.getOrDefault("totalRevenue", 0.0)));
        }

        // Add rank
        for (int i = 0; i < result.size(); i++) {
            result.get(i).put("rank", i + 1);
        }

        return result;
    }

    // =====================================================
    // SHOW OCCUPANCY HEATMAP
    // =====================================================

    /**
     * Get show occupancy data grouped by day and time slot for heatmap
     */
    public Map<String, Object> getShowOccupancyHeatmap(Integer theaterId, Integer movieId) {
        List<Show> shows = showRepository.findAll();

        // Apply filters
        if (theaterId != null) {
            shows = shows.stream()
                    .filter(s -> s.getTheater() != null && s.getTheater().getId().equals(theaterId))
                    .toList();
        }
        if (movieId != null) {
            shows = shows.stream()
                    .filter(s -> s.getMovie() != null && s.getMovie().getId().equals(movieId))
                    .toList();
        }

        // Group by day of week and time slot
        Map<String, Map<String, List<Double>>> heatmapData = new LinkedHashMap<>();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        String[] slots = {"Morning (6-12)", "Afternoon (12-16)", "Evening (16-20)", "Night (20-24)"};

        for (String day : days) {
            heatmapData.put(day, new LinkedHashMap<>());
            for (String slot : slots) {
                heatmapData.get(day).put(slot, new ArrayList<>());
            }
        }

        for (Show show : shows) {
            if (show.getDate() == null || show.getTime() == null) continue;

            // Get day of week
            LocalDate showDate = show.getDate().toLocalDate();
            String dayName = showDate.getDayOfWeek().name();
            dayName = dayName.charAt(0) + dayName.substring(1).toLowerCase();

            // Get time slot
            int hour = show.getTime().toLocalTime().getHour();
            String timeSlot;
            if (hour >= 6 && hour < 12) timeSlot = "Morning (6-12)";
            else if (hour >= 12 && hour < 16) timeSlot = "Afternoon (12-16)";
            else if (hour >= 16 && hour < 20) timeSlot = "Evening (16-20)";
            else timeSlot = "Night (20-24)";

            // Calculate occupancy
            List<ShowSeat> seats = show.getShowSeatList();
            if (seats != null && !seats.isEmpty()) {
                long total = seats.size();
                long booked = seats.stream().filter(s -> !s.getIsAvailable()).count();
                double occupancy = (double) booked / total * 100.0;
                
                if (heatmapData.containsKey(dayName) && heatmapData.get(dayName).containsKey(timeSlot)) {
                    heatmapData.get(dayName).get(timeSlot).add(occupancy);
                }
            }
        }

        // Convert to averages
        List<Map<String, Object>> heatmapCells = new ArrayList<>();
        for (String day : days) {
            for (String slot : slots) {
                Map<String, Object> cell = new LinkedHashMap<>();
                cell.put("day", day);
                cell.put("timeSlot", slot);
                List<Double> values = heatmapData.get(day).get(slot);
                double avg = values.stream().mapToDouble(d -> d).average().orElse(0);
                cell.put("avgOccupancy", Math.round(avg * 10.0) / 10.0);
                cell.put("showCount", values.size());
                heatmapCells.add(cell);
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("heatmapData", heatmapCells);
        response.put("days", days);
        response.put("timeSlots", slots);
        return response;
    }

    // =====================================================
    // USER ANALYTICS
    // =====================================================

    /**
     * Get user analytics - top users, registration trends, activity
     */
    public Map<String, Object> getUserAnalytics() {
        List<User> users = userRepository.findAll();
        List<Ticket> allTickets = ticketRepository.findAll();
        List<Payment> successPayments = paymentRepository.findByStatus(PaymentStatus.SUCCESS);

        Map<String, Object> analytics = new LinkedHashMap<>();

        // Total users
        analytics.put("totalUsers", users.size());
        long activeUsers = users.stream().filter(u -> u.getIsActive() != null && u.getIsActive()).count();
        analytics.put("activeUsers", activeUsers);
        analytics.put("inactiveUsers", users.size() - activeUsers);

        // User role distribution
        Map<String, Long> roleDistribution = users.stream()
                .collect(Collectors.groupingBy(
                        u -> u.getRole() != null ? u.getRole().name() : "USER",
                        Collectors.counting()));
        analytics.put("roleDistribution", roleDistribution);

        // Gender distribution
        Map<String, Long> genderDistribution = users.stream()
                .filter(u -> u.getGender() != null)
                .collect(Collectors.groupingBy(
                        u -> u.getGender().name(),
                        Collectors.counting()));
        analytics.put("genderDistribution", genderDistribution);

        // Registration trends (last 30 days)
        List<Map<String, Object>> registrationTrend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long count = users.stream()
                    .filter(u -> u.getCreatedAt() != null
                            && u.getCreatedAt().toLocalDate().equals(date))
                    .count();
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date.toString());
            point.put("count", count);
            registrationTrend.add(point);
        }
        analytics.put("registrationTrend", registrationTrend);

        // Top users by bookings
        Map<Integer, Long> userBookingCounts = allTickets.stream()
                .filter(t -> t.getUser() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getUser().getId(),
                        Collectors.counting()));

        List<Map<String, Object>> topUsers = userBookingCounts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    Map<String, Object> userData = new LinkedHashMap<>();
                    User user = users.stream()
                            .filter(u -> u.getId().equals(entry.getKey()))
                            .findFirst().orElse(null);
                    if (user != null) {
                        userData.put("userId", user.getId());
                        userData.put("name", user.getName());
                        userData.put("email", user.getEmailId());
                        userData.put("bookingCount", entry.getValue());
                        
                        // User's revenue
                        double userRevenue = successPayments.stream()
                                .filter(p -> p.getUser() != null && p.getUser().getId().equals(user.getId()))
                                .mapToDouble(p -> p.getTotalAmount() != null ? p.getTotalAmount() : 0)
                                .sum();
                        userData.put("totalSpent", userRevenue);
                    }
                    return userData;
                })
                .filter(m -> !m.isEmpty())
                .toList();
        analytics.put("topUsers", topUsers);

        // Average bookings per user
        double avgBookings = users.isEmpty() ? 0 : (double) allTickets.size() / users.size();
        analytics.put("avgBookingsPerUser", Math.round(avgBookings * 100.0) / 100.0);

        return analytics;
    }

    // =====================================================
    // REVENUE TRENDS (Time-series for charts)
    // =====================================================

    /**
     * Get revenue trends over time (daily/weekly/monthly)
     */
    public List<Map<String, Object>> getRevenueTrends(String period, String dateFrom, String dateTo) {
        List<Payment> successPayments = paymentRepository.findByStatus(PaymentStatus.SUCCESS);

        LocalDate from = dateFrom != null && !dateFrom.isEmpty() 
                ? LocalDate.parse(dateFrom) : LocalDate.now().minusDays(30);
        LocalDate to = dateTo != null && !dateTo.isEmpty() 
                ? LocalDate.parse(dateTo) : LocalDate.now();

        // Filter by date range
        List<Payment> filtered = successPayments.stream()
                .filter(p -> p.getCompletedAt() != null
                        && !p.getCompletedAt().toLocalDate().isBefore(from)
                        && !p.getCompletedAt().toLocalDate().isAfter(to))
                .toList();

        List<Map<String, Object>> trend = new ArrayList<>();

        if ("weekly".equalsIgnoreCase(period)) {
            // Group by week
            Map<String, List<Payment>> grouped = filtered.stream()
                    .collect(Collectors.groupingBy(p -> {
                        LocalDate d = p.getCompletedAt().toLocalDate();
                        LocalDate weekStart = d.minusDays(d.getDayOfWeek().getValue() - 1);
                        return weekStart.toString();
                    }));
            grouped.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        Map<String, Object> point = new LinkedHashMap<>();
                        point.put("period", "Week of " + entry.getKey());
                        point.put("date", entry.getKey());
                        point.put("revenue", entry.getValue().stream()
                                .mapToDouble(p -> p.getTotalAmount() != null ? p.getTotalAmount() : 0).sum());
                        point.put("transactions", entry.getValue().size());
                        trend.add(point);
                    });
        } else if ("monthly".equalsIgnoreCase(period)) {
            // Group by month
            Map<String, List<Payment>> grouped = filtered.stream()
                    .collect(Collectors.groupingBy(p -> {
                        LocalDate d = p.getCompletedAt().toLocalDate();
                        return d.getYear() + "-" + String.format("%02d", d.getMonthValue());
                    }));
            grouped.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        Map<String, Object> point = new LinkedHashMap<>();
                        point.put("period", entry.getKey());
                        point.put("date", entry.getKey() + "-01");
                        point.put("revenue", entry.getValue().stream()
                                .mapToDouble(p -> p.getTotalAmount() != null ? p.getTotalAmount() : 0).sum());
                        point.put("transactions", entry.getValue().size());
                        trend.add(point);
                    });
        } else {
            // Daily (default)
            long daysBetween = ChronoUnit.DAYS.between(from, to);
            for (long i = 0; i <= daysBetween; i++) {
                LocalDate date = from.plusDays(i);
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("period", date.toString());
                point.put("date", date.toString());

                double dayRevenue = filtered.stream()
                        .filter(p -> p.getCompletedAt().toLocalDate().equals(date))
                        .mapToDouble(p -> p.getTotalAmount() != null ? p.getTotalAmount() : 0)
                        .sum();
                long dayTxns = filtered.stream()
                        .filter(p -> p.getCompletedAt().toLocalDate().equals(date))
                        .count();
                point.put("revenue", dayRevenue);
                point.put("transactions", dayTxns);
                trend.add(point);
            }
        }

        return trend;
    }

    // =====================================================
    // OCCUPANCY TRENDS
    // =====================================================

    /**
     * Get occupancy trends over time
     */
    public List<Map<String, Object>> getOccupancyTrends(String dateFrom, String dateTo) {
        List<Show> allShows = showRepository.findAll();

        LocalDate from = dateFrom != null && !dateFrom.isEmpty()
                ? LocalDate.parse(dateFrom) : LocalDate.now().minusDays(30);
        LocalDate to = dateTo != null && !dateTo.isEmpty()
                ? LocalDate.parse(dateTo) : LocalDate.now().plusDays(7);

        // Group shows by date
        Map<LocalDate, List<Show>> showsByDate = allShows.stream()
                .filter(s -> s.getDate() != null
                        && !s.getDate().toLocalDate().isBefore(from)
                        && !s.getDate().toLocalDate().isAfter(to))
                .collect(Collectors.groupingBy(s -> s.getDate().toLocalDate()));

        List<Map<String, Object>> trend = new ArrayList<>();
        long daysBetween = ChronoUnit.DAYS.between(from, to);

        for (long i = 0; i <= daysBetween; i++) {
            LocalDate date = from.plusDays(i);
            List<Show> dayShows = showsByDate.getOrDefault(date, Collections.emptyList());

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date.toString());
            point.put("showCount", dayShows.size());

            long totalSeats = 0;
            long bookedSeats = 0;
            for (Show show : dayShows) {
                if (show.getShowSeatList() != null) {
                    totalSeats += show.getShowSeatList().size();
                    bookedSeats += show.getShowSeatList().stream()
                            .filter(s -> !s.getIsAvailable()).count();
                }
            }
            point.put("totalSeats", totalSeats);
            point.put("bookedSeats", bookedSeats);
            point.put("occupancyRate", totalSeats > 0
                    ? Math.round((double) bookedSeats / totalSeats * 100.0 * 10) / 10.0 : 0);
            trend.add(point);
        }

        return trend;
    }

    // =====================================================
    // CANCELLATION TRENDS
    // =====================================================

    /**
     * Get cancellation trends over time
     */
    public List<Map<String, Object>> getCancellationTrends(String dateFrom, String dateTo) {
        List<Ticket> allTickets = ticketRepository.findAll();

        LocalDate from = dateFrom != null && !dateFrom.isEmpty()
                ? LocalDate.parse(dateFrom) : LocalDate.now().minusDays(30);
        LocalDate to = dateTo != null && !dateTo.isEmpty()
                ? LocalDate.parse(dateTo) : LocalDate.now();

        List<Map<String, Object>> trend = new ArrayList<>();
        long daysBetween = ChronoUnit.DAYS.between(from, to);

        for (long i = 0; i <= daysBetween; i++) {
            LocalDate date = from.plusDays(i);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date.toString());

            long totalBookings = allTickets.stream()
                    .filter(t -> t.getBookedAt() != null
                            && t.getBookedAt().toLocalDate().equals(date))
                    .count();
            long cancelled = allTickets.stream()
                    .filter(t -> t.getStatus() == TicketStatus.CANCELLED
                            && t.getCancelledAt() != null
                            && t.getCancelledAt().toLocalDate().equals(date))
                    .count();

            point.put("totalBookings", totalBookings);
            point.put("cancellations", cancelled);
            point.put("cancellationRate", totalBookings > 0
                    ? Math.round((double) cancelled / totalBookings * 100.0 * 10) / 10.0 : 0);

            // Refund amount
            double refundAmount = allTickets.stream()
                    .filter(t -> t.getStatus() == TicketStatus.CANCELLED
                            && t.getCancelledAt() != null
                            && t.getCancelledAt().toLocalDate().equals(date)
                            && t.getRefundAmount() != null)
                    .mapToDouble(Ticket::getRefundAmount)
                    .sum();
            point.put("refundAmount", refundAmount);

            trend.add(point);
        }

        return trend;
    }

    // =====================================================
    // GENRE & LANGUAGE DISTRIBUTION
    // =====================================================

    /**
     * Get genre-wise and language-wise revenue & booking distributions
     */
    public Map<String, Object> getDistributionAnalytics() {
        List<Movie> movies = movieRepository.findAll();
        List<Ticket> allTickets = ticketRepository.findAll();
        List<Payment> successPayments = paymentRepository.findByStatus(PaymentStatus.SUCCESS);

        Map<String, Object> result = new LinkedHashMap<>();

        // Genre distribution
        List<Map<String, Object>> genreData = new ArrayList<>();
        Map<String, List<Movie>> moviesByGenre = movies.stream()
                .filter(m -> m.getGenre() != null)
                .collect(Collectors.groupingBy(m -> m.getGenre().name()));

        for (Map.Entry<String, List<Movie>> entry : moviesByGenre.entrySet()) {
            Map<String, Object> genre = new LinkedHashMap<>();
            genre.put("genre", entry.getKey());
            genre.put("movieCount", entry.getValue().size());

            Set<Integer> showIds = entry.getValue().stream()
                    .flatMap(m -> (m.getShows() != null ? m.getShows() : Collections.<Show>emptyList()).stream())
                    .map(Show::getId)
                    .collect(Collectors.toSet());

            long bookings = allTickets.stream()
                    .filter(t -> t.getShow() != null && showIds.contains(t.getShow().getId()))
                    .count();
            genre.put("bookings", bookings);

            double revenue = successPayments.stream()
                    .filter(p -> p.getTicket() != null && p.getTicket().getShow() != null
                            && showIds.contains(p.getTicket().getShow().getId()))
                    .mapToDouble(p -> p.getTotalAmount() != null ? p.getTotalAmount() : 0)
                    .sum();
            genre.put("revenue", revenue);

            genreData.add(genre);
        }
        genreData.sort((a, b) -> Double.compare(
                (double) b.getOrDefault("revenue", 0.0),
                (double) a.getOrDefault("revenue", 0.0)));
        result.put("genreDistribution", genreData);

        // Language distribution
        List<Map<String, Object>> langData = new ArrayList<>();
        Map<String, List<Movie>> moviesByLang = movies.stream()
                .filter(m -> m.getLanguage() != null)
                .collect(Collectors.groupingBy(m -> m.getLanguage().name()));

        for (Map.Entry<String, List<Movie>> entry : moviesByLang.entrySet()) {
            Map<String, Object> lang = new LinkedHashMap<>();
            lang.put("language", entry.getKey());
            lang.put("movieCount", entry.getValue().size());

            Set<Integer> showIds = entry.getValue().stream()
                    .flatMap(m -> (m.getShows() != null ? m.getShows() : Collections.<Show>emptyList()).stream())
                    .map(Show::getId)
                    .collect(Collectors.toSet());

            long bookings = allTickets.stream()
                    .filter(t -> t.getShow() != null && showIds.contains(t.getShow().getId()))
                    .count();
            lang.put("bookings", bookings);

            double revenue = successPayments.stream()
                    .filter(p -> p.getTicket() != null && p.getTicket().getShow() != null
                            && showIds.contains(p.getTicket().getShow().getId()))
                    .mapToDouble(p -> p.getTotalAmount() != null ? p.getTotalAmount() : 0)
                    .sum();
            lang.put("revenue", revenue);

            langData.add(lang);
        }
        langData.sort((a, b) -> Double.compare(
                (double) b.getOrDefault("revenue", 0.0),
                (double) a.getOrDefault("revenue", 0.0)));
        result.put("languageDistribution", langData);

        // Payment method distribution
        List<Map<String, Object>> payMethodData = new ArrayList<>();
        Map<String, List<Payment>> paymentsByMethod = successPayments.stream()
                .filter(p -> p.getPaymentMethod() != null)
                .collect(Collectors.groupingBy(p -> p.getPaymentMethod().name()));

        for (Map.Entry<String, List<Payment>> entry : paymentsByMethod.entrySet()) {
            Map<String, Object> method = new LinkedHashMap<>();
            method.put("method", entry.getKey());
            method.put("count", entry.getValue().size());
            method.put("revenue", entry.getValue().stream()
                    .mapToDouble(p -> p.getTotalAmount() != null ? p.getTotalAmount() : 0)
                    .sum());
            payMethodData.add(method);
        }
        result.put("paymentMethodDistribution", payMethodData);

        return result;
    }

    // =====================================================
    // CSV EXPORT DATA
    // =====================================================

    /**
     * Get data for CSV export based on type
     */
    public List<Map<String, Object>> getExportData(String type, String dateFrom, String dateTo) {
        LocalDate from = dateFrom != null && !dateFrom.isEmpty()
                ? LocalDate.parse(dateFrom) : LocalDate.now().minusYears(1);
        LocalDate to = dateTo != null && !dateTo.isEmpty()
                ? LocalDate.parse(dateTo) : LocalDate.now();

        switch (type.toLowerCase()) {
            case "revenue":
                return getRevenueExportData(from, to);
            case "bookings":
                return getBookingsExportData(from, to);
            case "movies":
                return getMovieAnalytics(null, null, from.toString(), to.toString());
            case "theaters":
                return getTheaterRankings("revenue");
            case "users":
                return getUsersExportData();
            case "cities":
                return getCityAnalytics();
            default:
                return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> getRevenueExportData(LocalDate from, LocalDate to) {
        List<Payment> payments = paymentRepository.findByStatus(PaymentStatus.SUCCESS);
        return payments.stream()
                .filter(p -> p.getCompletedAt() != null
                        && !p.getCompletedAt().toLocalDate().isBefore(from)
                        && !p.getCompletedAt().toLocalDate().isAfter(to))
                .map(p -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("Transaction ID", p.getTransactionId());
                    row.put("Date", p.getCompletedAt() != null ? p.getCompletedAt().toString() : "");
                    row.put("User", p.getUser() != null ? p.getUser().getName() : "");
                    row.put("Base Amount", p.getBaseAmount());
                    row.put("Convenience Fee", p.getConvenienceFee());
                    row.put("Tax", p.getTax());
                    row.put("Total Amount", p.getTotalAmount());
                    row.put("Payment Method", p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "");
                    row.put("Movie", p.getTicket() != null && p.getTicket().getShow() != null 
                            && p.getTicket().getShow().getMovie() != null 
                            ? p.getTicket().getShow().getMovie().getMovieName() : "");
                    row.put("Theater", p.getTicket() != null && p.getTicket().getShow() != null 
                            && p.getTicket().getShow().getTheater() != null 
                            ? p.getTicket().getShow().getTheater().getName() : "");
                    return row;
                })
                .toList();
    }

    private List<Map<String, Object>> getBookingsExportData(LocalDate from, LocalDate to) {
        List<Ticket> tickets = ticketRepository.findAll();
        return tickets.stream()
                .filter(t -> t.getBookedAt() != null
                        && !t.getBookedAt().toLocalDate().isBefore(from)
                        && !t.getBookedAt().toLocalDate().isAfter(to))
                .map(t -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("Ticket ID", t.getId());
                    row.put("Booked At", t.getBookedAt() != null ? t.getBookedAt().toString() : "");
                    row.put("User", t.getUser() != null ? t.getUser().getName() : "");
                    row.put("Movie", t.getShow() != null && t.getShow().getMovie() != null 
                            ? t.getShow().getMovie().getMovieName() : "");
                    row.put("Theater", t.getShow() != null && t.getShow().getTheater() != null 
                            ? t.getShow().getTheater().getName() : "");
                    row.put("Show Date", t.getShow() != null && t.getShow().getDate() != null
                            ? t.getShow().getDate().toString() : "");
                    row.put("Seats", t.getBookedSeats());
                    row.put("Price", t.getTotalTicketsPrice());
                    row.put("Status", t.getStatus() != null ? t.getStatus().name() : "");
                    row.put("Cancelled At", t.getCancelledAt() != null ? t.getCancelledAt().toString() : "");
                    row.put("Refund Amount", t.getRefundAmount() != null ? t.getRefundAmount() : 0);
                    return row;
                })
                .toList();
    }

    private List<Map<String, Object>> getUsersExportData() {
        List<User> users = userRepository.findAll();
        List<Ticket> allTickets = ticketRepository.findAll();

        return users.stream().map(u -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("User ID", u.getId());
            row.put("Name", u.getName());
            row.put("Email", u.getEmailId());
            row.put("Mobile", u.getMobileNo());
            row.put("Role", u.getRole() != null ? u.getRole().name() : "");
            row.put("Active", u.getIsActive() != null ? u.getIsActive() : true);
            row.put("Wallet Balance", u.getWalletBalance());
            row.put("Total Bookings", allTickets.stream()
                    .filter(t -> t.getUser() != null && t.getUser().getId().equals(u.getId()))
                    .count());
            row.put("Registered", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
            return row;
        }).toList();
    }

    // =====================================================
    // ENHANCED DASHBOARD SUMMARY
    // =====================================================

    /**
     * Get full enhanced dashboard summary with all KPIs
     */
    public Map<String, Object> getEnhancedDashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>();

        long totalUsers = userRepository.count();
        long totalMovies = movieRepository.count();
        long totalTheaters = theaterRepository.count();
        long totalShows = showRepository.count();
        long totalTickets = ticketRepository.count();
        long totalCities = cityRepository.count();

        dashboard.put("totalUsers", totalUsers);
        dashboard.put("totalMovies", totalMovies);
        dashboard.put("totalTheaters", totalTheaters);
        dashboard.put("totalShows", totalShows);
        dashboard.put("totalBookings", totalTickets);
        dashboard.put("totalCities", totalCities);

        // Revenue stats
        List<Payment> successPayments = paymentRepository.findByStatus(PaymentStatus.SUCCESS);
        double totalRevenue = successPayments.stream()
                .mapToDouble(p -> p.getTotalAmount() != null ? p.getTotalAmount() : 0)
                .sum();
        dashboard.put("totalRevenue", totalRevenue);

        // Today's stats
        LocalDate today = LocalDate.now();
        double todayRevenue = successPayments.stream()
                .filter(p -> p.getCompletedAt() != null && p.getCompletedAt().toLocalDate().equals(today))
                .mapToDouble(p -> p.getTotalAmount() != null ? p.getTotalAmount() : 0)
                .sum();
        dashboard.put("todayRevenue", todayRevenue);

        List<Ticket> allTickets = ticketRepository.findAll();
        long todayBookings = allTickets.stream()
                .filter(t -> t.getBookedAt() != null && t.getBookedAt().toLocalDate().equals(today))
                .count();
        dashboard.put("todayBookings", todayBookings);

        // Cancellation stats
        long totalCancelled = allTickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.CANCELLED).count();
        dashboard.put("totalCancellations", totalCancelled);
        dashboard.put("cancellationRate", totalTickets > 0
                ? Math.round((double) totalCancelled / totalTickets * 100.0 * 10) / 10.0 : 0);

        // Average revenue per booking
        double avgRevenue = totalTickets > 0 ? totalRevenue / totalTickets : 0;
        dashboard.put("avgRevenuePerBooking", Math.round(avgRevenue * 100.0) / 100.0);

        // Overall occupancy
        List<Show> allShows = showRepository.findAll();
        long totalSeats = 0;
        long bookedSeats = 0;
        for (Show show : allShows) {
            if (show.getShowSeatList() != null) {
                totalSeats += show.getShowSeatList().size();
                bookedSeats += show.getShowSeatList().stream()
                        .filter(s -> !s.getIsAvailable()).count();
            }
        }
        dashboard.put("overallOccupancy", totalSeats > 0
                ? Math.round((double) bookedSeats / totalSeats * 100.0 * 10) / 10.0 : 0);

        // Payment method summary
        Map<String, Long> paymentMethods = successPayments.stream()
                .filter(p -> p.getPaymentMethod() != null)
                .collect(Collectors.groupingBy(p -> p.getPaymentMethod().name(), Collectors.counting()));
        dashboard.put("paymentMethods", paymentMethods);

        // Recommendation stats
        Map<String, Object> recStats = getRecommendationAnalytics();
        dashboard.put("recommendations", recStats);

        return dashboard;
    }

    // =====================================================
    // RECOMMENDATION ANALYTICS
    // =====================================================

    /**
     * Get recommendation analytics: acceptance rate, conversion, theatre-wise breakdown
     */
    public Map<String, Object> getRecommendationAnalytics() {
        List<MovieRecommendation> all = movieRecommendationRepository.findAll();
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("totalRecommendations", all.size());

        long pending = all.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
        long accepted = all.stream().filter(r -> "ACCEPTED".equals(r.getStatus())).count();
        long rejected = all.stream().filter(r -> "REJECTED".equals(r.getStatus())).count();

        stats.put("pending", pending);
        stats.put("accepted", accepted);
        stats.put("rejected", rejected);

        double acceptRate = all.size() > 0 ? ((double) accepted / all.size()) * 100 : 0;
        double rejectRate = all.size() > 0 ? ((double) rejected / all.size()) * 100 : 0;
        stats.put("acceptanceRate", Math.round(acceptRate * 100.0) / 100.0);
        stats.put("rejectionRate", Math.round(rejectRate * 100.0) / 100.0);

        // Theatre-wise recommendation breakdown
        Map<String, Map<String, Object>> theatreBreakdown = new LinkedHashMap<>();
        for (MovieRecommendation rec : all) {
            String theatreName = rec.getTheater().getName();
            Map<String, Object> entry = theatreBreakdown.computeIfAbsent(theatreName, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("theatreName", theatreName);
                m.put("theatreId", rec.getTheater().getId());
                m.put("total", 0L);
                m.put("pending", 0L);
                m.put("accepted", 0L);
                m.put("rejected", 0L);
                return m;
            });
            entry.put("total", (long) entry.get("total") + 1);
            entry.put(rec.getStatus().toLowerCase(), (long) entry.get(rec.getStatus().toLowerCase()) + 1);
        }
        stats.put("theatreBreakdown", new ArrayList<>(theatreBreakdown.values()));

        // Movie-wise recommendation breakdown
        Map<String, Map<String, Object>> movieBreakdown = new LinkedHashMap<>();
        for (MovieRecommendation rec : all) {
            String movieName = rec.getMovie().getMovieName();
            Map<String, Object> entry = movieBreakdown.computeIfAbsent(movieName, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("movieName", movieName);
                m.put("movieId", rec.getMovie().getId());
                m.put("total", 0L);
                m.put("accepted", 0L);
                m.put("rejected", 0L);
                return m;
            });
            entry.put("total", (long) entry.get("total") + 1);
            if ("ACCEPTED".equals(rec.getStatus())) {
                entry.put("accepted", (long) entry.get("accepted") + 1);
            } else if ("REJECTED".equals(rec.getStatus())) {
                entry.put("rejected", (long) entry.get("rejected") + 1);
            }
        }
        stats.put("movieBreakdown", new ArrayList<>(movieBreakdown.values()));

        return stats;
    }

    // =====================================================
    // SEAT TYPE ANALYTICS
    // =====================================================

    /**
     * Get global seat type analytics: revenue, occupancy, popularity per seat type
     */
    public Map<String, Object> getSeatTypeAnalytics() {
        List<Show> allShows = showRepository.findAll();
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Double> revenueByType = new LinkedHashMap<>();
        Map<String, Long> totalByType = new LinkedHashMap<>();
        Map<String, Long> bookedByType = new LinkedHashMap<>();

        for (Show show : allShows) {
            if (show.getShowSeatList() != null) {
                for (ShowSeat ss : show.getShowSeatList()) {
                    String type = ss.getSeatType().name();
                    totalByType.merge(type, 1L, Long::sum);
                    if (!ss.getIsAvailable()) {
                        bookedByType.merge(type, 1L, Long::sum);
                        revenueByType.merge(type, (double) ss.getPrice(), Double::sum);
                    }
                }
            }
        }

        List<Map<String, Object>> breakdown = new ArrayList<>();
        double totalRevenue = revenueByType.values().stream().mapToDouble(d -> d).sum();

        for (String type : new String[]{"PREMIUM", "GOLD", "SILVER", "COUPLE", "CLASSIC"}) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("seatType", type);
            entry.put("totalSeats", totalByType.getOrDefault(type, 0L));
            entry.put("bookedSeats", bookedByType.getOrDefault(type, 0L));
            entry.put("revenue", revenueByType.getOrDefault(type, 0.0));
            long total = totalByType.getOrDefault(type, 0L);
            long booked = bookedByType.getOrDefault(type, 0L);
            double occupancy = total > 0 ? ((double) booked / total) * 100 : 0;
            entry.put("occupancyRate", Math.round(occupancy * 100.0) / 100.0);
            double share = totalRevenue > 0 ? (revenueByType.getOrDefault(type, 0.0) / totalRevenue) * 100 : 0;
            entry.put("revenueShare", Math.round(share * 100.0) / 100.0);
            breakdown.add(entry);
        }

        result.put("seatTypeBreakdown", breakdown);
        result.put("totalRevenue", totalRevenue);

        return result;
    }

    // =====================================================
    // PEAK TIME ANALYTICS
    // =====================================================

    /**
     * Get peak time analytics across all theatres
     */
    public List<Map<String, Object>> getPeakTimeAnalytics() {
        List<Show> allShows = showRepository.findAll();

        // Group by time slot
        Map<String, List<Show>> slots = new LinkedHashMap<>();
        slots.put("Morning (6AM-12PM)", new ArrayList<>());
        slots.put("Afternoon (12PM-5PM)", new ArrayList<>());
        slots.put("Evening (5PM-9PM)", new ArrayList<>());
        slots.put("Night (9PM+)", new ArrayList<>());

        for (Show show : allShows) {
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

    // =====================================================
    // LANGUAGE ANALYTICS
    // =====================================================

    /**
     * Get language-wise performance analytics
     */
    public List<Map<String, Object>> getLanguageAnalytics() {
        List<Show> allShows = showRepository.findAll();

        Map<String, List<Show>> langShows = new LinkedHashMap<>();
        for (Show show : allShows) {
            String lang = show.getMovie().getLanguage() != null ? show.getMovie().getLanguage().name() : "UNKNOWN";
            langShows.computeIfAbsent(lang, k -> new ArrayList<>()).add(show);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Show>> entry : langShows.entrySet()) {
            Map<String, Object> langData = new LinkedHashMap<>();
            langData.put("language", entry.getKey());
            langData.put("showCount", entry.getValue().size());

            long bookings = entry.getValue().stream()
                    .mapToLong(s -> s.getTicketList() != null ? s.getTicketList().size() : 0).sum();
            double revenue = entry.getValue().stream()
                    .flatMap(s -> s.getTicketList() != null ? s.getTicketList().stream() : java.util.stream.Stream.empty())
                    .mapToDouble(t -> t.getTotalTicketsPrice() != null ? t.getTotalTicketsPrice().doubleValue() : 0.0).sum();

            // Unique movies
            long uniqueMovies = entry.getValue().stream()
                    .map(s -> s.getMovie().getId()).distinct().count();

            langData.put("movieCount", uniqueMovies);
            langData.put("bookings", bookings);
            langData.put("revenue", revenue);
            result.add(langData);
        }

        result.sort((a, b) -> Double.compare((double) b.get("revenue"), (double) a.get("revenue")));
        return result;
    }
}
