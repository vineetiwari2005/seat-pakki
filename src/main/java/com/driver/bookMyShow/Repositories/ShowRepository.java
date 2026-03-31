package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Models.Movie;
import com.driver.bookMyShow.Models.Show;
import com.driver.bookMyShow.Models.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Date;
import java.sql.Time;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Integer> {

    @Query(value = "select time from shows where date = :date and movie_id = :movieId and theater_id = :theaterId" , nativeQuery = true)
    public List<Time> getShowTimingsOnDate(@Param("date")Date date, @Param("theaterId")Integer theaterId, @Param("movieId")Integer movieId);

    @Query(value = "select movie_id from shows group by movie_id order by count(*) desc limit 1" , nativeQuery = true)
    public Integer getMostShowsMovie();

    @Query(value = "select * from shows where movie_id = :movieId" , nativeQuery = true)
    public List<Show> getAllShowsOfMovie(@Param("movieId")Integer movieId);

    public List<Show> findByMovie(Movie movie);

    List<Show> findByMovieIdAndTheaterId(Integer movieId, Integer theaterId);
    
    public List<Show> findByTheater(Theater theater);

    /**
     * Find all shows for a specific theatre by theatre ID
     */
    List<Show> findByTheaterId(Integer theaterId);

    /**
     * Find shows by theatre ID and date
     */
    List<Show> findByTheaterIdAndDate(Integer theaterId, Date date);

    /**
     * Find all shows for a specific date
     */
    List<Show> findByDate(Date date);

    /**
     * Find all shows between two dates (inclusive)
     */
    List<Show> findByDateBetween(Date startDate, Date endDate);
}
