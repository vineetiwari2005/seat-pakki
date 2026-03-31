package com.driver.bookMyShow.Controllers;

import com.driver.bookMyShow.Dtos.RequestDtos.TheaterEntryDto;
import com.driver.bookMyShow.Dtos.RequestDtos.TheaterSeatEntryDto;
import com.driver.bookMyShow.Models.Theater;
import com.driver.bookMyShow.Services.TheaterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/theater")
@CrossOrigin(origins = "*")
public class TheaterController {

    @Autowired
    private TheaterService theaterService;

    /**
     * Public endpoint - Get all theaters (used by login page for city/theatre dropdown)
     * GET /theater/get-all-theaters
     */
    @GetMapping("/get-all-theaters")
    public ResponseEntity<?> getAllTheaters() {
        try {
            List<Theater> theaters = theaterService.getAllTheaters();
            List<Map<String, Object>> result = theaters.stream().map(t -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", t.getId());
                map.put("name", t.getName());
                map.put("address", t.getAddress());
                map.put("cityName", t.getCityName());
                if (t.getCity() != null) {
                    Map<String, Object> cityMap = new LinkedHashMap<>();
                    cityMap.put("id", t.getCity().getId());
                    cityMap.put("name", t.getCity().getName());
                    map.put("city", cityMap);
                }
                map.put("seatCount", t.getTheaterSeatList() != null ? t.getTheaterSeatList().size() : 0);
                return map;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/addNew")
    public ResponseEntity<String> addTheater(@RequestBody TheaterEntryDto theaterEntryDto) {
        try {
            String result = theaterService.addTheater(theaterEntryDto);
            return new ResponseEntity<>(result, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/addTheaterSeat")
    public ResponseEntity<String> addTheaterSeat(@RequestBody TheaterSeatEntryDto entryDto) {
        try {
            String result = theaterService.addTheaterSeat(entryDto);
            return new ResponseEntity<>(result, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
