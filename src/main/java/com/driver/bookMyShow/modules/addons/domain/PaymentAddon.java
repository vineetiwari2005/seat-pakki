package com.driver.bookMyShow.modules.addons.domain;

import com.driver.bookMyShow.modules.addons.enums.AddonType;
import com.driver.bookMyShow.modules.addons.enums.AddonStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * PaymentAddon - Represents optional add-ons during payment
 * 
 * Design Principles:
 * - Separate entity from core booking (SRP)
 * - Optional feature (graceful degradation)
 * - Linked to payment session (loose coupling)
 */
@Entity
@Table(name = "payment_addons")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAddon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "payment_id")
    private Integer paymentId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "addon_type", nullable = false)
    private AddonType addonType; // PARKING, FOOD_BEVERAGE

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AddonStatus status; // SELECTED, CONFIRMED, CANCELLED, FAILED

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "reference_id") // ParkingTicket ID or FoodOrder ID
    private Integer referenceId;

    @Column(name = "metadata", length = 2000) // JSON metadata for addon details
    private String metadata;

    @Column(name = "failure_reason")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
