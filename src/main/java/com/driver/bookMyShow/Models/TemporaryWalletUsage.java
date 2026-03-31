package com.driver.bookMyShow.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "temporary_wallet_usage", indexes = {
    @Index(name = "idx_temp_wallet_usage_user", columnList = "user_id"),
    @Index(name = "idx_temp_wallet_usage_payment", columnList = "payment_transaction_id"),
    @Index(name = "idx_temp_wallet_usage_wallet", columnList = "temporary_wallet_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemporaryWalletUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "temporary_wallet_id")
    private Long temporaryWalletId;

    @Column(name = "payment_transaction_id", nullable = false)
    private String paymentTransactionId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "source_type", nullable = false, length = 100)
    private String sourceType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}