package com.driver.bookMyShow.Dtos.RequestDtos;

import lombok.Data;

import java.sql.Date;
import java.sql.Time;
import java.util.List;

@Data
public class ShowEntryDto {

    private Time showStartTime;
    private Date showDate;
    private Integer theaterId;
    private Integer movieId;

    /**
     * Optional seat configuration for the show.
     * When provided, these seat configs are used instead of copying from theater layout.
     * Theatre admin specifies seat types, counts, row prefixes, and prices.
     */
    private List<SeatConfigDto> seatConfigs;

    /**
     * Whether to use custom seat config (true) or copy from theater layout (false/null).
     * If true, seatConfigs must be provided.
     */
    private Boolean useCustomSeats;
}
