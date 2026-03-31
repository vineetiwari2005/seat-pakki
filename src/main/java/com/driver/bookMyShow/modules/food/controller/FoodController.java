package com.driver.bookMyShow.modules.food.controller;

import com.driver.bookMyShow.common.dto.ApiResponse;
import com.driver.bookMyShow.modules.food.dto.FoodOrderRequest;
import com.driver.bookMyShow.modules.food.dto.FoodOrderResponse;
import com.driver.bookMyShow.modules.food.entity.FoodItem;
import com.driver.bookMyShow.modules.food.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Food Controller - REST API
 * 
 * Endpoints:
 * GET    /api/food/menu/{theaterId}           - Get theater menu
 * GET    /api/food/menu/{theaterId}/{category} - Get menu by category
 * POST   /api/food/order                      - Create order
 * PUT    /api/food/order/{orderNumber}/confirm - Confirm order
 * PUT    /api/food/order/{orderNumber}/prepare - Mark preparing
 * PUT    /api/food/order/{orderNumber}/deliver - Mark delivered
 * DELETE /api/food/order/{orderNumber}         - Cancel order
 * GET    /api/food/order/{orderNumber}         - Get order
 * GET    /api/food/orders/user/{userId}        - Get user orders
 */
@RestController
@RequestMapping("/api/food")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @GetMapping("/menu/{theaterId}")
    public ResponseEntity<ApiResponse<List<FoodItem>>> getMenu(@PathVariable Integer theaterId) {
        List<FoodItem> menu = foodService.getMenu(theaterId);
        return ResponseEntity.ok(ApiResponse.success(menu));
    }

    @GetMapping("/menu/{theaterId}/{category}")
    public ResponseEntity<ApiResponse<List<FoodItem>>> getMenuByCategory(
            @PathVariable Integer theaterId,
            @PathVariable String category) {
        List<FoodItem> menu = foodService.getMenuByCategory(theaterId, category);
        return ResponseEntity.ok(ApiResponse.success(menu));
    }

    @PostMapping("/order")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> createOrder(
            @RequestBody FoodOrderRequest request) {
        FoodOrderResponse response = foodService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", response));
    }

    @PutMapping("/order/{orderNumber}/confirm")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> confirmOrder(
            @PathVariable String orderNumber) {
        FoodOrderResponse response = foodService.confirmOrder(orderNumber);
        return ResponseEntity.ok(ApiResponse.success("Order confirmed", response));
    }

    @PutMapping("/order/{orderNumber}/prepare")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> prepareOrder(
            @PathVariable String orderNumber) {
        FoodOrderResponse response = foodService.prepareOrder(orderNumber);
        return ResponseEntity.ok(ApiResponse.success("Order is being prepared", response));
    }

    @PutMapping("/order/{orderNumber}/deliver")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> deliverOrder(
            @PathVariable String orderNumber) {
        FoodOrderResponse response = foodService.deliverOrder(orderNumber);
        return ResponseEntity.ok(ApiResponse.success("Order delivered", response));
    }

    @DeleteMapping("/order/{orderNumber}")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable String orderNumber) {
        foodService.cancelOrder(orderNumber);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", null));
    }

    @GetMapping("/order/{orderNumber}")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> getOrder(
            @PathVariable String orderNumber) {
        FoodOrderResponse response = foodService.getOrder(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/orders/user/{userId}")
    public ResponseEntity<ApiResponse<List<FoodOrderResponse>>> getUserOrders(
            @PathVariable Integer userId) {
        List<FoodOrderResponse> orders = foodService.getUserOrders(userId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
}
