package com.driver.bookMyShow.modules.receipt.service;

import com.driver.bookMyShow.Models.Payment;
import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.Repositories.PaymentRepository;
import com.driver.bookMyShow.Repositories.TicketRepository;
import com.driver.bookMyShow.common.exceptions.BusinessException;
import com.driver.bookMyShow.common.exceptions.ResourceNotFoundException;
import com.driver.bookMyShow.modules.food.entity.FoodOrder;
import com.driver.bookMyShow.modules.food.repository.FoodOrderRepository;
import com.driver.bookMyShow.modules.parking.entity.ParkingTicket;
import com.driver.bookMyShow.modules.parking.repository.ParkingTicketRepository;
import com.driver.bookMyShow.modules.receipt.dto.QrCodePayload;
import com.driver.bookMyShow.modules.receipt.dto.ReceiptGenerationResult;
import com.driver.bookMyShow.modules.receipt.dto.ReceiptResponse;
import com.driver.bookMyShow.modules.receipt.entity.Receipt;
import com.driver.bookMyShow.modules.receipt.enums.ReceiptStatus;
import com.driver.bookMyShow.modules.receipt.enums.ReceiptType;
import com.driver.bookMyShow.modules.receipt.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Receipt Generation Service Implementation
 * 
 * Design Principles:
 * - Single Responsibility: Only receipt generation
 * - Fail-safe: Receipt failure doesn't affect booking
 * - Async-friendly: Non-blocking operations
 * - Idempotent: Can regenerate receipts safely
 * 
 * System Design:
 * - Each receipt type generated independently
 * - QR codes generated only for parking and food
 * - Separate transaction from booking (REQUIRES_NEW)
 * - Retry mechanism for failed receipts
 * 
 * Transaction Strategy:
 * - Uses REQUIRES_NEW propagation
 * - Receipt failure doesn't rollback booking
 * - Each receipt saved in separate transaction
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptGenerationServiceImpl implements ReceiptGenerationService {

    private final ReceiptRepository receiptRepository;
    private final TicketRepository ticketRepository;
    private final ParkingTicketRepository parkingTicketRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final QrCodeGenerationService qrCodeGenerationService;
    private final PaymentRepository paymentRepository;

    private static final DateTimeFormatter RECEIPT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int QR_EXPIRY_HOURS = 24; // QR code valid for 24 hours

    /**
     * Generate all receipts synchronously
     * 
     * @param ticketId Movie ticket ID
     * @param parkingTicketId Parking ticket ID (optional)
     * @param foodOrderId Food order ID (optional)
     * @return Receipt generation result
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReceiptGenerationResult generateAllReceipts(
            Integer ticketId,
            Integer parkingTicketId,
            Integer foodOrderId
    ) {
        log.info("Generating receipts for ticket: {}, parking: {}, food: {}", 
                 ticketId, parkingTicketId, foodOrderId);

        List<ReceiptResponse> allReceipts = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        ReceiptResponse ticketReceipt = null;
        ReceiptResponse parkingReceipt = null;
        ReceiptResponse foodReceipt = null;

        try {
            // 1. Generate ticket receipt (always)
            ticketReceipt = generateTicketReceipt(ticketId);
            allReceipts.add(ticketReceipt);
            log.info("Ticket receipt generated: {}", ticketReceipt.getReceiptNumber());
        } catch (Exception e) {
            log.error("Failed to generate ticket receipt", e);
            errors.add("Ticket receipt: " + e.getMessage());
        }

        try {
            // 2. Generate parking receipt (if parking selected)
            if (parkingTicketId != null) {
                parkingReceipt = generateParkingReceipt(ticketId, parkingTicketId);
                allReceipts.add(parkingReceipt);
                log.info("Parking receipt generated: {}", parkingReceipt.getReceiptNumber());
            }
        } catch (Exception e) {
            log.error("Failed to generate parking receipt", e);
            errors.add("Parking receipt: " + e.getMessage());
        }

        try {
            // 3. Generate food receipt (if food selected)
            if (foodOrderId != null) {
                foodReceipt = generateFoodReceipt(ticketId, foodOrderId);
                allReceipts.add(foodReceipt);
                log.info("Food receipt generated: {}", foodReceipt.getReceiptNumber());
            }
        } catch (Exception e) {
            log.error("Failed to generate food receipt", e);
            errors.add("Food receipt: " + e.getMessage());
        }

        return ReceiptGenerationResult.builder()
                .ticketReceipt(ticketReceipt)
                .parkingReceipt(parkingReceipt)
                .foodReceipt(foodReceipt)
                .allReceipts(allReceipts)
                .success(errors.isEmpty())
                .errors(errors)
                .build();
    }

    /**
     * Generate all receipts asynchronously
     * 
     * @param ticketId Movie ticket ID
     * @param parkingTicketId Parking ticket ID (optional)
     * @param foodOrderId Food order ID (optional)
     * @return CompletableFuture with result
     */
    @Override
    @Async
    public CompletableFuture<ReceiptGenerationResult> generateAllReceiptsAsync(
            Integer ticketId,
            Integer parkingTicketId,
            Integer foodOrderId
    ) {
        log.info("Generating receipts asynchronously for ticket: {}", ticketId);
        ReceiptGenerationResult result = generateAllReceipts(ticketId, parkingTicketId, foodOrderId);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Generate ticket receipt (no QR code)
     * 
     * @param ticketId Movie ticket ID
     * @return Receipt response
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReceiptResponse generateTicketReceipt(Integer ticketId) {
        log.debug("Generating ticket receipt for ticket: {}", ticketId);

        // Check if already exists
        Receipt existingReceipt = receiptRepository
                .findByTicketIdAndReceiptType(ticketId, ReceiptType.TICKET)
                .orElse(null);

        if (existingReceipt != null && existingReceipt.getStatus() == ReceiptStatus.GENERATED) {
            log.info("Ticket receipt already exists: {}", existingReceipt.getReceiptNumber());
            return mapToResponse(existingReceipt);
        }

        // Fetch ticket
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        // Fetch the payment to get the actual total amount paid
        List<Payment> payments = paymentRepository.findByTicket(ticket);
        Double receiptAmount = !payments.isEmpty() ? payments.get(0).getTotalAmount() : ticket.getTotalTicketsPrice().doubleValue();

        try {
            // Create receipt
            Receipt receipt = Receipt.builder()
                    .receiptNumber(generateReceiptNumber(ReceiptType.TICKET))
                    .receiptType(ReceiptType.TICKET)
                    .ticket(ticket)
                    .referenceId(ticketId)
                    .amount(receiptAmount)
                    .status(ReceiptStatus.GENERATED)
                    .qrCodeData(null) // Ticket receipt has no QR code
                    .qrPayload(null)
                    .validationToken(null)
                    .qrExpiryTime(null)
                    .build();

            receipt = receiptRepository.save(receipt);
            log.info("Ticket receipt created: {}", receipt.getReceiptNumber());

            return mapToResponse(receipt);

        } catch (Exception e) {
            log.error("Failed to generate ticket receipt", e);
            throw new BusinessException("Ticket receipt generation failed: " + e.getMessage());
        }
    }

    /**
     * Generate parking receipt with QR code
     * 
     * @param ticketId Movie ticket ID
     * @param parkingTicketId Parking ticket ID
     * @return Receipt response with QR code
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReceiptResponse generateParkingReceipt(Integer ticketId, Integer parkingTicketId) {
        log.debug("Generating parking receipt for ticket: {}, parking: {}", ticketId, parkingTicketId);

        // Check if already exists
        Receipt existingReceipt = receiptRepository
                .findByReferenceIdAndReceiptType(parkingTicketId, ReceiptType.PARKING)
                .orElse(null);

        if (existingReceipt != null && existingReceipt.getStatus() == ReceiptStatus.GENERATED) {
            log.info("Parking receipt already exists: {}", existingReceipt.getReceiptNumber());
            return mapToResponse(existingReceipt);
        }

        // Fetch entities
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        ParkingTicket parkingTicket = parkingTicketRepository.findById(parkingTicketId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking ticket not found: " + parkingTicketId));

        try {
            String receiptNumber = generateReceiptNumber(ReceiptType.PARKING);
            LocalDateTime expiryTime = LocalDateTime.now().plusHours(QR_EXPIRY_HOURS);

            // Generate validation token
            String validationToken = qrCodeGenerationService.generateValidationToken(ticketId, "PARKING");

            // Create QR payload
            QrCodePayload qrPayload = QrCodePayload.builder()
                    .bookingId(ticketId)
                    .serviceType("PARKING")
                    .validationToken(validationToken)
                    .expiryTime(expiryTime)
                    .receiptNumber(receiptNumber)
                    .referenceId(parkingTicketId)
                    .metadata(String.format("{\"vehicleNumber\":\"%s\",\"slotNumber\":\"%s\"}", 
                                          parkingTicket.getVehicleNumber(), 
                                          parkingTicket.getParkingSlot().getSlotNumber()))
                    .build();

            // Generate QR code
            String qrCodeData = qrCodeGenerationService.generateQrCode(qrPayload);
            String qrPayloadJson = qrCodeGenerationService.encodePayload(qrPayload);

            // Create receipt
            Receipt receipt = Receipt.builder()
                    .receiptNumber(receiptNumber)
                    .receiptType(ReceiptType.PARKING)
                    .ticket(ticket)
                    .referenceId(parkingTicketId)
                    .amount(parkingTicket.getAmountPaid().doubleValue())
                    .status(ReceiptStatus.GENERATED)
                    .qrCodeData(qrCodeData)
                    .qrPayload(qrPayloadJson)
                    .validationToken(validationToken)
                    .qrExpiryTime(expiryTime)
                    .build();

            receipt = receiptRepository.save(receipt);
            log.info("Parking receipt created with QR code: {}", receipt.getReceiptNumber());

            return mapToResponse(receipt);

        } catch (Exception e) {
            log.error("Failed to generate parking receipt", e);
            throw new BusinessException("Parking receipt generation failed: " + e.getMessage());
        }
    }

    /**
     * Generate food receipt with QR code
     * 
     * @param ticketId Movie ticket ID
     * @param foodOrderId Food order ID
     * @return Receipt response with QR code
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReceiptResponse generateFoodReceipt(Integer ticketId, Integer foodOrderId) {
        log.debug("Generating food receipt for ticket: {}, food order: {}", ticketId, foodOrderId);

        // Check if already exists
        Receipt existingReceipt = receiptRepository
                .findByReferenceIdAndReceiptType(foodOrderId, ReceiptType.FOOD)
                .orElse(null);

        if (existingReceipt != null && existingReceipt.getStatus() == ReceiptStatus.GENERATED) {
            log.info("Food receipt already exists: {}", existingReceipt.getReceiptNumber());
            return mapToResponse(existingReceipt);
        }

        // Fetch entities
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        FoodOrder foodOrder = foodOrderRepository.findById(foodOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Food order not found: " + foodOrderId));

        try {
            String receiptNumber = generateReceiptNumber(ReceiptType.FOOD);
            LocalDateTime expiryTime = LocalDateTime.now().plusHours(QR_EXPIRY_HOURS);

            // Generate validation token
            String validationToken = qrCodeGenerationService.generateValidationToken(ticketId, "FOOD");

            // Create QR payload
            QrCodePayload qrPayload = QrCodePayload.builder()
                    .bookingId(ticketId)
                    .serviceType("FOOD")
                    .validationToken(validationToken)
                    .expiryTime(expiryTime)
                    .receiptNumber(receiptNumber)
                    .referenceId(foodOrderId)
                    .metadata(String.format("{\"orderNumber\":\"%s\",\"itemCount\":%d}", 
                                          foodOrder.getOrderNumber(), 
                                          foodOrder.getItems().size()))
                    .build();

            // Generate QR code
            String qrCodeData = qrCodeGenerationService.generateQrCode(qrPayload);
            String qrPayloadJson = qrCodeGenerationService.encodePayload(qrPayload);

            // Create receipt
            Receipt receipt = Receipt.builder()
                    .receiptNumber(receiptNumber)
                    .receiptType(ReceiptType.FOOD)
                    .ticket(ticket)
                    .referenceId(foodOrderId)
                    .amount(foodOrder.getTotalAmount().doubleValue())
                    .status(ReceiptStatus.GENERATED)
                    .qrCodeData(qrCodeData)
                    .qrPayload(qrPayloadJson)
                    .validationToken(validationToken)
                    .qrExpiryTime(expiryTime)
                    .build();

            receipt = receiptRepository.save(receipt);
            log.info("Food receipt created with QR code: {}", receipt.getReceiptNumber());

            return mapToResponse(receipt);

        } catch (Exception e) {
            log.error("Failed to generate food receipt", e);
            throw new BusinessException("Food receipt generation failed: " + e.getMessage());
        }
    }

    /**
     * Retry failed receipt generation
     * 
     * @param receiptId Receipt ID to retry
     * @return Updated receipt response
     */
    @Override
    @Transactional
    public ReceiptResponse retryReceiptGeneration(Integer receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + receiptId));

        if (receipt.getStatus() == ReceiptStatus.GENERATED) {
            log.warn("Receipt already generated: {}", receipt.getReceiptNumber());
            return mapToResponse(receipt);
        }

        receipt.incrementRetryCount();
        
        try {
            // Regenerate based on type
            return switch (receipt.getReceiptType()) {
                case TICKET -> generateTicketReceipt(receipt.getTicket().getId());
                case PARKING -> generateParkingReceipt(receipt.getTicket().getId(), receipt.getReferenceId());
                case FOOD -> generateFoodReceipt(receipt.getTicket().getId(), receipt.getReferenceId());
            };
        } catch (Exception e) {
            receipt.markAsFailed(e.getMessage());
            receiptRepository.save(receipt);
            throw e;
        }
    }

    /**
     * Get all receipts for a ticket
     * 
     * @param ticketId Movie ticket ID
     * @return Receipt generation result
     */
    @Override
    @Transactional(readOnly = true)
    public ReceiptGenerationResult getReceiptsForTicket(Integer ticketId) {
        List<Receipt> receipts = receiptRepository.findByTicketId(ticketId);

        ReceiptResponse ticketReceipt = null;
        ReceiptResponse parkingReceipt = null;
        ReceiptResponse foodReceipt = null;
        List<ReceiptResponse> allReceipts = new ArrayList<>();

        for (Receipt receipt : receipts) {
            ReceiptResponse response = mapToResponse(receipt);
            allReceipts.add(response);

            switch (receipt.getReceiptType()) {
                case TICKET -> ticketReceipt = response;
                case PARKING -> parkingReceipt = response;
                case FOOD -> foodReceipt = response;
            }
        }

        return ReceiptGenerationResult.builder()
                .ticketReceipt(ticketReceipt)
                .parkingReceipt(parkingReceipt)
                .foodReceipt(foodReceipt)
                .allReceipts(allReceipts)
                .success(true)
                .errors(new ArrayList<>())
                .build();
    }

    // Helper methods

    /**
     * Generate unique receipt number
     * Format: RCP-{TYPE}-{YYYYMMDD}-{RANDOM}
     */
    private String generateReceiptNumber(ReceiptType type) {
        String dateStr = LocalDateTime.now().format(RECEIPT_DATE_FORMAT);
        String typeCode = type.name().substring(0, 3).toUpperCase();
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return String.format("RCP-%s-%s-%s", typeCode, dateStr, random);
    }

    /**
     * Map entity to response DTO
     */
    private ReceiptResponse mapToResponse(Receipt receipt) {
        return ReceiptResponse.builder()
                .id(receipt.getId())
                .receiptNumber(receipt.getReceiptNumber())
                .receiptType(receipt.getReceiptType())
                .ticketId(receipt.getTicket().getId())
                .referenceId(receipt.getReferenceId())
                .qrCodeData(receipt.getQrCodeData())
                .status(receipt.getStatus())
                .amount(receipt.getAmount())
                .generatedAt(receipt.getGeneratedAt())
                .qrExpiryTime(receipt.getQrExpiryTime())
                .qrExpired(receipt.isQrExpired())
                .build();
    }
}
