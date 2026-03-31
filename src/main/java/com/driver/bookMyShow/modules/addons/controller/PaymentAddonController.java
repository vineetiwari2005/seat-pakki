package com.driver.bookMyShow.modules.addons.controller;

import com.driver.bookMyShow.modules.addons.dto.*;
import com.driver.bookMyShow.modules.addons.service.EnhancedPaymentOrchestrator;
import com.driver.bookMyShow.modules.addons.service.PaymentAddonOrchestrator;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PaymentAddonController - Payment with add-ons API
 * 
 * NEW ENDPOINTS (does not affect existing /api/payment endpoints):
 * - POST /api/payment/addons/select-parking - Select parking add-on
 * - POST /api/payment/addons/select-food - Select food add-on
 * - DELETE /api/payment/addons/{sessionId}/{addonType} - Remove add-on
 * - GET /api/payment/addons/{sessionId} - Get session add-ons
 * - POST /api/payment/with-addons/initiate - Initiate payment with add-ons
 * - POST /api/payment/with-addons/process/{transactionId} - Process payment with add-ons
 * 
 * Design Principles:
 * - Thin controller (business logic in service)
 * - REST conventions
 * - Proper HTTP status codes
 * - Clear error messages
 */
@RestController
@RequestMapping("/api/payment/addons")
@CrossOrigin(origins = "*")
@Slf4j
public class PaymentAddonController {

    @Autowired
    private PaymentAddonOrchestrator addonOrchestrator;

    @Autowired
    private EnhancedPaymentOrchestrator enhancedPaymentOrchestrator;

    /**
     * Select parking add-on
     * POST /api/payment/addons/select-parking
     */
    @PostMapping("/select-parking")
    public ResponseEntity<?> selectParking(@Valid @RequestBody ParkingAddonRequest request) {
        try {
            addonOrchestrator.selectParkingAddon(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Parking add-on selected successfully");
            response.put("addons", addonOrchestrator.getSessionAddons(request.getSessionId()));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to select parking: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Select food add-on
     * POST /api/payment/addons/select-food
     */
    @PostMapping("/select-food")
    public ResponseEntity<?> selectFood(@Valid @RequestBody FoodAddonRequest request) {
        try {
            addonOrchestrator.selectFoodAddon(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Food add-on selected successfully");
            response.put("addons", addonOrchestrator.getSessionAddons(request.getSessionId()));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to select food: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Remove add-on
     * DELETE /api/payment/addons/{sessionId}/{addonType}
     */
    @DeleteMapping("/{sessionId}/{addonType}")
    public ResponseEntity<?> removeAddon(
            @PathVariable String sessionId,
            @PathVariable String addonType) {
        try {
            addonOrchestrator.removeAddon(sessionId, addonType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Add-on removed successfully");
            response.put("addons", addonOrchestrator.getSessionAddons(sessionId));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to remove add-on: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get session add-ons
     * GET /api/payment/addons/{sessionId}
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSessionAddons(@PathVariable String sessionId) {
        try {
            List<AddonSummaryDto> addons = addonOrchestrator.getSessionAddons(sessionId);
            return ResponseEntity.ok(addons);
        } catch (Exception e) {
            log.error("Failed to get session add-ons: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get add-on pricing info
     * GET /api/payment/addons/pricing/{sessionId}
     */
    @GetMapping("/pricing/{sessionId}")
    public ResponseEntity<?> getAddonPricing(
            @PathVariable String sessionId,
            @RequestParam Double ticketAmount) {
        try {
            Double total = addonOrchestrator.calculateTotalWithAddons(sessionId, ticketAmount);
            List<AddonSummaryDto> addons = addonOrchestrator.getSessionAddons(sessionId);
            
            Double addonTotal = addons.stream()
                    .mapToDouble(AddonSummaryDto::getAmount)
                    .sum();
            
            Map<String, Object> response = new HashMap<>();
            response.put("ticketAmount", ticketAmount);
            response.put("addonTotal", addonTotal);
            response.put("totalAmount", total);
            response.put("addons", addons);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to calculate pricing: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
