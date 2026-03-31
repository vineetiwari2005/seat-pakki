package com.driver.bookMyShow.modules.parking.service;

import com.driver.bookMyShow.modules.parking.enums.VehicleType;

import java.time.LocalDateTime;

/**
 * Parking Fee Calculation Service Interface
 * 
 * Design Principles:
 * - Single Responsibility Principle (only handles fee calculation)
 * - Open-Closed Principle (extensible for new pricing rules)
 * - Deterministic and idempotent
 * - No side effects
 * 
 * System Design:
 * - Stateless service (pure function - input → output)
 * - Configurable pricing (no hard-coded values)
 * - Supports time-based calculations
 * - Extensible for dynamic pricing (surge, discounts, etc.)
 */
public interface ParkingFeeCalculationService {

    /**
     * Calculate parking fee based on vehicle type and duration
     * 
     * @param vehicleType Type of vehicle
     * @param durationHours Parking duration in hours
     * @return Calculated fee in INR
     */
    Double calculateFee(VehicleType vehicleType, Integer durationHours);

    /**
     * Calculate parking fee based on entry and exit time
     * 
     * @param vehicleType Type of vehicle
     * @param entryTime Entry timestamp
     * @param exitTime Exit timestamp
     * @return Calculated fee in INR (includes overstay penalties if applicable)
     */
    Double calculateFee(VehicleType vehicleType, LocalDateTime entryTime, LocalDateTime exitTime);

    /**
     * Get hourly rate for a vehicle type
     * 
     * @param vehicleType Type of vehicle
     * @return Hourly rate in INR
     */
    Double getHourlyRate(VehicleType vehicleType);

    /**
     * Validate if duration is within acceptable limits
     * 
     * @param durationHours Duration in hours
     * @return true if valid, false otherwise
     */
    boolean isValidDuration(Integer durationHours);

    /**
     * Calculate overstay penalty
     * 
     * @param vehicleType Type of vehicle
     * @param overstayMinutes Minutes beyond grace period
     * @return Penalty amount in INR
     */
    Double calculateOverstayPenalty(VehicleType vehicleType, Long overstayMinutes);
}
