package com.driver.bookMyShow.modules.food.repository;

import com.driver.bookMyShow.modules.food.entity.FoodOrder;
import com.driver.bookMyShow.modules.food.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodOrderRepository extends JpaRepository<FoodOrder, Integer> {
    
    Optional<FoodOrder> findByOrderNumber(String orderNumber);
    
    List<FoodOrder> findByUserId(Integer userId);
    
    List<FoodOrder> findByTicketId(Integer ticketId);
    
    List<FoodOrder> findByStatus(OrderStatus status);
    
    Optional<FoodOrder> findByTicketIdAndStatus(Integer ticketId, OrderStatus status);
}
