package com.driver.bookMyShow.Dtos.ResponseDtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponseDto {
    private Time time;
    private Date date;
    private String movieName;
    private String theaterName;
    private String address;
    private String bookedSeats;
    private Integer totalPrice;
    
    // Unified QR Code (added for payment success flow)
    private String qrCodeData;  // Base64 encoded QR image
    private String qrPayload;   // JSON payload encoded in QR
    private LocalDateTime qrExpiryTime;
}
