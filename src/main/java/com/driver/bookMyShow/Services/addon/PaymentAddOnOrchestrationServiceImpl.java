package com.driver.bookMyShow.Services.addon;

import com.driver.bookMyShow.Dtos.PaymentAddOns.*;
import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.modules.food.dto.FoodOrderRequest;
import com.driver.bookMyShow.modules.food.dto.FoodOrderResponse;
import com.driver.bookMyShow.modules.food.dto.OrderItemRequest;
import com.driver.bookMyShow.modules.food.service.FoodService;
import com.driver.bookMyShow.modules.parking.dto.ParkingBookingRequest;
import com.driver.bookMyShow.modules.parking.dto.ParkingTicketResponse;
import com.driver.bookMyShow.modules.parking.service.ParkingPricingService;
import com.driver.bookMyShow.modules.parking.service.ParkingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * Payment Add-On Orchestration Service Implementation
 * 
 * Orchestrates parking and food add-ons during payment stage
 * 
 * Key Design Decisions:
 * 1. Each add-on in SEPARATE transaction (REQUIRES_NEW)
 * 2. Graceful degradation - failures logged but don't block payment
 * 3. Compensating transactions for rollbacks
 * 4. Loose coupling via service interfaces
 * 
 * Transaction Strategy:
 * - Main payment transaction is independent
 * - Add-ons use REQUIRES_NEW for isolation
 * - Rollback uses compensating actions (not DB rollback)
 */
@Slf4j
@Service
public class PaymentAddOnOrchestrationServiceImpl implements PaymentAddOnOrchestrationService {

    @Autowired
    private ParkingService parkingService;

    @Autowired
    private ParkingPricingService parkingPricingService;

    @Autowired
    private FoodService foodService;

    /**
     * Process parking add-on in separate transaction
     * 
     * Uses REQUIRES_NEW to isolate from main payment transaction
     * Failures are caught and logged - don't propagate to caller
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ParkingAddOnResponse processParking(ParkingAddOnRequest request, Ticket ticket) {
        try {
            log.info("Processing parking add-on for ticket: {}", ticket.getId());

            // Calculate duration (default from movie duration)
            Integer durationHours = request.getDurationHours();
            if (durationHours == null) {
                // Default: movie duration + buffer
                durationHours = calculateParkingDuration(ticket);
            }

            // Calculate price using pricing service
            Double parkingPrice = parkingPricingService.calculatePrice(
                request.getVehicleType(),
                durationHours
            );

            // Book parking via existing parking service
            ParkingBookingRequest parkingRequest = ParkingBookingRequest.builder()
                .theaterId(request.getTheaterId())
                .vehicleType(request.getVehicleType())
                .vehicleNumber(request.getVehicleNumber())
                .movieTicketId(ticket.getId())
                .build();

            ParkingTicketResponse parkingTicket = parkingService.bookParking(parkingRequest);

            log.info("Parking booked successfully: {}", parkingTicket.getId());

            return ParkingAddOnResponse.builder()
                .parkingTicketId(parkingTicket.getId())
                .parkingSlotNumber(parkingTicket.getSlotNumber())
                .vehicleType(request.getVehicleType())
                .amount(parkingPrice)
                .status("CONFIRMED")
                .message("Parking booked successfully")
                .build();

        } catch (Exception e) {
            log.error("Failed to process parking add-on: {}", e.getMessage(), e);
            
            return ParkingAddOnResponse.builder()
                .status("FAILED")
                .message("Parking booking failed: " + e.getMessage())
                .amount(0.0)
                .build();
        }
    }

    /**
     * Process food & beverages add-on in separate transaction
     * 
     * Uses REQUIRES_NEW to isolate from main payment transaction
     * Failures are caught and logged - don't propagate to caller
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FoodAddOnResponse processFood(FoodAddOnRequest request, Ticket ticket) {
        try {
            log.info("Processing food add-on for ticket: {}", ticket.getId());

            User user = ticket.getUser();

            // Convert to food service request format
            FoodOrderRequest foodRequest = FoodOrderRequest.builder()
                .userId(user.getId())
                .ticketId(ticket.getId())
                .theaterId(ticket.getShow().getTheater().getId())
                .items(request.getItems().stream()
                    .map(item -> OrderItemRequest.builder()
                        .foodItemId(item.getFoodItemId())
                        .quantity(item.getQuantity())
                        .build())
                    .collect(Collectors.toList()))
                .deliveryInstructions(request.getSpecialInstructions())
                .build();

            // Place food order via existing food service
            FoodOrderResponse foodOrder = foodService.createOrder(foodRequest);

            log.info("Food order placed successfully: {}", foodOrder.getId());

            return FoodAddOnResponse.builder()
                .orderId(foodOrder.getId())
                .items(foodOrder.getItems().stream()
                    .map(item -> item.getItemName() + " x" + item.getQuantity())
                    .collect(Collectors.toList()))
                .amount(foodOrder.getTotalAmount().doubleValue())
                .status(foodOrder.getStatus().name())
                .message("Food order placed successfully")
                .build();

        } catch (Exception e) {
            log.error("Failed to process food add-on: {}", e.getMessage(), e);
            
            return FoodAddOnResponse.builder()
                .status("FAILED")
                .message("Food order failed: " + e.getMessage())
                .amount(0.0)
                .build();
        }
    }

    /**
     * Rollback parking (compensating transaction)
     * 
     * Called if main payment fails after parking was booked
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rollbackParking(Integer parkingTicketId) {
        try {
            log.info("Rolling back parking ticket: {}", parkingTicketId);
            parkingService.cancelParking(parkingTicketId);
            log.info("Parking rollback successful");
        } catch (Exception e) {
            log.error("Failed to rollback parking: {}", e.getMessage(), e);
            // Log but don't throw - best effort rollback
        }
    }

    /**
     * Rollback food order (compensating transaction)
     * 
     * Called if main payment fails after food was ordered
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rollbackFood(Integer foodOrderId) {
        try {
            log.info("Rolling back food order: {}", foodOrderId);
            foodService.cancelOrder(foodOrderId);
            log.info("Food order rollback successful");
        } catch (Exception e) {
            log.error("Failed to rollback food order: {}", e.getMessage(), e);
            // Log but don't throw - best effort rollback
        }
    }

    /**
     * Calculate parking duration based on movie duration
     * 
     * Formula: Movie duration + 30 min buffer (rounded up to hours)
     */
    private Integer calculateParkingDuration(Ticket ticket) {
        try {
            Integer movieDurationMinutes = ticket.getShow().getMovie().getDuration();
            Integer totalMinutes = movieDurationMinutes + 30; // Buffer
            return (int) Math.ceil(totalMinutes / 60.0); // Round up to hours
        } catch (Exception e) {
            log.warn("Could not calculate parking duration, using default 3 hours");
            return 3; // Default fallback
        }
    }
}
