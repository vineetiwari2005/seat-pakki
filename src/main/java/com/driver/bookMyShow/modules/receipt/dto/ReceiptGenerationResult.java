package com.driver.bookMyShow.modules.receipt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Receipt Generation Result DTO
 * 
 * Contains all receipts generated for a booking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptGenerationResult {

    /**
     * Movie ticket receipt (always present)
     */
    private ReceiptResponse ticketReceipt;

    /**
     * Parking receipt with QR code (optional)
     */
    private ReceiptResponse parkingReceipt;

    /**
     * Food receipt with QR code (optional)
     */
    private ReceiptResponse foodReceipt;

    /**
     * All receipts combined
     */
    private List<ReceiptResponse> allReceipts;

    /**
     * Generation success flag
     */
    private boolean success;

    /**
     * Error messages (if any)
     */
    private List<String> errors;
}
