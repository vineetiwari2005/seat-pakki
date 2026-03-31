package com.driver.bookMyShow.modules.receipt.enums;

/**
 * Receipt Status Enum
 * 
 * Lifecycle of a receipt:
 * - PENDING: Receipt generation initiated
 * - GENERATED: Receipt successfully created
 * - DELIVERED: Receipt sent to user (email/SMS)
 * - FAILED: Receipt generation failed
 */
public enum ReceiptStatus {
    PENDING,
    GENERATED,
    DELIVERED,
    FAILED
}
