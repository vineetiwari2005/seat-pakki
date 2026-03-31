package com.driver.bookMyShow.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * StaticPage Entity - Stores content for static pages
 * 
 * Design Principles:
 * - Single entity for all static pages (About, FAQ, Privacy, etc.)
 * - Content stored in database (no hardcoded values)
 * - Future-proof: Add new pages without code changes
 * - JPA manages DDL automatically
 */
@Entity
@Table(name = "static_pages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaticPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Page identifier (e.g., "about_us", "faq", "privacy_policy")
     * Used for API lookups
     */
    @Column(name = "page_key", unique = true, nullable = false, length = 100)
    private String pageKey;

    /**
     * Display title (e.g., "About Us", "Frequently Asked Questions")
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Page content (supports HTML/Markdown)
     */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Display order (for navigation/menu)
     */
    @Column(name = "display_order")
    private Integer displayOrder;

    /**
     * Active flag (to hide pages without deletion)
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
