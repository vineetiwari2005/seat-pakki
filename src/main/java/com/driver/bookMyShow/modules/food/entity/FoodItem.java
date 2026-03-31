package com.driver.bookMyShow.modules.food.entity;

import com.driver.bookMyShow.Models.Theater;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FoodItem entity - Menu items available at theaters
 * 
 * System Design:
 * - Each theater can have different pricing for same item
 * - Category-based filtering (COMBO, BEVERAGE, SNACK, etc.)
 * - Theater-specific inventory (loose coupling)
 * 
 * Future Enhancement:
 * - Can add availability tracking
 * - Dynamic pricing based on demand
 */
@Entity
@Table(name = "food_items",
       uniqueConstraints = @UniqueConstraint(columnNames = {"theater_id", "item_name"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private String category; // COMBO, POPCORN, BEVERAGE, SNACK, DESSERT

    @Column(nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(name = "image_url")
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id", nullable = false)
    @JsonIgnore
    private Theater theater;

    @Column(name = "is_vegetarian")
    @Builder.Default
    private Boolean isVegetarian = true;
}
