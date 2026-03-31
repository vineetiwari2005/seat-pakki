package com.driver.bookMyShow.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "spin_transactions", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_spin_date", columnList = "spin_date"),
    @Index(name = "idx_user_spin_date", columnList = "user_id,spin_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpinTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    
    @Column(name = "reward_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal rewardAmount;
    
    @Column(name = "spin_date", nullable = false)
    private LocalDate spinDate;
    
    @Column(name = "used_extra_spin", nullable = false)
    private Boolean usedExtraSpin = false;
    
    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (spinDate == null) {
            spinDate = LocalDate.now();
        }
        if (usedExtraSpin == null) {
            usedExtraSpin = false;
        }
        if (transactionId == null) {
            // Generate unique transaction ID
            transactionId = "SPIN-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);
        }
    }
}
