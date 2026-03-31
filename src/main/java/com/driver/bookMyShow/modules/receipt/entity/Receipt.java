package com.driver.bookMyShow.modules.receipt.entity;

import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.modules.receipt.enums.ReceiptStatus;
import com.driver.bookMyShow.modules.receipt.enums.ReceiptType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Receipt Entity - Base class for all receipt types
 * 
 * Design Principles:
 * - Single Responsibility: Represents a receipt
 * - Open-Closed: Extensible via ReceiptType enum
 * - Independent lifecycle from booking
 * 
 * System Design:
 * - Receipts are optional artifacts (fail-safe)
 * - Each receipt has unique ID and QR code
 * - Async generation friendly (status tracking)
 * - Idempotent generation (receiptNumber as unique key)
 * 
 * Transaction Boundary:
 * - Receipt creation is in separate transaction from booking
 * - Booking success does NOT depend on receipt generation
 * - Failed receipt generation does not rollback booking
 */
@Entity
@Table(
    name = "receipts",
    indexes = {
        @Index(name = "idx_receipt_number", columnList = "receipt_number"),
        @Index(name = "idx_ticket_id", columnList = "ticket_id"),
        @Index(name = "idx_reference_id_type", columnList = "reference_id, receipt_type")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Unique receipt identifier (user-facing)
     * Format: RCP-YYYYMMDD-XXXXXX
     */
    @Column(name = "receipt_number", nullable = false, unique = true, length = 50)
    private String receiptNumber;

    /**
     * Receipt type (TICKET, PARKING, FOOD)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "receipt_type", nullable = false)
    private ReceiptType receiptType;

    /**
     * Reference to movie ticket (always present)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    /**
     * Reference ID to specific entity
     * - For TICKET: ticket ID
     * - For PARKING: parking ticket ID
     * - For FOOD: food order ID
     */
    @Column(name = "reference_id", nullable = false)
    private Integer referenceId;

    /**
     * QR code data (Base64 encoded image)
     * NULL for TICKET receipts
     * Required for PARKING and FOOD receipts
     */
    @Lob
    @Column(name = "qr_code_data", columnDefinition = "TEXT")
    private String qrCodeData;

    /**
     * QR code payload (JSON)
     * Contains validation data encoded in QR
     */
    @Lob
    @Column(name = "qr_payload", columnDefinition = "TEXT")
    private String qrPayload;

    /**
     * Receipt status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ReceiptStatus status = ReceiptStatus.PENDING;

    /**
     * Amount on receipt (for auditing)
     */
    @Column(name = "amount")
    private Double amount;

    /**
     * QR code expiry time (for validation)
     * NULL means no expiry
     */
    @Column(name = "qr_expiry_time")
    private LocalDateTime qrExpiryTime;

    /**
     * Validation token (encrypted)
     * Used to prevent QR code tampering
     */
    @Column(name = "validation_token", length = 500)
    private String validationToken;

    /**
     * Receipt generation timestamp
     */
    @CreationTimestamp
    @Column(name = "generated_at", updatable = false)
    private LocalDateTime generatedAt;

    /**
     * Last update timestamp
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Error message (if generation failed)
     */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /**
     * Retry count (for async retry logic)
     */
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Check if QR code is expired
     */
    public boolean isQrExpired() {
        if (qrExpiryTime == null) {
            return false; // No expiry
        }
        return LocalDateTime.now().isAfter(qrExpiryTime);
    }

    /**
     * Check if receipt requires QR code
     */
    public boolean requiresQrCode() {
        return receiptType == ReceiptType.PARKING || receiptType == ReceiptType.FOOD;
    }

    /**
     * Mark as generated
     */
    public void markAsGenerated() {
        this.status = ReceiptStatus.GENERATED;
    }

    /**
     * Mark as failed
     */
    public void markAsFailed(String errorMessage) {
        this.status = ReceiptStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    /**
     * Increment retry count
     */
    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }
}
