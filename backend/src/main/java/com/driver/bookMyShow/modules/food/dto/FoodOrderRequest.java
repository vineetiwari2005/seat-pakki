package com.driver.bookMyShow.modules.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodOrderRequest {
    private Integer userId;
    private Integer ticketId; // Optional
    private Integer theaterId;
    private String seatNumbers;
    private List<OrderItemRequest> items;
    private String deliveryInstructions;
}
