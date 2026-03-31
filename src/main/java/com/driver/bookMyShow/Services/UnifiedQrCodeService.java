package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.dto.UnifiedQrPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Unified QR Code Service
 * 
 * Generates SINGLE QR CODE containing:
 * - Ticket details (always)
 * - Parking details (if selected)
 * - Food details (if selected)
 * 
 * Design Principles:
 * - Single Responsibility: QR generation only
 * - Stateless: Thread-safe, no instance variables
 * - Fail-safe: QR generation failure doesn't block booking
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedQrCodeService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Security salt for token generation (PRODUCTION: Move to config)
    private static final String SECRET_SALT = "bookmyshow_qr_secret_2026";
    private static final int QR_CODE_SIZE = 300;

    /**
     * Generate unified QR code for ticket
     * 
     * @param ticket Movie ticket (required)
     * @param parkingTicketId Parking ticket ID (nullable)
     * @param vehicleType Vehicle type (nullable)
     * @param vehicleNumber Vehicle number (nullable)
     * @param parkingFee Parking fee (nullable)
     * @param foodOrderId Food order ID (nullable)
     * @param foodItems Food items JSON (nullable)
     * @param foodTotal Food total (nullable)
     * @return Base64 QR code image
     */
    public String generateUnifiedQrCode(
            Ticket ticket,
            Integer parkingTicketId,
            String vehicleType,
            String vehicleNumber,
            Double parkingFee,
            Integer foodOrderId,
            String foodItems,
            Double foodTotal
    ) {
        try {
            // Build unified payload
            UnifiedQrPayload payload = buildPayload(
                    ticket, parkingTicketId, vehicleType, vehicleNumber, parkingFee,
                    foodOrderId, foodItems, foodTotal
            );

            // Generate validation token
            String validationToken = generateValidationToken(payload);
            payload.setValidationToken(validationToken);

            // Convert payload to JSON
            String jsonPayload = objectMapper.writeValueAsString(payload);

            // Generate QR code image
            String qrCodeBase64 = generateQrCodeImage(jsonPayload);

            log.info("Unified QR generated for ticket: {}, hasParking: {}, hasFood: {}",
                    ticket.getId(), payload.hasParking(), payload.hasFood());

            return qrCodeBase64;

        } catch (Exception e) {
            log.error("Failed to generate unified QR for ticket: {}", ticket.getId(), e);
            return null;  // Fail-safe: booking continues even if QR fails
        }
    }

    /**
     * Build unified payload from ticket and add-ons
     */
    private UnifiedQrPayload buildPayload(
            Ticket ticket,
            Integer parkingTicketId,
            String vehicleType,
            String vehicleNumber,
            Double parkingFee,
            Integer foodOrderId,
            String foodItems,
            Double foodTotal
    ) {
        LocalDateTime now = LocalDateTime.now();
        // Convert SQL Date and Time to LocalDateTime
        LocalDateTime showTime = ticket.getShow().getDate().toLocalDate()
                .atTime(ticket.getShow().getTime().toLocalTime());
        LocalDateTime expiryTime = showTime.plusHours(2);  // Valid until 2 hours after show

        return UnifiedQrPayload.builder()
                // Ticket details
                .ticketId(ticket.getId())
                .userId(ticket.getUser().getId())
                .movieName(ticket.getShow().getMovie().getMovieName())
                .theaterName(ticket.getShow().getTheater().getName())
                .showTime(showTime.toString())
                .bookedSeats(ticket.getBookedSeats())
                .totalTicketsPrice(ticket.getTotalTicketsPrice())
                
                // Parking details (optional)
                .hasParkingSelection(parkingTicketId != null)
                .parkingTicketId(parkingTicketId)
                .vehicleType(vehicleType)
                .vehicleNumber(vehicleNumber)
                .parkingFee(parkingFee)
                
                // Food details (optional)
                .hasFoodSelection(foodOrderId != null)
                .foodOrderId(foodOrderId)
                .foodItems(foodItems)
                .foodTotal(foodTotal)
                
                // Validation
                .generatedAt(now)
                .expiryTime(expiryTime)
                .build();
    }

    /**
     * Generate SHA-256 validation token
     */
    private String generateValidationToken(UnifiedQrPayload payload) {
        try {
            String data = String.format("%d:%s:%s:%s",
                    payload.getTicketId(),
                    payload.getUserId(),
                    payload.getGeneratedAt(),
                    SECRET_SALT
            );

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {
            log.error("Failed to generate validation token", e);
            return "FALLBACK_TOKEN_" + System.currentTimeMillis();
        }
    }

    /**
     * Validate QR code token
     */
    public boolean validateQrCode(String ticketId, String providedToken, LocalDateTime generatedAt) {
        try {
            String data = String.format("%s:%s:%s",
                    ticketId,
                    generatedAt,
                    SECRET_SALT
            );

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            String expectedToken = Base64.getEncoder().encodeToString(hash);

            return expectedToken.equals(providedToken);

        } catch (Exception e) {
            log.error("QR validation failed", e);
            return false;
        }
    }

    /**
     * Generate QR code image from data
     */
    private String generateQrCodeImage(String data) {
        try {
            // Configure QR code parameters
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            // Generate QR code matrix
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    data,
                    BarcodeFormat.QR_CODE,
                    QR_CODE_SIZE,
                    QR_CODE_SIZE,
                    hints
            );

            // Convert to image
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            // Convert to Base64
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            
            return Base64.getEncoder().encodeToString(imageBytes);

        } catch (Exception e) {
            log.error("QR code generation failed", e);
            return null;
        }
    }
}
