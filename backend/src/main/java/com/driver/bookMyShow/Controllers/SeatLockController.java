package com.driver.bookMyShow.Controllers;

import com.driver.bookMyShow.Dtos.RequestDtos.SeatLockRequestDto;
import com.driver.bookMyShow.Dtos.ResponseDtos.SeatLockResponseDto;
import com.driver.bookMyShow.Services.SeatLockService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SeatLockController - Manages seat locking operations
 * 
 * NEW ENDPOINTS for seat reservation:
 * - Lock seats before payment
 * - Check seat availability
 * - Release locks on cancellation
 * - Get remaining lock time
 */
@RestController
@RequestMapping("/api/seat-locks")
@CrossOrigin(origins = "*")
public class SeatLockController {

    @Autowired
    private SeatLockService seatLockService;

    /**
     * Lock seats for booking
     * POST /api/seat-lock/lock
     * 
    * Temporarily reserves seats for 15 minutes
     * Returns session ID for payment processing
     */
    @PostMapping("/lock")
    public ResponseEntity<?> lockSeats(@Valid @RequestBody SeatLockRequestDto request) {
        try {
            String sessionId = seatLockService.lockSeats(
                    request.getShowId(),
                    request.getUserId(),
                    request.getSeatNumbers()
            );

            Long expiryTime = seatLockService.getRemainingTime(sessionId);

            SeatLockResponseDto response = SeatLockResponseDto.builder()
                    .sessionId(sessionId)
                    .showId(request.getShowId())
                    .lockedSeats(request.getSeatNumbers())
                    .expiryTimeSeconds(expiryTime)
                    .message("Seats locked successfully. Complete payment within " + 
                            (expiryTime / 60) + " minutes")
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Release locks for a session
     * POST /api/seat-lock/release/{sessionId}
     * 
     * Used when user cancels or payment fails
     */
    @PostMapping("/release/{sessionId}")
    public ResponseEntity<?> releaseLocks(@PathVariable String sessionId) {
        try {
            seatLockService.releaseLocks(sessionId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Locks released successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get locked seats for a show
     * GET /api/seat-lock/show/{showId}/locked-seats
     * 
     * Returns list of currently locked seats
     */
    @GetMapping("/show/{showId}/locked-seats")
    public ResponseEntity<List<String>> getLockedSeats(@PathVariable Integer showId) {
        List<String> lockedSeats = seatLockService.getLockedSeatsForShow(showId);
        return ResponseEntity.ok(lockedSeats);
    }

    /**
     * Check seat availability
     * POST /api/seat-lock/check-availability
     * 
     * Returns availability status for requested seats
     */
    @PostMapping("/check-availability")
    public ResponseEntity<Map<String, Boolean>> checkAvailability(
            @RequestParam Integer showId,
            @RequestBody List<String> seatNumbers) {
        
        Map<String, Boolean> availability = seatLockService.checkSeatsAvailability(
                showId, seatNumbers);
        return ResponseEntity.ok(availability);
    }

    /**
     * Get remaining time for locks
     * GET /api/seat-lock/session/{sessionId}/remaining-time
     * 
     * Returns remaining seconds before lock expires
     */
    @GetMapping("/session/{sessionId}/remaining-time")
    public ResponseEntity<Map<String, Long>> getRemainingTime(@PathVariable String sessionId) {
        Long remainingSeconds = seatLockService.getRemainingTime(sessionId);
        Map<String, Long> response = new HashMap<>();
        response.put("remainingSeconds", remainingSeconds);
        return ResponseEntity.ok(response);
    }

    /**
     * Extend lock time
     * POST /api/seat-lock/session/{sessionId}/extend
     * 
     * Extends lock by specified minutes (max 5 minutes)
     */
    @PostMapping("/session/{sessionId}/extend")
    public ResponseEntity<?> extendLockTime(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "5") int minutes) {
        try {
            if (minutes > 5) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Cannot extend by more than 5 minutes");
                return ResponseEntity.badRequest().body(error);
            }

            seatLockService.extendLockTime(sessionId, minutes);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Lock time extended by " + minutes + " minutes");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
