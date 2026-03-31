package com.driver.bookMyShow.Dtos.PaymentAddOns;

import com.driver.bookMyShow.Enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Enhanced Payment Request with Optional Add-Ons
 * 
 * This DTO extends payment functionality WITHOUT modifying existing payment flow.
 * Add-ons are completely optional and processed during payment stage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWithAddOnsRequest {

    // Core payment fields (existing flow)
    @NotNull(message = "Session ID is required")
    private String sessionId;

    @NotNull(message = "User ID is required")
    private Integer userId;

    // Ticket ID (required for linking add-ons)
    @NotNull(message = "Ticket ID is required for add-ons")
    private Integer ticketId;

    @NotNull(message = "Base amount is required")
    private Double baseAmount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String promoCode;

    // Optional Add-Ons (NEW - payment stage)
    @Valid
    private ParkingAddOnRequest parking;

    @Valid
    private FoodAddOnRequest food;
}
