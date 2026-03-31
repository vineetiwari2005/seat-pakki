package com.driver.bookMyShow.Dtos.ResponseDtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SeatLockResponseDto - Response after locking seats
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatLockResponseDto {

    private String sessionId;
    private Integer showId;
    private List<String> lockedSeats;
    private Long expiryTimeSeconds;
    private String message;
}
