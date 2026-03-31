package com.driver.bookMyShow.modules.addons.service;

import com.driver.bookMyShow.modules.addons.domain.PaymentAddon;
import com.driver.bookMyShow.modules.addons.dto.*;
import com.driver.bookMyShow.modules.addons.enums.AddonStatus;
import com.driver.bookMyShow.modules.addons.enums.AddonType;
import com.driver.bookMyShow.modules.addons.repository.PaymentAddonRepository;
import com.driver.bookMyShow.modules.parking.service.ParkingService;
import com.driver.bookMyShow.modules.parking.dto.ParkingBookingRequest;
import com.driver.bookMyShow.modules.parking.dto.ParkingTicketResponse;
import com.driver.bookMyShow.modules.parking.enums.VehicleType;
import com.driver.bookMyShow.modules.food.service.FoodService;
import com.driver.bookMyShow.modules.food.dto.FoodOrderRequest;
import com.driver.bookMyShow.modules.food.dto.FoodOrderResponse;
import com.driver.bookMyShow.modules.food.dto.OrderItemRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PaymentAddonOrchestratorImpl - Orchestrates parking and food add-ons during payment
 * 
 * Design Principles:
 * - Single Responsibility: Only coordinates add-ons, doesn't handle payment
 * - Open-Closed: New add-on types can be added without modifying existing code
 * - Dependency Inversion: Depends on ParkingService and FoodService interfaces
 * - Graceful Degradation: Add-on failures don't affect ticket booking
 * 
 * Transaction Strategy:
 * - Add-on selection: Separate transaction (independent of payment)
 * - Add-on confirmation: REQUIRES_NEW (isolated from payment transaction)
 * - Failure handling: Compensating transactions
 */
@Service
@Slf4j
public class PaymentAddonOrchestratorImpl implements PaymentAddonOrchestrator {

    @Autowired
    private PaymentAddonRepository addonRepository;

    @Autowired
    private ParkingService parkingService;

    @Autowired
    private FoodService foodService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public PaymentAddon selectParkingAddon(ParkingAddonRequest request) throws Exception {
        log.info("Selecting parking add-on for session: {}", request.getSessionId());

        // Check if parking already selected for this session
        addonRepository.findBySessionIdAndAddonType(request.getSessionId(), AddonType.PARKING)
                .ifPresent(existing -> {
                    throw new RuntimeException("Parking already selected for this session");
                });

        // Create tentative parking booking
        ParkingBookingRequest parkingRequest = ParkingBookingRequest.builder()
                .theaterId(request.getTheaterId())
                .vehicleNumber(request.getVehicleNumber())
                .vehicleType(VehicleType.valueOf(request.getVehicleType()))
                .build();

        ParkingTicketResponse parkingTicket = parkingService.bookParking(parkingRequest);

        // Create add-on record
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("parkingTicketId", parkingTicket.getId());
        metadata.put("vehicleType", request.getVehicleType());
        metadata.put("vehicleNumber", request.getVehicleNumber());
        metadata.put("slotNumber", parkingTicket.getSlotNumber());

        PaymentAddon addon = PaymentAddon.builder()
                .sessionId(request.getSessionId())
                .userId(request.getUserId())
                .addonType(AddonType.PARKING)
                .status(AddonStatus.SELECTED)
                .amount(parkingTicket.getAmountPaid().doubleValue())
                .referenceId(parkingTicket.getId())
                .metadata(objectMapper.writeValueAsString(metadata))
                .build();

        return addonRepository.save(addon);
    }

