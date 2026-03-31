package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Models.Theater;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TheaterRepository extends JpaRepository<Theater, Integer> {
    Theater findByAddress(String address);

    /**
     * Find the theatre assigned to a specific admin user
     */
    Optional<Theater> findByAdminId(Integer adminId);

    /**
     * Find all theatres assigned to a specific admin user
     */
    List<Theater> findAllByAdminId(Integer adminId);

    /**
     * Find theatres by city name
     */
    List<Theater> findByCityName(String cityName);

    /**
     * Find theatres by city id
     */
    List<Theater> findByCityId(Integer cityId);
}
