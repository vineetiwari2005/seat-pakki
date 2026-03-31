package com.driver.bookMyShow.modules.receipt.service;

import com.driver.bookMyShow.modules.receipt.dto.ReceiptGenerationResult;
import com.driver.bookMyShow.modules.receipt.dto.ReceiptResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Receipt Generation Service Interface
 * 
 * Design Principles:
 * - Single Responsibility: Receipt generation logic
 * - Open-Closed: Extensible for new receipt types
 * - Async-friendly: Supports non-blocking generation
 * 
 * System Design:
 * - Receipt generation does NOT block payment completion
 * - Failed receipt generation does NOT rollback booking
 * - Each receipt type generated independently
 * - Supports retry mechanism for failed receipts
 * 
 * Transaction Boundary:
 * - Receipts created in separate transaction from booking
 * - Fail-safe: booking succeeds even if receipts fail
 */
public interface ReceiptGenerationService {

    /**
     * Generate all receipts for a completed booking (synchronous)
     * 
     * @param ticketId Movie ticket ID
     * @param parkingTicketId Parking ticket ID (optional)
     * @param foodOrderId Food order ID (optional)
     * @return Receipt generation result
     */
    ReceiptGenerationResult generateAllReceipts(
            Integer ticketId,
            Integer parkingTicketId,
            Integer foodOrderId
    );

    /**
     * Generate all receipts asynchronously
     * 
     * @param ticketId Movie ticket ID
     * @param parkingTicketId Parking ticket ID (optional)
     * @param foodOrderId Food order ID (optional)
     * @return CompletableFuture with result
     */
    CompletableFuture<ReceiptGenerationResult> generateAllReceiptsAsync(
            Integer ticketId,
            Integer parkingTicketId,
            Integer foodOrderId
    );

    /**
     * Generate ticket receipt (always required)
     * 
     * @param ticketId Movie ticket ID
     * @return Receipt response
     */
    ReceiptResponse generateTicketReceipt(Integer ticketId);

    /**
     * Generate parking receipt with QR code (optional)
     * 
     * @param ticketId Movie ticket ID
     * @param parkingTicketId Parking ticket ID
     * @return Receipt response with QR code
     */
    ReceiptResponse generateParkingReceipt(Integer ticketId, Integer parkingTicketId);

    /**
     * Generate food receipt with QR code (optional)
     * 
     * @param ticketId Movie ticket ID
     * @param foodOrderId Food order ID
     * @return Receipt response with QR code
     */
    ReceiptResponse generateFoodReceipt(Integer ticketId, Integer foodOrderId);

    /**
     * Retry failed receipt generation
     * 
     * @param receiptId Receipt ID to retry
     * @return Updated receipt response
     */
    ReceiptResponse retryReceiptGeneration(Integer receiptId);

    /**
     * Get all receipts for a ticket
     * 
     * @param ticketId Movie ticket ID
     * @return List of receipts
     */
    ReceiptGenerationResult getReceiptsForTicket(Integer ticketId);
}
