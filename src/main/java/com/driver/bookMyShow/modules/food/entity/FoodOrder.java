package com.driver.bookMyShow.modules.food.entity;

import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.modules.food.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FoodOrder entity - Food booking lifecycle
 * 
 * System Design:
 * - Independent lifecycle from movie ticket (can exist without ticket)
 * - One-to-many with OrderItems (composite pattern)
 * - Status-driven state machine
 * 
 * Transaction Boundary:
 * - Food order creation is separate transaction
 * - If ticket cancellation happens, food order gets auto-cancelled via listener
 * 
 * Eventual Consistency:
 * - Order status updates are eventually consistent
 * - Payment confirmation can be delayed
 */
@Entity
@Table(name = "food_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket; // Optional - can order food without ticket

    @Column(name = "seat_numbers")
    private String seatNumbers; // For delivery: "A12, A13"

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FoodOrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    private Integer totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "delivery_instructions")
    private String deliveryInstructions;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime deliveredAt;

    // Business logic: Add item to order
    public void addItem(FoodOrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    // Business logic: Calculate total
    public void calculateTotal() {
        this.totalAmount = items.stream()
                .mapToInt(item -> item.getPrice() * item.getQuantity())
                .sum();
    }

    // State transitions
    public void confirm() {
        this.status = OrderStatus.CONFIRMED;
    }

    public void prepare() {
        this.status = OrderStatus.PREPARING;
    }

    public void deliver() {
        this.status = OrderStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }
}
