package com.driver.bookMyShow.modules.addons.service;

import com.driver.bookMyShow.modules.addons.dto.*;
import com.driver.bookMyShow.modules.addons.domain.PaymentAddon;

import java.util.List;

/**
 * PaymentAddonOrchestrator - Interface for payment-stage add-on coordination
 * 
 * Design Principles:
 * - Interface Segregation Principle
 * - Dependency Inversion (depend on abstraction)
 * - Open-Closed (new add-on types can be added)
 */
public interface PaymentAddonOrchestrator {
    
    /**
     * Select parking add-on for session
     * Creates tentative parking booking
     * 
     * @return PaymentAddon with SELECTED status
     */
    PaymentAddon selectParkingAddon(ParkingAddonRequest request) throws Exception;
    
    /**
     * Select food add-on for session
     * Creates tentative food order
     * 
     * @return PaymentAddon with SELECTED status
     */
    PaymentAddon selectFoodAddon(FoodAddonRequest request) throws Exception;
    
    /**
     * Remove selected add-on before payment
     */
    void removeAddon(String sessionId, String addonType) throws Exception;
    
    /**
     * Get all add-ons for a session
     */
    List<AddonSummaryDto> getSessionAddons(String sessionId);
    
    /**
     * Calculate total amount including all add-ons
     */
    Double calculateTotalWithAddons(String sessionId, Double ticketAmount);
    
    /**
     * Confirm all add-ons after successful payment
     * This is called by the payment success handler
     * Failures here should NOT rollback payment
     */
    void confirmAddons(String sessionId, Integer paymentId);
    
    /**
     * Cancel all add-ons if payment fails
     */
    void cancelAddons(String sessionId, String reason);
}
