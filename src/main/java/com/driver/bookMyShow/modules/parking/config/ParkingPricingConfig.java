package com.driver.bookMyShow.modules.parking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Parking Pricing Configuration
 * 
 * Design Principles:
 * - Externalized configuration (12-factor app)
 * - No hard-coded values
 * - Runtime configurable via application.properties
 * - Type-safe configuration binding
 * 
 * Usage:
 * application.properties:
 *   parking.pricing.rates.TWO_WHEELER=30.0
 *   parking.pricing.rates.FOUR_WHEELER=50.0
 *   parking.pricing.default-hours=4
 */
@Configuration
@ConfigurationProperties(prefix = "parking.pricing")
@Data
public class ParkingPricingConfig {

    /**
     * Hourly rates per vehicle type
     * Map key: VehicleType enum name (TWO_WHEELER, FOUR_WHEELER, etc.)
     * Map value: Rate per hour in INR
     */
    private Map<String, Double> rates = new HashMap<>();

    /**
     * Default parking duration in hours
     */
    private Integer defaultHours = 4;

    /**
     * Minimum parking hours (for pricing calculation)
     */
    private Integer minimumHours = 1;

    /**
     * Maximum parking hours (for single booking)
     */
    private Integer maximumHours = 12;

    /**
     * Grace period in minutes (no extra charge)
     */
    private Integer gracePeriodMinutes = 15;

    /**
     * Penalty multiplier for overstay (e.g., 1.5x hourly rate)
     */
    private Double overstayMultiplier = 1.5;
}
