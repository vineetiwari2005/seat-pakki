package com.driver.bookMyShow.Dtos.RequestDtos;

import lombok.Data;

@Data
public class TheaterEntryDto {

    private String name;
    private String address;
    private Integer cityId; // City ID for city association
    private Integer adminUserId; // Optional: assign a THEATER_OWNER user as admin
}
