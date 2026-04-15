package com.driver.bookMyShow.modules.receipt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * QR Code Payload DTO
 * 
 * Data encoded in QR codes for validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCodePayload {

    /**
     * Booking reference (ticket ID)
     */
    private Integer bookingId;

    /**
     * Service type (PARKING / FOOD)
     */
    private String serviceType;

    /**
     * Unique validation token (encrypted)
     */
    private String validationToken;

    /**
     * QR expiry timestamp
     */
    private LocalDateTime expiryTime;

    /**
     * Receipt number
     */
    private String receiptNumber;

    /**
     * Reference entity ID (parking ticket ID or food order ID)
     */
    private Integer referenceId;

    /**
     * Additional metadata (JSON string)
     */
    private String metadata;
}
