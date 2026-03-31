package com.driver.bookMyShow.Controllers;

import com.driver.bookMyShow.Dtos.RequestDtos.CityEntryDto;
import com.driver.bookMyShow.Dtos.RequestDtos.MovieEntryDto;
import com.driver.bookMyShow.Dtos.RequestDtos.ShowEntryDto;
import com.driver.bookMyShow.Dtos.RequestDtos.TheaterEntryDto;
import com.driver.bookMyShow.Dtos.RequestDtos.TheaterSeatEntryDto;
import com.driver.bookMyShow.Enums.PaymentStatus;
import com.driver.bookMyShow.Models.*;
import com.driver.bookMyShow.Services.AdminService;
import com.driver.bookMyShow.Services.MovieService;
import com.driver.bookMyShow.Services.ShowService;
import com.driver.bookMyShow.Services.TheaterService;
import com.driver.bookMyShow.modules.food.entity.FoodItem;
import com.driver.bookMyShow.modules.food.repository.FoodItemRepository;
import com.driver.bookMyShow.modules.parking.entity.ParkingSlot;
import com.driver.bookMyShow.modules.parking.repository.ParkingSlotRepository;
import com.driver.bookMyShow.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AdminController - Admin panel endpoints
 * 
 * All endpoints require ADMIN role
 * Provides:
 * - Dashboard analytics
 * - Movie/Theater/Show management
 * - User management
 * - Revenue reports
 */
