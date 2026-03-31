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
public class FoodAddonRequest {
    private String sessionId;
    private Integer userId;
    private Integer theaterId;
    private List<FoodItemSelection> items;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FoodItemSelection {
        private Integer foodItemId;
        private Integer quantity;
    }
}
