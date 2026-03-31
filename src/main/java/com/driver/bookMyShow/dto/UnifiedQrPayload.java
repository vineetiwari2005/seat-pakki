package com.driver.bookMyShow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Unified QR Payload - Single QR for Ticket + Parking + Food
 * 
 * Design: ONE QR CODE containing all booking information
 * - Ticket details (always present)
 * - Parking details (optional, only if parking selected)
 * - Food details (optional, only if food selected)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedQrPayload {

    // ===== TICKET DETAILS (Always present) =====
    private Integer ticketId;
    private Integer userId;
    private String movieName;
    private String theaterName;
    private String showTime;
    private String bookedSeats;
    private Integer totalTicketsPrice;

    // ===== PARKING DETAILS (Optional) =====
    private Boolean hasParkingSelection;
    private Integer parkingTicketId;
    private String vehicleType;
    private String vehicleNumber;
    private Double parkingFee;
    private LocalDateTime parkingEntryTime;
    private LocalDateTime parkingExitTime;

    // ===== FOOD DETAILS (Optional) =====
    private Boolean hasFoodSelection;
    private Integer foodOrderId;
    private String foodItems;  // JSON string or comma-separated
    private Double foodTotal;

    // ===== VALIDATION =====
    private String validationToken;  // SHA-256 token
    private LocalDateTime expiryTime;
    private LocalDateTime generatedAt;

    /**
     * Check if QR is expired
     */
    public boolean isExpired() {
        return expiryTime != null && LocalDateTime.now().isAfter(expiryTime);
    }

    /**
     * Check if parking is included
     */
    public boolean hasParking() {
        return Boolean.TRUE.equals(hasParkingSelection) && parkingTicketId != null;
    }

    /**
     * Check if food is included
     */
    public boolean hasFood() {
        return Boolean.TRUE.equals(hasFoodSelection) && foodOrderId != null;
    }
}
