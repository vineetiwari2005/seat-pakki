package com.driver.bookMyShow.modules.parking.service;

import com.driver.bookMyShow.common.exceptions.BusinessException;
import com.driver.bookMyShow.modules.parking.config.ParkingPricingConfig;
import com.driver.bookMyShow.modules.parking.enums.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Test class for Parking Fee Calculation Service
 * 
 * Verifies:
 * - Correct fee calculation based on vehicle type and duration
 * - Configurable pricing from application.properties
 * - Time-based calculations with grace period
 * - Overstay penalty calculations
 * - Edge cases and validation
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ParkingFeeCalculationServiceImplTest {

    @Mock
    private ParkingPricingConfig pricingConfig;

    @InjectMocks
    private ParkingFeeCalculationServiceImpl feeCalculationService;

    private Map<String, Double> testRates;

    @BeforeEach
    void setUp() {
        // Setup test rates
        testRates = new HashMap<>();
        testRates.put("TWO_WHEELER", 30.0);
        testRates.put("FOUR_WHEELER", 50.0);
        testRates.put("EV", 40.0);

        // Mock configuration
        when(pricingConfig.getRates()).thenReturn(testRates);
        when(pricingConfig.getMinimumHours()).thenReturn(1);
        when(pricingConfig.getMaximumHours()).thenReturn(12);
        when(pricingConfig.getGracePeriodMinutes()).thenReturn(15);
        when(pricingConfig.getOverstayMultiplier()).thenReturn(1.5);
    }

    @Test
    void testCalculateFee_TwoWheeler_3Hours() {
        // Arrange
        VehicleType vehicleType = VehicleType.TWO_WHEELER;
        Integer durationHours = 3;

        // Act
        Double fee = feeCalculationService.calculateFee(vehicleType, durationHours);

        // Assert
        assertEquals(90.0, fee, "Fee should be 30 * 3 = 90");
    }

    @Test
    void testCalculateFee_FourWheeler_4Hours() {
        // Arrange
        VehicleType vehicleType = VehicleType.FOUR_WHEELER;
        Integer durationHours = 4;

        // Act
        Double fee = feeCalculationService.calculateFee(vehicleType, durationHours);

        // Assert
        assertEquals(200.0, fee, "Fee should be 50 * 4 = 200");
    }

    @Test
    void testCalculateFee_EV_2Hours() {
        // Arrange
        VehicleType vehicleType = VehicleType.EV;
        Integer durationHours = 2;

        // Act
        Double fee = feeCalculationService.calculateFee(vehicleType, durationHours);

        // Assert
        assertEquals(80.0, fee, "Fee should be 40 * 2 = 80");
    }

    @Test
    void testCalculateFee_MinimumHours() {
        // Arrange
        VehicleType vehicleType = VehicleType.TWO_WHEELER;
        Integer durationHours = 1;

        // Act
        Double fee = feeCalculationService.calculateFee(vehicleType, durationHours);

        // Assert
        assertEquals(30.0, fee, "Fee should be minimum 1 hour = 30");
    }

    @Test
    void testCalculateFee_NullVehicleType() {
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            feeCalculationService.calculateFee(null, 3);
        }, "Should throw exception for null vehicle type");
    }

    @Test
    void testCalculateFee_InvalidDuration() {
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            feeCalculationService.calculateFee(VehicleType.TWO_WHEELER, 0);
        }, "Should throw exception for invalid duration");

        assertThrows(BusinessException.class, () -> {
            feeCalculationService.calculateFee(VehicleType.TWO_WHEELER, -1);
        }, "Should throw exception for negative duration");
    }

    @Test
    void testCalculateFee_TimeBased_WithGracePeriod() {
        // Arrange
        LocalDateTime entryTime = LocalDateTime.of(2026, 1, 6, 10, 0);
        LocalDateTime exitTime = LocalDateTime.of(2026, 1, 6, 13, 10); // 3 hours 10 minutes

        // Act
        Double fee = feeCalculationService.calculateFee(VehicleType.TWO_WHEELER, entryTime, exitTime);

        // Assert
        // 3h 10m - 15m grace = 2h 55m → rounded up to 3 hours
        assertEquals(90.0, fee, "Fee should be 3 hours after grace period");
    }

    @Test
    void testCalculateFee_TimeBased_ExactHours() {
        // Arrange
        LocalDateTime entryTime = LocalDateTime.of(2026, 1, 6, 10, 0);
        LocalDateTime exitTime = LocalDateTime.of(2026, 1, 6, 14, 0); // Exact 4 hours

        // Act
        Double fee = feeCalculationService.calculateFee(VehicleType.FOUR_WHEELER, entryTime, exitTime);

        // Assert
        // 4h - 15m grace = 3h 45m → rounded up to 4 hours
        assertEquals(200.0, fee, "Fee should be 4 hours");
    }

    @Test
    void testGetHourlyRate_ConfiguredVehicleType() {
        // Act
        Double rate = feeCalculationService.getHourlyRate(VehicleType.TWO_WHEELER);

        // Assert
        assertEquals(30.0, rate, "Hourly rate should match configuration");
    }

    @Test
    void testIsValidDuration_Valid() {
        // Act & Assert
        assertTrue(feeCalculationService.isValidDuration(1));
        assertTrue(feeCalculationService.isValidDuration(6));
        assertTrue(feeCalculationService.isValidDuration(12));
    }

    @Test
    void testIsValidDuration_Invalid() {
        // Act & Assert
        assertFalse(feeCalculationService.isValidDuration(0));
        assertFalse(feeCalculationService.isValidDuration(13));
        assertFalse(feeCalculationService.isValidDuration(null));
    }

    @Test
    void testCalculateOverstayPenalty() {
        // Arrange
        Long overstayMinutes = 90L; // 1.5 hours

        // Act
        Double penalty = feeCalculationService.calculateOverstayPenalty(VehicleType.TWO_WHEELER, overstayMinutes);

        // Assert
        // 30 * 2 hours (rounded up) * 1.5 multiplier = 90
        assertEquals(90.0, penalty, "Penalty should be calculated with multiplier");
    }

    @Test
    void testCalculateOverstayPenalty_NoOverstay() {
        // Act
        Double penalty = feeCalculationService.calculateOverstayPenalty(VehicleType.TWO_WHEELER, 0L);

        // Assert
        assertEquals(0.0, penalty, "No penalty for no overstay");
    }

    @Test
    void testDeterministicCalculation() {
        // Arrange
        VehicleType vehicleType = VehicleType.FOUR_WHEELER;
        Integer duration = 5;

        // Act
        Double fee1 = feeCalculationService.calculateFee(vehicleType, duration);
        Double fee2 = feeCalculationService.calculateFee(vehicleType, duration);

        // Assert
        assertEquals(fee1, fee2, "Same input should always produce same output (deterministic)");
    }

    @Test
    void testIdempotentCalculation() {
        // Arrange
        VehicleType vehicleType = VehicleType.EV;
        Integer duration = 3;

        // Act - Call multiple times
        for (int i = 0; i < 10; i++) {
            Double fee = feeCalculationService.calculateFee(vehicleType, duration);
            assertEquals(120.0, fee, "Multiple calls should return same result (idempotent)");
        }
    }
}
