package com.driver.bookMyShow.Dtos.PaymentAddOns;

import com.driver.bookMyShow.modules.parking.enums.VehicleType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Parking Add-On Request DTO
 * Used during payment stage to request parking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingAddOnRequest {

    @NotNull(message = "Theater ID is required")
    private Integer theaterId;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    private String vehicleNumber;

    private Integer durationHours; // Optional, default based on movie duration
}
