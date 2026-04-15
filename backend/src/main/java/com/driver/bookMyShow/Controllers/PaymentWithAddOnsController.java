package com.driver.bookMyShow.Controllers;

import com.driver.bookMyShow.Dtos.PaymentAddOns.*;
import com.driver.bookMyShow.Dtos.ResponseDtos.PaymentResponseDto;
import com.driver.bookMyShow.Models.Payment;
import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.Repositories.TicketRepository;
import com.driver.bookMyShow.Services.PaymentService;
import com.driver.bookMyShow.Services.addon.PaymentAddOnOrchestrationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced Payment Controller with Add-On Support
 * 
 * NEW ENDPOINT: /api/payment/with-addons
 * 
 * This controller EXTENDS payment functionality WITHOUT modifying existing endpoints.
 * 
 * Design Principles:
 * - Open-Closed Principle: New functionality via new endpoint
 * - Single Responsibility: Coordinates payment + add-ons
 * - Backward Compatibility: Existing /api/payment endpoints unchanged
 * 
 * Flow:
 * 1. Process main payment (existing PaymentService)
 * 2. If payment successful, process optional add-ons
 * 3. If add-ons fail, ticket booking still succeeds (graceful degradation)
 * 4. If main payment fails, rollback any successful add-ons
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class PaymentWithAddOnsController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PaymentAddOnOrchestrationService addOnOrchestrationService;

    /**
     * Process Payment with Optional Add-Ons
     * 
     * POST /api/payment/with-addons
     * 
     * Flow:
     * 1. Initiate payment (core flow - unchanged)
     * 2. Process payment through gateway
     * 3. On success: Book ticket + process add-ons
     * 4. On failure: Rollback everything
     * 
     * Add-ons are best-effort - failures don't cancel ticket
     */
    @PostMapping("/with-addons")
    public ResponseEntity<?> processPaymentWithAddOns(
            @Valid @RequestBody PaymentWithAddOnsRequest request) {
        
        log.info("Processing payment with add-ons for session: {}", request.getSessionId());

        ParkingAddOnResponse parkingResponse = null;
        FoodAddOnResponse foodResponse = null;

        try {
            // Step 1: Initiate payment (existing flow)
            Payment payment = paymentService.initiatePayment(
                request.getSessionId(),
                request.getUserId(),
                request.getBaseAmount(),
                request.getPaymentMethod(),
                request.getPromoCode(),
                true
            );

            log.info("Payment initiated: {}", payment.getTransactionId());

            // Step 2: Process payment (existing flow)
            payment = paymentService.processPayment(payment.getTransactionId());

            if (!"SUCCESS".equals(payment.getStatus().name())) {
                log.error("Payment failed: {}", payment.getTransactionId());
                
                Map<String, String> error = new HashMap<>();
                error.put("error", "Payment processing failed");
                error.put("transactionId", payment.getTransactionId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            log.info("Payment successful: {}", payment.getTransactionId());

            // Step 3: Get ticket from database
            Integer ticketId = request.getTicketId();
            Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
            
            log.info("Processing add-ons for ticket: {}", ticketId);

            // Step 4: Process optional add-ons (NEW - best effort)
            // Note: Add-on failures don't affect ticket booking

            if (request.getParking() != null) {
                log.info("Processing parking add-on...");
                parkingResponse = addOnOrchestrationService.processParking(
                    request.getParking(),
                    ticket
                );
            }

            if (request.getFood() != null) {
                log.info("Processing food add-on...");
                foodResponse = addOnOrchestrationService.processFood(
                    request.getFood(),
                    ticket
                );
            }

            // Step 5: Build response
            PaymentResponseDto paymentResponse = buildPaymentResponse(payment);

            PaymentWithAddOnsResponse response = PaymentWithAddOnsResponse.builder()
                .payment(paymentResponse)
                .parking(parkingResponse)
                .food(foodResponse)
                .ticketAmount(payment.getTotalAmount())
                .parkingAmount(parkingResponse != null ? parkingResponse.getAmount() : 0.0)
                .foodAmount(foodResponse != null ? foodResponse.getAmount() : 0.0)
                .totalAmount(calculateTotalAmount(payment, parkingResponse, foodResponse))
                .message("Booking successful!")
                .build();

            log.info("Payment with add-ons completed successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing payment with add-ons: {}", e.getMessage(), e);

            // Rollback add-ons if they were processed
            if (parkingResponse != null && "CONFIRMED".equals(parkingResponse.getStatus())) {
                log.info("Rolling back parking...");
                addOnOrchestrationService.rollbackParking(parkingResponse.getParkingTicketId());
            }

            if (foodResponse != null && !"FAILED".equals(foodResponse.getStatus())) {
                log.info("Rolling back food order...");
                addOnOrchestrationService.rollbackFood(foodResponse.getOrderId());
            }

            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get Add-On Pricing (NEW endpoint)
     * 
     * GET /api/payment/addons/pricing
     * 
     * Returns pricing information for parking and food items
     * Used by frontend to display prices before payment
     */
    @GetMapping("/addons/pricing")
    public ResponseEntity<?> getAddOnPricing() {
        // TODO: Implement pricing endpoint
        Map<String, Object> pricing = new HashMap<>();
        pricing.put("parking", getParkingPricing());
        pricing.put("food", "Use /api/food/items for food pricing");
        
        return ResponseEntity.ok(pricing);
    }

    /**
     * Build payment response DTO from payment entity
     */
    private PaymentResponseDto buildPaymentResponse(Payment payment) {
        return PaymentResponseDto.builder()
            .transactionId(payment.getTransactionId())
            .sessionId(payment.getSessionId())
            .baseAmount(payment.getBaseAmount())
            .convenienceFee(payment.getConvenienceFee())
            .tax(payment.getTax())
            .totalAmount(payment.getTotalAmount())
            .discountAmount(payment.getDiscountAmount())
            .status(payment.getStatus())
            .paymentMethod(payment.getPaymentMethod())
            .message("Payment processed successfully")
            .build();
    }

    /**
     * Calculate total amount including add-ons
     */
    private Double calculateTotalAmount(Payment payment, 
                                       ParkingAddOnResponse parking,
                                       FoodAddOnResponse food) {
        Double total = payment.getTotalAmount();
        
        if (parking != null && "CONFIRMED".equals(parking.getStatus())) {
            total += parking.getAmount();
        }
        
        if (food != null && !"FAILED".equals(food.getStatus())) {
            total += food.getAmount();
        }
        
        return total;
    }

    /**
     * Get parking pricing info (stub)
     */
    private Map<String, String> getParkingPricing() {
        Map<String, String> pricing = new HashMap<>();
        pricing.put("TWO_WHEELER", "₹30/hour");
        pricing.put("FOUR_WHEELER", "₹50/hour");
        pricing.put("EV", "₹40/hour");
        return pricing;
    }
}
