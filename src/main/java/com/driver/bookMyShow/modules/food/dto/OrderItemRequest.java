package com.driver.bookMyShow.modules.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {
    private Integer foodItemId;
    private Integer quantity;
    private String specialInstructions;
}
