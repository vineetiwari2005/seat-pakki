package com.driver.bookMyShow.Dtos.PaymentAddOns;

import com.driver.bookMyShow.modules.parking.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Parking Add-On Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingAddOnResponse {

    private Integer parkingTicketId;
    private String parkingSlotNumber;
    private VehicleType vehicleType;
    private Double amount;
    private String status;
    private String message;
}
