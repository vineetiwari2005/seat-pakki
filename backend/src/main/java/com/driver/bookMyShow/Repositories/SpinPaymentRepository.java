package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Models.SpinPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpinPaymentRepository extends JpaRepository<SpinPayment, Long> {
    
    /**
     * Find all payments for a user
     */
    List<SpinPayment> findByUserId(Integer userId);
    
    /**
     * Find completed payments for a user
     */
    List<SpinPayment> findByUserIdAndPaymentStatus(Integer userId, String paymentStatus);
}
