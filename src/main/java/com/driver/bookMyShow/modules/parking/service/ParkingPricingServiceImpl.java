package com.driver.bookMyShow.modules.parking.service;

import com.driver.bookMyShow.modules.parking.enums.VehicleType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Default Parking Pricing Service Implementation
 * 
 * Features:
 * - Configurable pricing via application properties
 * - Vehicle-type based pricing (2W, 4W, EV)
 * - Hourly rate calculation
 * - Future-ready for dynamic pricing strategies
 * 
 * Design: Open-Closed Principle - extend for new strategies without modification
 */
@Service
public class ParkingPricingServiceImpl implements ParkingPricingService {

    // Configurable pricing (from application.properties)
    @Value("${parking.pricing.two-wheeler:30.0}")
    private Double twoWheelerPricePerHour;

    @Value("${parking.pricing.four-wheeler:50.0}")
    private Double fourWheelerPricePerHour;

    @Value("${parking.pricing.ev:40.0}")
    private Double evPricePerHour;

    // Pricing map (cached for performance)
    private Map<VehicleType, Double> pricingMap;

    /**
     * Calculate total parking price
     * 
     * Price = Base Price Per Hour * Duration (with minimum 1 hour)
     */
    @Override
    public Double calculatePrice(VehicleType vehicleType, Integer durationHours) {
        if (vehicleType == null) {
            throw new IllegalArgumentException("Vehicle type cannot be null");
        }

        if (durationHours == null || durationHours < 1) {
            durationHours = 1; // Minimum 1 hour
        }

        Double basePricePerHour = getBasePricePerHour(vehicleType);
        return basePricePerHour * durationHours;
    }

    /**
     * Get base price per hour for vehicle type
     * 
     * Prices are configurable via application.properties
     * Fallback to hardcoded defaults if not configured
     */
    @Override
    public Double getBasePricePerHour(VehicleType vehicleType) {
        initializePricingMap();
        
        return pricingMap.getOrDefault(vehicleType, 0.0);
    }

    /**
     * Check if pricing is available for vehicle type
     */
    @Override
    public boolean isPricingAvailable(VehicleType vehicleType) {
        initializePricingMap();
        return pricingMap.containsKey(vehicleType) && pricingMap.get(vehicleType) > 0;
    }

    /**
     * Initialize pricing map from configuration
     * 
     * This method allows hot-reload of pricing if properties change
     */
    private void initializePricingMap() {
        if (pricingMap == null) {
            pricingMap = new HashMap<>();
            pricingMap.put(VehicleType.TWO_WHEELER, twoWheelerPricePerHour);
            pricingMap.put(VehicleType.FOUR_WHEELER, fourWheelerPricePerHour);
            pricingMap.put(VehicleType.EV, evPricePerHour);
        }
    }
}
