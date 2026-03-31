package com.driver.bookMyShow.modules.parking.entity;

import com.driver.bookMyShow.Models.Theater;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ParkingLot entity - One per theater
 * 
 * System Design:
 * - 1:1 relationship with Theater (each theater has one parking lot)
 * - Aggregates ParkingSlots (composition relationship)
 * - Tracks total capacity for availability queries
 * 
 * Scalability:
 * - Can be sharded by theater_id
 * - Read-heavy queries can use Redis cache for availability
 */
@Entity
@Table(name = "parking_lots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer totalSlots;

    @Column(nullable = false)
    private Integer availableSlots;

    @OneToOne
    @JoinColumn(name = "theater_id", nullable = false, unique = true)
    private Theater theater;

    @OneToMany(mappedBy = "parkingLot", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ParkingSlot> slots = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // Business method: Decrease available count (atomicity handled by service layer)
    public void decrementAvailableSlots() {
        if (availableSlots > 0) {
            availableSlots--;
        }
    }

    public void incrementAvailableSlots() {
        if (availableSlots < totalSlots) {
            availableSlots++;
        }
    }
}
