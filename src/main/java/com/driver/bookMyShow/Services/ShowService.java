package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Dtos.RequestDtos.ShowEntryDto;
import com.driver.bookMyShow.Dtos.RequestDtos.ShowSeatEntryDto;
import com.driver.bookMyShow.Dtos.RequestDtos.ShowTimingsDto;
import com.driver.bookMyShow.Dtos.ResponseDtos.ShowResponseDto;
import com.driver.bookMyShow.Enums.SeatType;
import com.driver.bookMyShow.Exceptions.MovieDoesNotExists;
import com.driver.bookMyShow.Exceptions.ShowDoesNotExists;
import com.driver.bookMyShow.Exceptions.TheaterDoesNotExists;
import com.driver.bookMyShow.Models.*;
import com.driver.bookMyShow.Repositories.MovieRepository;
import com.driver.bookMyShow.Repositories.ShowRepository;
import com.driver.bookMyShow.Repositories.ShowSeatRepository;
import com.driver.bookMyShow.Repositories.TheaterRepository;
import com.driver.bookMyShow.Transformers.ShowTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShowService {

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Transactional
    public String addShow(ShowEntryDto showEntryDto) throws MovieDoesNotExists, TheaterDoesNotExists{
        Show show = ShowTransformer.showDtoToShow(showEntryDto);

        Optional<Movie> movieOpt = movieRepository.findById(showEntryDto.getMovieId());
        if(movieOpt.isEmpty()) {
            throw new MovieDoesNotExists();
        }
        Optional<Theater> theaterOpt = theaterRepository.findById(showEntryDto.getTheaterId());
        if(theaterOpt.isEmpty()) {
            throw new TheaterDoesNotExists();
        }

        Theater theater = theaterOpt.get();
        Movie movie = movieOpt.get();

        show.setMovie(movie);
        show.setTheater(theater);
        show = showRepository.save(show);

        movie.getShows().add(show);
        theater.getShowList().add(show);

        movieRepository.save(movie);
        theaterRepository.save(theater);

        // AUTO-CREATE SEATS: Automatically generate seats for the show from theater seats
        List<TheaterSeat> theaterSeatList = theater.getTheaterSeatList();
        if (theaterSeatList != null && !theaterSeatList.isEmpty()) {
            List<ShowSeat> showSeatList = show.getShowSeatList();
            for (TheaterSeat theaterSeat : theaterSeatList) {
                ShowSeat showSeat = new ShowSeat();
                showSeat.setSeatNo(theaterSeat.getSeatNo());
                showSeat.setSeatType(theaterSeat.getSeatType());

                // Set default prices based on seat type
                // COUPLE: ₹600, PREMIUM: ₹350, GOLD: ₹250, CLASSIC/SILVER: ₹150
                switch (theaterSeat.getSeatType()) {
                    case COUPLE:
                        showSeat.setPrice(600);
                        break;
                    case PREMIUM:
                        showSeat.setPrice(350);
                        break;
                    case GOLD:
                        showSeat.setPrice(250);
                        break;
                    case SILVER:
                        showSeat.setPrice(150);
                        break;
                    case CLASSIC:
                    default:
                        showSeat.setPrice(150);
                        break;
                }

                showSeat.setShow(show);
                showSeat.setIsAvailable(Boolean.TRUE);
                showSeat.setIsFoodContains(Boolean.FALSE);

                showSeatList.add(showSeat);
            }
            showRepository.save(show);
        }

        return "Show has been added Successfully";
    }

    public String associateShowSeats(ShowSeatEntryDto showSeatEntryDto) throws ShowDoesNotExists{
        Optional<Show> showOpt = showRepository.findById(showSeatEntryDto.getShowId());
        if(showOpt.isEmpty()) {
            throw new ShowDoesNotExists();
        }
        Show show = showOpt.get();
        Theater theater = show.getTheater();

        List<TheaterSeat> theaterSeatList = theater.getTheaterSeatList();

        List<ShowSeat> showSeatList = show.getShowSeatList();
        for(TheaterSeat theaterSeat : theaterSeatList) {
            ShowSeat showSeat = new ShowSeat();
            showSeat.setSeatNo(theaterSeat.getSeatNo());
            showSeat.setSeatType(theaterSeat.getSeatType());

            if(showSeat.getSeatType().equals(SeatType.CLASSIC)) {
                showSeat.setPrice((showSeatEntryDto.getPriceOfClassicSeat()));
            } else {
                showSeat.setPrice(showSeatEntryDto.getPriceOfPremiumSeat());
            }

            showSeat.setShow(show);
            showSeat.setIsAvailable(Boolean.TRUE);
            showSeat.setIsFoodContains(Boolean.FALSE);

            showSeatList.add(showSeat);
        }
        showRepository.save(show);

        return "Show seats have been associated successfully";
    }

    public List<Time> showTimingsOnDate(ShowTimingsDto showTimingsDto) {
        Date date = showTimingsDto.getDate();
        Integer theaterId = showTimingsDto.getTheaterId();
        Integer movieId = showTimingsDto.getMovieId();
        return showRepository.getShowTimingsOnDate(date, theaterId, movieId);
    }

    public String movieHavingMostShows() {
        Integer movieId = showRepository.getMostShowsMovie();
        return movieRepository.findById(movieId).get().getMovieName();
    }

    public List<Show> getShowsByMovieId(Integer movieId) throws MovieDoesNotExists {
        Optional<Movie> movieOpt = movieRepository.findById(movieId);
        if(movieOpt.isEmpty()) {
            throw new MovieDoesNotExists();
        }
        return showRepository.findByMovie(movieOpt.get());
    }

    public List<Show> getShowsByTheaterId(Integer theaterId) throws TheaterDoesNotExists {
        Optional<Theater> theaterOpt = theaterRepository.findById(theaterId);
        if(theaterOpt.isEmpty()) {
            throw new TheaterDoesNotExists();
        }
        return showRepository.findByTheater(theaterOpt.get());
    }

    public Show getShowById(Integer showId) throws ShowDoesNotExists {
        Optional<Show> showOpt = showRepository.findById(showId);
        if(showOpt.isEmpty()) {
            throw new ShowDoesNotExists();
        }
        return showOpt.get();
    }

    public List<ShowSeat> getShowSeats(Integer showId) throws ShowDoesNotExists {
        // Verify show exists
        getShowById(showId);
        // Use direct repo query instead of lazy-loaded collection
        return showSeatRepository.findByShowId(showId);
    }

    /**
     * Get ShowResponseDtos for a movie (filtered to 3+ hours in future, with seat counts)
     * Business logic moved from ShowController to maintain layered architecture.
     */
    public List<ShowResponseDto> getShowDtosByMovieId(Integer movieId) throws MovieDoesNotExists {
        List<Show> shows = getShowsByMovieId(movieId);
        return filterAndBuildShowDtos(shows);
    }

    /**
     * Get ShowResponseDtos for a theater (filtered to 3+ hours in future, with seat counts)
     * Business logic moved from ShowController to maintain layered architecture.
     */
    public List<ShowResponseDto> getShowDtosByTheaterId(Integer theaterId) throws TheaterDoesNotExists {
        List<Show> shows = getShowsByTheaterId(theaterId);
        return filterAndBuildShowDtos(shows);
    }

    /**
     * Filter shows to future only and build DTOs with seat counts from DB.
     * Shows are visible until 30 minutes before showtime.
     */
    private List<ShowResponseDto> filterAndBuildShowDtos(List<Show> shows) {
        LocalDateTime minShowTime = LocalDateTime.now().plusMinutes(30);

        return shows.stream()
            .filter(show -> {
                LocalDateTime showDateTime = LocalDateTime.of(
                    show.getDate().toLocalDate(),
                    show.getTime().toLocalTime()
                );
                return showDateTime.isAfter(minShowTime);
            })
            .map(show -> {
                long availableSeats = showSeatRepository.countAvailableSeats(show);
                long totalSeats = showSeatRepository.countByShow(show);

                return ShowResponseDto.builder()
                    .id(show.getId())
                    .time(show.getTime().toLocalTime())
                    .date(show.getDate().toLocalDate())
                    .createdAt(show.getCreatedAt())
                    .theaterId(show.getTheater().getId())
                    .theaterName(show.getTheater().getName())
                    .theaterAddress(show.getTheater().getAddress())
                    .theaterCity(show.getTheater().getCity() != null ? show.getTheater().getCity().getName() : show.getTheater().getCityName())
                    .movieId(show.getMovie().getId())
                    .movieName(show.getMovie().getMovieName())
                    .posterUrl(show.getMovie().getPosterUrl())
                    .availableSeats((int) availableSeats)
                    .totalSeats((int) totalSeats)
                    .build();
            })
            .collect(Collectors.toList());
    }
}
