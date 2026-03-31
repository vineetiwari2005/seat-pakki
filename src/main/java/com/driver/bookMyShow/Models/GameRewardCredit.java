package com.driver.bookMyShow.Models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_reward_credits", indexes = {
        @Index(name = "idx_game_reward_user_expiry", columnList = "user_id,expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameRewardCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "game_play_log_id", nullable = false)
    private Integer gamePlayLogId;

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
