package com.driver.bookMyShow.modules.addons.repository;

import com.driver.bookMyShow.modules.addons.domain.PaymentAddon;
import com.driver.bookMyShow.modules.addons.enums.AddonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentAddonRepository extends JpaRepository<PaymentAddon, Integer> {
    
    List<PaymentAddon> findBySessionId(String sessionId);
    
    List<PaymentAddon> findByPaymentId(Integer paymentId);
    
    Optional<PaymentAddon> findBySessionIdAndAddonType(String sessionId, AddonType addonType);
    
    List<PaymentAddon> findByUserId(Integer userId);
}
