package com.smartcampus.eventmanagement.controllers;

import com.smartcampus.eventmanagement.models.User;
import com.smartcampus.eventmanagement.repositories.UserRepository;
import com.smartcampus.eventmanagement.services.EmailService;
import com.smartcampus.eventmanagement.services.OtpService;
import com.smartcampus.eventmanagement.services.SmsService;
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
    private final SmsService smsService;
    private final EmailService emailService;

    @Value("${admin.phone.number:}")
    private String adminPhoneNumber;

    @Value("${otp.ttl-minutes:5}")
    private long otpTtlMinutes;

    private final ConcurrentMap<String, PendingRegistration> pendingRegistrations = new ConcurrentHashMap<>();

    public UserController(UserRepository userRepository, OtpService otpService, SmsService smsService, EmailService emailService) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.smsService = smsService;
        this.emailService = emailService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<?> initiateAuth(@RequestBody AuthRequestDto request) {
        if (request == null || isBlank(request.getMode())) {
            return badRequest("Authentication mode (login or register) is required.");
        }

        String mode = request.getMode().trim().toLowerCase();
        boolean isEmail = !isBlank(request.getEmail());
        boolean isPhone = !isBlank(request.getPhone());
        
        if (!isEmail && !isPhone) {
            return badRequest("Please provide either an email or a phone number.");
        }

        String identifier = isEmail ? request.getEmail().trim() : normalizeIndianPhone(request.getPhone());
        if (identifier == null) {
            return badRequest("Invalid phone number format.");
        }

        Optional<User> existingUser = isEmail ? userRepository.findByEmail(identifier) : userRepository.findByPhone(identifier);

        if ("register".equals(mode)) {
            if (existingUser.isPresent()) {
                return badRequest((isEmail ? "Email" : "Phone number") + " already registered. Please login.");
            }
            String name = trimToNull(request.getName());
            if (name == null) {
                return badRequest("Name is required for registration.");
            }

            User tempUser = new User();
            tempUser.setName(name);
            if (isEmail) tempUser.setEmail(identifier);
            else tempUser.setPhone(identifier);
            tempUser.setPassword("OTP_AUTH"); // No password used anymore
            
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

        boolean sentSuccessfully;
        if (isEmail) {
            sentSuccessfully = emailService.sendEmail(identifier, "Vel Tech Campus Events — Your OTP", messageText);
        } else {
            sentSuccessfully = smsService.sendSms(identifier, messageText);
        }

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
            return badRequest("Verification details and OTP are required.");
        }

        String mode = request.getMode().trim().toLowerCase();
        boolean isEmail = !isBlank(request.getEmail());
        String identifier = isEmail ? request.getEmail().trim() : normalizeIndianPhone(request.getPhone());
        String otp = request.getOtp().trim();

        if (identifier == null) {
            return badRequest("Invalid phone number or email.");
        }

        if (!otpService.validateOtp(identifier, otp)) {
            return badRequest("Invalid or expired OTP.");
        }

        Optional<User> existingUser = isEmail ? userRepository.findByEmail(identifier) : userRepository.findByPhone(identifier);

        if ("register".equals(mode)) {
            PendingRegistration pendingRegistration = pendingRegistrations.get(identifier);
            if (pendingRegistration == null || pendingRegistration.isExpired()) {
                pendingRegistrations.remove(identifier);
                return badRequest("Registration expired or not found. Please request a new OTP.");
            }
            if (existingUser.isPresent()) {
                pendingRegistrations.remove(identifier);
                return badRequest("Account already exists.");
            }

            User userToSave = pendingRegistration.user();
            userRepository.save(userToSave);
            pendingRegistrations.remove(identifier);

            String normalizedAdminPhone = normalizeIndianPhone(adminPhoneNumber);
            if (normalizedAdminPhone != null) {
                smsService.sendSms(normalizedAdminPhone, "New student registered: " + userToSave.getName() + " (" + identifier + ")");
            }

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
        response.put("phone", user.getPhone());
        response.put("email", user.getEmail());
        return response;
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }

    private String normalizeIndianPhone(String phone) {
        if (isBlank(phone)) {
            return null;
        }

        String digitsOnly = phone.replaceAll("\\D", "");
        if (digitsOnly.length() == 10) {
            return "+91" + digitsOnly;
        }

        if (digitsOnly.length() == 12 && digitsOnly.startsWith("91")) {
            return "+" + digitsOnly;
        }

        return null;
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
        private String phone;
        private String email;

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class AuthVerificationDto {
        private String mode;
        private String phone;
        private String email;
        private String otp;

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
    }
}
