package com.driver.bookMyShow.Controllers;

import com.driver.bookMyShow.Services.BookingCancellationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.HashMap;
import java.util.Map;

/**
 * BookingManagementController - Manage user bookings
 * 
 * NEW CONTROLLER for booking cancellation and management
 */
@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingManagementController {

    @Autowired
    private BookingCancellationService cancellationService;

    /**
     * Get cancellation policy for a ticket
     * GET /api/bookings/{ticketId}/cancellation-policy
     */
    @GetMapping("/{ticketId}/cancellation-policy")
    public ResponseEntity<?> getCancellationPolicy(@PathVariable Integer ticketId) {
        try {
            BookingCancellationService.CancellationPolicy policy = 
                cancellationService.getRefundEstimate(ticketId);
            return ResponseEntity.ok(policy);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}
