package com.driver.bookMyShow.Models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_play_logs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_game_play_user_day", columnNames = {"user_id", "played_date"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GamePlayLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "played_date", nullable = false)
    private LocalDate playedDate;

    @CreationTimestamp
    @Column(name = "played_at", nullable = false, updatable = false)
    private LocalDateTime playedAt;
}
