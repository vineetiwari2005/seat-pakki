package com.driver.bookMyShow.Dtos.RequestDtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameScoreSubmissionDto {
    private Long userId;
    private Integer score;
    private Integer levelReached;
    private Long gridSizeReached;
}
