package com.driver.bookMyShow.modules.addons.service;

import com.driver.bookMyShow.Enums.PaymentMethod;
import com.driver.bookMyShow.Models.Payment;
import com.driver.bookMyShow.Services.PaymentService;
import com.driver.bookMyShow.modules.addons.dto.*;
import com.driver.bookMyShow.modules.addons.domain.PaymentAddon;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * EnhancedPaymentOrchestrator - Extends payment with add-ons support
 * 
 * Design Principles:
 * - Decorator Pattern: Extends payment with add-on functionality
 * - Single Responsibility: Coordinates payment + add-ons
 * - Open-Closed: Existing PaymentService remains unchanged
 * - Graceful Degradation: Add-on failures don't affect payment
 * 
 * Transaction Strategy:
 * - Payment and add-ons are in separate transactions
 * - Add-on confirmation happens AFTER payment success
 * - Add-on failures are logged but don't rollback payment
 */
@Service
@Slf4j
public class EnhancedPaymentOrchestrator {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentAddonOrchestrator addonOrchestrator;

    /**
     * Initiate payment with add-ons
     * 
     * Flow:
     * 1. Calculate total amount (tickets + add-ons)
     * 2. Create payment record via existing PaymentService
     * 3. Return combined response
     * 
     * Note: Add-ons are already selected before this call
     */
    @Transactional
    public PaymentWithAddonsResponse initiatePaymentWithAddons(
            PaymentWithAddonsRequest request) throws Exception {
        
        log.info("Initiating payment with add-ons for session: {}", request.getSessionId());

        // Process add-ons if provided
        if (request.getParking() != null) {
            request.getParking().setSessionId(request.getSessionId());
            request.getParking().setUserId(request.getUserId());
            addonOrchestrator.selectParkingAddon(request.getParking());
        }

        if (request.getFood() != null) {
            request.getFood().setSessionId(request.getSessionId());
            request.getFood().setUserId(request.getUserId());
            addonOrchestrator.selectFoodAddon(request.getFood());
        }

        // Calculate total with add-ons
        Double totalWithAddons = addonOrchestrator.calculateTotalWithAddons(
            request.getSessionId(), 
            request.getTicketAmount()
        );

        // Create payment via existing service
        PaymentMethod method = PaymentMethod.valueOf(request.getPaymentMethod());
        Payment payment = paymentService.initiatePayment(
            request.getSessionId(),
            request.getUserId(),
            totalWithAddons, // Total includes add-ons
            method,
            request.getPromoCode(),
            true
        );

        // Get add-on summaries
        List<AddonSummaryDto> addonSummaries = addonOrchestrator.getSessionAddons(
            request.getSessionId()
        );

        // Calculate add-on totals for breakdown
        Double parkingAmount = addonSummaries.stream()
            .filter(a -> a.getType().name().equals("PARKING"))
            .mapToDouble(AddonSummaryDto::getAmount)
            .sum();

        Double foodAmount = addonSummaries.stream()
            .filter(a -> a.getType().name().equals("FOOD_BEVERAGE"))
            .mapToDouble(AddonSummaryDto::getAmount)
            .sum();

        return PaymentWithAddonsResponse.builder()
                .transactionId(payment.getTransactionId())
                .sessionId(request.getSessionId())
                .paymentStatus(payment.getStatus().name())
                .ticketAmount(request.getTicketAmount())
                .parkingAmount(parkingAmount)
                .foodAmount(foodAmount)
                .convenienceFee(payment.getConvenienceFee())
                .tax(payment.getTax())
                .discount(payment.getDiscountAmount())
                .totalAmount(payment.getTotalAmount())
                .addons(addonSummaries)
                .message("Payment initiated successfully. Please complete payment.")
                .build();
    }

    /**
     * Process payment with add-ons
     * 
     * Flow:
     * 1. Process payment via existing PaymentService
     * 2. If payment succeeds, confirm add-ons (separate transaction)
     * 3. If payment fails, cancel add-ons
     * 
     * Note: Add-on failures don't affect payment success
     */
    @Transactional
    public PaymentWithAddonsResponse processPaymentWithAddons(String transactionId) throws Exception {
        log.info("Processing payment with add-ons: {}", transactionId);

        // Process payment via existing service
        Payment payment = paymentService.processPayment(transactionId);

        String sessionId = payment.getSessionId();
        List<AddonSummaryDto> addonSummaries = addonOrchestrator.getSessionAddons(sessionId);

        // Handle add-ons based on payment result
        if (payment.getStatus().name().equals("SUCCESS")) {
            // Confirm add-ons in separate transaction (graceful degradation)
            try {
                addonOrchestrator.confirmAddons(sessionId, payment.getId());
                log.info("Add-ons confirmed for payment: {}", transactionId);
            } catch (Exception e) {
                log.error("Failed to confirm add-ons, but payment succeeded: {}", e.getMessage());
                // Don't throw - payment is successful, add-on failure is acceptable
            }

            // Refresh add-on summaries after confirmation
            addonSummaries = addonOrchestrator.getSessionAddons(sessionId);

        } else {
            // Cancel add-ons if payment failed
            try {
                addonOrchestrator.cancelAddons(sessionId, "Payment failed");
                log.info("Add-ons cancelled for failed payment: {}", transactionId);
            } catch (Exception e) {
                log.error("Failed to cancel add-ons: {}", e.getMessage());
            }
        }

        // Calculate add-on totals
        Double parkingAmount = addonSummaries.stream()
            .filter(a -> a.getType().name().equals("PARKING"))
            .mapToDouble(AddonSummaryDto::getAmount)
            .sum();

        Double foodAmount = addonSummaries.stream()
            .filter(a -> a.getType().name().equals("FOOD_BEVERAGE"))
            .mapToDouble(AddonSummaryDto::getAmount)
            .sum();

        return PaymentWithAddonsResponse.builder()
                .transactionId(payment.getTransactionId())
                .sessionId(sessionId)
                .paymentStatus(payment.getStatus().name())
                .ticketAmount(payment.getBaseAmount() - parkingAmount - foodAmount)
                .parkingAmount(parkingAmount)
                .foodAmount(foodAmount)
                .convenienceFee(payment.getConvenienceFee())
                .tax(payment.getTax())
                .discount(payment.getDiscountAmount())
                .totalAmount(payment.getTotalAmount())
                .addons(addonSummaries)
                .message(payment.getStatus().name().equals("SUCCESS") 
                    ? "Payment completed successfully" 
                    : "Payment failed: " + payment.getGatewayResponse())
                .build();
    }

    /**
     * Get payment summary with add-ons
     */
    public PaymentWithAddonsResponse getPaymentSummary(String sessionId) throws Exception {
        List<AddonSummaryDto> addonSummaries = addonOrchestrator.getSessionAddons(sessionId);

        Double parkingAmount = addonSummaries.stream()
            .filter(a -> a.getType().name().equals("PARKING"))
            .mapToDouble(AddonSummaryDto::getAmount)
            .sum();

        Double foodAmount = addonSummaries.stream()
            .filter(a -> a.getType().name().equals("FOOD_BEVERAGE"))
            .mapToDouble(AddonSummaryDto::getAmount)
            .sum();

        return PaymentWithAddonsResponse.builder()
                .sessionId(sessionId)
                .parkingAmount(parkingAmount)
                .foodAmount(foodAmount)
                .addons(addonSummaries)
                .message("Add-on summary retrieved")
                .build();
    }
}
