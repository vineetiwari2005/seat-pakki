package com.driver.bookMyShow.Services;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpinStatusData {
    private Boolean hasSpunToday;
    private Integer extraSpinsBalance;
    private String timeUntilNextSpin;
}
