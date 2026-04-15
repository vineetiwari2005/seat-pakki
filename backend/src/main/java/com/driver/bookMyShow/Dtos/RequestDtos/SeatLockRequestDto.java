package com.driver.bookMyShow.Dtos.RequestDtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SeatLockRequestDto - Request to lock seats temporarily
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatLockRequestDto {

    @NotNull(message = "Show ID is required")
    private Integer showId;

    @NotNull(message = "User ID is required")
    private Integer userId;

    @NotEmpty(message = "At least one seat must be selected")
    private List<String> seatNumbers;
}