    @Override
    @Transactional
    public PaymentAddon selectFoodAddon(FoodAddonRequest request) throws Exception {
        log.info("Selecting food add-on for session: {}", request.getSessionId());

        // Check if food already selected for this session
        addonRepository.findBySessionIdAndAddonType(request.getSessionId(), AddonType.FOOD_BEVERAGE)
                .ifPresent(existing -> {
                    throw new RuntimeException("Food already selected for this session");
                });

        // Create tentative food order
        List<OrderItemRequest> orderItems = request.getItems().stream()
                .map(item -> OrderItemRequest.builder()
                        .foodItemId(item.getFoodItemId())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        FoodOrderRequest foodRequest = FoodOrderRequest.builder()
                .userId(request.getUserId())
                .theaterId(request.getTheaterId())
                .items(orderItems)
                .build();

        FoodOrderResponse foodOrder = foodService.createOrder(foodRequest);

        // Create add-on record
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("foodOrderId", foodOrder.getId());
        metadata.put("itemCount", request.getItems().size());
        metadata.put("items", request.getItems());

        PaymentAddon addon = PaymentAddon.builder()
                .sessionId(request.getSessionId())
                .userId(request.getUserId())
                .addonType(AddonType.FOOD_BEVERAGE)
                .status(AddonStatus.SELECTED)
                .amount(foodOrder.getTotalAmount().doubleValue())
                .referenceId(foodOrder.getId())
                .metadata(objectMapper.writeValueAsString(metadata))
                .build();

        return addonRepository.save(addon);
    }

    @Override
    @Transactional
    public void removeAddon(String sessionId, String addonType) throws Exception {
        log.info("Removing {} add-on for session: {}", addonType, sessionId);

        AddonType type = AddonType.valueOf(addonType);
        PaymentAddon addon = addonRepository.findBySessionIdAndAddonType(sessionId, type)
                .orElseThrow(() -> new Exception("Add-on not found"));

        if (addon.getStatus() != AddonStatus.SELECTED) {
            throw new Exception("Cannot remove add-on in status: " + addon.getStatus());
        }

        // Cancel the underlying service
        try {
            if (type == AddonType.PARKING) {
                parkingService.cancelParking(addon.getReferenceId());
            } else if (type == AddonType.FOOD_BEVERAGE) {
                foodService.cancelOrder(addon.getReferenceId());
            }
        } catch (Exception e) {
            log.warn("Failed to cancel {} service: {}", type, e.getMessage());
            // Continue to delete add-on record
        }

        addonRepository.delete(addon);
    }

    @Override
    public List<AddonSummaryDto> getSessionAddons(String sessionId) {
        List<PaymentAddon> addons = addonRepository.findBySessionId(sessionId);
        
        return addons.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Override
    public Double calculateTotalWithAddons(String sessionId, Double ticketAmount) {
        List<PaymentAddon> addons = addonRepository.findBySessionId(sessionId);
        
        Double addonTotal = addons.stream()
                .filter(addon -> addon.getStatus() == AddonStatus.SELECTED)
                .mapToDouble(PaymentAddon::getAmount)
                .sum();
        
        return ticketAmount + addonTotal;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void confirmAddons(String sessionId, Integer paymentId) {
        log.info("Confirming add-ons for session: {}", sessionId);

        List<PaymentAddon> addons = addonRepository.findBySessionId(sessionId);

        for (PaymentAddon addon : addons) {
            if (addon.getStatus() != AddonStatus.SELECTED) {
                continue;
            }

            try {
                // Confirm the underlying service
                if (addon.getAddonType() == AddonType.PARKING) {
                    parkingService.confirmParking(addon.getReferenceId());
                } else if (addon.getAddonType() == AddonType.FOOD_BEVERAGE) {
                    foodService.confirmOrder(addon.getReferenceId());
                }

                // Update add-on status
                addon.setStatus(AddonStatus.CONFIRMED);
                addon.setPaymentId(paymentId);
                addonRepository.save(addon);

                log.info("Confirmed {} add-on: {}", addon.getAddonType(), addon.getId());

            } catch (Exception e) {
                log.error("Failed to confirm {} add-on: {}", addon.getAddonType(), e.getMessage());
                
                // Graceful degradation: Mark as FAILED but don't throw
                addon.setStatus(AddonStatus.FAILED);
                addon.setFailureReason(e.getMessage());
                addonRepository.save(addon);
                
                // TODO: Send notification to support team
                // TODO: Credit refund to user wallet
            }
        }
    }

    @Override
    @Transactional
    public void cancelAddons(String sessionId, String reason) {
        log.info("Cancelling add-ons for session: {}", sessionId);

        List<PaymentAddon> addons = addonRepository.findBySessionId(sessionId);

        for (PaymentAddon addon : addons) {
            if (addon.getStatus() != AddonStatus.SELECTED) {
                continue;
            }

            try {
                // Cancel the underlying service
                if (addon.getAddonType() == AddonType.PARKING) {
                    parkingService.cancelParking(addon.getReferenceId());
                } else if (addon.getAddonType() == AddonType.FOOD_BEVERAGE) {
                    foodService.cancelOrder(addon.getReferenceId());
                }

                addon.setStatus(AddonStatus.CANCELLED);
                addon.setFailureReason(reason);
                addonRepository.save(addon);

            } catch (Exception e) {
                log.error("Failed to cancel {} add-on: {}", addon.getAddonType(), e.getMessage());
            }
        }
    }

    private AddonSummaryDto toSummaryDto(PaymentAddon addon) {
        String details = "";
        Map<String, Object> metadataMap = null;

        try {
            metadataMap = objectMapper.readValue(addon.getMetadata(), Map.class);
            
            if (addon.getAddonType() == AddonType.PARKING) {
                details = String.format("Parking - %s (%s)", 
                    metadataMap.get("vehicleType"), 
                    metadataMap.get("slotNumber"));
            } else if (addon.getAddonType() == AddonType.FOOD_BEVERAGE) {
                details = String.format("Food & Beverages - %d items", 
                    metadataMap.get("itemCount"));
            }
        } catch (Exception e) {
            details = addon.getAddonType().name();
        }

        return AddonSummaryDto.builder()
                .addonId(addon.getId())
                .type(addon.getAddonType())
                .amount(addon.getAmount())
                .status(addon.getStatus().name())
                .details(details)
                .metadata(metadataMap)
                .build();
    }
}
