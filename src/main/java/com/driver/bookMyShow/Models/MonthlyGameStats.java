package com.driver.bookMyShow.Models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "monthly_game_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyGameStats {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "month_year", nullable = false, unique = true)
    private String monthYear; // Format: "YYYY-MM"
    
    @Column(name = "highest_score", nullable = false)
    private Integer highestScore;
    
    @Column(name = "average_score", nullable = false, columnDefinition = "DECIMAL(10,2)")
    private BigDecimal averageScore;
    
    @Column(name = "total_players", nullable = false)
    private Integer totalPlayers;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
