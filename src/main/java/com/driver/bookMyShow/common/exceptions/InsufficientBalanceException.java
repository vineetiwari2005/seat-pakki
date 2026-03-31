package com.driver.bookMyShow.common.exceptions;

/**
 * Thrown when wallet balance is insufficient
 * HTTP 402 Payment Required
 */
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
