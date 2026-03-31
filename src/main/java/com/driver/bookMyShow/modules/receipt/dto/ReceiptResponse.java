package com.driver.bookMyShow.modules.receipt.dto;

import com.driver.bookMyShow.modules.receipt.enums.ReceiptStatus;
import com.driver.bookMyShow.modules.receipt.enums.ReceiptType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Receipt Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptResponse {

    private Integer id;
    private String receiptNumber;
    private ReceiptType receiptType;
    private Integer ticketId;
    private Integer referenceId;
    private String qrCodeData; // Base64 encoded image
    private ReceiptStatus status;
    private Double amount;
    private LocalDateTime generatedAt;
    private LocalDateTime qrExpiryTime;
    private boolean qrExpired;
}
