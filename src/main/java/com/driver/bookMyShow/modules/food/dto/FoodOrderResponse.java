package com.driver.bookMyShow.modules.food.dto;

import com.driver.bookMyShow.modules.food.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodOrderResponse {
    private Integer id;
    private String orderNumber;
    private Integer userId;
    private String userName;
    private Integer ticketId;
    private String seatNumbers;
    private List<OrderItemResponse> items;
    private Integer totalAmount;
    private OrderStatus status;
    private String deliveryInstructions;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
}
