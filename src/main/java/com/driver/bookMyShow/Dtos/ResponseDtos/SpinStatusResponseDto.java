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
public class SpinStatusResponseDto {
    
    @JsonProperty("hasSpunToday")
    private Boolean hasSpunToday;
    
    @JsonProperty("extraSpinsBalance")
    private Integer extraSpinsBalance;
    
    @JsonProperty("timeUntilNextSpin")
    private String timeUntilNextSpin;
}
