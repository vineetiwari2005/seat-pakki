package com.driver.bookMyShow.modules.parking.enums;

/**
 * Parking ticket lifecycle states
 * State machine: BOOKED → ACTIVE → COMPLETED
 *                  ↓
 *              CANCELLED
 */
public enum ParkingStatus {
    BOOKED,     // Slot reserved, customer hasn't arrived
    ACTIVE,     // Customer arrived, vehicle parked
    COMPLETED,  // Customer left, payment settled
    CANCELLED   // Booking cancelled (refund issued)
}
