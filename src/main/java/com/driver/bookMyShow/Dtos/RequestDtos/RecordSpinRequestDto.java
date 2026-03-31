package com.driver.bookMyShow.Dtos.RequestDtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordSpinRequestDto {
    
    @JsonProperty("userId")
    private Integer userId;
    
    @JsonProperty("rewardAmount")
    private BigDecimal rewardAmount;
    
    @JsonProperty("timestamp")
    private String timestamp;
    
    @JsonProperty("usedExtraSpin")
    private Boolean usedExtraSpin = false;
}
