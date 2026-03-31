package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Enums.Genre;
import com.driver.bookMyShow.Enums.Language;
import com.driver.bookMyShow.Models.Movie;
import com.driver.bookMyShow.Models.Show;
import com.driver.bookMyShow.Models.Theater;
import com.driver.bookMyShow.Repositories.MovieRepository;
import com.driver.bookMyShow.Repositories.TheaterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MovieSearchService - Advanced search and filter operations
 * 
 * NEW SERVICE - Does not affect existing MovieService
 * Provides enhanced search capabilities
 */
@Service
public class MovieSearchService {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private TheaterRepository theaterRepository;

        @Autowired
        private MovieShowRedisCacheService movieShowRedisCacheService;

    /**
     * Search movies by name (partial match, case-insensitive)
     */
    public List<Movie> searchMoviesByName(String keyword) {
        List<Movie> allMovies = movieRepository.findAll();
        return allMovies.stream()
                .filter(movie -> movie.getMovieName().toLowerCase()
                        .contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Filter movies by genre
     */
    public List<Movie> filterByGenre(Genre genre) {
        List<Movie> allMovies = movieRepository.findAll();
        return allMovies.stream()
                .filter(movie -> movie.getGenre() == genre)
                .collect(Collectors.toList());
    }

    /**
     * Filter movies by language
     */
    public List<Movie> filterByLanguage(Language language) {
        List<Movie> allMovies = movieRepository.findAll();
        return allMovies.stream()
                .filter(movie -> movie.getLanguage() == language)
                .collect(Collectors.toList());
    }

    /**
     * Filter movies by minimum rating
     */
    public List<Movie> filterByMinimumRating(Double minRating) {
        List<Movie> allMovies = movieRepository.findAll();
        return allMovies.stream()
                .filter(movie -> movie.getRating() != null && movie.getRating() >= minRating)
                .collect(Collectors.toList());
    }

    /**
         * Get movies for listing on browse pages.
         *
         * Intentionally returns all movies so users can open a movie even when no
         * shows are currently available in their city/date.
     */
    public List<Movie> getCurrentlyRunningMovies() {
        try {
            movieShowRedisCacheService.ensureFourDayCache();
        } catch (Exception e) {
        }

                return movieRepository.findAll();
    }

    /**
     * Get upcoming movies (release date in future)
     */
    public List<Movie> getUpcomingMovies() {
        List<Movie> allMovies = movieRepository.findAll();
        Date today = new Date(System.currentTimeMillis());
        
        return allMovies.stream()
                .filter(movie -> movie.getReleaseDate() != null && 
                               movie.getReleaseDate().after(today))
                .collect(Collectors.toList());
    }

    /**
     * Get movies by city (based on theater location)
     */
    public List<Movie> getMoviesByCity(String city) {
                // Keep movie listing independent of show availability/city cache.
        return getCurrentlyRunningMovies();
    }

    /**
     * Advanced filter with multiple criteria
     */
    public List<Movie> advancedFilter(String keyword, Genre genre, Language language, 
                                     Double minRating, String city) {
        List<Movie> movies = movieRepository.findAll();

        // Apply filters sequentially
        if (keyword != null && !keyword.isEmpty()) {
            movies = movies.stream()
                    .filter(movie -> movie.getMovieName().toLowerCase()
                            .contains(keyword.toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (genre != null) {
            movies = movies.stream()
                    .filter(movie -> movie.getGenre() == genre)
                    .collect(Collectors.toList());
        }

        if (language != null) {
            movies = movies.stream()
                    .filter(movie -> movie.getLanguage() == language)
                    .collect(Collectors.toList());
        }

        if (minRating != null) {
            movies = movies.stream()
                    .filter(movie -> movie.getRating() != null && movie.getRating() >= minRating)
                    .collect(Collectors.toList());
        }

        if (city != null && !city.isEmpty()) {
            List<Movie> cityMovies = getMoviesByCity(city);
            movies = movies.stream()
                    .filter(cityMovies::contains)
                    .collect(Collectors.toList());
        }

        return movies;
    }

    /**
     * Get shows for a movie in a specific city
     */
    public List<Show> getShowsForMovieInCity(Integer movieId, String city) {
                try {
                        movieShowRedisCacheService.ensureFourDayCache();
                        List<Show> cachedShows = movieShowRedisCacheService.getShowsForMovieInCityFromCache(movieId, city);
                        if (!cachedShows.isEmpty()) {
                                return cachedShows;
                        }
                } catch (Exception e) {
                }

        List<Theater> cityTheaters = theaterRepository.findAll().stream()
                .filter(theater -> theater.getAddress().toLowerCase()
                        .contains(city.toLowerCase()))
                .collect(Collectors.toList());

        return cityTheaters.stream()
                .flatMap(theater -> theater.getShowList().stream())
                .filter(show -> show.getMovie().getId().equals(movieId))
                .collect(Collectors.toList());
    }
}
