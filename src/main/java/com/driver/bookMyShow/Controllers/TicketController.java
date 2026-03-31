package com.driver.bookMyShow.Controllers;

import com.driver.bookMyShow.Dtos.RequestDtos.TicketEntryDto;
import com.driver.bookMyShow.Dtos.ResponseDtos.TicketResponseDto;
import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.Services.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ticket")
@CrossOrigin(origins = "*")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping("/book")
    public ResponseEntity<TicketResponseDto> ticketBooking(@RequestBody TicketEntryDto ticketEntryDto) {
        try {
            TicketResponseDto result = ticketService.ticketBooking(ticketEntryDto);
            return new ResponseEntity<>(result, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get all bookings for a user
     * GET /ticket/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Ticket>> getUserBookings(@PathVariable Integer userId) {
        try {
            List<Ticket> bookings = ticketService.getUserBookings(userId);
            return new ResponseEntity<>(bookings, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Cancel a booking
     * PUT /ticket/cancel/{ticketId}
     */
    @PutMapping("/cancel/{ticketId}")
    public ResponseEntity<Map<String, String>> cancelBooking(@PathVariable Integer ticketId) {
        try {
            ticketService.cancelBooking(ticketId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Booking cancelled successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Change booking seats
     * PUT /ticket/change/{ticketId}
     * Body: { "newSeats": ["A1", "A2"] }
     */
    @PutMapping("/change/{ticketId}")
    public ResponseEntity<?> changeBooking(
            @PathVariable Integer ticketId,
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> newSeats = (List<String>) request.get("newSeats");
            
            if (newSeats == null || newSeats.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "newSeats list is required");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }
            
            TicketResponseDto result = ticketService.changeBooking(ticketId, newSeats);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
}
