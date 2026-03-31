package com.driver.bookMyShow.Models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "temporary_wallet")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemporaryWallet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
    
    @Column(name = "source_type", length = 100)
    private String sourceType; // GAME_REWARD, TICKET_CANCELLATION, TICKET_CHANGE_REFUND
    
    @Column(name = "earned_at", nullable = false, updatable = false)
    private LocalDateTime earnedAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "is_expired", nullable = false)
    private Boolean isExpired = false;
    
    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;
    
    @Column(name = "used_at")
    private LocalDateTime usedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (earnedAt == null) {
            earnedAt = LocalDateTime.now();
        }
        if (expiresAt == null) {
            // 15 days from earned_at
            expiresAt = earnedAt.plusDays(15);
        }
        if (isExpired == null) {
            isExpired = false;
        }
        if (isUsed == null) {
            isUsed = false;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
