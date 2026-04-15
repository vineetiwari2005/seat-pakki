package com.driver.bookMyShow.modules.parking.repository;

import com.driver.bookMyShow.modules.parking.entity.ParkingSlot;
import com.driver.bookMyShow.modules.parking.enums.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Integer> {
    
    List<ParkingSlot> findByParkingLotIdAndVehicleTypeAndIsOccupiedFalse(
            Integer parkingLotId, VehicleType vehicleType);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ps FROM ParkingSlot ps WHERE ps.id = :slotId")
    Optional<ParkingSlot> findByIdWithLock(@Param("slotId") Integer slotId);
    
    long countByParkingLotIdAndIsOccupiedFalse(Integer parkingLotId);

    @Query("SELECT ps FROM ParkingSlot ps JOIN ps.parkingLot pl JOIN pl.theater t WHERE t.id = :theaterId")
    List<ParkingSlot> findByTheaterId(@Param("theaterId") Integer theaterId);

    @Query("SELECT ps FROM ParkingSlot ps JOIN ps.parkingLot pl JOIN pl.theater t WHERE t.id = :theaterId AND ps.vehicleType = :vehicleType")
    List<ParkingSlot> findByTheaterIdAndVehicleType(@Param("theaterId") Integer theaterId, @Param("vehicleType") VehicleType vehicleType);
}
