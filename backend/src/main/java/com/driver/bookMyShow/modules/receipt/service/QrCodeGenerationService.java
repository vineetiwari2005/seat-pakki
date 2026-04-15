package com.driver.bookMyShow.modules.receipt.service;

import com.driver.bookMyShow.modules.receipt.dto.QrCodePayload;

/**
 * QR Code Generation Service Interface
 * 
 * Design Principles:
 * - Single Responsibility: Only generates QR codes
 * - Open-Closed: Extensible for different QR formats
 * - Stateless: Thread-safe, no instance state
 * 
 * System Design:
 * - Generates Base64 encoded QR code images
 * - Encodes validation data for security
 * - Supports expiry timestamps
 * - Deterministic generation (same input → same output)
 */
public interface QrCodeGenerationService {

    /**
     * Generate QR code from payload
     * 
     * @param payload QR code data
     * @return Base64 encoded QR code image
     */
    String generateQrCode(QrCodePayload payload);

    /**
     * Generate QR code from raw data
     * 
     * @param data Raw string data to encode
     * @param size QR code size in pixels
     * @return Base64 encoded QR code image
     */
    String generateQrCode(String data, int size);

    /**
     * Encode payload to JSON string
     * 
     * @param payload QR code payload
     * @return JSON string representation
     */
    String encodePayload(QrCodePayload payload);

    /**
     * Decode JSON string to payload
     * 
     * @param json JSON string
     * @return QR code payload
     */
    QrCodePayload decodePayload(String json);

    /**
     * Validate QR code payload
     * 
     * @param payload Payload to validate
     * @return true if valid and not expired
     */
    boolean validatePayload(QrCodePayload payload);

    /**
     * Generate validation token
     * 
     * @param bookingId Booking reference
     * @param serviceType Service type
     * @return Encrypted validation token
     */
    String generateValidationToken(Integer bookingId, String serviceType);
}
