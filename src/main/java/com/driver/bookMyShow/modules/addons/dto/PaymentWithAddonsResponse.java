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
public class PaymentWithAddonsResponse {
    private String transactionId;
    private String sessionId;
    private String paymentStatus;
    
    // Price breakdown
    private Double ticketAmount;
    private Double parkingAmount;
    private Double foodAmount;
    private Double convenienceFee;
    private Double tax;
    private Double discount;
    private Double totalAmount;
    
    // Add-on statuses
    private List<AddonSummaryDto> addons;
    
    private String message;
    private Long remainingTime; // For seat lock
}
