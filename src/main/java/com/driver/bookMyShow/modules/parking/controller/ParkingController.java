package com.driver.bookMyShow.modules.parking.controller;

import com.driver.bookMyShow.common.dto.ApiResponse;
import com.driver.bookMyShow.modules.parking.dto.ParkingAvailability;
import com.driver.bookMyShow.modules.parking.dto.ParkingBookingRequest;
import com.driver.bookMyShow.modules.parking.dto.ParkingTicketResponse;
import com.driver.bookMyShow.modules.parking.entity.ParkingSlot;
import com.driver.bookMyShow.modules.parking.enums.VehicleType;
import com.driver.bookMyShow.modules.parking.repository.ParkingSlotRepository;
import com.driver.bookMyShow.modules.parking.service.ParkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parking Controller - REST API
 * 
 * API Design:
 * - RESTful conventions (POST for create, GET for read, PUT for update, DELETE for cancel)
 * - Consistent response format via ApiResponse wrapper
 * - Proper HTTP status codes
 * 
 * Endpoints:
 * POST   /api/parking/book         - Book parking slot
 * PUT    /api/parking/{id}/activate - Activate parking (customer arrived)
 * PUT    /api/parking/{id}/complete - Complete parking (customer leaving)
 * DELETE /api/parking/{id}          - Cancel parking
 * GET    /api/parking/availability/{theaterId} - Get availability
 * GET    /api/parking/{ticketNumber} - Get ticket details
 */
@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
public class ParkingController {

    private final ParkingService parkingService;

    @Autowired
    private ParkingSlotRepository parkingSlotRepository;

    @PostMapping("/book")
    public ResponseEntity<ApiResponse<ParkingTicketResponse>> bookParking(
            @RequestBody ParkingBookingRequest request) {
        
        ParkingTicketResponse response = parkingService.bookParking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Parking booked successfully", response));
    }

    @PutMapping("/{ticketNumber}/activate")
    public ResponseEntity<ApiResponse<ParkingTicketResponse>> activateParking(
            @PathVariable String ticketNumber) {
        
        ParkingTicketResponse response = parkingService.activateParking(ticketNumber);
        return ResponseEntity.ok(ApiResponse.success("Parking activated", response));
    }

    @PutMapping("/{ticketNumber}/complete")
    public ResponseEntity<ApiResponse<ParkingTicketResponse>> completeParking(
            @PathVariable String ticketNumber) {
        
        ParkingTicketResponse response = parkingService.completeParking(ticketNumber);
        return ResponseEntity.ok(ApiResponse.success("Parking completed", response));
    }

    @DeleteMapping("/{ticketNumber}")
    public ResponseEntity<ApiResponse<Void>> cancelParking(@PathVariable String ticketNumber) {
        parkingService.cancelParking(ticketNumber);
        return ResponseEntity.ok(ApiResponse.success("Parking cancelled", null));
    }

    @GetMapping("/availability/{theaterId}")
    public ResponseEntity<ApiResponse<ParkingAvailability>> getAvailability(
            @PathVariable Integer theaterId) {
        
        ParkingAvailability availability = parkingService.getAvailability(theaterId);
        return ResponseEntity.ok(ApiResponse.success(availability));
    }

    @GetMapping("/{ticketNumber}")
    public ResponseEntity<ApiResponse<ParkingTicketResponse>> getTicket(
            @PathVariable String ticketNumber) {
        
        ParkingTicketResponse response = parkingService.getTicket(ticketNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get parking pricing for vehicle type and duration from DB
     * GET /api/parking/pricing?vehicleType=2W&hours=3&theaterId=1
     */
    @GetMapping("/pricing")
    public ResponseEntity<?> getParkingPricing(
            @RequestParam String vehicleType,
            @RequestParam int hours,
            @RequestParam(required = false) Integer theaterId) {
        
        // Map frontend vehicle codes to DB enum
        VehicleType vt;
        switch (vehicleType) {
            case "2W": vt = VehicleType.TWO_WHEELER; break;
            case "3W": // 3-wheelers use FOUR_WHEELER rates
            case "4W": vt = VehicleType.FOUR_WHEELER; break;
            default:
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid vehicle type"));
        }
        
        // Fetch hourly rate from DB for this theatre
        int hourlyRate = 50; // fallback default
        if (theaterId != null) {
            List<ParkingSlot> slots = parkingSlotRepository.findByTheaterIdAndVehicleType(theaterId, vt);
            if (!slots.isEmpty()) {
                hourlyRate = slots.get(0).getHourlyRate();
            }
        }
        
        int totalPrice = hourlyRate * hours;
        
        Map<String, Object> result = new HashMap<>();
        result.put("vehicleType", vehicleType);
        result.put("hours", hours);
        result.put("hourlyRate", hourlyRate);
        result.put("price", totalPrice);
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
