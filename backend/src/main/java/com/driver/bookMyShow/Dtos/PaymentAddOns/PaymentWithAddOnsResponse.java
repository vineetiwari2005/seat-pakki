package com.driver.bookMyShow.Dtos.PaymentAddOns;

import com.driver.bookMyShow.Dtos.ResponseDtos.PaymentResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Enhanced Payment Response with Add-On Details
 * 
 * Provides complete breakdown of payment + optional add-ons
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWithAddOnsResponse {

    // Core payment response (existing)
    private PaymentResponseDto payment;

    // Add-on responses (optional)
    private ParkingAddOnResponse parking;
    private FoodAddOnResponse food;

    // Consolidated totals
    private Double ticketAmount;
    private Double parkingAmount;
    private Double foodAmount;
    private Double totalAmount;

    private String message;
}
