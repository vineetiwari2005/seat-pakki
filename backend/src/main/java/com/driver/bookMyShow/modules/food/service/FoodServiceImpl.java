package com.driver.bookMyShow.modules.food.service;

import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.Repositories.TicketRepository;
import com.driver.bookMyShow.Repositories.UserRepository;
import com.driver.bookMyShow.common.exceptions.BusinessException;
import com.driver.bookMyShow.common.exceptions.ResourceNotFoundException;
import com.driver.bookMyShow.modules.food.dto.FoodOrderRequest;
import com.driver.bookMyShow.modules.food.dto.FoodOrderResponse;
import com.driver.bookMyShow.modules.food.dto.OrderItemRequest;
import com.driver.bookMyShow.modules.food.dto.OrderItemResponse;
import com.driver.bookMyShow.modules.food.entity.FoodItem;
import com.driver.bookMyShow.modules.food.entity.FoodOrder;
import com.driver.bookMyShow.modules.food.entity.FoodOrderItem;
import com.driver.bookMyShow.modules.food.enums.OrderStatus;
import com.driver.bookMyShow.modules.food.repository.FoodItemRepository;
import com.driver.bookMyShow.modules.food.repository.FoodOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Food Service Implementation
 * 
 * Transaction Management:
 * - Each order operation is atomic
 * - Food order independent of ticket (loose coupling)
 * 
 * Business Rules:
 * - Order can exist without ticket
 * - If ticket cancelled, food order auto-cancels
 * - Price snapshot taken at order time (historical accuracy)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FoodServiceImpl implements FoodService {

    private final FoodItemRepository foodItemRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FoodItem> getMenu(Integer theaterId) {
        return foodItemRepository.findByTheaterIdAndIsAvailableTrue(theaterId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodItem> getMenuByCategory(Integer theaterId, String category) {
        return foodItemRepository.findByTheaterIdAndCategoryAndIsAvailableTrue(
                theaterId, category.toUpperCase());
    }

    @Override
    @Transactional
    public FoodOrderResponse createOrder(FoodOrderRequest request) {
        log.info("Creating food order for user: {}", request.getUserId());

        // 1. Validate user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. Validate ticket if provided
        Ticket ticket = null;
        if (request.getTicketId() != null) {
            ticket = ticketRepository.findById(request.getTicketId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        }

        // 3. Validate all food items exist and are available
        List<FoodItem> foodItems = new ArrayList<>();
        for (OrderItemRequest itemReq : request.getItems()) {
            FoodItem foodItem = foodItemRepository.findById(itemReq.getFoodItemId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Food item not found: " + itemReq.getFoodItemId()));

            if (!foodItem.getIsAvailable()) {
                throw new BusinessException("Food item not available: " + foodItem.getItemName());
            }

            foodItems.add(foodItem);
        }

        // 4. Create order
        FoodOrder order = FoodOrder.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .ticket(ticket)
                .seatNumbers(request.getSeatNumbers())
                .deliveryInstructions(request.getDeliveryInstructions())
                .status(OrderStatus.PENDING)
                .build();

        // 5. Create order items
        for (int i = 0; i < request.getItems().size(); i++) {
            OrderItemRequest itemReq = request.getItems().get(i);
            FoodItem foodItem = foodItems.get(i);

            FoodOrderItem orderItem = FoodOrderItem.builder()
                    .order(order)
                    .foodItem(foodItem)
                    .itemName(foodItem.getItemName()) // Snapshot
                    .quantity(itemReq.getQuantity())
                    .price(foodItem.getPrice()) // Snapshot
                    .specialInstructions(itemReq.getSpecialInstructions())
                    .build();

            order.addItem(orderItem);
        }

        // 6. Calculate total
        order.calculateTotal();

        // 7. Save order
        order = foodOrderRepository.save(order);
        log.info("Food order created: {}", order.getOrderNumber());

        return mapToResponse(order);
    }

    @Override
    @Transactional
    public FoodOrderResponse confirmOrder(String orderNumber) {
        FoodOrder order = foodOrderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Cannot confirm order in status: " + order.getStatus());
        }

        order.confirm();
        order = foodOrderRepository.save(order);
        log.info("Food order confirmed: {}", orderNumber);

        return mapToResponse(order);
    }

    @Override
    @Transactional
    public FoodOrderResponse prepareOrder(String orderNumber) {
        FoodOrder order = foodOrderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException("Cannot prepare order in status: " + order.getStatus());
        }

        order.prepare();
        order = foodOrderRepository.save(order);
        log.info("Food order preparing: {}", orderNumber);

        return mapToResponse(order);
    }

    @Override
    @Transactional
    public FoodOrderResponse deliverOrder(String orderNumber) {
        FoodOrder order = foodOrderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PREPARING) {
            throw new BusinessException("Cannot deliver order in status: " + order.getStatus());
        }

        order.deliver();
        order = foodOrderRepository.save(order);
        log.info("Food order delivered: {}", orderNumber);

        return mapToResponse(order);
    }

    @Override
    @Transactional
    public void cancelOrder(String orderNumber) {
        FoodOrder order = foodOrderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException("Cannot cancel delivered order");
        }

        order.cancel();
        foodOrderRepository.save(order);
        log.info("Food order cancelled: {}", orderNumber);
    }

    @Override
    @Transactional
    public void cancelOrder(Integer orderId) {
        FoodOrder order = foodOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        cancelOrder(order.getOrderNumber());
    }

    @Override
    @Transactional
    public void confirmOrder(Integer orderId) {
        FoodOrder order = foodOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        confirmOrder(order.getOrderNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public FoodOrderResponse getOrder(String orderNumber) {
        FoodOrder order = foodOrderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodOrderResponse> getUserOrders(Integer userId) {
        List<FoodOrder> orders = foodOrderRepository.findByUserId(userId);
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Helper methods
    private String generateOrderNumber() {
        return "FOOD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private FoodOrderResponse mapToResponse(FoodOrder order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .foodItemId(item.getFoodItem().getId())
                        .itemName(item.getItemName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .lineTotal(item.getLineTotal())
                        .specialInstructions(item.getSpecialInstructions())
                        .build())
                .collect(Collectors.toList());

        return FoodOrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser().getId())
                .userName(order.getUser().getName())
                .ticketId(order.getTicket() != null ? order.getTicket().getId() : null)
                .seatNumbers(order.getSeatNumbers())
                .items(items)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .deliveryInstructions(order.getDeliveryInstructions())
                .createdAt(order.getCreatedAt())
                .deliveredAt(order.getDeliveredAt())
                .build();
    }
}
