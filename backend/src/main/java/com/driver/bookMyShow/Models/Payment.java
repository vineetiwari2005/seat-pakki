package com.driver.bookMyShow.Models;

import com.driver.bookMyShow.Enums.PaymentMethod;
import com.driver.bookMyShow.Enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Payment Entity - Tracks all payment transactions
 * 
 * Features:
 * - Idempotency support with unique transaction ID
 * - Complete payment lifecycle tracking
 * - Support for refunds and cancellations
 * - Price breakdown (base price + taxes + fees)
 */
@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId; // Unique ID for idempotency

    @Column(name = "session_id", nullable = false)
    private String sessionId; // Links to seat lock session

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"ticketList", "password"})
    private User user;

    @ManyToOne
    @JoinColumn(name = "ticket_id")
    @JsonIgnoreProperties({"user", "qrCodeData", "qrPayload"})
    private Ticket ticket; // Null until booking is confirmed

    @Column(name = "base_amount", nullable = false)
    private Double baseAmount; // Ticket price without fees

    @Column(name = "convenience_fee", nullable = false)
    private Double convenienceFee;

    @Column(nullable = false)
    private Double tax;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "temporary_wallet_amount")
    private Double temporaryWalletAmount = 0.0;

    @Column(name = "payable_amount")
    private Double payableAmount;

    @Column(name = "discount_amount")
    private Double discountAmount = 0.0;

    @Column(name = "promo_code")
    private String promoCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    // Split payment breakdown
    @Column(name = "wallet_amount")
    private Double walletAmount = 0.0;
    
    @Column(name = "card_amount")
    private Double cardAmount = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    // External payment gateway details
    @Column(name = "gateway_transaction_id")
    private String gatewayTransactionId;
    
    @Column(name = "gateway_response")
    private String gatewayResponse;

    // Refund details
    @Column(name = "refund_amount")
    private Double refundAmount;
    
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;
    
    @Column(name = "refund_reason")
    private String refundReason;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Calculate total amount
     */
    public void calculateTotal() {
        this.totalAmount = this.baseAmount + this.convenienceFee + this.tax - this.discountAmount;
    }
}
