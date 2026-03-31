package com.driver.bookMyShow.modules.receipt.service;

import com.driver.bookMyShow.common.exceptions.BusinessException;
import com.driver.bookMyShow.modules.receipt.dto.QrCodePayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * QR Code Generation Service Implementation
 * 
 * Design Principles:
 * - Single Responsibility: Only QR code generation logic
 * - Stateless: Thread-safe, no instance state
 * - Deterministic: Same input produces same output
 * - Open-Closed: Extensible via configuration
 * 
 * Technical Implementation:
 * - Uses ZXing library for QR code generation
 * - Base64 encoding for image transport
 * - JSON encoding for payload
 * - SHA-256 for validation token
 * - Error correction level: HIGH (30% recovery)
 * 
 * System Design:
 * - QR codes are self-contained (no DB lookup for validation)
 * - Expiry embedded in payload
 * - Tamper-proof via validation token
 */
@Service
@Slf4j
public class QrCodeGenerationServiceImpl implements QrCodeGenerationService {

    private static final int DEFAULT_QR_SIZE = 300; // pixels
    private static final String IMAGE_FORMAT = "PNG";
    private static final String SECRET_SALT = "BookMyShow-QR-Secret-2026"; // In production, use from config

    private final ObjectMapper objectMapper;

    public QrCodeGenerationServiceImpl() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Generate QR code from payload
     * 
     * @param payload QR code data
     * @return Base64 encoded QR code image
     */
    @Override
    public String generateQrCode(QrCodePayload payload) {
        log.debug("Generating QR code for payload: {}", payload);

        try {
            // Encode payload to JSON
            String jsonPayload = encodePayload(payload);

            // Generate QR code
            return generateQrCode(jsonPayload, DEFAULT_QR_SIZE);

        } catch (Exception e) {
            log.error("Failed to generate QR code", e);
            throw new BusinessException("QR code generation failed: " + e.getMessage());
        }
    }

    /**
     * Generate QR code from raw data
     * 
     * @param data Raw string data to encode
     * @param size QR code size in pixels
     * @return Base64 encoded QR code image
     */
    @Override
    public String generateQrCode(String data, int size) {
        log.debug("Generating QR code for data length: {}", data.length());

        try {
            // Configure QR code hints
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // High error correction
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.MARGIN, 1);

            // Generate QR code matrix
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, size, size, hints);

            // Convert to BufferedImage
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            // Convert to Base64
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(qrImage, IMAGE_FORMAT, outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            log.info("QR code generated successfully. Size: {} bytes", imageBytes.length);
            return base64Image;

        } catch (WriterException | IOException e) {
            log.error("Failed to generate QR code", e);
            throw new BusinessException("QR code generation failed: " + e.getMessage());
        }
    }

    /**
     * Encode payload to JSON string
     * 
     * @param payload QR code payload
     * @return JSON string representation
     */
    @Override
    public String encodePayload(QrCodePayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to encode payload to JSON", e);
            throw new BusinessException("Payload encoding failed: " + e.getMessage());
        }
    }

    /**
     * Decode JSON string to payload
     * 
     * @param json JSON string
     * @return QR code payload
     */
    @Override
    public QrCodePayload decodePayload(String json) {
        try {
            return objectMapper.readValue(json, QrCodePayload.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to decode JSON to payload", e);
            throw new BusinessException("Payload decoding failed: " + e.getMessage());
        }
    }

    /**
     * Validate QR code payload
     * Checks:
     * 1. Expiry time
     * 2. Validation token
     * 
     * @param payload Payload to validate
     * @return true if valid and not expired
     */
    @Override
    public boolean validatePayload(QrCodePayload payload) {
        log.debug("Validating QR payload: {}", payload);

        // Check expiry
        if (payload.getExpiryTime() != null && LocalDateTime.now().isAfter(payload.getExpiryTime())) {
            log.warn("QR code expired: {}", payload.getExpiryTime());
            return false;
        }

        // Validate token
        String expectedToken = generateValidationToken(payload.getBookingId(), payload.getServiceType());
        boolean isValid = expectedToken.equals(payload.getValidationToken());

        if (!isValid) {
            log.warn("Invalid validation token for bookingId: {}", payload.getBookingId());
        }

        return isValid;
    }

    /**
     * Generate validation token using SHA-256
     * 
     * Format: SHA256(bookingId + serviceType + SECRET_SALT)
     * 
     * @param bookingId Booking reference
     * @param serviceType Service type
     * @return Encrypted validation token
     */
    @Override
    public String generateValidationToken(Integer bookingId, String serviceType) {
        try {
            String data = bookingId + "-" + serviceType + "-" + SECRET_SALT;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate validation token", e);
            throw new BusinessException("Token generation failed: " + e.getMessage());
        }
    }
}
