package com.driver.bookMyShow.Dtos.PaymentAddOns;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Food & Beverages Add-On Request DTO
 * Used during payment stage to order food
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodAddOnRequest {

    @NotEmpty(message = "At least one food item is required")
    private List<FoodItemRequest> items;

    private String specialInstructions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FoodItemRequest {
        @NotNull(message = "Food item ID is required")
        private Integer foodItemId;

        @NotNull(message = "Quantity is required")
        private Integer quantity;
    }
}
