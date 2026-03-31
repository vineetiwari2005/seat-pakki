package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Models.StaticPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * StaticPageRepository - Data access for static pages
 * 
 * Design Principles:
 * - Spring Data JPA naming conventions
 * - No hardcoded queries
 * - Repository pattern (no business logic)
 */
@Repository
public interface StaticPageRepository extends JpaRepository<StaticPage, Integer> {

    /**
     * Find page by unique key
     * @param pageKey Page identifier (e.g., "about_us", "faq")
     * @return StaticPage if found
     */
    Optional<StaticPage> findByPageKey(String pageKey);

    /**
     * Find all active pages ordered by display order
     * @return List of active pages
     */
    List<StaticPage> findByIsActiveTrueOrderByDisplayOrderAsc();

    /**
     * Find all active pages
     * @param isActive Active status
     * @return List of pages
     */
    List<StaticPage> findByIsActive(Boolean isActive);

    /**
     * Check if page exists by key
     * @param pageKey Page identifier
     * @return true if exists
     */
    boolean existsByPageKey(String pageKey);
}
