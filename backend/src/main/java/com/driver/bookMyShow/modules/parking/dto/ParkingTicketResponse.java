package com.driver.bookMyShow.modules.parking.dto;

import com.driver.bookMyShow.modules.parking.enums.ParkingStatus;
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
public class ParkingTicketResponse {
    private Integer id;
    private String ticketNumber;
    private String vehicleNumber;
    private VehicleType vehicleType;
    private String slotNumber;
    private Integer hourlyRate;
    private Integer amountPaid;
    private ParkingStatus status;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private String parkingLotName;
    private String theaterName;
}
