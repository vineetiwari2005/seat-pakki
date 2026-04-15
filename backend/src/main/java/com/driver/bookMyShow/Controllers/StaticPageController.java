package com.driver.bookMyShow.Controllers;

import com.driver.bookMyShow.Models.StaticPage;
import com.driver.bookMyShow.Services.StaticPageService;
import com.driver.bookMyShow.common.dto.ApiResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;
import java.util.Optional;

/**
 * StaticPageController - REST API for static page content
 * 
 * Design Principles:
 * - Thin controller (no business logic)
 * - All logic in StaticPageService
 * - All data from database via Repository
 * - No hardcoded content
 * 
 * Flow: Controller → Service → Repository → Database
 * 
 * Endpoints:
 * GET /api/pages/{pageKey}  - Get specific page (about_us, faq, etc.)
 * GET /api/pages/all        - Get all active pages
 */
@Slf4j
@RestController
@RequestMapping("/api/pages")
@CrossOrigin(origins = "*")
public class StaticPageController {

    @Autowired
    private StaticPageService staticPageService;

    /**
     * Get static page by key
     * 
     * @param pageKey Page identifier (e.g., "about_us", "faq", "privacy_policy", "terms_conditions", "contact_us")
     * @return Page content
     */
    @GetMapping("/{pageKey}")
    public ResponseEntity<ApiResponse<StaticPageDTO>> getPage(@PathVariable String pageKey) {
        log.info("GET /api/pages/{}", pageKey);

        Optional<StaticPage> page = staticPageService.getPageByKey(pageKey);

        if (page.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Page not found: " + pageKey));
        }

        StaticPageDTO dto = StaticPageDTO.fromEntity(page.get());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * Get all active static pages
     * 
     * @return List of active pages
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<StaticPageDTO>>> getAllPages() {
        log.info("GET /api/pages/all");

        List<StaticPage> pages = staticPageService.getAllActivePages();
        List<StaticPageDTO> dtos = pages.stream()
                .map(StaticPageDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /**
     * Admin endpoint: Create or update static page
     * 
     * @param request Page request
     * @return Saved page
     */
    @PostMapping("/admin")
    public ResponseEntity<ApiResponse<StaticPageDTO>> savePage(@RequestBody StaticPageRequest request) {
        log.info("POST /api/pages/admin - {}", request.getPageKey());

        StaticPage staticPage = StaticPage.builder()
                .pageKey(request.getPageKey())
                .title(request.getTitle())
                .content(request.getContent())
                .displayOrder(request.getDisplayOrder())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        StaticPage saved = staticPageService.savePage(staticPage);
        StaticPageDTO dto = StaticPageDTO.fromEntity(saved);

        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    // ==================== DTOs ====================

    /**
     * Data Transfer Object for StaticPage
     */
    @Data
    public static class StaticPageDTO {
        private Integer id;
        private String pageKey;
        private String title;
        private String content;
        private Integer displayOrder;
        private Boolean isActive;

        public static StaticPageDTO fromEntity(StaticPage page) {
            StaticPageDTO dto = new StaticPageDTO();
            dto.setId(page.getId());
            dto.setPageKey(page.getPageKey());
            dto.setTitle(page.getTitle());
            dto.setContent(page.getContent());
            dto.setDisplayOrder(page.getDisplayOrder());
            dto.setIsActive(page.getIsActive());
            return dto;
        }
    }

    /**
     * Request DTO for creating/updating pages
     */
    @Data
    public static class StaticPageRequest {
        private String pageKey;
        private String title;
        private String content;
        private Integer displayOrder;
        private Boolean isActive;
    }
}
