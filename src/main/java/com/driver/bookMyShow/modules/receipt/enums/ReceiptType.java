package com.driver.bookMyShow.modules.receipt.enums;

/**
 * Receipt Type Enum
 * 
 * Types of receipts generated in the system:
 * - TICKET: Movie ticket receipt (always generated)
 * - PARKING: Parking receipt with QR code (conditional)
 * - FOOD: Food & beverage receipt with QR code (conditional)
 */
public enum ReceiptType {
    TICKET,
    PARKING,
    FOOD
}
