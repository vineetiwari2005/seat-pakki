package com.driver.bookMyShow.Controllers;

import com.driver.bookMyShow.Dtos.RequestDtos.ShowEntryDto;
import com.driver.bookMyShow.Dtos.RequestDtos.ShowSeatEntryDto;
import com.driver.bookMyShow.Dtos.RequestDtos.ShowTimingsDto;
import com.driver.bookMyShow.Dtos.ResponseDtos.ShowResponseDto;
import com.driver.bookMyShow.Models.Show;
import com.driver.bookMyShow.Models.ShowSeat;
import com.driver.bookMyShow.Services.ShowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.sql.Time;
import java.util.List;

/**
 * ShowController - REST API for show management
 * 
 * Strict Layered Architecture: Controller → Service → Repository → Database
 * No repository injections. No business logic.
 */
@RestController
@RequestMapping("/show")
@CrossOrigin(origins = "*")
public class ShowController {

    @Autowired
    private ShowService showService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/addNew")
    public ResponseEntity<String> addShow(@RequestBody ShowEntryDto showEntryDto) {
        try {
            String result = showService.addShow(showEntryDto);
            return new ResponseEntity<>(result, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/associateSeats")
    public ResponseEntity<String> associateShowSeats(@RequestBody ShowSeatEntryDto showSeatEntryDto) {
        try {
            String result = showService.associateShowSeats(showSeatEntryDto);
            return new ResponseEntity<>(result, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/showTimingsOnDate")
    public ResponseEntity<List<Time>> showTimingsOnDate(ShowTimingsDto showTimingsDto) {
        try {
            List<Time> result = showService.showTimingsOnDate(showTimingsDto);
            return new ResponseEntity<>(result, HttpStatus.FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/movieHavingMostShows")
    public ResponseEntity<String> movieHavingMostShows() {
        try {
            String movie = showService.movieHavingMostShows();
            return new ResponseEntity<>(movie, HttpStatus.FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/by-movie/{movieId}")
    public ResponseEntity<List<ShowResponseDto>> getShowsByMovie(@PathVariable Integer movieId) {
        try {
            List<ShowResponseDto> showDtos = showService.getShowDtosByMovieId(movieId);
            return new ResponseEntity<>(showDtos, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/by-theater/{theaterId}")
    public ResponseEntity<List<ShowResponseDto>> getShowsByTheater(@PathVariable Integer theaterId) {
        try {
            List<ShowResponseDto> showDtos = showService.getShowDtosByTheaterId(theaterId);
            return new ResponseEntity<>(showDtos, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{showId}")
    public ResponseEntity<Show> getShowById(@PathVariable Integer showId) {
        try {
            Show show = showService.getShowById(showId);
            return new ResponseEntity<>(show, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Get seat availability for a show
     */
    @GetMapping("/{showId}/seats/availability")
    public ResponseEntity<?> getSeatAvailability(@PathVariable Integer showId) {
        try {
            List<ShowSeat> seats = showService.getShowSeats(showId);
            return ResponseEntity.ok(seats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
