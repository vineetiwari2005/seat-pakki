package com.driver.bookMyShow.Dtos.RequestDtos;

import com.driver.bookMyShow.Enums.SeatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SeatConfigDto - Used by Theatre Admin when adding a show to configure seat types.
 * 
 * Allows specifying:
 * - seatType: PREMIUM, GOLD, SILVER, COUPLE, CLASSIC
 * - count: Number of seats of this type
 * - rowPrefix: Row letter (e.g., "A", "B", "C")
 * - price: Price per seat for this type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatConfigDto {

    private SeatType seatType;
    private int count;
    private String rowPrefix;
    private int price;
}
