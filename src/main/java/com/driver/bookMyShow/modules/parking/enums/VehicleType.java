package com.driver.bookMyShow.modules.parking.enums;

/**
 * Vehicle types for parking slot categorization
 * Pricing strategy based on vehicle type
 */
public enum VehicleType {
    TWO_WHEELER,  // Bikes, scooters - ₹20/hr
    FOUR_WHEELER, // Cars, SUVs - ₹50/hr
    EV            // Electric vehicles - ₹40/hr (subsidized)
}
