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
public class SpinPaymentResponseDto {
    
    @JsonProperty("transactionId")
    private String transactionId;
    
    @JsonProperty("paymentAmount")
    private BigDecimal paymentAmount;
    
    @JsonProperty("extraSpinsPurchased")
    private Integer extraSpinsPurchased;
    
    @JsonProperty("newBalance")
    private Integer newBalance;
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("message")
    private String message;
}
