package com.driver.bookMyShow.Services.addon;

import com.driver.bookMyShow.Dtos.PaymentAddOns.*;
import com.driver.bookMyShow.Models.Ticket;

/**
 * Payment Add-On Orchestration Service Interface
 * 
 * Coordinates optional add-ons (parking, food) during payment stage
 * 
 * Design Principles:
 * - Single Responsibility: Manages add-on coordination only
 * - Open-Closed: Extensible for new add-on types
 * - Dependency Inversion: Depends on abstractions
 * 
 * Key Features:
 * - Graceful degradation: Add-on failures don't block ticket booking
 * - Transaction isolation: Each add-on in separate transaction
 * - Idempotency: Safe to retry
 */
public interface PaymentAddOnOrchestrationService {

    /**
     * Process parking add-on (optional, best-effort)
     * 
     * @param request Parking add-on request
     * @param ticket Associated ticket (for linking)
     * @return Parking response with booking details (null if failed)
     */
    ParkingAddOnResponse processParking(ParkingAddOnRequest request, Ticket ticket);

    /**
     * Process food & beverages add-on (optional, best-effort)
     * 
     * @param request Food add-on request
     * @param ticket Associated ticket (for linking)
     * @return Food order response with details (null if failed)
     */
    FoodAddOnResponse processFood(FoodAddOnRequest request, Ticket ticket);

    /**
     * Rollback parking add-on (compensating transaction)
     * Called if main payment fails after parking was booked
     * 
     * @param parkingTicketId Parking ticket to cancel
     */
    void rollbackParking(Integer parkingTicketId);

    /**
     * Rollback food add-on (compensating transaction)
     * Called if main payment fails after food was ordered
     * 
     * @param foodOrderId Food order to cancel
     */
    void rollbackFood(Integer foodOrderId);
}
