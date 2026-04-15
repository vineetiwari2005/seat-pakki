package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Models.UserExtraSpins;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserExtraSpinsRepository extends JpaRepository<UserExtraSpins, Long> {
    
    /**
     * Find extra spins record by user ID
     */
    Optional<UserExtraSpins> findByUserId(Integer userId);
}
