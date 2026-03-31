package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Enums.Genre;
import com.driver.bookMyShow.Enums.Language;
import com.driver.bookMyShow.Models.City;
import com.driver.bookMyShow.Models.Movie;
import com.driver.bookMyShow.Models.Show;
import com.driver.bookMyShow.Models.Theater;
import com.driver.bookMyShow.Repositories.ShowRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Time;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Redis-backed cache for movie/show data (today + next 3 days).
 */
@Service
@Slf4j
public class MovieShowRedisCacheService {

    private static final String CACHE_KEY_PREFIX = "movie_show_cache:";

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.cache.movie-show.ttl-hours:30}")
    private long cacheTtlHours;

    public void ensureFourDayCache() {
        LocalDate today = LocalDate.now();
        for (int i = 0; i <= 3; i++) {
            ensureDateCached(today.plusDays(i));
        }
    }

    public List<Movie> getNowShowingMoviesFromCache() {
        List<CacheShowRow> rows = getFourDayRows();

        Map<Integer, Movie> movieMap = new LinkedHashMap<>();
        for (CacheShowRow row : rows) {
            movieMap.putIfAbsent(row.getMovieId(), toMovie(row));
        }
        return new ArrayList<>(movieMap.values());
    }

    public List<Movie> getMoviesByCityFromCache(String city) {
        String normalizedCity = city == null ? "" : city.trim().toLowerCase();
        if (normalizedCity.isEmpty()) {
            return getNowShowingMoviesFromCache();
        }

        List<CacheShowRow> rows = getFourDayRows().stream()
                .filter(row -> row.getCity() != null && row.getCity().toLowerCase().contains(normalizedCity))
                .collect(Collectors.toList());

        Map<Integer, Movie> movieMap = new LinkedHashMap<>();
        for (CacheShowRow row : rows) {
            movieMap.putIfAbsent(row.getMovieId(), toMovie(row));
        }
        return new ArrayList<>(movieMap.values());
    }

    public List<Show> getShowsForMovieInCityFromCache(Integer movieId, String city) {
        String normalizedCity = city == null ? "" : city.trim().toLowerCase();

        return getFourDayRows().stream()
                .filter(row -> Objects.equals(row.getMovieId(), movieId))
                .filter(row -> normalizedCity.isEmpty() || (row.getCity() != null && row.getCity().toLowerCase().contains(normalizedCity)))
                .map(this::toShow)
                .collect(Collectors.toList());
    }

    private synchronized void ensureDateCached(LocalDate localDate) {
        String key = getKey(localDate);
        Boolean exists = stringRedisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            return;
        }

        Date targetDate = Date.valueOf(localDate);
        List<Show> shows = showRepository.findByDate(targetDate);
        List<CacheShowRow> rows = shows.stream().map(this::toRow).collect(Collectors.toList());

        try {
            String payload = objectMapper.writeValueAsString(rows);
            stringRedisTemplate.opsForValue().set(key, payload, Duration.ofHours(cacheTtlHours));
            log.info("Cached {} shows for {}", rows.size(), localDate);
        } catch (Exception e) {
            log.error("Failed to cache data for {}: {}", localDate, e.getMessage());
        }
    }

    private List<CacheShowRow> getFourDayRows() {
        ensureFourDayCache();

        LocalDate today = LocalDate.now();
        List<CacheShowRow> result = new ArrayList<>();

        for (int i = 0; i <= 3; i++) {
            LocalDate target = today.plusDays(i);
            String key = getKey(target);
            String payload = stringRedisTemplate.opsForValue().get(key);

            if (payload == null || payload.isBlank()) {
                ensureDateCached(target);
                payload = stringRedisTemplate.opsForValue().get(key);
            }

            if (payload == null || payload.isBlank()) {
                continue;
            }

            try {
                List<CacheShowRow> rows = objectMapper.readValue(payload, new TypeReference<List<CacheShowRow>>() {});
                result.addAll(rows);
            } catch (Exception e) {
                log.error("Failed to parse cache key {}: {}", key, e.getMessage());
            }
        }

        return result;
    }

    private String getKey(LocalDate localDate) {
        return CACHE_KEY_PREFIX + localDate;
    }

    private CacheShowRow toRow(Show show) {
        String cityName = null;
        if (show.getTheater() != null) {
            if (show.getTheater().getCity() != null) {
                cityName = show.getTheater().getCity().getName();
            }
            if ((cityName == null || cityName.isBlank()) && show.getTheater().getCityName() != null) {
                cityName = show.getTheater().getCityName();
            }
            if ((cityName == null || cityName.isBlank()) && show.getTheater().getAddress() != null) {
                cityName = show.getTheater().getAddress();
            }
        }

        return CacheShowRow.builder()
                .showId(show.getId())
                .showDate(show.getDate() != null ? show.getDate().toString() : null)
                .showTime(show.getTime() != null ? show.getTime().toString() : null)
                .movieId(show.getMovie() != null ? show.getMovie().getId() : null)
                .movieName(show.getMovie() != null ? show.getMovie().getMovieName() : null)
                .duration(show.getMovie() != null ? show.getMovie().getDuration() : null)
                .rating(show.getMovie() != null ? show.getMovie().getRating() : null)
                .releaseDate(show.getMovie() != null && show.getMovie().getReleaseDate() != null ? show.getMovie().getReleaseDate().toString() : null)
                .genre(show.getMovie() != null && show.getMovie().getGenre() != null ? show.getMovie().getGenre().name() : null)
                .language(show.getMovie() != null && show.getMovie().getLanguage() != null ? show.getMovie().getLanguage().name() : null)
                .description(show.getMovie() != null ? show.getMovie().getDescription() : null)
                .director(show.getMovie() != null ? show.getMovie().getDirector() : null)
                .cast(show.getMovie() != null ? show.getMovie().getCast() : null)
                .posterUrl(show.getMovie() != null ? show.getMovie().getPosterUrl() : null)
                .trailerUrl(show.getMovie() != null ? show.getMovie().getTrailerUrl() : null)
                .nowShowing(show.getMovie() != null && Boolean.TRUE.equals(show.getMovie().getNowShowing()))
                .theaterId(show.getTheater() != null ? show.getTheater().getId() : null)
                .theaterName(show.getTheater() != null ? show.getTheater().getName() : null)
                .theaterAddress(show.getTheater() != null ? show.getTheater().getAddress() : null)
                .city(cityName)
                .build();
    }

    private Movie toMovie(CacheShowRow row) {
        return Movie.builder()
                .id(row.getMovieId())
                .movieName(row.getMovieName())
                .duration(row.getDuration())
                .rating(row.getRating())
                .releaseDate(parseDate(row.getReleaseDate()))
                .genre(parseGenre(row.getGenre()))
                .language(parseLanguage(row.getLanguage()))
                .description(row.getDescription())
                .director(row.getDirector())
                .cast(row.getCast())
                .posterUrl(row.getPosterUrl())
                .trailerUrl(row.getTrailerUrl())
                .nowShowing(row.getNowShowing())
                .build();
    }

    private Show toShow(CacheShowRow row) {
        Movie movie = toMovie(row);

        Theater theater = Theater.builder()
                .id(row.getTheaterId())
                .name(row.getTheaterName())
                .address(row.getTheaterAddress())
                .cityName(row.getCity())
                .city(City.builder().name(row.getCity()).build())
                .build();

        return Show.builder()
                .id(row.getShowId())
                .date(parseDate(row.getShowDate()))
                .time(parseTime(row.getShowTime()))
                .movie(movie)
                .theater(theater)
                .build();
    }

    private Date parseDate(String value) {
        try {
            return value == null ? null : Date.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Time parseTime(String value) {
        try {
            return value == null ? null : Time.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Genre parseGenre(String value) {
        try {
            return value == null ? null : Genre.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Language parseLanguage(String value) {
        try {
            return value == null ? null : Language.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheShowRow {
        private Integer showId;
        private String showDate;
        private String showTime;

        private Integer movieId;
        private String movieName;
        private Integer duration;
        private Double rating;
        private String releaseDate;
        private String genre;
        private String language;
        private String description;
        private String director;
        private String cast;
        private String posterUrl;
        private String trailerUrl;
        private Boolean nowShowing;

        private Integer theaterId;
        private String theaterName;
        private String theaterAddress;
        private String city;
    }
}
