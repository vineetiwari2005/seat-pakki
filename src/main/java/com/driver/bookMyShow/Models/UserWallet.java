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
 * UserWallet Entity - Separate wallet management for each user
 * 
 * Design Principles:
 * - One wallet per user (1:1 relationship)
 * - Tracks balance separately from User entity
 * - Maintains audit trail with timestamps
 * - Optimistic locking for concurrent updates
 * 
 * Features:
 * - Separate table for wallet data
 * - Version control for concurrent updates
 * - Created/Updated timestamps
 * - Non-negative balance constraint
 */
@Entity
@Table(name = "user_wallets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Builder.Default
    @Column(name = "balance", nullable = false)
    private Double balance = 10000.0; // Default 10,000 for all new users

    @Version
    @Column(name = "version")
    private Long version; // For optimistic locking

    @Column(name = "last_credited_at")
    private LocalDateTime lastCreditedAt;

    @Column(name = "last_debited_at")
    private LocalDateTime lastDebitedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Credit wallet
     */
    public void credit(Double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        this.balance += amount;
        this.lastCreditedAt = LocalDateTime.now();
    }

    /**
     * Debit wallet
     */
    public void debit(Double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        if (this.balance < amount) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }
        this.balance -= amount;
        this.lastDebitedAt = LocalDateTime.now();
    }

    /**
     * Check if sufficient balance
     */
    public boolean hasSufficientBalance(Double amount) {
        return this.balance >= amount;
    }
}
