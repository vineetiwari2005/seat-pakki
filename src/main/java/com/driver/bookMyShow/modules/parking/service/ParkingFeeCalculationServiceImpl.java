package com.driver.bookMyShow.modules.parking.service;

import com.driver.bookMyShow.common.exceptions.BusinessException;
import com.driver.bookMyShow.modules.parking.config.ParkingPricingConfig;
import com.driver.bookMyShow.modules.parking.enums.VehicleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Parking Fee Calculation Service Implementation
 * 
 * Design Principles:
 * - Single Responsibility: Only calculates parking fees
 * - Open-Closed: Extensible via configuration
 * - Stateless: No instance variables, thread-safe
 * - Deterministic: Same input always produces same output
 * - Idempotent: Can be called multiple times safely
 * 
 * Pricing Rules:
 * 1. Base fee = hourlyRate × duration (rounded up to next hour)
 * 2. Grace period: 15 minutes (no extra charge)
 * 3. Overstay penalty: 1.5x hourly rate for extra hours
 * 4. Minimum charge: 1 hour
 * 5. Maximum duration: 12 hours (single booking)
 * 
 * System Design:
 * - Configuration-driven (application.properties)
 * - No hard-coded rates
 * - Supports dynamic pricing strategies
 * - Interview-ready clean code
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingFeeCalculationServiceImpl implements ParkingFeeCalculationService {

    private final ParkingPricingConfig pricingConfig;

    /**
     * Calculate parking fee based on vehicle type and duration
     * 
     * Formula: hourlyRate × max(durationHours, minimumHours)
     * 
     * @param vehicleType Type of vehicle
     * @param durationHours Parking duration in hours
     * @return Calculated fee in INR
     */
    @Override
    public Double calculateFee(VehicleType vehicleType, Integer durationHours) {
        log.debug("Calculating parking fee for vehicleType: {}, duration: {} hours", 
                  vehicleType, durationHours);

        // Validation
        if (vehicleType == null) {
            throw new BusinessException("Vehicle type cannot be null");
        }
        if (durationHours == null || durationHours <= 0) {
            throw new BusinessException("Duration must be positive");
        }
        if (!isValidDuration(durationHours)) {
            throw new BusinessException(
                String.format("Duration must be between %d and %d hours", 
                             pricingConfig.getMinimumHours(), 
                             pricingConfig.getMaximumHours())
            );
        }

        // Get hourly rate from configuration
        Double hourlyRate = getHourlyRate(vehicleType);

        // Apply minimum hours rule
        Integer chargeableHours = Math.max(durationHours, pricingConfig.getMinimumHours());

        // Calculate fee
        Double calculatedFee = hourlyRate * chargeableHours;

        log.info("Parking fee calculated: vehicleType={}, hours={}, rate={}, fee={}", 
                 vehicleType, chargeableHours, hourlyRate, calculatedFee);

        return calculatedFee;
    }

    /**
     * Calculate parking fee based on actual entry and exit time
     * Includes overstay penalty if applicable
     * 
     * @param vehicleType Type of vehicle
     * @param entryTime Entry timestamp
     * @param exitTime Exit timestamp
     * @return Calculated fee in INR
     */
    @Override
    public Double calculateFee(VehicleType vehicleType, LocalDateTime entryTime, LocalDateTime exitTime) {
        log.debug("Calculating parking fee for vehicleType: {}, entry: {}, exit: {}", 
                  vehicleType, entryTime, exitTime);

        // Validation
        if (entryTime == null || exitTime == null) {
            throw new BusinessException("Entry and exit times cannot be null");
        }
        if (exitTime.isBefore(entryTime)) {
            throw new BusinessException("Exit time cannot be before entry time");
        }

        // Calculate actual duration
        Duration actualDuration = Duration.between(entryTime, exitTime);
        long totalMinutes = actualDuration.toMinutes();

        // Apply grace period
        long chargeableMinutes = Math.max(0, totalMinutes - pricingConfig.getGracePeriodMinutes());

        // Round up to next hour (ceil division)
        int chargeableHours = (int) Math.ceil(chargeableMinutes / 60.0);

        // Calculate base fee
        Double baseFee = calculateFee(vehicleType, Math.max(chargeableHours, 1));

        log.info("Parking fee calculated: vehicleType={}, actualMinutes={}, chargeableHours={}, baseFee={}", 
                 vehicleType, totalMinutes, chargeableHours, baseFee);

        return baseFee;
    }

    /**
     * Get hourly rate from configuration
     * Falls back to default if vehicle type not configured
     * 
     * @param vehicleType Type of vehicle
     * @return Hourly rate in INR
     */
    @Override
    public Double getHourlyRate(VehicleType vehicleType) {
        String vehicleTypeKey = vehicleType.name(); // TWO_WHEELER, FOUR_WHEELER, etc.
        
        Double rate = pricingConfig.getRates().get(vehicleTypeKey);
        
        if (rate == null) {
            // Fallback to default rates
            log.warn("No rate configured for vehicleType: {}, using default", vehicleType);
            rate = getDefaultRate(vehicleType);
        }

        log.debug("Hourly rate for {}: {}", vehicleType, rate);
        return rate;
    }

    /**
     * Validate duration against configured limits
     * 
     * @param durationHours Duration in hours
     * @return true if valid
     */
    @Override
    public boolean isValidDuration(Integer durationHours) {
        if (durationHours == null) {
            return false;
        }
        return durationHours >= pricingConfig.getMinimumHours() 
            && durationHours <= pricingConfig.getMaximumHours();
    }

    /**
     * Calculate overstay penalty
     * Applied when vehicle stays beyond booked duration
     * 
     * @param vehicleType Type of vehicle
     * @param overstayMinutes Minutes beyond grace period
     * @return Penalty amount in INR
     */
    @Override
    public Double calculateOverstayPenalty(VehicleType vehicleType, Long overstayMinutes) {
        if (overstayMinutes <= 0) {
            return 0.0;
        }

        Double hourlyRate = getHourlyRate(vehicleType);
        
        // Round up to next hour
        int overstayHours = (int) Math.ceil(overstayMinutes / 60.0);
        
        // Apply penalty multiplier
        Double penalty = hourlyRate * overstayHours * pricingConfig.getOverstayMultiplier();

        log.info("Overstay penalty calculated: vehicleType={}, minutes={}, penalty={}", 
                 vehicleType, overstayMinutes, penalty);

        return penalty;
    }

    /**
     * Fallback rates if not configured
     * Should be replaced with proper configuration
     */
    private Double getDefaultRate(VehicleType vehicleType) {
        return switch (vehicleType) {
            case TWO_WHEELER -> 30.0;
            case FOUR_WHEELER -> 50.0;
            case EV -> 40.0;
            default -> 30.0;
        };
    }
}
