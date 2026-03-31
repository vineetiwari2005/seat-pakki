package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Models.TheaterSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TheaterSeatRepository extends JpaRepository<TheaterSeat, Integer> {

    @Query("SELECT ts FROM TheaterSeat ts WHERE ts.theater.id = :theaterId ORDER BY ts.seatNo")
    List<TheaterSeat> findByTheaterId(@Param("theaterId") Integer theaterId);
}
