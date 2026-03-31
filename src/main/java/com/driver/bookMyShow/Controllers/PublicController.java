package com.driver.bookMyShow.Controllers;

import com.driver.bookMyShow.Repositories.CityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * PublicController - Unauthenticated endpoints for public data
 * Used by signup page, city dropdowns, etc.
 */
@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*")
public class PublicController {

    @Autowired
    private CityRepository cityRepository;

    @GetMapping("/cities")
    public ResponseEntity<?> getAllCities() {
        return ResponseEntity.ok(cityRepository.findAll());
    }
}
