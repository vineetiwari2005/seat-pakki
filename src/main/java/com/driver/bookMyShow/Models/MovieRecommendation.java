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
 * MovieRecommendation - Represents a movie recommendation from Main Admin to a Theatre.
 * When the Main Admin suggests a movie for a theatre, a record is created here.
 * The Theatre Admin sees these recommendations on their dashboard and can schedule shows.
 */
@Entity
@Table(name = "movie_recommendations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"movie_id", "theater_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne
    @JoinColumn(name = "theater_id", nullable = false)
    private Theater theater;

    /**
     * Status of the recommendation:
     * PENDING - Suggested by admin, not yet acted on by theatre admin
     * ACCEPTED - Theatre admin has accepted and scheduled shows
     * REJECTED - Theatre admin has declined the suggestion
     */
    @Column(nullable = false)
    private String status = "PENDING";

    /**
     * Optional message from main admin to theatre admin
     */
    @Column(length = 500)
    private String adminMessage;

    /**
     * Optional response message from theatre admin
     */
    @Column(name = "theatre_admin_response", length = 500)
    private String theatreAdminResponse;

    /**
     * The admin who made the recommendation
     */
    @ManyToOne
    @JoinColumn(name = "recommended_by")
    private User recommendedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
