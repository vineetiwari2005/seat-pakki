package com.driver.bookMyShow.Enums;

/**
 * TransactionType - Types of wallet transactions
 * 
 * Design:
 * - Enum for type safety
 * - No hardcoded values in business logic
 * - Extensible for future transaction types
 */
public enum TransactionType {
    CREDIT,      // Money added to wallet
    DEBIT,       // Money deducted from wallet
    REFUND       // Money refunded to wallet
}
