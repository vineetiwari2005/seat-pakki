package com.driver.bookMyShow.Models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_leaderboard", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "month_year"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameLeaderboard {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "month_year", nullable = false)
    private String monthYear; // Format: "YYYY-MM"
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "highest_score", nullable = false)
    private Integer highestScore;
    
    @Column(name = "total_plays", nullable = false)
    private Integer totalPlays = 1;
    
    @Column(name = "average_score", nullable = false, columnDefinition = "DECIMAL(10,2)")
    private BigDecimal averageScore;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
