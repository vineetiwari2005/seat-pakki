package com.driver.bookMyShow.modules.parking.service;

import com.driver.bookMyShow.modules.parking.enums.VehicleType;

/**
 * Parking Pricing Service Interface
 * 
 * Handles parking price calculation based on vehicle type
 * Supports future extension (e.g., EV pricing, dynamic pricing)
 * 
 * Design Pattern: Strategy Pattern for extensibility
 */
public interface ParkingPricingService {

    /**
     * Calculate parking price for given vehicle type and duration
     * 
     * @param vehicleType Type of vehicle (2W, 4W, EV)
     * @param durationHours Parking duration in hours
     * @return Total parking price
     */
    Double calculatePrice(VehicleType vehicleType, Integer durationHours);

    /**
     * Get base price for vehicle type (per hour)
     * 
     * @param vehicleType Type of vehicle
     * @return Base price per hour
     */
    Double getBasePricePerHour(VehicleType vehicleType);

    /**
     * Check if pricing is available for vehicle type
     * 
     * @param vehicleType Type of vehicle
     * @return true if pricing exists
     */
    boolean isPricingAvailable(VehicleType vehicleType);
}
