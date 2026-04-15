package com.driver.bookMyShow.Dtos.ResponseDtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Response DTO for Show to avoid circular references
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowResponseDto {
    private Integer id;
    private LocalTime time;
    private LocalDate date;
    private LocalDateTime createdAt;
    
    // Theater information
    private Integer theaterId;
    private String theaterName;
    private String theaterAddress;
    private String theaterCity;
    
    // Movie information
    private Integer movieId;
    private String movieName;
    private String posterUrl;
    
    // Seat availability
    private Integer availableSeats;
    private Integer totalSeats;
}
