package com.driver.bookMyShow.Models;

import com.driver.bookMyShow.Enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * WalletTransaction Entity - Tracks all wallet transactions
 * 
 * Design Principles:
 * - Immutable after creation (audit trail)
 * - Complete transaction history
 * - Support for all transaction types (CREDIT, DEBIT, REFUND)
 * - Linked to user for easy retrieval
 * - Indexed by timestamp for performance
 * 
 * System Design:
 * - Append-only log (no updates/deletes)
 * - Ordered by timestamp descending (latest first)
 * - Transaction reference for traceability
 * - Balance snapshot at transaction time
 */
@Entity
@Table(name = "wallet_transactions", indexes = {
    @Index(name = "idx_user_timestamp", columnList = "user_id,created_at"),
    @Index(name = "idx_transaction_ref", columnList = "transaction_reference")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "balance_before", nullable = false)
    private Double balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private Double balanceAfter;

    @Column(name = "transaction_reference", unique = true)
    private String transactionReference; // Reference to payment/refund

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
