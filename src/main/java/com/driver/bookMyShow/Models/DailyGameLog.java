package com.driver.bookMyShow.Models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_game_logs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "played_date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyGameLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "score", nullable = false)
    private Integer score;
    
    @Column(name = "played_date", nullable = false)
    private LocalDate playedDate;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (playedDate == null) {
            playedDate = LocalDate.now();
        }
    }
}
