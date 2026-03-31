package com.driver.bookMyShow.modules.parking.service;

import com.driver.bookMyShow.Models.Theater;
import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.Repositories.TheaterRepository;
import com.driver.bookMyShow.Repositories.TicketRepository;
import com.driver.bookMyShow.common.exceptions.BusinessException;
import com.driver.bookMyShow.common.exceptions.ResourceNotFoundException;
import com.driver.bookMyShow.modules.parking.dto.ParkingAvailability;
import com.driver.bookMyShow.modules.parking.dto.ParkingBookingRequest;
import com.driver.bookMyShow.modules.parking.dto.ParkingTicketResponse;
import com.driver.bookMyShow.modules.parking.entity.ParkingLot;
import com.driver.bookMyShow.modules.parking.entity.ParkingSlot;
import com.driver.bookMyShow.modules.parking.entity.ParkingTicket;
import com.driver.bookMyShow.modules.parking.enums.ParkingStatus;
import com.driver.bookMyShow.modules.parking.enums.VehicleType;
import com.driver.bookMyShow.modules.parking.repository.ParkingLotRepository;
import com.driver.bookMyShow.modules.parking.repository.ParkingSlotRepository;
import com.driver.bookMyShow.modules.parking.repository.ParkingTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Parking Service Implementation
 * 
 * Concurrency Handling:
 * - Pessimistic locking on slot booking (prevents double booking)
 * - Optimistic locking via @Version in ParkingSlot
 * 
 * Transaction Management:
 * - Each booking is atomic (@Transactional)
 * - Slot lock → Decrement availability → Create ticket (all or nothing)
 * 
 * Scalability:
 * - Stateless service (can horizontally scale)
 * - DB-level locking ensures consistency across instances
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingServiceImpl implements ParkingService {

    private final ParkingLotRepository parkingLotRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final ParkingTicketRepository parkingTicketRepository;
    private final TheaterRepository theaterRepository;
    private final TicketRepository ticketRepository;
    private final ParkingFeeCalculationService feeCalculationService;

    private static final int BOOKING_TIMEOUT_MINUTES = 15;

    @Override
    @Transactional
    public ParkingTicketResponse bookParking(ParkingBookingRequest request) {
        log.info("Booking parking for vehicle: {} at theater: {}", 
                 request.getVehicleNumber(), request.getTheaterId());

        // 1. Validate theater and get parking lot
        ParkingLot parkingLot = parkingLotRepository.findByTheaterId(request.getTheaterId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Parking lot not found for theater: " + request.getTheaterId()));

        // 2. Check availability
        if (parkingLot.getAvailableSlots() <= 0) {
            throw new BusinessException("No parking slots available");
        }

        // 3. Find available slot for vehicle type (with pessimistic lock)
        List<ParkingSlot> availableSlots = parkingSlotRepository
                .findByParkingLotIdAndVehicleTypeAndIsOccupiedFalse(
                        parkingLot.getId(), request.getVehicleType());

        if (availableSlots.isEmpty()) {
            throw new BusinessException("No slots available for vehicle type: " + 
                                      request.getVehicleType());
        }

        ParkingSlot slot = availableSlots.get(0);

        // 4. Lock the slot (pessimistic write lock)
        slot = parkingSlotRepository.findByIdWithLock(slot.getId())
                .orElseThrow(() -> new BusinessException("Slot locking failed"));

        // Double-check availability after acquiring lock
        if (slot.getIsOccupied()) {
            throw new BusinessException("Slot already occupied");
        }

        // 5. Mark slot as occupied
        slot.setIsOccupied(true);
        parkingSlotRepository.save(slot);

        // 6. Update parking lot availability
        parkingLot.decrementAvailableSlots();
        parkingLotRepository.save(parkingLot);

        // 7. Validate movie ticket if provided
        Ticket movieTicket = null;
        if (request.getMovieTicketId() != null) {
            movieTicket = ticketRepository.findById(request.getMovieTicketId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Movie ticket not found: " + request.getMovieTicketId()));
        }

        // 8. Create parking ticket with calculated fee
        Integer durationHours = 4; // Default duration (TODO: get from request)
        Double calculatedFee = feeCalculationService.calculateFee(request.getVehicleType(), durationHours);
        
        ParkingTicket parkingTicket = ParkingTicket.builder()
                .ticketNumber(generateTicketNumber())
                .vehicleNumber(request.getVehicleNumber().toUpperCase())
                .vehicleType(request.getVehicleType())
                .parkingSlot(slot)
                .movieTicket(movieTicket)
                .entryTime(request.getExpectedArrival() != null ? 
                          request.getExpectedArrival() : LocalDateTime.now().plusHours(1))
                .amountPaid(calculatedFee.intValue())
                .status(ParkingStatus.BOOKED)
                .build();

        parkingTicket = parkingTicketRepository.save(parkingTicket);
        log.info("Parking booked successfully. Ticket: {}", parkingTicket.getTicketNumber());

        return mapToResponse(parkingTicket);
    }

    @Override
    @Transactional
    public ParkingTicketResponse activateParking(String ticketNumber) {
        ParkingTicket ticket = parkingTicketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Parking ticket not found: " + ticketNumber));

        if (ticket.getStatus() != ParkingStatus.BOOKED) {
            throw new BusinessException("Cannot activate ticket in status: " + ticket.getStatus());
        }

        ticket.activate();
        ticket = parkingTicketRepository.save(ticket);
        log.info("Parking activated: {}", ticketNumber);

        return mapToResponse(ticket);
    }

    @Override
    @Transactional
    public ParkingTicketResponse completeParking(String ticketNumber) {
        ParkingTicket ticket = parkingTicketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Parking ticket not found: " + ticketNumber));

        if (ticket.getStatus() != ParkingStatus.ACTIVE) {
            throw new BusinessException("Cannot complete ticket in status: " + ticket.getStatus());
        }

        // Mark ticket as completed
        ticket.complete();
        parkingTicketRepository.save(ticket);

        // Release the slot
        ParkingSlot slot = ticket.getParkingSlot();
        slot.setIsOccupied(false);
        parkingSlotRepository.save(slot);

        // Update parking lot availability
        ParkingLot parkingLot = slot.getParkingLot();
        parkingLot.incrementAvailableSlots();
        parkingLotRepository.save(parkingLot);

        log.info("Parking completed: {}. Duration: {} hours", 
                 ticketNumber, ticket.getDurationInHours());

        return mapToResponse(ticket);
    }

    @Override
    @Transactional
    public void cancelParking(String ticketNumber) {
        ParkingTicket ticket = parkingTicketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Parking ticket not found: " + ticketNumber));

        if (ticket.getStatus() == ParkingStatus.COMPLETED) {
            throw new BusinessException("Cannot cancel completed parking");
        }

        // Mark as cancelled
        ticket.cancel();
        parkingTicketRepository.save(ticket);

        // Release slot if it was occupied
        if (ticket.getStatus() == ParkingStatus.BOOKED || 
            ticket.getStatus() == ParkingStatus.ACTIVE) {
            ParkingSlot slot = ticket.getParkingSlot();
            slot.setIsOccupied(false);
            parkingSlotRepository.save(slot);

            ParkingLot parkingLot = slot.getParkingLot();
            parkingLot.incrementAvailableSlots();
            parkingLotRepository.save(parkingLot);
        }

        log.info("Parking cancelled: {}", ticketNumber);
    }

    @Override
    @Transactional
    public void cancelParking(Integer ticketId) {
        ParkingTicket ticket = parkingTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Parking ticket not found: " + ticketId));
        cancelParking(ticket.getTicketNumber());
    }

    @Override
    @Transactional
    public void confirmParking(Integer ticketId) {
        ParkingTicket ticket = parkingTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Parking ticket not found: " + ticketId));
        
        // Parking is already confirmed when booked (BOOKED status)
        // This method is for add-on integration (no-op for now)
        log.info("Parking confirmed: {}", ticket.getTicketNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingAvailability getAvailability(Integer theaterId) {
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new ResourceNotFoundException("Theater not found: " + theaterId));

        ParkingLot parkingLot = parkingLotRepository.findByTheaterId(theaterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No parking lot for theater: " + theaterId));

        long twoWheeler = parkingSlotRepository
                .findByParkingLotIdAndVehicleTypeAndIsOccupiedFalse(
                        parkingLot.getId(), VehicleType.TWO_WHEELER).size();

        long fourWheeler = parkingSlotRepository
                .findByParkingLotIdAndVehicleTypeAndIsOccupiedFalse(
                        parkingLot.getId(), VehicleType.FOUR_WHEELER).size();

        long ev = parkingSlotRepository
                .findByParkingLotIdAndVehicleTypeAndIsOccupiedFalse(
                        parkingLot.getId(), VehicleType.EV).size();

        return ParkingAvailability.builder()
                .theaterId(theaterId)
                .theaterName(theater.getName())
                .parkingLotName(parkingLot.getName())
                .totalSlots(parkingLot.getTotalSlots())
                .availableSlots(parkingLot.getAvailableSlots())
                .twoWheelerSlots((int) twoWheeler)
                .fourWheelerSlots((int) fourWheeler)
                .evSlots((int) ev)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingTicketResponse getTicket(String ticketNumber) {
        ParkingTicket ticket = parkingTicketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Parking ticket not found: " + ticketNumber));
        return mapToResponse(ticket);
    }

    /**
     * Scheduled job to release expired bookings
     * Runs every 5 minutes
     * Prevents slot deadlock from no-show customers
     */
    @Override
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void releaseExpiredBookings() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(BOOKING_TIMEOUT_MINUTES);
        List<ParkingTicket> expiredTickets = parkingTicketRepository
                .findByStatusAndCreatedAtBefore(ParkingStatus.BOOKED, cutoffTime);

        for (ParkingTicket ticket : expiredTickets) {
            try {
                cancelParking(ticket.getTicketNumber());
                log.info("Auto-cancelled expired parking booking: {}", ticket.getTicketNumber());
            } catch (Exception e) {
                log.error("Failed to cancel expired booking: {}", ticket.getTicketNumber(), e);
            }
        }
    }

    // Helper methods
    private String generateTicketNumber() {
        return "PKG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * @deprecated Use ParkingFeeCalculationService instead
     * Kept for backward compatibility only
     */
    @Deprecated
    private Integer calculateParkingFee(Integer hourlyRate, int hours) {
        return hourlyRate * hours;
    }

    private ParkingTicketResponse mapToResponse(ParkingTicket ticket) {
        return ParkingTicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .vehicleNumber(ticket.getVehicleNumber())
                .vehicleType(ticket.getVehicleType())
                .slotNumber(ticket.getParkingSlot().getSlotNumber())
                .hourlyRate(ticket.getParkingSlot().getHourlyRate())
                .amountPaid(ticket.getAmountPaid())
                .status(ticket.getStatus())
                .entryTime(ticket.getEntryTime())
                .exitTime(ticket.getExitTime())
                .parkingLotName(ticket.getParkingSlot().getParkingLot().getName())
                .theaterName(ticket.getParkingSlot().getParkingLot().getTheater().getName())
                .build();
    }
}
