package com.driver.bookMyShow.modules.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private Integer foodItemId;
    private String itemName;
    private Integer quantity;
    private Integer price;
    private Integer lineTotal;
    private String specialInstructions;
}
