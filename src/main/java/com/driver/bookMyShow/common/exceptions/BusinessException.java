package com.driver.bookMyShow.common.exceptions;

/**
 * Thrown when business rule validation fails
 * HTTP 400 mapping
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
