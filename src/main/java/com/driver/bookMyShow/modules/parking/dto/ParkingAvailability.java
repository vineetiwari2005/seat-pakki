package com.driver.bookMyShow.modules.parking.dto;

import com.driver.bookMyShow.modules.parking.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingAvailability {
    private Integer theaterId;
    private String theaterName;
    private String parkingLotName;
    private Integer totalSlots;
    private Integer availableSlots;
    private Integer twoWheelerSlots;
    private Integer fourWheelerSlots;
    private Integer evSlots;
}
