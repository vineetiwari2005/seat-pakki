package com.driver.bookMyShow.Controllers;

import com.driver.bookMyShow.Dtos.RequestDtos.ShowEntryDto;
import com.driver.bookMyShow.Enums.SeatType;
import com.driver.bookMyShow.Models.TheaterSeat;
import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.Repositories.UserRepository;
import com.driver.bookMyShow.Services.TheatreAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TheatreAdminController - Endpoints for Theatre Admin (THEATER_OWNER role).
 * 
 * Theatre Admin can only manage their assigned theatre.
 * All endpoints require THEATER_OWNER role.
 * 
 * Provides:
 * - Dashboard with theatre-specific stats
 * - Movie recommendation management
 * - Show scheduling
 * - Seat management
 * - Theatre-specific analytics
 */
@RestController
@RequestMapping("/owner")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('THEATER_OWNER')")
public class TheatreAdminController {

    @Autowired
    private TheatreAdminService theatreAdminService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get the current logged-in user's ID from JWT token
     */
    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmailId(email);
        return user != null ? user.getId() : null;
    }

    // =====================================================
    // DASHBOARD
    // =====================================================

    /**
     * GET /owner/dashboard
     * Get theatre dashboard with stats
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            Integer userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }
            Map<String, Object> dashboard = theatreAdminService.getTheatreDashboard(userId);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /owner/theatre
     * Get assigned theatre details
     */
    @GetMapping("/theatre")
    public ResponseEntity<?> getAssignedTheatre() {
        try {
            Integer userId = getCurrentUserId();
            var theatre = theatreAdminService.getAssignedTheatre(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("id", theatre.getId());
            result.put("name", theatre.getName());
            result.put("address", theatre.getAddress());
            result.put("cityName", theatre.getCityName());
            if (theatre.getCity() != null) {
                result.put("city", theatre.getCity().getName());
            }
            result.put("seatCount", theatre.getTheaterSeatList() != null ? theatre.getTheaterSeatList().size() : 0);
            result.put("showCount", theatre.getShowList() != null ? theatre.getShowList().size() : 0);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // =====================================================
    // MOVIE RECOMMENDATIONS
    // =====================================================

    /**
     * GET /owner/recommendations
     * Get all movie recommendations from Main Admin
     */
    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations() {
        try {
            Integer userId = getCurrentUserId();
            var recommendations = theatreAdminService.getRecommendations(userId);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /owner/recommendations/pending
     * Get only pending recommendations
     */
    @GetMapping("/recommendations/pending")
    public ResponseEntity<?> getPendingRecommendations() {
        try {
            Integer userId = getCurrentUserId();
            var recommendations = theatreAdminService.getPendingRecommendations(userId);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /owner/recommendations/{id}/accept
     * Accept a movie recommendation
     */
    @PostMapping("/recommendations/{id}/accept")
    public ResponseEntity<?> acceptRecommendation(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            Integer userId = getCurrentUserId();
            String message = body != null ? body.get("message") : null;
            String result = theatreAdminService.acceptRecommendation(userId, id, message);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /owner/recommendations/{id}/reject
     * Reject a movie recommendation
     */
    @PostMapping("/recommendations/{id}/reject")
    public ResponseEntity<?> rejectRecommendation(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            Integer userId = getCurrentUserId();
            String reason = body != null ? body.get("reason") : null;
            String result = theatreAdminService.rejectRecommendation(userId, id, reason);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // =====================================================
    // SHOW MANAGEMENT
    // =====================================================

    /**
     * GET /owner/shows
     * Get all shows for the theatre
     */
    @GetMapping("/shows")
    public ResponseEntity<?> getShows() {
        try {
            Integer userId = getCurrentUserId();
            var shows = theatreAdminService.getShows(userId);
            return ResponseEntity.ok(shows);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /owner/shows
     * Add a new show
     */
    @PostMapping("/shows")
    public ResponseEntity<?> addShow(@RequestBody ShowEntryDto showEntryDto) {
        try {
            Integer userId = getCurrentUserId();
            String result = theatreAdminService.addShow(userId, showEntryDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /owner/shows/{showId}
     * Delete a show (only if no bookings exist)
     */
    @DeleteMapping("/shows/{showId}")
    public ResponseEntity<?> deleteShow(@PathVariable Integer showId) {
        try {
            Integer userId = getCurrentUserId();
            theatreAdminService.deleteShow(userId, showId);
            return ResponseEntity.ok(Map.of("message", "Show deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /owner/shows/{showId}/bookings
     * Get bookings for a specific show
     */
    @GetMapping("/shows/{showId}/bookings")
    public ResponseEntity<?> getShowBookings(@PathVariable Integer showId) {
        try {
            Integer userId = getCurrentUserId();
            var bookings = theatreAdminService.getShowBookings(userId, showId);
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // =====================================================
    // SEAT MANAGEMENT
    // =====================================================

    /**
     * GET /owner/seats
     * Get theatre seats
     */
    @GetMapping("/seats")
    public ResponseEntity<?> getTheatreSeats() {
        try {
            Integer userId = getCurrentUserId();
            List<TheaterSeat> seats = theatreAdminService.getTheatreSeats(userId);
            return ResponseEntity.ok(seats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /owner/shows/{showId}/seats
     * Get seats for a specific show
     */
    @GetMapping("/shows/{showId}/seats")
    public ResponseEntity<?> getShowSeats(@PathVariable Integer showId) {
        try {
            Integer userId = getCurrentUserId();
            var seats = theatreAdminService.getShowSeats(userId, showId);
            return ResponseEntity.ok(seats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // =====================================================
    // ANALYTICS
    // =====================================================

    /**
     * GET /owner/analytics
     * Get theatre-specific analytics
     */
    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics() {
        try {
            Integer userId = getCurrentUserId();
            Map<String, Object> analytics = theatreAdminService.getTheatreAnalytics(userId);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // =====================================================
    // SEAT MANAGEMENT (Theatre admin manages own seats)
    // =====================================================

    /**
     * POST /owner/seats/row
     * Add a row of seats to the theatre
     * Body: { "rowPrefix": "A", "seatType": "GOLD", "count": 20 }
     */
    @PostMapping("/seats/row")
    public ResponseEntity<?> addTheatreSeatsRow(@RequestBody Map<String, Object> request) {
        try {
            Integer userId = getCurrentUserId();
            String rowPrefix = (String) request.get("rowPrefix");
            String seatTypeStr = (String) request.get("seatType");
            int count = ((Number) request.get("count")).intValue();

            SeatType seatType = SeatType.valueOf(seatTypeStr);
            String result = theatreAdminService.addTheatreSeatsRow(userId, rowPrefix, seatType, count);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /owner/seats/{seatId}
     * Delete a theatre seat
     */
    @DeleteMapping("/seats/{seatId}")
    public ResponseEntity<?> deleteTheatreSeat(@PathVariable Integer seatId) {
        try {
            Integer userId = getCurrentUserId();
            theatreAdminService.deleteTheatreSeat(userId, seatId);
            return ResponseEntity.ok(Map.of("message", "Seat deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /owner/seats/summary
     * Get seat type summary for the theatre
     */
    @GetMapping("/seats/summary")
    public ResponseEntity<?> getSeatSummary() {
        try {
            Integer userId = getCurrentUserId();
            Map<String, Object> summary = theatreAdminService.getSeatTypeSummary(userId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // =====================================================
    // ENHANCED ANALYTICS ENDPOINTS
    // =====================================================

    /**
     * GET /owner/analytics/seat-revenue
     * Get seat type revenue breakdown
     */
    @GetMapping("/analytics/seat-revenue")
    public ResponseEntity<?> getSeatTypeRevenue() {
        try {
            Integer userId = getCurrentUserId();
            Map<String, Object> data = theatreAdminService.getSeatTypeRevenue(userId);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /owner/analytics/recommendations
     * Get recommendation conversion stats
     */
    @GetMapping("/analytics/recommendations")
    public ResponseEntity<?> getRecommendationStats() {
        try {
            Integer userId = getCurrentUserId();
            Map<String, Object> stats = theatreAdminService.getRecommendationStats(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /owner/analytics/time-slots
     * Get time slot performance
     */
    @GetMapping("/analytics/time-slots")
    public ResponseEntity<?> getTimeSlotAnalytics() {
        try {
            Integer userId = getCurrentUserId();
            var data = theatreAdminService.getTimeSlotAnalytics(userId);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /owner/analytics/cancellations
     * Get cancellation stats
     */
    @GetMapping("/analytics/cancellations")
    public ResponseEntity<?> getCancellationStats() {
        try {
            Integer userId = getCurrentUserId();
            Map<String, Object> stats = theatreAdminService.getCancellationStats(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /owner/analytics/weekly-revenue
     * Get weekly revenue trend
     */
    @GetMapping("/analytics/weekly-revenue")
    public ResponseEntity<?> getWeeklyRevenueTrend() {
        try {
            Integer userId = getCurrentUserId();
            var data = theatreAdminService.getWeeklyRevenueTrend(userId);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /owner/analytics/genres
     * Get genre performance analytics
     */
    @GetMapping("/analytics/genres")
    public ResponseEntity<?> getGenrePerformance() {
        try {
            Integer userId = getCurrentUserId();
            var data = theatreAdminService.getGenrePerformance(userId);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /owner/analytics/payments
     * Get comprehensive payment analytics for the theatre
     */
    @GetMapping("/analytics/payments")
    public ResponseEntity<?> getPaymentAnalytics() {
        try {
            Integer userId = getCurrentUserId();
            Map<String, Object> data = theatreAdminService.getPaymentAnalytics(userId);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
