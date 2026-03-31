package com.driver.bookMyShow.modules.food.enums;

/**
 * Food order lifecycle states
 * State machine:
 * PENDING → CONFIRMED → PREPARING → DELIVERED
 *    ↓
 * CANCELLED
 */
public enum OrderStatus {
    PENDING,    // Order created, payment pending
    CONFIRMED,  // Payment confirmed, sent to kitchen
    PREPARING,  // Kitchen preparing order
    DELIVERED,  // Delivered to seat
    CANCELLED   // Order cancelled (refund initiated)
}
