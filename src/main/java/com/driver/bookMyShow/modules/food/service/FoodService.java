package com.driver.bookMyShow.modules.food.service;

import com.driver.bookMyShow.modules.food.dto.FoodOrderRequest;
import com.driver.bookMyShow.modules.food.dto.FoodOrderResponse;
import com.driver.bookMyShow.modules.food.entity.FoodItem;

import java.util.List;

/**
 * Food Service Interface
 * 
 * Separation of Concerns:
 * - Order lifecycle management separate from menu management
 * - Each method has single responsibility
 */
public interface FoodService {
    
    /**
     * Get menu for a theater
     * Read-heavy - candidate for caching
     */
    List<FoodItem> getMenu(Integer theaterId);
    
    /**
     * Get menu by category
     */
    List<FoodItem> getMenuByCategory(Integer theaterId, String category);
    
    /**
     * Create food order
     * Transaction: Create order + items atomically
     */
    FoodOrderResponse createOrder(FoodOrderRequest request);
    
    /**
     * Confirm order (payment received)
     * State transition: PENDING → CONFIRMED
     */
    FoodOrderResponse confirmOrder(String orderNumber);
    
    /**
     * Mark order as preparing
     * State transition: CONFIRMED → PREPARING
     */
    FoodOrderResponse prepareOrder(String orderNumber);
    
    /**
     * Mark order as delivered
     * State transition: PREPARING → DELIVERED
     */
    FoodOrderResponse deliverOrder(String orderNumber);
    
    /**
     * Cancel order
     * Triggers refund if payment was made
     */
    void cancelOrder(String orderNumber);
    
    /**
     * Cancel order by ID (for add-on integration)
     */
    void cancelOrder(Integer orderId);
    
    /**
     * Confirm order by ID (for add-on integration)
     */
    void confirmOrder(Integer orderId);
    
    /**
     * Get order by number
     */
    FoodOrderResponse getOrder(String orderNumber);
    
    /**
     * Get user's orders
     */
    List<FoodOrderResponse> getUserOrders(Integer userId);
}
