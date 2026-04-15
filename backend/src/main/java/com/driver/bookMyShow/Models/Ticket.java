package com.driver.bookMyShow.Models;

import com.driver.bookMyShow.Enums.TicketStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Integer id;

    @Column(name = "total_tickets_price", nullable = false)
    private Integer totalTicketsPrice;

    @Column(name = "booked_seats", nullable = false, length = 500)
    private String bookedSeats; // Comma-separated seat numbers

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TicketStatus status = TicketStatus.BOOKED;

    @CreationTimestamp
    @Column(name = "booked_at", updatable = false)
    private LocalDateTime bookedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "refund_amount")
    private Double refundAmount;

    @ManyToOne
    @JoinColumn(name = "show_id", nullable = false)
    @JsonIgnoreProperties({"ticketList", "showSeatList"})
    private Show show;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"ticketList", "password"})
    private User user;

    /**
     * Unified QR Code (Base64 encoded image)
     * Contains: Ticket + Parking (if selected) + Food (if selected)
     * Generated at payment success
     */
    @Lob
    @Column(name = "qr_code_data", columnDefinition = "TEXT")
    private String qrCodeData;

    /**
     * QR Code Payload (JSON)
     * Contains all booking details encoded in the QR
     */
    @Lob
    @Column(name = "qr_payload", columnDefinition = "TEXT")
    private String qrPayload;

    /**
     * Validation Token (SHA-256)
     * Used to verify QR authenticity
     */
    @Column(name = "validation_token", length = 255)
    private String validationToken;

    /**
     * QR Expiry Time
     * Default: Show time + 2 hours
     */
    @Column(name = "qr_expiry_time")
    private LocalDateTime qrExpiryTime;
}
