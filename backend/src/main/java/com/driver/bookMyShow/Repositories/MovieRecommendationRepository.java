package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Models.MovieRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRecommendationRepository extends JpaRepository<MovieRecommendation, Integer> {

    /**
     * Find all recommendations for a specific theatre
     */
    List<MovieRecommendation> findByTheaterId(Integer theaterId);

    /**
     * Find all recommendations for a specific theatre with a given status
     */
    List<MovieRecommendation> findByTheaterIdAndStatus(Integer theaterId, String status);

    /**
     * Find all recommendations for a specific movie
     */
    List<MovieRecommendation> findByMovieId(Integer movieId);

    /**
     * Check if a recommendation already exists for a movie-theatre pair
     */
    Optional<MovieRecommendation> findByMovieIdAndTheaterId(Integer movieId, Integer theaterId);

    /**
     * Find all pending recommendations for a theatre
     */
    List<MovieRecommendation> findByTheaterIdAndStatusOrderByCreatedAtDesc(Integer theaterId, String status);

    /**
     * Count pending recommendations for a theatre
     */
    long countByTheaterIdAndStatus(Integer theaterId, String status);
}
