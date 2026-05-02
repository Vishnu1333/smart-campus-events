package com.smartcampus.eventmanagement.controllers;

import com.smartcampus.eventmanagement.models.User;
import com.smartcampus.eventmanagement.repositories.UserRepository;
import com.smartcampus.eventmanagement.services.EmailService;
import com.smartcampus.eventmanagement.services.OtpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final EmailService emailService;

    @Value("${otp.ttl-minutes:5}")
    private long otpTtlMinutes;

    private final ConcurrentMap<String, PendingRegistration> pendingRegistrations = new ConcurrentHashMap<>();

    public UserController(UserRepository userRepository, OtpService otpService, EmailService emailService) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<?> initiateAuth(@RequestBody AuthRequestDto request) {
        if (request == null || isBlank(request.getMode())) {
            return badRequest("Authentication mode (login or register) is required.");
        }

        String mode = request.getMode().trim().toLowerCase();
        String identifier = trimToNull(request.getEmail());
        
        if (isBlank(identifier)) {
            return badRequest("Please provide a valid email address.");
        }

        Optional<User> existingUser = userRepository.findByEmail(identifier);

        if ("register".equals(mode)) {
            if (existingUser.isPresent()) {
                return badRequest("An account already exists with this email.");
            }
            if (isBlank(request.getName())) {
                return badRequest("Full Name is required for registration.");
            }
            
            User tempUser = new User();
            tempUser.setName(request.getName().trim());
            tempUser.setEmail(identifier);
            tempUser.setPassword("password_managed_by_otp");

            pendingRegistrations.put(
                    identifier,
                    new PendingRegistration(tempUser, Instant.now().plus(Math.max(1, otpTtlMinutes), ChronoUnit.MINUTES))
            );
        } else if ("login".equals(mode)) {
            if (existingUser.isEmpty()) {
                return badRequest("Account not found. Please register first.");
            }
        } else {
            return badRequest("Invalid mode.");
        }

        String otp = otpService.generateOtp(identifier);
        String action = "register".equals(mode) ? "Registration" : "Login";
        String messageText =
                "Dear Student,\n\n" +
                "Your One-Time Password (OTP) for " + action + " on the Vel Tech Campus Events Portal is:\n\n" +
                "  " + otp + "\n\n" +
                "This OTP is valid for " + Math.max(1, otpTtlMinutes) + " minute(s). Do not share it with anyone.\n\n" +
                "If you did not request this OTP, please ignore this email.\n\n" +
                "Regards,\n" +
                "Vel Tech Campus Events Team\n" +
                "Vel Tech Rangarajan Dr. Sagunthala R&D Institute of Science and Technology";

        boolean sentSuccessfully = emailService.sendEmail(identifier, "Vel Tech Campus Events — Your OTP", messageText);

        if (!sentSuccessfully) {
            otpService.clearOtp(identifier);
            pendingRegistrations.remove(identifier);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "SMTP Error: Gmail rejected the connection. Please check your App Password and ensure 2FA is enabled."));
        }

        return ResponseEntity.ok(Map.of("message", "OTP sent successfully to " + identifier));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyAuth(@RequestBody AuthVerificationDto request) {
        if (request == null || isBlank(request.getMode()) || isBlank(request.getOtp())) {
            return badRequest("Mode and OTP are required.");
        }

        String mode = request.getMode().trim().toLowerCase();
        String identifier = trimToNull(request.getEmail());
        String otp = request.getOtp().trim();

        if (isBlank(identifier)) {
            return badRequest("Email address is required.");
        }

        boolean isValid = otpService.validateOtp(identifier, otp);
        if (!isValid) {
            return badRequest("Invalid or expired OTP. Please try again.");
        }

        Optional<User> existingUser = userRepository.findByEmail(identifier);

        if ("register".equals(mode)) {
            PendingRegistration pendingRegistration = pendingRegistrations.get(identifier);
            if (pendingRegistration == null || pendingRegistration.isExpired()) {
                pendingRegistrations.remove(identifier);
                return badRequest("Registration session expired. Please request a new OTP.");
            }

            if (existingUser.isPresent()) {
                pendingRegistrations.remove(identifier);
                return badRequest("Account already exists.");
            }

            User userToSave = pendingRegistration.user();
            userRepository.save(userToSave);
            pendingRegistrations.remove(identifier);

            return ResponseEntity.ok(userResponse(userToSave, "registered", "Registration successful!"));
        } else if ("login".equals(mode)) {
            if (existingUser.isEmpty()) {
                return badRequest("Account not found.");
            }
            return ResponseEntity.ok(userResponse(existingUser.get(), "logged_in", "Login successful."));
        }

        return badRequest("Invalid mode.");
    }

    private Map<String, Object> userResponse(User user, String status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("status", status);
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        return response;
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }

    private String trimToNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record PendingRegistration(User user, Instant expiresAt) {
        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    public static class AuthRequestDto {
        private String mode;
        private String name;
        private String email;

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class AuthVerificationDto {
        private String mode;
        private String email;
        private String otp;

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
    }
}