@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private TheaterService theaterService;

    @Autowired
    private ShowService showService;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private ParkingSlotRepository parkingSlotRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get the current logged-in admin user's ID from JWT token
     */
    private Integer getCurrentAdminUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmailId(email);
        return user != null ? user.getId() : null;
    }

    /**
     * Get dashboard analytics
     * GET /admin/dashboard
     * 
     * Returns comprehensive statistics
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> analytics = adminService.getDashboardAnalytics();
        return ResponseEntity.ok(analytics);
    }

    /**
     * Get revenue report
     * GET /admin/revenue-report
     * 
     * Query params: startDate, endDate (ISO format)
     */
    @GetMapping("/revenue-report")
    public ResponseEntity<Map<String, Object>> getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        Map<String, Object> report = adminService.getRevenueReport(startDate, endDate);
        return ResponseEntity.ok(report);
    }

    /**
     * Get all users
     * GET /admin/users
     * Returns lightweight user DTOs to avoid circular reference serialization
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = adminService.getAllUsers();
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getName());
            map.put("emailId", u.getEmailId());
            map.put("role", u.getRole());
            map.put("isActive", u.getIsActive());
            map.put("walletBalance", u.getWalletBalance());
            map.put("age", u.getAge());
            map.put("gender", u.getGender());
            map.put("mobileNo", u.getMobileNo());
            map.put("address", u.getAddress());
            map.put("createdAt", u.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Update user status
     * PUT /admin/users/{userId}/status
     */
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Integer userId,
            @RequestParam boolean isActive) {
        try {
            adminService.updateUserStatus(userId, isActive);
            Map<String, String> response = new HashMap<>();
            response.put("message", "User status updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get all movies
     * GET /admin/movies
     */
    @GetMapping("/movies")
    public ResponseEntity<List<Movie>> getAllMovies() {
        List<Movie> movies = adminService.getAllMovies();
        return ResponseEntity.ok(movies);
    }

    /**
     * Get popular movies
     * GET /admin/movies/popular
     */
    @GetMapping("/movies/popular")
    public ResponseEntity<List<Movie>> getPopularMovies(
            @RequestParam(defaultValue = "10") int limit) {
        List<Movie> movies = adminService.getPopularMovies(limit);
        return ResponseEntity.ok(movies);
    }

    /**
     * Add new movie
     * POST /admin/movies
     */
    @PostMapping("/movies")
    public ResponseEntity<?> addMovie(@RequestBody MovieEntryDto movieEntryDto) {
        try {
            String response = movieService.addMovie(movieEntryDto);
            Map<String, String> result = new HashMap<>();
            result.put("message", response);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Update movie
     * PUT /admin/movies/{movieId}
     */
    @PutMapping("/movies/{movieId}")
    public ResponseEntity<?> updateMovie(
            @PathVariable Integer movieId,
            @RequestBody MovieEntryDto movieEntryDto) {
        try {
            String response = adminService.updateMovie(movieId, movieEntryDto);
            Map<String, String> result = new HashMap<>();
            result.put("message", response);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Delete movie
     * DELETE /admin/movies/{movieId}
     */
    @DeleteMapping("/movies/{movieId}")
    public ResponseEntity<?> deleteMovie(@PathVariable Integer movieId) {
        try {
            adminService.deleteMovie(movieId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Movie deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get all theaters
     * GET /admin/theaters
     * Returns lightweight theater DTOs to avoid circular reference serialization
     */
    @GetMapping("/theaters")
    public ResponseEntity<?> getAllTheaters() {
        List<Theater> theaters = adminService.getAllTheaters();
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
            map.put("createdAt", t.getCreatedAt());
            map.put("seatCount", t.getTheaterSeatList() != null ? t.getTheaterSeatList().size() : 0);
            map.put("showCount", t.getShowList() != null ? t.getShowList().size() : 0);
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Add new theater
     * POST /admin/theaters
     */
    @PostMapping("/theaters")
    public ResponseEntity<?> addTheater(@RequestBody TheaterEntryDto theaterEntryDto) {
        try {
            String response = theaterService.addTheater(theaterEntryDto);
            Map<String, String> result = new HashMap<>();
            result.put("message", response);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Update theater
     * PUT /admin/theaters/{theaterId}
     */
    @PutMapping("/theaters/{theaterId}")
    public ResponseEntity<?> updateTheater(
            @PathVariable Integer theaterId,
            @RequestBody TheaterEntryDto theaterEntryDto) {
        try {
            String response = adminService.updateTheater(theaterId, theaterEntryDto);
            Map<String, String> result = new HashMap<>();
            result.put("message", response);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Add seats to theater
     * POST /admin/theaters/seats
     */
    @PostMapping("/theaters/seats")
    public ResponseEntity<?> addTheaterSeats(@RequestBody TheaterSeatEntryDto theaterSeatEntryDto) {
        try {
            String response = theaterService.addTheaterSeat(theaterSeatEntryDto);
            Map<String, String> result = new HashMap<>();
            result.put("message", response);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Delete theater
     * DELETE /admin/theaters/{theaterId}
     */
    @DeleteMapping("/theaters/{theaterId}")
    public ResponseEntity<?> deleteTheater(@PathVariable Integer theaterId) {
        try {
            adminService.deleteTheater(theaterId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Theater deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get all shows
     * GET /admin/shows
     * Returns lightweight show DTOs to avoid circular reference serialization
     */
    @GetMapping("/shows")
    public ResponseEntity<?> getAllShows() {
        List<Show> shows = adminService.getAllShows();
        List<Map<String, Object>> result = shows.stream().map(s -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", s.getId());
            map.put("date", s.getDate());
            map.put("time", s.getTime());
            if (s.getMovie() != null) {
                Map<String, Object> movieMap = new LinkedHashMap<>();
                movieMap.put("id", s.getMovie().getId());
                movieMap.put("movieName", s.getMovie().getMovieName());
                map.put("movie", movieMap);
            }
            if (s.getTheater() != null) {
                Map<String, Object> theaterMap = new LinkedHashMap<>();
                theaterMap.put("id", s.getTheater().getId());
                theaterMap.put("name", s.getTheater().getName());
                theaterMap.put("cityName", s.getTheater().getCityName());
                map.put("theater", theaterMap);
            }
            map.put("seatCount", s.getShowSeatList() != null ? s.getShowSeatList().size() : 0);
            map.put("ticketCount", s.getTicketList() != null ? s.getTicketList().size() : 0);
            map.put("createdAt", s.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Add new show
     * POST /admin/shows
     */
    @PostMapping("/shows")
    public ResponseEntity<?> addShow(@RequestBody ShowEntryDto showEntryDto) {
        try {
            String response = showService.addShow(showEntryDto);
            Map<String, String> result = new HashMap<>();
            result.put("message", response);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Update show
     * PUT /admin/shows/{showId}
     */
    @PutMapping("/shows/{showId}")
    public ResponseEntity<?> updateShow(
            @PathVariable Integer showId,
            @RequestBody ShowEntryDto showEntryDto) {
        try {
            String response = adminService.updateShow(showId, showEntryDto);
            Map<String, String> result = new HashMap<>();
            result.put("message", response);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Delete show
     * DELETE /admin/shows/{showId}
     */
    @DeleteMapping("/shows/{showId}")
    public ResponseEntity<?> deleteShow(@PathVariable Integer showId) {
        try {
            adminService.deleteShow(showId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Show deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // =====================================================
    // FOOD MANAGEMENT ENDPOINTS
    // =====================================================

    /**
     * Get all food items
     * GET /admin/food
     */
    @GetMapping("/food")
    public ResponseEntity<?> getAllFoodItems() {
        List<FoodItem> foodItems = foodItemRepository.findAll();
        List<Map<String, Object>> result = foodItems.stream().map(f -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", f.getId());
            m.put("itemName", f.getItemName());
            m.put("description", f.getDescription());
            m.put("price", f.getPrice());
            m.put("category", f.getCategory());
            m.put("isAvailable", f.getIsAvailable());
            m.put("imageUrl", f.getImageUrl());
            m.put("isVegetarian", f.getIsVegetarian());
            if (f.getTheater() != null) {
                Map<String, Object> theater = new HashMap<>();
                theater.put("id", f.getTheater().getId());
                theater.put("name", f.getTheater().getName());
                m.put("theater", theater);
            }
            return m;
        }).toList();
        return ResponseEntity.ok(result);
    }

    /**
     * Get food items by theater
     * GET /admin/food/theater/{theaterId}
     */
    @GetMapping("/food/theater/{theaterId}")
    public ResponseEntity<?> getFoodItemsByTheater(@PathVariable Integer theaterId) {
        List<FoodItem> foodItems = foodItemRepository.findAll().stream()
            .filter(f -> f.getTheater() != null && f.getTheater().getId().equals(theaterId))
            .toList();
        List<Map<String, Object>> result = foodItems.stream().map(f -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", f.getId());
            m.put("itemName", f.getItemName());
            m.put("description", f.getDescription());
            m.put("price", f.getPrice());
            m.put("category", f.getCategory());
            m.put("isAvailable", f.getIsAvailable());
            m.put("imageUrl", f.getImageUrl());
            m.put("isVegetarian", f.getIsVegetarian());
            Map<String, Object> theater = new HashMap<>();
            theater.put("id", f.getTheater().getId());
            theater.put("name", f.getTheater().getName());
            m.put("theater", theater);
            return m;
        }).toList();
        return ResponseEntity.ok(result);
    }

    /**
     * Add new food item
     * POST /admin/food
     */
    @PostMapping("/food")
    public ResponseEntity<?> addFoodItem(@RequestBody Map<String, Object> body) {
        try {
            Integer theaterId = null;
            if (body.get("theater") instanceof Map) {
                theaterId = ((Number) ((Map<?, ?>) body.get("theater")).get("id")).intValue();
            } else if (body.get("theaterId") != null) {
                theaterId = ((Number) body.get("theaterId")).intValue();
            }
            if (theaterId == null) throw new Exception("Theater ID is required");
            Theater theater = adminService.getTheaterById(theaterId);
            FoodItem foodItem = new FoodItem();
            foodItem.setItemName((String) body.get("itemName"));
            foodItem.setDescription((String) body.getOrDefault("description", ""));
            foodItem.setPrice(((Number) body.get("price")).intValue());
            foodItem.setCategory((String) body.getOrDefault("category", "SNACKS"));
            foodItem.setIsAvailable(body.get("isAvailable") != null ? (Boolean) body.get("isAvailable") : true);
            foodItem.setTheater(theater);
            FoodItem saved = foodItemRepository.save(foodItem);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Food item added successfully");
            response.put("id", saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Update food item
     * PUT /admin/food/{foodId}
     */
    @PutMapping("/food/{foodId}")
    public ResponseEntity<?> updateFoodItem(
            @PathVariable Integer foodId,
            @RequestBody FoodItem foodItemUpdate) {
        try {
            FoodItem foodItem = foodItemRepository.findById(foodId)
                .orElseThrow(() -> new Exception("Food item not found"));
            
            if (foodItemUpdate.getItemName() != null) {
                foodItem.setItemName(foodItemUpdate.getItemName());
            }
            if (foodItemUpdate.getDescription() != null) {
                foodItem.setDescription(foodItemUpdate.getDescription());
            }
            if (foodItemUpdate.getPrice() != null) {
                foodItem.setPrice(foodItemUpdate.getPrice());
            }
            if (foodItemUpdate.getCategory() != null) {
                foodItem.setCategory(foodItemUpdate.getCategory());
            }
            if (foodItemUpdate.getIsAvailable() != null) {
                foodItem.setIsAvailable(foodItemUpdate.getIsAvailable());
            }
            if (foodItemUpdate.getImageUrl() != null) {
                foodItem.setImageUrl(foodItemUpdate.getImageUrl());
            }
            
            foodItemRepository.save(foodItem);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Food item updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Delete food item
     * DELETE /admin/food/{foodId}
     */
    @DeleteMapping("/food/{foodId}")
    public ResponseEntity<?> deleteFoodItem(@PathVariable Integer foodId) {
        try {
            foodItemRepository.deleteById(foodId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Food item deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // =====================================================
    // PARKING MANAGEMENT ENDPOINTS
    // =====================================================

    /**
     * Get all parking slots
     * GET /admin/parking
     */
    @GetMapping("/parking")
    public ResponseEntity<List<ParkingSlot>> getAllParkingSlots() {
        List<ParkingSlot> parkingSlots = parkingSlotRepository.findAll();
        return ResponseEntity.ok(parkingSlots);
    }

    /**
     * Get parking slots by theater
     * GET /admin/parking/theater/{theaterId}
     */
    @GetMapping("/parking/theater/{theaterId}")
    public ResponseEntity<List<Map<String, Object>>> getParkingSlotsByTheater(@PathVariable Integer theaterId) {
        List<ParkingSlot> parkingSlots = parkingSlotRepository.findAll().stream()
            .filter(p -> p.getParkingLot() != null && 
                        p.getParkingLot().getTheater() != null &&
                        p.getParkingLot().getTheater().getId().equals(theaterId))
            .toList();
        List<Map<String, Object>> result = parkingSlots.stream().map(slot -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", slot.getId());
            m.put("slotNumber", slot.getSlotNumber());
            m.put("vehicleType", slot.getVehicleType());
            m.put("hourlyRate", slot.getHourlyRate());
            m.put("isOccupied", slot.getIsOccupied());
            return m;
        }).toList();
        return ResponseEntity.ok(result);
    }

    /**
     * Update parking slot
     * PUT /admin/parking/{slotId}
     */
    @PutMapping("/parking/{slotId}")
    public ResponseEntity<?> updateParkingSlot(
            @PathVariable Integer slotId,
            @RequestBody ParkingSlot slotUpdate) {
        try {
            ParkingSlot slot = parkingSlotRepository.findById(slotId)
                .orElseThrow(() -> new Exception("Parking slot not found"));
            
            if (slotUpdate.getIsOccupied() != null) {
                slot.setIsOccupied(slotUpdate.getIsOccupied());
            }
            if (slotUpdate.getHourlyRate() != null) {
                slot.setHourlyRate(slotUpdate.getHourlyRate());
            }
            
            parkingSlotRepository.save(slot);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Parking slot updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Enable/Disable parking slot
     * PUT /admin/parking/{slotId}/status
     */
    @PutMapping("/parking/{slotId}/status")
    public ResponseEntity<?> updateParkingSlotStatus(
            @PathVariable Integer slotId,
            @RequestParam boolean isOccupied) {
        try {
            ParkingSlot slot = parkingSlotRepository.findById(slotId)
                .orElseThrow(() -> new Exception("Parking slot not found"));
            
            slot.setIsOccupied(isOccupied);
            parkingSlotRepository.save(slot);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Parking slot status updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // =====================================================
    // BOOKING & PAYMENT ADMIN VIEW ENDPOINTS
    // =====================================================

    /**
     * Get all bookings
     * GET /admin/bookings
     */
    @GetMapping("/bookings")
    public ResponseEntity<List<Ticket>> getAllBookings() {
        List<Ticket> bookings = adminService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }

    /**
     * Get all payments
     * GET /admin/payments
     */
    @GetMapping("/payments")
    public ResponseEntity<List<Payment>> getAllPayments(
            @RequestParam(required = false) String status) {
        try {
            List<Payment> payments;
            if (status != null && !status.isEmpty()) {
                PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
                payments = adminService.getPaymentsByStatus(paymentStatus);
            } else {
                payments = adminService.getAllPayments();
            }
            return ResponseEntity.ok(payments);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Get all wallet transactions
     * GET /admin/wallet/transactions
     */
    @GetMapping("/wallet/transactions")
    public ResponseEntity<List<WalletTransaction>> getAllWalletTransactions() {
        List<WalletTransaction> transactions = adminService.getAllWalletTransactions();
        return ResponseEntity.ok(transactions);
    }

    /**
     * Adjust user wallet balance
     * PUT /admin/wallet/{userId}/adjust
     */
    @PutMapping("/wallet/{userId}/adjust")
    public ResponseEntity<?> adjustUserWallet(
            @PathVariable Integer userId,
            @RequestParam Double amount,
            @RequestParam(required = false) String reason) {
        try {
            String result = adminService.adjustUserWallet(userId, amount, 
                reason != null ? reason : "Admin adjustment");
            Map<String, String> response = new HashMap<>();
            response.put("message", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // =====================================================
    // CITY MANAGEMENT ENDPOINTS
    // =====================================================

    /**
     * Get all cities
     * GET /admin/cities
     */
    @GetMapping("/cities")
    public ResponseEntity<List<City>> getAllCities() {
        List<City> cities = adminService.getAllCities();
        return ResponseEntity.ok(cities);
    }

    /**
     * Add new city
     * POST /admin/cities
     */
    @PostMapping("/cities")
    public ResponseEntity<?> addCity(@RequestBody CityEntryDto cityEntryDto) {
        try {
            String response = adminService.addCity(cityEntryDto);
            Map<String, String> result = new HashMap<>();
            result.put("message", response);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Update city
     * PUT /admin/cities/{cityId}
     */
    @PutMapping("/cities/{cityId}")
    public ResponseEntity<?> updateCity(
            @PathVariable Integer cityId,
            @RequestBody CityEntryDto cityEntryDto) {
        try {
            String response = adminService.updateCity(cityId, cityEntryDto);
            Map<String, String> result = new HashMap<>();
            result.put("message", response);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Delete city
     * DELETE /admin/cities/{cityId}
     */
    @DeleteMapping("/cities/{cityId}")
    public ResponseEntity<?> deleteCity(@PathVariable Integer cityId) {
        try {
            adminService.deleteCity(cityId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "City deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Delete user
     * DELETE /admin/users/{userId}
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Integer userId) {
        try {
            adminService.deleteUser(userId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Upload movie poster
     * POST /admin/movies/upload-poster
     */
    @PostMapping("/movies/upload-poster")
    public ResponseEntity<?> uploadMoviePoster(@RequestParam("file") MultipartFile file) {
        try {
            // Create upload directory if not exists
            String uploadDir = "uploads/posters/";
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : "";
            String filename = UUID.randomUUID().toString() + extension;
            
            // Save file
            Path filepath = Paths.get(uploadDir, filename);
            Files.write(filepath, file.getBytes());

            // Return the URL path (for frontend to access)
            String fileUrl = "/uploads/posters/" + filename;
            
            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);
            response.put("message", "File uploaded successfully");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // =====================================================
    // THEATER SEAT MANAGEMENT
    // =====================================================

    /**
     * Get all seats for a theater
     * GET /admin/theaters/{theaterId}/seats
     */
    @GetMapping("/theaters/{theaterId}/seats")
    public ResponseEntity<?> getTheaterSeats(@PathVariable Integer theaterId) {
        try {
            List<TheaterSeat> seats = adminService.getTheaterSeats(theaterId);
            return ResponseEntity.ok(seats);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Add a row of seats to a theater
     * POST /admin/theaters/{theaterId}/seats/row
     * Body: { "rowPrefix": "A", "seatType": "GOLD", "count": 20 }
     */
    @PostMapping("/theaters/{theaterId}/seats/row")
    public ResponseEntity<?> addTheaterSeatsRow(
            @PathVariable Integer theaterId,
            @RequestBody Map<String, Object> request) {
        try {
            String rowPrefix = (String) request.get("rowPrefix");
            String seatTypeStr = (String) request.get("seatType");
            int count = ((Number) request.get("count")).intValue();

            com.driver.bookMyShow.Enums.SeatType seatType = com.driver.bookMyShow.Enums.SeatType.valueOf(seatTypeStr);
            String result = adminService.addTheaterSeatsRow(theaterId, rowPrefix, seatType, count);

            Map<String, String> response = new HashMap<>();
            response.put("message", result);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Delete a theater seat
     * DELETE /admin/theaters/{theaterId}/seats/{seatId}
     */
    @DeleteMapping("/theaters/{theaterId}/seats/{seatId}")
    public ResponseEntity<?> deleteTheaterSeat(
            @PathVariable Integer theaterId,
            @PathVariable Integer seatId) {
        try {
            adminService.deleteTheaterSeat(theaterId, seatId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Theater seat deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // =====================================================
    // SHOW SEAT MANAGEMENT
    // =====================================================

    /**
     * Get all seats for a show
     * GET /admin/shows/{showId}/seats
     */
    @GetMapping("/shows/{showId}/seats")
    public ResponseEntity<?> getShowSeats(@PathVariable Integer showId) {
        try {
            List<ShowSeat> seats = adminService.getShowSeats(showId);
            return ResponseEntity.ok(seats);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Add a single seat to a show
     * POST /admin/shows/{showId}/seats
     * Body: { "seatNo": "A1", "seatType": "GOLD", "price": 250 }
     */
    @PostMapping("/shows/{showId}/seats")
    public ResponseEntity<?> addShowSeat(
            @PathVariable Integer showId,
            @RequestBody Map<String, Object> request) {
        try {
            String seatNo = (String) request.get("seatNo");
            String seatTypeStr = (String) request.get("seatType");
            int price = ((Number) request.get("price")).intValue();

            com.driver.bookMyShow.Enums.SeatType seatType = com.driver.bookMyShow.Enums.SeatType.valueOf(seatTypeStr);
            ShowSeat seat = adminService.addShowSeat(showId, seatNo, seatType, price);

            return ResponseEntity.status(HttpStatus.CREATED).body(seat);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Add a row of seats to a show
     * POST /admin/shows/{showId}/seats/row
     * Body: { "rowPrefix": "A", "seatType": "GOLD", "count": 20, "price": 250 }
     */
    @PostMapping("/shows/{showId}/seats/row")
    public ResponseEntity<?> addShowSeatsRow(
            @PathVariable Integer showId,
            @RequestBody Map<String, Object> request) {
        try {
            String rowPrefix = (String) request.get("rowPrefix");
            String seatTypeStr = (String) request.get("seatType");
            int count = ((Number) request.get("count")).intValue();
            int price = ((Number) request.get("price")).intValue();

            com.driver.bookMyShow.Enums.SeatType seatType = com.driver.bookMyShow.Enums.SeatType.valueOf(seatTypeStr);
            String result = adminService.addShowSeatsRow(showId, rowPrefix, seatType, count, price);

            Map<String, String> response = new HashMap<>();
            response.put("message", result);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Auto-generate show seats from theater seat layout
     * POST /admin/shows/{showId}/seats/generate
     */
    @PostMapping("/shows/{showId}/seats/generate")
    public ResponseEntity<?> generateShowSeats(@PathVariable Integer showId) {
        try {
            String result = adminService.generateShowSeatsFromTheater(showId);
            Map<String, String> response = new HashMap<>();
            response.put("message", result);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Delete a show seat
     * DELETE /admin/shows/{showId}/seats/{seatId}
     */
    @DeleteMapping("/shows/{showId}/seats/{seatId}")
    public ResponseEntity<?> deleteShowSeat(
            @PathVariable Integer showId,
            @PathVariable Integer seatId) {
        try {
            adminService.deleteShowSeat(showId, seatId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Show seat deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Toggle show seat availability (admin override)
     * PUT /admin/shows/{showId}/seats/{seatId}/availability
     */
    @PutMapping("/shows/{showId}/seats/{seatId}/availability")
    public ResponseEntity<?> toggleShowSeatAvailability(
            @PathVariable Integer showId,
            @PathVariable Integer seatId) {
        try {
            ShowSeat seat = adminService.toggleShowSeatAvailability(showId, seatId);
            return ResponseEntity.ok(seat);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Update show seat price
     * PUT /admin/shows/{showId}/seats/{seatId}/price
     */
    @PutMapping("/shows/{showId}/seats/{seatId}/price")
    public ResponseEntity<?> updateShowSeatPrice(
            @PathVariable Integer showId,
            @PathVariable Integer seatId,
            @RequestBody Map<String, Object> request) {
        try {
            int newPrice = ((Number) request.get("price")).intValue();
            ShowSeat seat = adminService.updateShowSeatPrice(showId, seatId, newPrice);
            return ResponseEntity.ok(seat);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // =====================================================
    // THEATRE ADMIN MANAGEMENT
    // =====================================================

    /**
     * Get all theatre admins (THEATER_OWNER users)
     * GET /admin/theatre-admins
     */
    @GetMapping("/theatre-admins")
    public ResponseEntity<?> getAllTheatreAdmins() {
        List<Map<String, Object>> result = adminService.getAllTheatreAdmins();
        return ResponseEntity.ok(result);
    }

    /**
     * Assign a theatre admin to a theatre
     * PUT /admin/theaters/{theaterId}/assign-admin
     * Body: { "adminUserId": 5 }
     */
    @PutMapping("/theaters/{theaterId}/assign-admin")
    public ResponseEntity<?> assignTheatreAdmin(
            @PathVariable Integer theaterId,
            @RequestBody Map<String, Object> request) {
        try {
            Integer adminUserId = ((Number) request.get("adminUserId")).intValue();
            String result = adminService.assignTheatreAdmin(theaterId, adminUserId);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Remove theatre admin from a theatre
     * PUT /admin/theaters/{theaterId}/remove-admin
     */
    @PutMapping("/theaters/{theaterId}/remove-admin")
    public ResponseEntity<?> removeTheatreAdmin(@PathVariable Integer theaterId) {
        try {
            String result = adminService.removeTheatreAdmin(theaterId);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // =====================================================
    // MOVIE RECOMMENDATION TO THEATRES
    // =====================================================

    /**
     * Recommend a movie to a theatre
     * POST /admin/recommendations
     * Body: { "movieId": 1, "theaterId": 2, "message": "Popular movie, please schedule" }
     */
    @PostMapping("/recommendations")
    public ResponseEntity<?> recommendMovie(@RequestBody Map<String, Object> request) {
        try {
            Integer movieId = ((Number) request.get("movieId")).intValue();
            Integer theaterId = ((Number) request.get("theaterId")).intValue();
            String message = (String) request.get("message");
            Integer adminUserId = getCurrentAdminUserId();

            String result = adminService.recommendMovieToTheatre(movieId, theaterId, adminUserId, message);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all movie recommendations
     * GET /admin/recommendations
     */
    @GetMapping("/recommendations")
    public ResponseEntity<?> getAllRecommendations() {
        List<Map<String, Object>> result = adminService.getAllRecommendations();
        return ResponseEntity.ok(result);
    }

    /**
     * Recommend a movie to all theatres in a city
     * POST /admin/recommendations/city
     * Body: { "movieId": 1, "cityId": 2, "message": "New release" }
     */
    @PostMapping("/recommendations/city")
    public ResponseEntity<?> recommendMovieToCity(@RequestBody Map<String, Object> request) {
        try {
            Integer movieId = ((Number) request.get("movieId")).intValue();
            Integer cityId = ((Number) request.get("cityId")).intValue();
            String message = (String) request.get("message");
            Integer adminUserId = getCurrentAdminUserId();

            String result = adminService.recommendMovieToCity(movieId, cityId, adminUserId, message);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // =====================================================
    // NEW: User Detail Analytics
    // =====================================================
    @GetMapping("/users/{userId}/analytics")
    public ResponseEntity<?> getUserDetailAnalytics(@PathVariable Integer userId) {
        try {
            Map<String, Object> analytics = adminService.getUserDetailAnalytics(userId);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // =====================================================
    // NEW: Parking Price by Theatre (bulk update)
    // =====================================================
    @PutMapping("/parking/theater/{theaterId}/price")
    public ResponseEntity<?> updateParkingPriceByTheater(
            @PathVariable Integer theaterId,
            @RequestParam(defaultValue = "ALL") String vehicleType,
            @RequestParam Integer hourlyRate) {
        try {
            String result = adminService.updateParkingPriceByTheater(theaterId, vehicleType, hourlyRate);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // =====================================================
    // NEW: Detailed Bookings & Payments
    // =====================================================
    @GetMapping("/bookings/detailed")
    public ResponseEntity<?> getDetailedBookings() {
        return ResponseEntity.ok(adminService.getAllBookingsDetailed());
    }

    @GetMapping("/payments/detailed")
    public ResponseEntity<?> getDetailedPayments(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminService.getAllPaymentsDetailed(status));
    }
}
