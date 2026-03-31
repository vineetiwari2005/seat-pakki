package com.driver.bookMyShow.Controllers;

import com.driver.bookMyShow.Services.BookingService;
import com.driver.bookMyShow.Services.BookingCancellationService;
import com.driver.bookMyShow.Services.BookingDateChangeService;
import com.driver.bookMyShow.common.dto.ApiResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

/**
 * UserBookingController - REST API for user booking history and cancellation
 * 
 * Strict Layered Architecture:
 *   Controller → Service → Repository → Database
 * 
 * No business logic here. All logic delegated to:
 *   - BookingService (fetching/displaying bookings)
 *   - BookingCancellationService (cancellation with time-decay refund)
 * 
 * Endpoints:
 * GET  /api/bookings/user/{userId}           - All bookings
 * GET  /api/bookings/user/{userId}/upcoming   - Upcoming shows
 * GET  /api/bookings/user/{userId}/past       - Past shows
 * GET  /api/bookings/user/{userId}/paginated  - Paginated bookings
 * GET  /api/bookings/user/{userId}/count      - Booking count
 * GET  /api/bookings/{ticketId}/refund-estimate - Refund estimate (time-decay)
 * POST /api/bookings/{ticketId}/cancel         - Cancel booking
 */
@Slf4j
@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class UserBookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingCancellationService cancellationService;

    @Autowired
    private BookingDateChangeService bookingDateChangeService;

    @Autowired
    private com.driver.bookMyShow.Services.EmailService emailService;

    /**
     * Get all bookings for a user (latest first)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<BookingService.BookingDTO>>> getUserBookings(
            @PathVariable Integer userId) {
        try {
            List<BookingService.BookingDTO> bookings = bookingService.getUserBookings(userId);
            return ResponseEntity.ok(ApiResponse.success(bookings));
        } catch (Exception e) {
            log.error("Error fetching user bookings: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get upcoming shows for a user
     */
    @GetMapping("/user/{userId}/upcoming")
    public ResponseEntity<ApiResponse<List<BookingService.BookingDTO>>> getUpcomingBookings(
            @PathVariable Integer userId) {
        try {
            List<BookingService.BookingDTO> bookings = bookingService.getUpcomingBookings(userId);
            return ResponseEntity.ok(ApiResponse.success(bookings));
        } catch (Exception e) {
            log.error("Error fetching upcoming bookings: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get past shows for a user
     */
    @GetMapping("/user/{userId}/past")
    public ResponseEntity<ApiResponse<List<BookingService.BookingDTO>>> getPastBookings(
            @PathVariable Integer userId) {
        try {
            List<BookingService.BookingDTO> bookings = bookingService.getPastBookings(userId);
            return ResponseEntity.ok(ApiResponse.success(bookings));
        } catch (Exception e) {
            log.error("Error fetching past bookings: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get paginated bookings for a user
     */
    @GetMapping("/user/{userId}/paginated")
    public ResponseEntity<ApiResponse<Page<BookingService.BookingDTO>>> getUserBookingsPaginated(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<BookingService.BookingDTO> bookings = bookingService.getPaginatedBookings(userId, page, size);
            return ResponseEntity.ok(ApiResponse.success(bookings));
        } catch (Exception e) {
            log.error("Error fetching paginated bookings: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get booking count for a user
     */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<ApiResponse<BookingCountResponse>> getUserBookingCount(
            @PathVariable Integer userId) {
        try {
            long totalBookings = bookingService.getBookingCount(userId);
            BookingCountResponse response = BookingCountResponse.builder()
                .userId(userId)
                .totalBookings(totalBookings)
                .build();
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error fetching booking count: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get refund estimate for a booking (uses time-decay formula)
     * Formula: Refund = P × (T_remaining - 30) / (T_total - 30)
     */
    @GetMapping("/{ticketId}/refund-estimate")
    public ResponseEntity<ApiResponse<BookingCancellationService.CancellationPolicy>> getRefundEstimate(
            @PathVariable Integer ticketId) {
        try {
            BookingCancellationService.CancellationPolicy policy = 
                cancellationService.getRefundEstimate(ticketId);
            return ResponseEntity.ok(ApiResponse.success(policy));
        } catch (Exception e) {
            log.error("Error calculating refund estimate: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Cancel a booking and process time-decay refund
     * Formula: Refund = P × (T_remaining - 30) / (T_total - 30)
     * Blocked if T_remaining ≤ 30 minutes
     */
    @PostMapping("/{ticketId}/cancel")
    public ResponseEntity<ApiResponse<BookingCancellationService.CancellationResult>> cancelBooking(
            @PathVariable Integer ticketId,
            @RequestParam Integer userId) {
        try {
            log.info("Processing cancellation for ticket: {} by user: {}", ticketId, userId);
            BookingCancellationService.CancellationResult result = 
                cancellationService.cancelTicket(ticketId, userId);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error cancelling booking: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get date-change options where theater is profitable and requested seats are available.
     */
    @GetMapping("/{ticketId}/change-date-options")
    public ResponseEntity<ApiResponse<List<BookingDateChangeService.DateChangeOption>>> getChangeDateOptions(
            @PathVariable Integer ticketId,
            @RequestParam Integer userId) {
        try {
            List<BookingDateChangeService.DateChangeOption> options =
                    bookingDateChangeService.getProfitableDateOptions(ticketId, userId);
            return ResponseEntity.ok(ApiResponse.success(options));
        } catch (Exception e) {
            log.error("Error fetching change date options: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Change ticket date to selected show in a DB transaction.
     * Releases current seats and reserves same seat numbers in selected show.
     */
    @PostMapping("/{ticketId}/change-date")
    public ResponseEntity<ApiResponse<BookingDateChangeService.DateChangeResult>> changeTicketDate(
            @PathVariable Integer ticketId,
            @RequestParam Integer userId,
            @RequestParam(required = false) Integer newShowId) {
        try {
            BookingDateChangeService.DateChangeResult result =
                    bookingDateChangeService.changeTicketDate(ticketId, userId);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error changing ticket date: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Email ticket PDF generated by frontend download flow
     * The uploaded PDF is sent as-is to preserve exact visual quality/colors.
     */
    @PostMapping(value = "/email-ticket", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> emailTicketPdf(
            @RequestParam String email,
            @RequestParam MultipartFile pdfFile,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String bookingId,
            @RequestParam(required = false) String movieName,
            @RequestParam(required = false) String theaterName,
            @RequestParam(required = false) String showDate,
            @RequestParam(required = false) String showTime,
            @RequestParam(required = false) String seats,
            @RequestParam(required = false) String totalPaid,
            @RequestParam(required = false) String parkingInfo,
            @RequestParam(required = false) String parkingAmount,
            @RequestParam(required = false) String foodItems,
            @RequestParam(required = false) String foodAmount) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Email is required"));
            }

            if (pdfFile == null || pdfFile.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("PDF file is required"));
            }

            String finalSubject = (subject != null && !subject.trim().isEmpty())
                    ? subject.trim()
                    : (transactionId != null && !transactionId.trim().isEmpty()
                        ? "Your Ticket - " + transactionId.trim()
                        : "Your Ticket - BookMyShow");

                String plainBody = "Booking Confirmed. Please find your ticket PDF attached.";
                String htmlBody = buildTicketEmailHtml(
                    movieName, theaterName, showDate, showTime, seats, bookingId, totalPaid,
                    parkingInfo, parkingAmount, foodItems, foodAmount
                );
            String fileName = (pdfFile.getOriginalFilename() != null && !pdfFile.getOriginalFilename().trim().isEmpty())
                    ? pdfFile.getOriginalFilename().trim()
                    : "ticket.pdf";

            emailService.sendTicketPdfEmail(
                    email.trim(),
                    finalSubject,
                    plainBody,
                    htmlBody,
                    pdfFile.getBytes(),
                    fileName
            );

            return ResponseEntity.ok(ApiResponse.success("Ticket emailed successfully", "SUCCESS"));
        } catch (Exception e) {
            log.error("Error sending ticket PDF email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

        private String buildTicketEmailHtml(
                        String movieName,
                        String theaterName,
                        String showDate,
                        String showTime,
                        String seats,
                        String bookingId,
                        String totalPaid,
                        String parkingInfo,
                        String parkingAmount,
                        String foodItems,
                        String foodAmount
        ) {
                String safeMovie = valueOrFallback(movieName, "N/A");
                String safeTheater = valueOrFallback(theaterName, "N/A");
                String safeDate = valueOrFallback(showDate, "N/A");
                String safeTime = valueOrFallback(showTime, "N/A");
                String safeSeats = valueOrFallback(seats, "N/A");
                String safeBookingId = valueOrFallback(bookingId, "N/A");
                String safeTotal = valueOrFallback(totalPaid, "N/A");

                String parkingSection = "";
                if (parkingInfo != null && !parkingInfo.isBlank()) {
                        parkingSection = """
                                <h3 style=\"margin:16px 0 8px;font-size:15px;color:#111;\">🚗 Parking Information</h3>
                                <table cellpadding=\"6\" cellspacing=\"0\" style=\"border-collapse:collapse;border:1px solid #ddd;\">
                                    <tr><td style=\"border:1px solid #ddd;\"><b>Details</b></td><td style=\"border:1px solid #ddd;\">%s</td></tr>
                                    <tr><td style=\"border:1px solid #ddd;\"><b>Amount</b></td><td style=\"border:1px solid #ddd;\">₹%s</td></tr>
                                </table>
                                """.formatted(escapeHtml(valueOrFallback(parkingInfo, "N/A")), escapeHtml(valueOrFallback(parkingAmount, "0.00")));
                }

                String foodSection = "";
                if (foodItems != null && !foodItems.isBlank()) {
                        foodSection = """
                                <h3 style=\"margin:16px 0 8px;font-size:15px;color:#111;\">🍿 Food Information</h3>
                                <table cellpadding=\"6\" cellspacing=\"0\" style=\"border-collapse:collapse;border:1px solid #ddd;\">
                                    <tr><td style=\"border:1px solid #ddd;\"><b>Items</b></td><td style=\"border:1px solid #ddd;\">%s</td></tr>
                                    <tr><td style=\"border:1px solid #ddd;\"><b>Amount</b></td><td style=\"border:1px solid #ddd;\">₹%s</td></tr>
                                </table>
                                """.formatted(escapeHtml(valueOrFallback(foodItems, "N/A")), escapeHtml(valueOrFallback(foodAmount, "0.00")));
                }

                return """
                        <div style=\"font-family:Arial,sans-serif;color:#222;line-height:1.45;\">
                            <h2 style=\"margin:0 0 10px;color:#111;\">Booking Confirmed ✅</h2>
                            <p style=\"margin:0 0 14px;\">Hello,</p>
                            <p style=\"margin:0 0 14px;\">Your ticket has been confirmed successfully. Please find your ticket PDF attached.</p>

                            <h3 style=\"margin:12px 0 8px;font-size:15px;color:#111;\">🎟️ Ticket Details</h3>
                            <table cellpadding=\"6\" cellspacing=\"0\" style=\"border-collapse:collapse;border:1px solid #ddd;\">
                                <tr><td style=\"border:1px solid #ddd;\"><b>Movie</b></td><td style=\"border:1px solid #ddd;\">%s</td></tr>
                                <tr><td style=\"border:1px solid #ddd;\"><b>Theater</b></td><td style=\"border:1px solid #ddd;\">%s</td></tr>
                                <tr><td style=\"border:1px solid #ddd;\"><b>Date</b></td><td style=\"border:1px solid #ddd;\">%s</td></tr>
                                <tr><td style=\"border:1px solid #ddd;\"><b>Time</b></td><td style=\"border:1px solid #ddd;\">%s</td></tr>
                                <tr><td style=\"border:1px solid #ddd;\"><b>Seats</b></td><td style=\"border:1px solid #ddd;\">%s</td></tr>
                                <tr><td style=\"border:1px solid #ddd;\"><b>Booking ID</b></td><td style=\"border:1px solid #ddd;\">%s</td></tr>
                                <tr><td style=\"border:1px solid #ddd;\"><b>Total Paid</b></td><td style=\"border:1px solid #ddd;\">₹%s</td></tr>
                            </table>
                            %s
                            %s
                            <p style=\"margin-top:18px;\">Have a great show!<br/>SeatPakki Team</p>
                        </div>
                        """.formatted(
                                escapeHtml(safeMovie),
                                escapeHtml(safeTheater),
                                escapeHtml(safeDate),
                                escapeHtml(safeTime),
                                escapeHtml(safeSeats),
                                escapeHtml(safeBookingId),
                                escapeHtml(safeTotal),
                                parkingSection,
                                foodSection
                        );
        }

        private String valueOrFallback(String value, String fallback) {
                return (value == null || value.trim().isEmpty()) ? fallback : value.trim();
        }

        private String escapeHtml(String value) {
                if (value == null) {
                        return "";
                }
                return value
                                .replace("&", "&amp;")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;")
                                .replace("\"", "&quot;")
                                .replace("'", "&#39;");
        }

    // ==================== Response DTOs ====================

    @Data
    @lombok.Builder
    public static class BookingCountResponse {
        private Integer userId;
        private long totalBookings;
    }
}
