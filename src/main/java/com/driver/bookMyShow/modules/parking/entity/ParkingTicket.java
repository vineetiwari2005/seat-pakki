package com.driver.bookMyShow.modules.parking.entity;

import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.modules.parking.enums.ParkingStatus;
import com.driver.bookMyShow.modules.parking.enums.VehicleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ParkingTicket entity - Booking record
 * 
 * System Design:
 * - Links to movie Ticket (optional - parking can be standalone)
 * - Lifecycle independent of movie ticket (loose coupling)
 * - Status-driven state machine (BOOKED → ACTIVE → COMPLETED/CANCELLED)
 * 
 * Transaction Boundary:
 * - Parking booking is a separate transaction from ticket booking
 * - Uses compensating transaction on failure (Saga pattern)
 * 
 * CAP Theorem:
 * - Prioritizes Availability over Consistency (AP system)
 * - Eventual consistency acceptable for parking
 */
@Entity
@Table(name = "parking_tickets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String ticketNumber;

    @Column(nullable = false)
    private String vehicleNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_slot_id", nullable = false)
    private ParkingSlot parkingSlot;

    @OneToOne
    @JoinColumn(name = "movie_ticket_id")
    private Ticket movieTicket; // Optional - can park without movie ticket

    @Column(nullable = false)
    private LocalDateTime entryTime;

    private LocalDateTime exitTime;

    @Column(nullable = false)
    private Integer amountPaid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ParkingStatus status = ParkingStatus.BOOKED;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Business logic: Calculate duration in hours
    public long getDurationInHours() {
        if (exitTime == null) {
            return java.time.Duration.between(entryTime, LocalDateTime.now()).toHours();
        }
        return java.time.Duration.between(entryTime, exitTime).toHours();
    }

    // Business logic: Mark as active (customer arrived)
    public void activate() {
        this.status = ParkingStatus.ACTIVE;
        this.entryTime = LocalDateTime.now();
    }

    // Business logic: Complete parking
    public void complete() {
        this.status = ParkingStatus.COMPLETED;
        this.exitTime = LocalDateTime.now();
    }

    // Business logic: Cancel booking
    public void cancel() {
        this.status = ParkingStatus.CANCELLED;
    }
}
