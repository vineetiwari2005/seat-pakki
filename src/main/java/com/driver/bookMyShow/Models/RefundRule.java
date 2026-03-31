package com.driver.bookMyShow.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * RefundRule Entity - Database-driven refund policy rules
 * 
 * Design Principles:
 * - NO HARDCODED REFUND PERCENTAGES
 * - All rules stored in database
 * - Future-proof: Change rules without code deployment
 * - JPA auto-creates table
 * 
 * Example Data:
 * hours_threshold=1, refund_percentage=100 → 100% refund within 1 hour
 * hours_threshold=6, refund_percentage=75  → 75% refund within 6 hours
 * hours_threshold=12, refund_percentage=50 → 50% refund within 12 hours
 */
@Entity
@Table(name = "refund_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Hours after booking (threshold)
     * Example: 1, 6, 12
     */
    @Column(name = "hours_threshold", nullable = false, unique = true)
    private Integer hoursThreshold;

    /**
     * Refund percentage (0-100)
     * Example: 100, 75, 50, 0
     */
    @Column(name = "refund_percentage", nullable = false)
    private Integer refundPercentage;

    /**
     * Rule description (for admin reference)
     */
    @Column(length = 500)
    private String description;

    /**
     * Active flag (to disable rules without deletion)
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    /**
     * Priority/Order (lower number = higher priority)
     * Used when multiple rules match
     */
    @Column(name = "priority", nullable = false)
    private Integer priority;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
