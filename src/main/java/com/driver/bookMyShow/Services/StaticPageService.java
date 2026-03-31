package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Models.StaticPage;
import com.driver.bookMyShow.Repositories.StaticPageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * StaticPageService - Business logic for static pages
 * 
 * Design Principles:
 * - Single Responsibility: Only handles static page logic
 * - Stateless service (thread-safe)
 * - All data from Repository (no in-memory storage)
 * - No hardcoded content
 */
@Slf4j
@Service
public class StaticPageService {

    @Autowired
    private StaticPageRepository staticPageRepository;

    /**
     * Get page by key
     * 
     * @param pageKey Page identifier
     * @return StaticPage if found and active
     */
    public Optional<StaticPage> getPageByKey(String pageKey) {
        log.info("Fetching static page: {}", pageKey);
        Optional<StaticPage> page = staticPageRepository.findByPageKey(pageKey);
        
        // Return only if page is active
        if (page.isPresent() && page.get().getIsActive()) {
            return page;
        }
        
        log.warn("Static page not found or inactive: {}", pageKey);
        return Optional.empty();
    }

    /**
     * Get all active pages (for navigation/menu)
     * 
     * @return List of active pages sorted by display order
     */
    public List<StaticPage> getAllActivePages() {
        log.info("Fetching all active static pages");
        return staticPageRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    /**
     * Create or update static page
     * 
     * @param staticPage Page to save
     * @return Saved page
     */
    @Transactional
    public StaticPage savePage(StaticPage staticPage) {
        log.info("Saving static page: {}", staticPage.getPageKey());
        
        // Set default values if not provided
        if (staticPage.getIsActive() == null) {
            staticPage.setIsActive(true);
        }
        if (staticPage.getDisplayOrder() == null) {
            staticPage.setDisplayOrder(0);
        }
        
        return staticPageRepository.save(staticPage);
    }

    /**
     * Delete page by ID
     * 
     * @param pageId Page ID
     */
    @Transactional
    public void deletePage(Integer pageId) {
        log.info("Deleting static page: {}", pageId);
        staticPageRepository.deleteById(pageId);
    }

    /**
     * Soft delete (deactivate) page
     * 
     * @param pageKey Page identifier
     */
    @Transactional
    public void deactivatePage(String pageKey) {
        log.info("Deactivating static page: {}", pageKey);
        Optional<StaticPage> page = staticPageRepository.findByPageKey(pageKey);
        
        if (page.isPresent()) {
            StaticPage staticPage = page.get();
            staticPage.setIsActive(false);
            staticPageRepository.save(staticPage);
        }
    }
}
