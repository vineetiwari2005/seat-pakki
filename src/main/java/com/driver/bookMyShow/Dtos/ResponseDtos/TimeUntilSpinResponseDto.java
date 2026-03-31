package com.driver.bookMyShow.Dtos.ResponseDtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeUntilSpinResponseDto {
    
    @JsonProperty("timeRemaining")
    private String timeRemaining; // Format: HH:MM:SS
    
    @JsonProperty("secondsRemaining")
    private Long secondsRemaining;
}
