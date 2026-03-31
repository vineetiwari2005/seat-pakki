package com.driver.bookMyShow.Enums;

/**
 * SeatLockStatus - Status of seat locks
 * - LOCKED: Seat is temporarily held by a user
 * - RELEASED: Lock expired or was manually released
 * - CONFIRMED: Seat booking was confirmed with payment
 */
public enum SeatLockStatus {
    LOCKED,
    RELEASED,
    CONFIRMED
}
