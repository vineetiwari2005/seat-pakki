package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.Repositories.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OtpService {

    private static final SecureRandom OTP_RANDOM = new SecureRandom();
    private final ConcurrentHashMap<String, OtpSession> otpStore = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailService emailService;

    @Value("${otp.enabled:true}")
    private boolean otpEnabled;

    @Value("${otp.expiry.minutes:5}")
    private int otpExpiryMinutes;

    @Value("${otp.max.attempts:5}")
    private int otpMaxAttempts;

    @Value("${otp.sms.textbelt.url:https://textbelt.com/text}")
    private String textBeltUrl;

    @Value("${otp.sms.textbelt.key:textbelt}")
    private String textBeltKey;

    @Value("${otp.sms.fallback.enabled:true}")
    private boolean smsFallbackEnabled;

    @Value("${otp.sms.fast2sms.url:https://www.fast2sms.com/dev/bulkV2}")
    private String fast2SmsUrl;

    @Value("${otp.sms.fast2sms.key:}")
    private String fast2SmsKey;

    @Value("${otp.sms.twofactor.url:https://2factor.in/API/V1}")
    private String twoFactorUrl;

    @Value("${otp.sms.twofactor.key:}")
    private String twoFactorKey;

    @Value("${otp.sms.textlocal.url:https://api.textlocal.in/send/}")
    private String textlocalUrl;

    @Value("${otp.sms.textlocal.key:}")
    private String textlocalKey;

    @Value("${otp.sms.textlocal.sender:TXTLCL}")
    private String textlocalSender;

    @Value("${otp.sms.circuitdigest.url:https://www.circuitdigest.cloud/api/send-sms}")
    private String circuitDigestUrl;

    @Value("${otp.sms.circuitdigest.key:}")
    private String circuitDigestKey;

    public OtpSendResult sendOtp(Integer userId, String purpose, String referenceId) throws Exception {
        if (userId == null) {
            throw new Exception("User ID is required");
        }
        if (purpose == null || purpose.trim().isEmpty()) {
            throw new Exception("OTP purpose is required");
        }

        cleanupExpiredOtps();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found"));

        String registeredMobile = normalizePhone(user.getMobileNo());
        if (registeredMobile == null || registeredMobile.isBlank()) {
            registeredMobile = "";
        }

        String registeredEmail = user.getEmailId();

        String otpCode = String.valueOf(100000 + OTP_RANDOM.nextInt(900000));
        String otpRequestId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpExpiryMinutes);

        OtpSession session = new OtpSession(
                otpRequestId,
                userId,
                purpose.trim().toUpperCase(),
                referenceId,
                sha256(otpCode),
                expiresAt,
                0,
                registeredMobile
        );

        otpStore.put(otpRequestId, session);

        if (!otpEnabled) {
            throw new Exception("OTP service is disabled by configuration");
        } else {
            try {
                if (registeredMobile == null || registeredMobile.isBlank()) {
                    throw new Exception("Registered mobile number is missing");
                }
                sendOtpViaSmsProviders(registeredMobile, buildOtpMessage(purpose, otpCode));
                return new OtpSendResult(otpRequestId, maskPhone(registeredMobile), otpExpiryMinutes, "SMS");
            } catch (Exception smsException) {
                if (!smsFallbackEnabled) {
                    throw smsException;
                }

                log.warn("SMS OTP delivery failed. Reason: {}", smsException.getMessage());

                try {
                    if (registeredEmail == null || registeredEmail.isBlank()) {
                        throw new Exception("Registered email not found for this user");
                    }
                    emailService.sendOtpCodeEmail(registeredEmail, otpCode, purpose, otpExpiryMinutes);
                    return new OtpSendResult(otpRequestId, maskEmail(registeredEmail), otpExpiryMinutes, "EMAIL");
                } catch (Exception emailException) {
                    log.error("Email OTP fallback failed. Reason: {}", emailException.getMessage());
                    throw new Exception("Unable to deliver OTP right now. Please verify SMS provider key or email settings.");
                }
            }
        }
    }

    public void verifyOtp(Integer userId, String otpRequestId, String otpCode, String purpose, String referenceId) throws Exception {
        if (userId == null) {
            throw new Exception("User ID is required for OTP verification");
        }
        if (otpRequestId == null || otpRequestId.trim().isEmpty()) {
            throw new Exception("OTP request ID is required");
        }
        if (otpCode == null || otpCode.trim().isEmpty()) {
            throw new Exception("OTP code is required");
        }
        if (purpose == null || purpose.trim().isEmpty()) {
            throw new Exception("OTP purpose is required");
        }

        cleanupExpiredOtps();

        OtpSession session = otpStore.get(otpRequestId);
        if (session == null) {
            throw new Exception("OTP session not found or expired");
        }

        if (!Objects.equals(session.getUserId(), userId)) {
            throw new Exception("OTP does not belong to this user");
        }

        if (!session.getPurpose().equalsIgnoreCase(purpose.trim())) {
            throw new Exception("Invalid OTP purpose");
        }

        if (referenceId != null && !referenceId.isBlank()) {
            if (!Objects.equals(session.getReferenceId(), referenceId)) {
                throw new Exception("OTP is not valid for this operation");
            }
        }

        if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            otpStore.remove(otpRequestId);
            throw new Exception("OTP has expired");
        }

        if (session.getAttempts() >= otpMaxAttempts) {
            otpStore.remove(otpRequestId);
            throw new Exception("Too many invalid OTP attempts. Please request a new OTP");
        }

        String expectedHash = session.getOtpHash();
        String providedHash = sha256(otpCode.trim());
        if (!expectedHash.equals(providedHash)) {
            session.setAttempts(session.getAttempts() + 1);
            if (session.getAttempts() >= otpMaxAttempts) {
                otpStore.remove(otpRequestId);
                throw new Exception("Too many invalid OTP attempts. Please request a new OTP");
            }
            throw new Exception("Invalid OTP");
        }

        otpStore.remove(otpRequestId);
    }

    private void sendOtpViaTextBelt(String mobile, String message) throws Exception {
        String resolvedTextbeltKey = resolveProviderKey("TEXTBELT_API_KEY", textBeltKey);

        String formData = "phone=" + urlEncode(mobile)
                + "&message=" + urlEncode(message)
            + "&key=" + urlEncode(resolvedTextbeltKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(textBeltUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new Exception("Failed to send OTP via SMS provider");
        }

        Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {});
        Object successObj = payload.get("success");
        boolean success = successObj instanceof Boolean && (Boolean) successObj;
        if (!success) {
            String error = payload.get("error") != null ? payload.get("error").toString() : "Unable to send OTP";
            throw new Exception(error);
        }
    }

    private void sendOtpViaFast2Sms(String mobile, String message) throws Exception {
        String resolvedFast2SmsKey = resolveProviderKey("FAST2SMS_API_KEY", fast2SmsKey);

        if (resolvedFast2SmsKey == null || resolvedFast2SmsKey.isBlank()) {
            throw new Exception("FAST2SMS API key is not configured");
        }

        String indianMobile = mobile;
        if (indianMobile.startsWith("+91")) {
            indianMobile = indianMobile.substring(3);
        }

        String formData = "route=q&message=" + urlEncode(message)
                + "&language=english"
                + "&numbers=" + urlEncode(indianMobile);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fast2SmsUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("authorization", resolvedFast2SmsKey)
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new Exception("Fast2SMS delivery failed");
        }

        Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {});
        Object returnObj = payload.get("return");
        boolean success = returnObj instanceof Boolean && (Boolean) returnObj;
        if (!success) {
            String messageObj = payload.get("message") != null ? payload.get("message").toString() : "Fast2SMS failed";
            throw new Exception(messageObj);
        }
    }

    private void sendOtpViaTwoFactor(String mobile, String otpCode) throws Exception {
        String resolvedTwoFactorKey = resolveProviderKey("TWOFACTOR_API_KEY", twoFactorKey);

        if (resolvedTwoFactorKey == null || resolvedTwoFactorKey.isBlank()) {
            throw new Exception("2Factor API key is not configured");
        }

        String indianMobile = mobile;
        if (indianMobile.startsWith("+91")) {
            indianMobile = indianMobile.substring(3);
        }

        String requestUrl = twoFactorUrl + "/" + resolvedTwoFactorKey + "/SMS/" + indianMobile + "/" + otpCode;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new Exception("2Factor delivery failed");
        }

        Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {});
        String status = payload.get("Status") != null ? payload.get("Status").toString() : "";
        if (!"Success".equalsIgnoreCase(status)) {
            String details = payload.get("Details") != null ? payload.get("Details").toString() : "2Factor failed";
            throw new Exception(details);
        }
    }

    private void sendOtpViaTextlocal(String mobile, String message) throws Exception {
        String resolvedTextlocalKey = resolveProviderKey("TEXTLOCAL_API_KEY", textlocalKey);
        if (resolvedTextlocalKey == null || resolvedTextlocalKey.isBlank()) {
            throw new Exception("Textlocal API key is not configured");
        }

        String indianMobile = mobile;
        if (indianMobile.startsWith("+91")) {
            indianMobile = indianMobile.substring(3);
        }

        String formData = "apikey=" + urlEncode(resolvedTextlocalKey)
                + "&numbers=" + urlEncode(indianMobile)
                + "&message=" + urlEncode(message)
                + "&sender=" + urlEncode(textlocalSender);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(textlocalUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new Exception("Textlocal delivery failed");
        }

        Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {});
        String status = payload.get("status") != null ? payload.get("status").toString() : "";
        if (!"success".equalsIgnoreCase(status)) {
            Object errors = payload.get("errors");
            String errorMessage = errors != null ? errors.toString() : "Textlocal failed";
            throw new Exception(errorMessage);
        }
    }

    private void sendOtpViaCircuitDigest(String mobile, String message) throws Exception {
        String resolvedKey = resolveProviderKey("CIRCUITDIGEST_API_KEY", circuitDigestKey);
        if (resolvedKey == null || resolvedKey.isBlank()) {
            throw new Exception("CircuitDigest API key is not configured");
        }

        String indianMobile = mobile;
        if (indianMobile.startsWith("+91")) {
            indianMobile = indianMobile.substring(3);
        }

        String[] candidateUrls = new String[] {
                circuitDigestUrl,
            "https://www.circuitdigest.cloud/api/otp/send",
                "https://www.circuitdigest.cloud/api/sms/send",
            "https://www.circuitdigest.cloud/api/send-sms"
        };

        Exception lastError = null;
        for (String candidateUrl : candidateUrls) {
            try {
            String query = "?api_key=" + urlEncode(resolvedKey)
                + "&apikey=" + urlEncode(resolvedKey)
                + "&phone=" + urlEncode(indianMobile)
                + "&mobile=" + urlEncode(indianMobile)
                + "&number=" + urlEncode(indianMobile)
                + "&message=" + urlEncode(message);

            HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(candidateUrl + query))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + resolvedKey)
                .header("X-API-KEY", resolvedKey)
                .GET()
                .build();

            HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
            if (isCircuitDigestSuccess(getResponse.statusCode(), getResponse.body())) {
                return;
            }

            String formData = "api_key=" + urlEncode(resolvedKey)
                + "&apikey=" + urlEncode(resolvedKey)
                + "&phone=" + urlEncode(indianMobile)
                + "&mobile=" + urlEncode(indianMobile)
                + "&number=" + urlEncode(indianMobile)
                + "&message=" + urlEncode(message);

            HttpRequest formPostRequest = HttpRequest.newBuilder()
                .uri(URI.create(candidateUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + resolvedKey)
                .header("X-API-KEY", resolvedKey)
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

            HttpResponse<String> formPostResponse = httpClient.send(formPostRequest, HttpResponse.BodyHandlers.ofString());
            if (isCircuitDigestSuccess(formPostResponse.statusCode(), formPostResponse.body())) {
                return;
            }

            HttpRequest jsonPostRequest = HttpRequest.newBuilder()
                        .uri(URI.create(candidateUrl))
                        .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                        .header("Authorization", "Bearer " + resolvedKey)
                        .header("X-API-KEY", resolvedKey)
                        .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"api_key\":\"" + escapeJson(resolvedKey) + "\",\"apikey\":\"" + escapeJson(resolvedKey) + "\",\"phone\":\"" + indianMobile + "\",\"mobile\":\"" + indianMobile + "\",\"number\":\"" + indianMobile + "\",\"message\":\"" + escapeJson(message) + "\"}"
                        ))
                        .build();

            HttpResponse<String> jsonPostResponse = httpClient.send(jsonPostRequest, HttpResponse.BodyHandlers.ofString());
            if (isCircuitDigestSuccess(jsonPostResponse.statusCode(), jsonPostResponse.body())) {
                    return;
                }

            lastError = new Exception("No successful response from " + candidateUrl
                + " [GET: " + getResponse.statusCode()
                + ", FORM_POST: " + formPostResponse.statusCode()
                + ", JSON_POST: " + jsonPostResponse.statusCode() + "]");
            } catch (Exception e) {
                lastError = e;
            }
        }

        throw new Exception(lastError != null ? lastError.getMessage() : "CircuitDigest delivery failed");
    }

    private boolean isCircuitDigestSuccess(int statusCode, String responseBody) {
        if (statusCode >= 400) {
            return false;
        }

        String body = responseBody == null ? "" : responseBody.toLowerCase();
        if (body.contains("method not allowed") || body.contains("not found")) {
            return false;
        }

        return body.contains("success")
                || body.contains("sent")
                || body.contains("queued")
                || body.contains("ok")
                || body.contains("message id")
                || body.contains("accepted");
    }

    private void sendOtpViaSmsProviders(String mobile, String message) throws Exception {
        Exception circuitDigestError = null;
        String otpCodeForProvider = extractOtpFromMessage(message);
        Exception textlocalError = null;
        Exception twoFactorError = null;
        Exception fast2SmsError = null;
        Exception textbeltError = null;

        try {
            sendOtpViaCircuitDigest(mobile, message);
            return;
        } catch (Exception e) {
            circuitDigestError = e;
        }

        try {
            sendOtpViaTextlocal(mobile, message);
            return;
        } catch (Exception e) {
            textlocalError = e;
        }

        try {
            sendOtpViaTwoFactor(mobile, otpCodeForProvider);
            return;
        } catch (Exception e) {
            twoFactorError = e;
        }

        try {
            sendOtpViaFast2Sms(mobile, message);
            return;
        } catch (Exception e) {
            fast2SmsError = e;
        }

        try {
            sendOtpViaTextBelt(mobile, message);
            return;
        } catch (Exception e) {
            textbeltError = e;
        }

        String textlocalMsg = textlocalError != null ? textlocalError.getMessage() : "not attempted";
        String twoFactorMsg = twoFactorError != null ? twoFactorError.getMessage() : "not attempted";
        String fast2SmsMsg = fast2SmsError != null ? fast2SmsError.getMessage() : "not attempted";
        String textbeltMsg = textbeltError != null ? textbeltError.getMessage() : "not attempted";
        String circuitDigestMsg = circuitDigestError != null ? circuitDigestError.getMessage() : "not attempted";
        throw new Exception("All SMS providers failed. CircuitDigest: " + circuitDigestMsg + ", Textlocal: " + textlocalMsg + ", 2Factor: " + twoFactorMsg + ", Fast2SMS: " + fast2SmsMsg + ", Textbelt: " + textbeltMsg);
    }

    private String extractOtpFromMessage(String message) throws Exception {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\b(\\d{6})\\b").matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new Exception("Unable to extract OTP code from message payload");
    }

    private String resolveProviderKey(String envKey, String propertyValue) {
        if (propertyValue != null && !propertyValue.trim().isBlank()) {
            return propertyValue.trim();
        }

        String fromSystem = System.getenv(envKey);
        if (fromSystem != null && !fromSystem.trim().isBlank()) {
            return fromSystem.trim();
        }

        String fromDotenv = resolveFromDotenv(envKey);
        if (fromDotenv != null && !fromDotenv.trim().isBlank()) {
            return fromDotenv.trim();
        }

        return "";
    }

    private String resolveFromDotenv(String key) {
        List<String> searchDirectories = List.of(
                "./",
                "./Book-My-Show",
                "../Book-My-Show"
        );

        for (String directory : searchDirectories) {
            try {
                Dotenv dotenv = Dotenv.configure()
                        .directory(directory)
                        .ignoreIfMissing()
                        .load();

                String value = dotenv.get(key);
                if (value != null && !value.trim().isBlank()) {
                    return value.trim();
                }
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    private void cleanupExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();
        otpStore.entrySet().removeIf(entry -> now.isAfter(entry.getValue().getExpiresAt()));
    }

    private String buildOtpMessage(String purpose, String otpCode) {
        String normalizedPurpose = purpose.trim().toUpperCase();
        String action = switch (normalizedPurpose) {
            case "PAYMENT" -> "payment";
            case "PROFILE_UPDATE" -> "profile update";
            default -> "verification";
        };
        return "Your BookMyShow OTP for " + action + " is " + otpCode + ". It is valid for " + otpExpiryMinutes + " minutes.";
    }

    private String normalizePhone(String mobile) {
        if (mobile == null) return null;
        String digits = mobile.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return "+91" + digits;
        }
        if (digits.length() == 12 && digits.startsWith("91")) {
            return "+" + digits;
        }
        if (mobile.startsWith("+")) {
            return mobile;
        }
        return mobile;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        String visible = phone.substring(phone.length() - 4);
        return "******" + visible;
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return "****";
        }
        String[] parts = email.split("@", 2);
        String name = parts[0];
        String domain = parts[1];
        String visible = name.length() <= 2 ? name.substring(0, 1) : name.substring(0, 2);
        return visible + "****@" + domain;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String sha256(String value) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] hash = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    @Data
    @AllArgsConstructor
    private static class OtpSession {
        private String otpRequestId;
        private Integer userId;
        private String purpose;
        private String referenceId;
        private String otpHash;
        private LocalDateTime expiresAt;
        private int attempts;
        private String phone;
    }

    @Data
    @AllArgsConstructor
    public static class OtpSendResult {
        private String otpRequestId;
        private String maskedMobile;
        private int expiresInMinutes;
        private String channel;
    }
}
