package com.driver.bookMyShow.modules.food.repository;

import com.driver.bookMyShow.modules.food.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Integer> {
    
    List<FoodItem> findByTheaterIdAndIsAvailableTrue(Integer theaterId);
    
    List<FoodItem> findByTheaterIdAndCategoryAndIsAvailableTrue(
            Integer theaterId, String category);
}
