package com.driver.bookMyShow.Models;

import com.driver.bookMyShow.Enums.SeatLockStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * SeatLock Entity - Manages temporary seat reservations
 * 
 * Purpose: Prevents race conditions during booking by locking seats temporarily
 * - Locks expire after 15 minutes if payment is not completed
 * - Ensures only one user can book specific seats at a time
 * - Released locks free up seats for other users
 */
@Entity
@Table(name = "seat_locks",
       uniqueConstraints = @UniqueConstraint(columnNames = {"show_id", "seat_number"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "lock_time", nullable = false)
    private LocalDateTime lockTime;

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatLockStatus status = SeatLockStatus.LOCKED;

    @Column(name = "session_id")
    private String sessionId; // For tracking booking session

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Check if lock has expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime) && status == SeatLockStatus.LOCKED;
    }

    /**
     * Check if lock is active (locked and not expired)
     */
    public boolean isActive() {
        return status == SeatLockStatus.LOCKED && LocalDateTime.now().isBefore(expiryTime);
    }
}
