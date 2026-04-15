package com.driver.bookMyShow.modules.parking.repository;

import com.driver.bookMyShow.modules.parking.entity.ParkingTicket;
import com.driver.bookMyShow.modules.parking.enums.ParkingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingTicketRepository extends JpaRepository<ParkingTicket, Integer> {
    
    Optional<ParkingTicket> findByTicketNumber(String ticketNumber);
    
    List<ParkingTicket> findByStatus(ParkingStatus status);
    
    // Find expired bookings (booked > 15 min ago, not activated)
    List<ParkingTicket> findByStatusAndCreatedAtBefore(
            ParkingStatus status, LocalDateTime timestamp);
    
    Optional<ParkingTicket> findByMovieTicketId(Integer movieTicketId);
}
