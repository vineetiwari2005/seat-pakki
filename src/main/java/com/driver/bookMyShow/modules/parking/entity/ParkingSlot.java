package com.driver.bookMyShow.modules.parking.entity;

import com.driver.bookMyShow.modules.parking.enums.VehicleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ParkingSlot entity - Individual parking space
 * 
 * System Design:
 * - Each slot belongs to exactly one ParkingLot
 * - Type determines pricing (Strategy Pattern potential)
 * - Status tracking for real-time availability
 * 
 * Concurrency:
 * - Optimistic locking via @Version (prevents double booking)
 * - Slot locking happens at service layer with timeout
 */
@Entity
@Table(name = "parking_slots", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"parking_lot_id", "slot_number"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "slot_number", nullable = false)
    private String slotNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isOccupied = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer hourlyRate = 50;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_lot_id", nullable = false)
    private ParkingLot parkingLot;

    @Version
    private Long version; // Optimistic locking for concurrent bookings
}
