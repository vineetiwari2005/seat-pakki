package com.driver.bookMyShow.Services.addon;

import com.driver.bookMyShow.Dtos.PaymentAddOns.FoodAddOnRequest;
import com.driver.bookMyShow.Dtos.PaymentAddOns.FoodAddOnResponse;
import com.driver.bookMyShow.Dtos.PaymentAddOns.ParkingAddOnRequest;
import com.driver.bookMyShow.Dtos.PaymentAddOns.ParkingAddOnResponse;
import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.modules.receipt.dto.ReceiptGenerationResult;
import com.driver.bookMyShow.modules.receipt.service.ReceiptGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment Add-On Orchestrator with Receipt Generation
 * 
 * Design Principles:
 * - Extends existing orchestration logic
 * - Adds receipt generation after successful payment
 * - Receipts are optional artifacts (fail-safe)
 * - Receipt failure does NOT rollback payment
 * 
 * System Design:
 * - Receipt generation in separate transaction (REQUIRES_NEW)
 * - Async receipt generation (non-blocking)
 * - Each receipt type generated independently
 * - Graceful degradation on receipt failure
 * 
 * Transaction Flow:
 * 1. Ticket booking completes (main transaction)
 * 2. Payment is processed
 * 3. Add-ons (parking / food) processed in REQUIRES_NEW
 * 4. Receipts generated asynchronously
 * 5. Booking marked successful
 * 
 * CRITICAL: Receipt failure does NOT affect booking success
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAddOnWithReceiptOrchestrator {

    private final PaymentAddOnOrchestrationService baseOrchestrator;
    private final ReceiptGenerationService receiptGenerationService;

    /**
     * Process parking add-on and generate receipt
     * 
     * @param request Parking add-on request
     * @param ticket Movie ticket
     * @return Parking add-on response
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ParkingAddOnResponse processParkingWithReceipt(ParkingAddOnRequest request, Ticket ticket) {
        // Process parking booking
        ParkingAddOnResponse response = baseOrchestrator.processParking(request, ticket);

        // Generate receipt asynchronously (fail-safe)
        if ("CONFIRMED".equals(response.getStatus()) && response.getParkingTicketId() != null) {
            try {
                log.info("Scheduling async receipt generation for parking ticket: {}", 
                         response.getParkingTicketId());
                
                receiptGenerationService.generateAllReceiptsAsync(
                    ticket.getId(),
                    response.getParkingTicketId(),
                    null
                );
            } catch (Exception e) {
                log.error("Failed to schedule parking receipt generation (non-critical): {}", 
                          e.getMessage());
                // Don't throw - receipt is optional
            }
        }

        return response;
    }

    /**
     * Process food add-on and generate receipt
     * 
     * @param request Food add-on request
     * @param ticket Movie ticket
     * @return Food add-on response
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FoodAddOnResponse processFoodWithReceipt(FoodAddOnRequest request, Ticket ticket) {
        // Process food order
        FoodAddOnResponse response = baseOrchestrator.processFood(request, ticket);

        // Generate receipt asynchronously (fail-safe)
        if (!"FAILED".equals(response.getStatus()) && response.getOrderId() != null) {
            try {
                log.info("Scheduling async receipt generation for food order: {}", 
                         response.getOrderId());
                
                receiptGenerationService.generateAllReceiptsAsync(
                    ticket.getId(),
                    null,
                    response.getOrderId()
                );
            } catch (Exception e) {
                log.error("Failed to schedule food receipt generation (non-critical): {}", 
                          e.getMessage());
                // Don't throw - receipt is optional
            }
        }

        return response;
    }

    /**
     * Generate all receipts for completed booking
     * 
     * Called after successful payment completion
     * 
     * @param ticketId Movie ticket ID
     * @param parkingTicketId Parking ticket ID (optional)
     * @param foodOrderId Food order ID (optional)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateAllReceiptsAfterPayment(
            Integer ticketId,
            Integer parkingTicketId,
            Integer foodOrderId
    ) {
        try {
            log.info("Generating all receipts for ticket: {}, parking: {}, food: {}", 
                     ticketId, parkingTicketId, foodOrderId);

            // Generate receipts asynchronously (non-blocking)
            receiptGenerationService.generateAllReceiptsAsync(
                ticketId,
                parkingTicketId,
                foodOrderId
            ).thenAccept(result -> {
                if (result.isSuccess()) {
                    log.info("All receipts generated successfully for ticket: {}", ticketId);
                } else {
                    log.warn("Some receipts failed: {}", result.getErrors());
                }
            }).exceptionally(throwable -> {
                log.error("Receipt generation failed for ticket: {}", ticketId, throwable);
                return null;
            });

        } catch (Exception e) {
            log.error("Failed to schedule receipt generation (non-critical): {}", e.getMessage());
            // Don't throw - receipt failure should not affect booking
        }
    }

    /**
     * Rollback parking and associated receipt
     * 
     * @param parkingTicketId Parking ticket ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rollbackParkingWithReceipt(Integer parkingTicketId) {
        baseOrchestrator.rollbackParking(parkingTicketId);
        // Receipt cleanup is handled by cascade or manual cleanup if needed
    }

    /**
     * Rollback food order and associated receipt
     * 
     * @param foodOrderId Food order ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rollbackFoodWithReceipt(Integer foodOrderId) {
        baseOrchestrator.rollbackFood(foodOrderId);
        // Receipt cleanup is handled by cascade or manual cleanup if needed
    }
}
