package com.driver.bookMyShow.Models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_monthly_leaderboard")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameMonthlyLeaderboard {

    @Id
    @Column(name = "month_key", length = 7)
    private String monthKey;

    @Column(name = "highest_score", nullable = false)
    private Integer highestScore;

    @Column(name = "highest_user_id")
    private Integer highestUserId;

    @Column(name = "average_score", nullable = false)
    private Double averageScore;

    @Column(name = "plays_count", nullable = false)
    private Integer playsCount;

    @Column(name = "total_score", nullable = false)
    private Long totalScore;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
