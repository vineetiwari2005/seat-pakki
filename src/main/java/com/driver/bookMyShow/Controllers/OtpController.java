package com.driver.bookMyShow.Controllers;

import com.driver.bookMyShow.Dtos.RequestDtos.SendOtpRequestDto;
import com.driver.bookMyShow.Services.OtpService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/otp")
@CrossOrigin(origins = "*")
public class OtpController {

    @Autowired
    private OtpService otpService;

    @PostMapping("/send")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody SendOtpRequestDto request) {
        try {
            OtpService.OtpSendResult result = otpService.sendOtp(
                    request.getUserId(),
                    request.getPurpose(),
                    request.getReferenceId()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "OTP sent successfully");
            response.put("otpRequestId", result.getOtpRequestId());
            response.put("maskedMobile", result.getMaskedMobile());
            response.put("expiresInMinutes", result.getExpiresInMinutes());
            response.put("channel", result.getChannel());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
