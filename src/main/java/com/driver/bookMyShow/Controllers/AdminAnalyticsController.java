package com.driver.bookMyShow.Controllers;

import com.driver.bookMyShow.Services.AdminAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AdminAnalyticsController - Analytics & reporting endpoints for admin dashboard
 * 
 * All endpoints are read-only aggregations from existing DB tables.
 * No business logic changes. No schema changes.
 */
@RestController
@RequestMapping("/admin/analytics")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    @Autowired
    private AdminAnalyticsService analyticsService;

    // =====================================================
    // ENHANCED DASHBOARD
    // =====================================================

    /**
     * GET /admin/analytics/dashboard
     * Enhanced dashboard with comprehensive KPIs
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getEnhancedDashboard() {
        return ResponseEntity.ok(analyticsService.getEnhancedDashboard());
    }

    // =====================================================
    // CITY ANALYTICS
    // =====================================================

    /**
     * GET /admin/analytics/cities
     * City-wise analytics: theatres, shows, bookings, revenue
     */
    @GetMapping("/cities")
    public ResponseEntity<List<Map<String, Object>>> getCityAnalytics() {
        return ResponseEntity.ok(analyticsService.getCityAnalytics());
    }

    // =====================================================
    // MOVIE ANALYTICS
    // =====================================================

    /**
     * GET /admin/analytics/movies
     * Movie analytics with optional filters
     */
    @GetMapping("/movies")
    public ResponseEntity<List<Map<String, Object>>> getMovieAnalytics(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return ResponseEntity.ok(analyticsService.getMovieAnalytics(genre, language, dateFrom, dateTo));
    }

    /**
     * GET /admin/analytics/movies/compare
     * Compare specific movies side by side
     */
    @GetMapping("/movies/compare")
    public ResponseEntity<List<Map<String, Object>>> compareMovies(
            @RequestParam(required = false) List<Integer> movieIds) {
        return ResponseEntity.ok(analyticsService.compareMovies(movieIds));
    }

    // =====================================================
    // THEATRE PERFORMANCE RANKING
    // =====================================================

    /**
     * GET /admin/analytics/theaters/rankings
     * Theatre rankings sortable by revenue, occupancy, bookings
     */
    @GetMapping("/theaters/rankings")
    public ResponseEntity<List<Map<String, Object>>> getTheaterRankings(
            @RequestParam(defaultValue = "revenue") String sortBy) {
        return ResponseEntity.ok(analyticsService.getTheaterRankings(sortBy));
    }

    // =====================================================
    // SHOW OCCUPANCY HEATMAP
    // =====================================================

    /**
     * GET /admin/analytics/shows/occupancy-heatmap
     * Show occupancy heatmap (day x time slot)
     */
    @GetMapping("/shows/occupancy-heatmap")
    public ResponseEntity<Map<String, Object>> getShowOccupancyHeatmap(
            @RequestParam(required = false) Integer theaterId,
            @RequestParam(required = false) Integer movieId) {
        return ResponseEntity.ok(analyticsService.getShowOccupancyHeatmap(theaterId, movieId));
    }

    // =====================================================
    // USER ANALYTICS
    // =====================================================

    /**
     * GET /admin/analytics/users
     * User analytics: distribution, top users, trends
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUserAnalytics() {
        return ResponseEntity.ok(analyticsService.getUserAnalytics());
    }

    // =====================================================
    // REVENUE TRENDS (Charts)
    // =====================================================

    /**
     * GET /admin/analytics/revenue/trends
     * Revenue time-series data for line/bar charts
     */
    @GetMapping("/revenue/trends")
    public ResponseEntity<List<Map<String, Object>>> getRevenueTrends(
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return ResponseEntity.ok(analyticsService.getRevenueTrends(period, dateFrom, dateTo));
    }

    // =====================================================
    // OCCUPANCY TRENDS (Charts)
    // =====================================================

    /**
     * GET /admin/analytics/occupancy/trends
     * Occupancy time-series data
     */
    @GetMapping("/occupancy/trends")
    public ResponseEntity<List<Map<String, Object>>> getOccupancyTrends(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return ResponseEntity.ok(analyticsService.getOccupancyTrends(dateFrom, dateTo));
    }

    // =====================================================
    // CANCELLATION TRENDS (Charts)
    // =====================================================

    /**
     * GET /admin/analytics/cancellation/trends
     * Cancellation time-series data
     */
    @GetMapping("/cancellation/trends")
    public ResponseEntity<List<Map<String, Object>>> getCancellationTrends(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return ResponseEntity.ok(analyticsService.getCancellationTrends(dateFrom, dateTo));
    }

    // =====================================================
    // DISTRIBUTION ANALYTICS (Pie Charts)
    // =====================================================

    /**
     * GET /admin/analytics/distributions
     * Genre, language, payment method distributions
     */
    @GetMapping("/distributions")
    public ResponseEntity<Map<String, Object>> getDistributionAnalytics() {
        return ResponseEntity.ok(analyticsService.getDistributionAnalytics());
    }

    // =====================================================
    // CSV EXPORT
    // =====================================================

    /**
     * GET /admin/analytics/export/{type}
     * Export data as JSON (frontend converts to CSV)
     * 
     * Supported types: revenue, bookings, movies, theaters, users, cities
     */
    @GetMapping("/export/{type}")
    public ResponseEntity<List<Map<String, Object>>> getExportData(
            @PathVariable String type,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return ResponseEntity.ok(analyticsService.getExportData(type, dateFrom, dateTo));
    }

    // =====================================================
    // RECOMMENDATION ANALYTICS
    // =====================================================

    /**
     * GET /admin/analytics/recommendations
     * Recommendation analytics: acceptance rate, conversion, theatre/movie breakdown
     */
    @GetMapping("/recommendations")
    public ResponseEntity<Map<String, Object>> getRecommendationAnalytics() {
        return ResponseEntity.ok(analyticsService.getRecommendationAnalytics());
    }

    // =====================================================
    // SEAT TYPE ANALYTICS
    // =====================================================

    /**
     * GET /admin/analytics/seat-types
     * Seat type analytics: revenue, occupancy per seat type
     */
    @GetMapping("/seat-types")
    public ResponseEntity<Map<String, Object>> getSeatTypeAnalytics() {
        return ResponseEntity.ok(analyticsService.getSeatTypeAnalytics());
    }

    // =====================================================
    // PEAK TIME ANALYTICS
    // =====================================================

    /**
     * GET /admin/analytics/peak-times
     * Peak time analytics across all theatres
     */
    @GetMapping("/peak-times")
    public ResponseEntity<List<Map<String, Object>>> getPeakTimeAnalytics() {
        return ResponseEntity.ok(analyticsService.getPeakTimeAnalytics());
    }

    // =====================================================
    // LANGUAGE ANALYTICS
    // =====================================================

    /**
     * GET /admin/analytics/languages
     * Language-wise performance analytics
     */
    @GetMapping("/languages")
    public ResponseEntity<List<Map<String, Object>>> getLanguageAnalytics() {
        return ResponseEntity.ok(analyticsService.getLanguageAnalytics());
    }
}
