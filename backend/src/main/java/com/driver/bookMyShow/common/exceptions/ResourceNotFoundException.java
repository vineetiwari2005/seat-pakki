package com.driver.bookMyShow.common.exceptions;

/**
 * Thrown when a requested resource doesn't exist
 * HTTP 404 mapping
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
