package com.driver.bookMyShow.Dtos.ResponseDtos;

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
public class SpinRecordResponseDto {
    
    @JsonProperty("transactionId")
    private String transactionId;
    
    @JsonProperty("rewardAmount")
    private BigDecimal rewardAmount;
    
    @JsonProperty("walletId")
    private Long walletId;
    
    @JsonProperty("remainingExtraSpins")
    private Integer remainingExtraSpins;
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("message")
    private String message;
}
