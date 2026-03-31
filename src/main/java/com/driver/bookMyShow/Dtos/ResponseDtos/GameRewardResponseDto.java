package com.driver.bookMyShow.Dtos.ResponseDtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRewardResponseDto {
    private Long rewardId;
    private BigDecimal rewardAmount;
    private String message;
    private Boolean isReward;
    private String expiresAt;
    private String messageDetail;
    
    public GameRewardResponseDto(BigDecimal rewardAmount, String message, Boolean isReward) {
        this.rewardAmount = rewardAmount;
        this.message = message;
        this.isReward = isReward;
    }
}
