package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Enums.UserRole;
import com.driver.bookMyShow.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    User findByEmailId(String emailId);

    /**
     * Find all users by role
     */
    List<User> findByRole(UserRole role);

    /**
     * Count users by role
     */
    long countByRole(UserRole role);

    /**
     * Find all active users by role
     */
    List<User> findByRoleAndIsActive(UserRole role, Boolean isActive);
}
