package com.driver.bookMyShow.modules.addons.controller;

import com.driver.bookMyShow.modules.addons.dto.PaymentWithAddonsRequest;
import com.driver.bookMyShow.modules.addons.dto.PaymentWithAddonsResponse;
import com.driver.bookMyShow.modules.addons.service.EnhancedPaymentOrchestrator;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * EnhancedPaymentController - Payment with add-ons workflow
 * 
 * NEW ENDPOINTS (complements existing /api/payment endpoints):
 * - POST /api/payment/with-addons/initiate - Initiate payment with parking & food
 * - POST /api/payment/with-addons/process/{transactionId} - Process payment with add-ons
 * - GET /api/payment/with-addons/summary/{sessionId} - Get payment summary with add-ons
 * 
 * Design Principles:
 * - Non-breaking: Existing /api/payment endpoints remain unchanged
 * - Optional: Clients can use old or new endpoints
 * - Backward compatible: Old flow continues to work
 */
@RestController
@RequestMapping("/api/payment/with-addons")
@CrossOrigin(origins = "*")
@Slf4j
public class EnhancedPaymentController {

    @Autowired
    private EnhancedPaymentOrchestrator enhancedPaymentOrchestrator;

    /**
     * Initiate payment with add-ons
     * POST /api/payment/with-addons/initiate
     * 
     * Accepts parking and food add-ons in the same request
     * Creates payment with total amount including add-ons
     */
    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePaymentWithAddons(
            @Valid @RequestBody PaymentWithAddonsRequest request) {
        try {
            PaymentWithAddonsResponse response = 
                    enhancedPaymentOrchestrator.initiatePaymentWithAddons(request);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to initiate payment with add-ons: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Process payment with add-ons
     * POST /api/payment/with-addons/process/{transactionId}
     * 
     * Processes payment and confirms/cancels add-ons based on result
     * Add-on failures don't affect payment success (graceful degradation)
     */
    @PostMapping("/process/{transactionId}")
    public ResponseEntity<?> processPaymentWithAddons(@PathVariable String transactionId) {
        try {
            PaymentWithAddonsResponse response = 
                    enhancedPaymentOrchestrator.processPaymentWithAddons(transactionId);
            
            if (response.getPaymentStatus().equals("SUCCESS")) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
            }
        } catch (Exception e) {
            log.error("Failed to process payment with add-ons: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get payment summary with add-ons
     * GET /api/payment/with-addons/summary/{sessionId}
     * 
     * Returns price breakdown including add-ons
     */
    @GetMapping("/summary/{sessionId}")
    public ResponseEntity<?> getPaymentSummary(@PathVariable String sessionId) {
        try {
            PaymentWithAddonsResponse response = 
                    enhancedPaymentOrchestrator.getPaymentSummary(sessionId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get payment summary: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
