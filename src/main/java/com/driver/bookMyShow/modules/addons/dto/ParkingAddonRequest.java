package com.driver.bookMyShow.modules.addons.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingAddonRequest {
    private String sessionId;
    private Integer userId;
    private String vehicleType; // TWO_WHEELER, FOUR_WHEELER, EV
    private String vehicleNumber;
    private Integer theaterId;
}
