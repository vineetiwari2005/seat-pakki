package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Enums.Genre;
import com.driver.bookMyShow.Enums.Language;
import com.driver.bookMyShow.Enums.TicketStatus;
import com.driver.bookMyShow.Models.Movie;
import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.Repositories.MovieRepository;
import com.driver.bookMyShow.Repositories.TicketRepository;
import com.driver.bookMyShow.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MovieRecommendationService - Personalized recommendations for user dashboard.
 *
 * Rules:
 * 1) If user has booking history: prioritize movies by similar genre/language and frequent selections.
 * 2) If no history: fallback to trending/popular movies by booking count.
 * 3) Return fixed number of recommendations.
 */
@Service
public class MovieRecommendationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private MovieRepository movieRepository;

    public List<Movie> getPersonalizedRecommendations(Integer userId, Integer limit) {
        int safeLimit = (limit == null || limit <= 0) ? 5 : Math.min(limit, 20);

        List<Movie> allMovies = movieRepository.findAll();
        if (allMovies.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, Long> popularityByMovieId = buildPopularityMap();

        if (userId == null) {
            return getTrendingMovies(allMovies, popularityByMovieId, safeLimit);
        }

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return getTrendingMovies(allMovies, popularityByMovieId, safeLimit);
        }

        List<Ticket> bookedTickets = ticketRepository.findByUserAndStatusOrderByBookedAtDesc(
                userOptional.get(), TicketStatus.BOOKED
        );

        if (bookedTickets.isEmpty()) {
            return getTrendingMovies(allMovies, popularityByMovieId, safeLimit);
        }

        Map<Integer, Long> userMovieFrequency = bookedTickets.stream()
                .map(ticket -> ticket.getShow().getMovie())
                .filter(movie -> movie != null && movie.getId() != null)
                .collect(Collectors.groupingBy(Movie::getId, Collectors.counting()));

        Set<Genre> preferredGenres = bookedTickets.stream()
                .map(ticket -> ticket.getShow().getMovie())
                .filter(movie -> movie != null && movie.getGenre() != null)
                .map(Movie::getGenre)
                .collect(Collectors.toSet());

        Set<Language> preferredLanguages = bookedTickets.stream()
                .map(ticket -> ticket.getShow().getMovie())
                .filter(movie -> movie != null && movie.getLanguage() != null)
                .map(Movie::getLanguage)
                .collect(Collectors.toSet());

        Set<Integer> seenMovieIds = new HashSet<>(userMovieFrequency.keySet());

        List<Movie> ranked = new ArrayList<>(allMovies);
        ranked.sort((m1, m2) -> {
            double score1 = scoreMovie(m1, userMovieFrequency, preferredGenres, preferredLanguages, popularityByMovieId, seenMovieIds);
            double score2 = scoreMovie(m2, userMovieFrequency, preferredGenres, preferredLanguages, popularityByMovieId, seenMovieIds);
            int byScore = Double.compare(score2, score1);
            if (byScore != 0) {
                return byScore;
            }
            return Integer.compare(m2.getId(), m1.getId());
        });

        return ranked.stream()
                .filter(movie -> movie.getId() != null)
                .limit(safeLimit)
                .collect(Collectors.toList());
    }

    private double scoreMovie(Movie movie,
                              Map<Integer, Long> userMovieFrequency,
                              Set<Genre> preferredGenres,
                              Set<Language> preferredLanguages,
                              Map<Integer, Long> popularityByMovieId,
                              Set<Integer> seenMovieIds) {
        if (movie == null || movie.getId() == null) {
            return Double.NEGATIVE_INFINITY;
        }

        double score = 0.0;

        if (seenMovieIds.contains(movie.getId())) {
            score += 40.0;
            score += userMovieFrequency.getOrDefault(movie.getId(), 0L) * 4.0;
        }

        if (movie.getGenre() != null && preferredGenres.contains(movie.getGenre())) {
            score += 25.0;
        }

        if (movie.getLanguage() != null && preferredLanguages.contains(movie.getLanguage())) {
            score += 15.0;
        }

        score += popularityByMovieId.getOrDefault(movie.getId(), 0L) * 2.0;

        if (movie.getRating() != null) {
            score += movie.getRating();
        }

        return score;
    }

    private List<Movie> getTrendingMovies(List<Movie> movies, Map<Integer, Long> popularityByMovieId, int limit) {
        return movies.stream()
                .sorted(
                        Comparator.comparingLong((Movie m) -> popularityByMovieId.getOrDefault(m.getId(), 0L)).reversed()
                                .thenComparing((Movie m) -> m.getRating() == null ? 0.0 : m.getRating(), Comparator.reverseOrder())
                                .thenComparing(Movie::getId, Comparator.reverseOrder())
                )
                .limit(limit)
                .collect(Collectors.toList());
    }

    private Map<Integer, Long> buildPopularityMap() {
        Map<Integer, Long> popularity = new HashMap<>();

        List<Ticket> bookedTickets = ticketRepository.findAll().stream()
                .filter(ticket -> ticket.getStatus() == TicketStatus.BOOKED)
                .collect(Collectors.toList());

        for (Ticket ticket : bookedTickets) {
            if (ticket.getShow() == null || ticket.getShow().getMovie() == null || ticket.getShow().getMovie().getId() == null) {
                continue;
            }
            Integer movieId = ticket.getShow().getMovie().getId();
            popularity.put(movieId, popularity.getOrDefault(movieId, 0L) + 1L);
        }

        return popularity;
    }
}
