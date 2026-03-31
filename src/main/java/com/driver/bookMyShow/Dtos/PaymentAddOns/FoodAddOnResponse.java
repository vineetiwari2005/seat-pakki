package com.driver.bookMyShow.Dtos.PaymentAddOns;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Food & Beverages Add-On Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodAddOnResponse {

    private Integer orderId;
    private List<String> items;
    private Double amount;
    private String status;
    private String message;
}
