package com.driver.bookMyShow.modules.parking.repository;

import com.driver.bookMyShow.modules.parking.entity.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, Integer> {
    
    Optional<ParkingLot> findByTheaterId(Integer theaterId);
    
    boolean existsByTheaterId(Integer theaterId);
}
