package com.driver.bookMyShow.Controllers;

import com.driver.bookMyShow.Enums.Genre;
import com.driver.bookMyShow.Enums.Language;
import com.driver.bookMyShow.Models.Movie;
import com.driver.bookMyShow.Models.Show;
import com.driver.bookMyShow.Services.MovieRecommendationService;
import com.driver.bookMyShow.Services.MovieSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

/**
 * MovieSearchController - Public search and filter endpoints
 * 
 * NEW CONTROLLER - Does not affect existing MovieController
 * No authentication required for browsing
 */
@RestController
@RequestMapping("/api/movies")
@CrossOrigin(origins = "*")
public class MovieSearchController {

    @Autowired
    private MovieSearchService movieSearchService;

    @Autowired
    private com.driver.bookMyShow.Services.MovieShowRedisCacheService movieShowRedisCacheService;

    @Autowired
    private MovieRecommendationService movieRecommendationService;

    private void prewarmFourDayCache() {
        try {
            movieShowRedisCacheService.ensureFourDayCache();
        } catch (Exception e) {
        }
    }

    /**
     * Search movies by name
     * GET /api/movies/search?keyword=avengers
     */
    @GetMapping("/search")
    public ResponseEntity<List<Movie>> searchMovies(@RequestParam String keyword) {
        prewarmFourDayCache();
        List<Movie> movies = movieSearchService.searchMoviesByName(keyword);
        return ResponseEntity.ok(movies);
    }

    /**
     * Filter by genre
     * GET /api/movies/filter/genre?genre=ACTION
     */
    @GetMapping("/filter/genre")
    public ResponseEntity<List<Movie>> filterByGenre(@RequestParam Genre genre) {
        prewarmFourDayCache();
        List<Movie> movies = movieSearchService.filterByGenre(genre);
        return ResponseEntity.ok(movies);
    }

    /**
     * Filter by language
     * GET /api/movies/filter/language?language=HINDI
     */
    @GetMapping("/filter/language")
    public ResponseEntity<List<Movie>> filterByLanguage(@RequestParam Language language) {
        prewarmFourDayCache();
        List<Movie> movies = movieSearchService.filterByLanguage(language);
        return ResponseEntity.ok(movies);
    }

    /**
     * Filter by minimum rating
     * GET /api/movies/filter/rating?minRating=4.0
     */
    @GetMapping("/filter/rating")
    public ResponseEntity<List<Movie>> filterByRating(@RequestParam Double minRating) {
        prewarmFourDayCache();
        List<Movie> movies = movieSearchService.filterByMinimumRating(minRating);
        return ResponseEntity.ok(movies);
    }

    /**
     * Get currently running movies
     * GET /api/movies/now-showing
     */
    @GetMapping("/now-showing")
    public ResponseEntity<List<Movie>> getNowShowing() {
        prewarmFourDayCache();
        List<Movie> movies = movieSearchService.getCurrentlyRunningMovies();
        return ResponseEntity.ok(movies);
    }

    /**
     * Get upcoming movies
     * GET /api/movies/upcoming
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<Movie>> getUpcoming() {
        prewarmFourDayCache();
        List<Movie> movies = movieSearchService.getUpcomingMovies();
        return ResponseEntity.ok(movies);
    }

    /**
     * Get movies by city
     * GET /api/movies/city/mumbai
     */
    @GetMapping("/city/{city}")
    public ResponseEntity<List<Movie>> getMoviesByCity(@PathVariable String city) {
        prewarmFourDayCache();
        List<Movie> movies = movieSearchService.getMoviesByCity(city);
        return ResponseEntity.ok(movies);
    }

    /**
     * Advanced filter with multiple criteria
     * GET /api/movies/filter/advanced
     */
    @GetMapping("/filter/advanced")
    public ResponseEntity<List<Movie>> advancedFilter(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Genre genre,
            @RequestParam(required = false) Language language,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) String city) {
        prewarmFourDayCache();
        
        List<Movie> movies = movieSearchService.advancedFilter(
                keyword, genre, language, minRating, city);
        return ResponseEntity.ok(movies);
    }

    /**
     * Get shows for a movie in a city
     * GET /api/movies/{movieId}/shows?city=mumbai
     */
    @GetMapping("/{movieId}/shows")
    public ResponseEntity<List<Show>> getShowsForMovie(
            @PathVariable Integer movieId,
            @RequestParam String city) {
        prewarmFourDayCache();
        List<Show> shows = movieSearchService.getShowsForMovieInCity(movieId, city);
        return ResponseEntity.ok(shows);
    }

    /**
     * Get personalized movie recommendations for user dashboard
     * GET /api/movies/recommendations?userId=1&limit=5
     */
    @GetMapping("/recommendations")
    public ResponseEntity<List<Movie>> getRecommendations(
            @RequestParam(required = false) Integer userId,
            @RequestParam(defaultValue = "5") Integer limit) {
        prewarmFourDayCache();
        List<Movie> movies = movieRecommendationService.getPersonalizedRecommendations(userId, limit);
        return ResponseEntity.ok(movies);
    }
}
