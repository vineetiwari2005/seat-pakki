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
public class ExtraSpinsBalanceResponseDto {
    
    @JsonProperty("balance")
    private Integer balance;
    
    @JsonProperty("totalPurchased")
    private Integer totalPurchased;
    
    @JsonProperty("totalUsed")
    private Integer totalUsed;
}
