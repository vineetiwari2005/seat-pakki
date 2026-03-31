package com.driver.bookMyShow.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "temporary_wallet_credits", indexes = {
        @Index(name = "idx_tmp_wallet_user_expiry", columnList = "user_id,expires_at"),
        @Index(name = "idx_tmp_wallet_ticket", columnList = "ticket_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemporaryWalletCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "ticket_id")
    private Ticket sourceTicket;

    @Column(name = "source_type", nullable = false, length = 30)
    @Builder.Default
    private String sourceType = "DATE_CHANGE";

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "remaining_amount", nullable = false)
    private Double remainingAmount;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}