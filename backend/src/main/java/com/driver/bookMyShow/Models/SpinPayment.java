package com.driver.bookMyShow.Models;

import com.driver.bookMyShow.Enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "spin_payments", indexes = {
    @Index(name = "idx_user_id_payment", columnList = "user_id"),
    @Index(name = "idx_payment_status", columnList = "payment_status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpinPayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "payment_method", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    
    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "COMPLETED"; // PENDING, COMPLETED, FAILED
    
    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId;
    
    @Column(name = "extra_spins_purchased", nullable = false)
    private Integer extraSpinsPurchased = 1; // Default 1 spin per purchase
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (paymentStatus == null) {
            paymentStatus = "COMPLETED";
        }
        if (extraSpinsPurchased == null) {
            extraSpinsPurchased = 1;
        }
        if (transactionId == null) {
            // Generate unique transaction ID
            transactionId = "PAY-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);
        }
    }
}
