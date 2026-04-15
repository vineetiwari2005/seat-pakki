package com.driver.bookMyShow.modules.addons.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWithAddonsRequest {
    private String sessionId;
    private Integer userId;
    private Double ticketAmount; // Base ticket price
    private String paymentMethod;
    private String promoCode;
    
    // Optional add-ons
    private ParkingAddonRequest parking;
    private FoodAddonRequest food;
}
