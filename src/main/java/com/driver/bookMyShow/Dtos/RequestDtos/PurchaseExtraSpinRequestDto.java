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
public class PurchaseExtraSpinRequestDto {
    
    @JsonProperty("userId")
    private Integer userId;
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @JsonProperty("paymentMethod")
    private String paymentMethod; // "CARD" or "WALLET"
    
    @JsonProperty("timestamp")
    private String timestamp;
    
    // Card payment fields
    @JsonProperty("transactionId")
    private String transactionId;
    
    @JsonProperty("otp")
    private String otp;
    
    @JsonProperty("cardLast4")
    private String cardLast4;
}
