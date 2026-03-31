package com.driver.bookMyShow.modules.parking.dto;

import com.driver.bookMyShow.modules.parking.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingBookingRequest {
    private Integer theaterId;
    private VehicleType vehicleType;
    private String vehicleNumber;
    private Integer movieTicketId; // Optional
    private LocalDateTime expectedArrival;
}
