package com.driver.bookMyShow.modules.parking.service;

import com.driver.bookMyShow.modules.parking.dto.ParkingAvailability;
import com.driver.bookMyShow.modules.parking.dto.ParkingBookingRequest;
import com.driver.bookMyShow.modules.parking.dto.ParkingTicketResponse;

/**
 * Parking Service Interface
 * 
 * System Design Principles:
 * - Interface segregation (clients depend on abstractions)
 * - Enables easy mocking for testing
 * - Future: Can have multiple implementations (ParkingServiceV2)
 */
public interface ParkingService {
    
    /**
     * Book parking slot
     * Transaction Boundary: Separate from ticket booking
     * Idempotency: Returns existing booking if duplicate request
     */
    ParkingTicketResponse bookParking(ParkingBookingRequest request);
    
    /**
     * Activate parking (customer arrived)
     * State transition: BOOKED → ACTIVE
     */
    ParkingTicketResponse activateParking(String ticketNumber);
    
    /**
     * Complete parking (customer leaving)
     * State transition: ACTIVE → COMPLETED
     * Triggers payment settlement
     */
    ParkingTicketResponse completeParking(String ticketNumber);
    
    /**
     * Cancel parking booking
     * State transition: BOOKED → CANCELLED
     * Releases slot, triggers refund
     */
    void cancelParking(String ticketNumber);
    
    /**
     * Cancel parking by ticket ID (for add-on integration)
     */
    void cancelParking(Integer ticketId);
    
    /**
     * Confirm parking booking (for payment confirmation)
     */
    void confirmParking(Integer ticketId);
    
    /**
     * Get parking availability for theater
     * Read-heavy operation - candidate for Redis caching
     */
    ParkingAvailability getAvailability(Integer theaterId);
    
    /**
     * Get parking ticket by number
     */
    ParkingTicketResponse getTicket(String ticketNumber);
    
    /**
     * Background job: Release expired bookings
     * Runs every 5 minutes via @Scheduled
     * Prevents slot locking beyond timeout
     */
    void releaseExpiredBookings();
}
