package com.driver.bookMyShow.modules.food.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FoodOrderItem entity - Line items in food order
 * 
 * System Design:
 * - Denormalized: Stores price snapshot at order time
 * - Prevents price change issues (historical accuracy)
 * - Part of FoodOrder aggregate (DDD)
 */
@Entity
@Table(name = "food_order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private FoodOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_item_id", nullable = false)
    private FoodItem foodItem;

    @Column(name = "item_name", nullable = false)
    private String itemName; // Snapshot at order time

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer price; // Snapshot at order time

    @Column(name = "special_instructions")
    private String specialInstructions; // "No ice", "Extra cheese", etc.

    // Calculate line total
    public Integer getLineTotal() {
        return price * quantity;
    }
}
