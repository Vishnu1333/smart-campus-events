package com.smartcampus.eventmanagement.services;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OtpService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentMap<String, OtpEntry> otpStorage = new ConcurrentHashMap<>();
    private final Duration otpTtl;
    private final int maxAttempts;

    public OtpService(
            @org.springframework.beans.factory.annotation.Value("${otp.ttl-minutes:5}") long otpTtlMinutes,
            @org.springframework.beans.factory.annotation.Value("${otp.max-attempts:5}") int maxAttempts) {
        this.otpTtl = Duration.ofMinutes(Math.max(1, otpTtlMinutes));
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public String generateOtp(String identifier) {
        // STATIC OTP for presentation mode to bypass Render email blocks
        String otp = "123456";
        otpStorage.put(identifier, new OtpEntry(otp, Instant.now().plus(otpTtl)));
        clearExpiredOtps();
        return otp;
    }

    public boolean validateOtp(String identifier, String otp) {
        if (identifier == null || otp == null) {
            return false;
        }

        OtpEntry storedOtp = otpStorage.get(identifier);
        if (storedOtp == null) {
            return false;
        }

        if (storedOtp.isExpired()) {
            otpStorage.remove(identifier, storedOtp);
            return false;
        }

        int attempts = storedOtp.attempts.incrementAndGet();
        if (storedOtp.otp.equals(otp)) {
            otpStorage.remove(identifier);
            return true;
        }

        if (attempts >= maxAttempts) {
            otpStorage.remove(identifier, storedOtp);
        }

        return false;
    }

    public void clearOtp(String identifier) {
        otpStorage.remove(identifier);
    }

    private void clearExpiredOtps() {
        otpStorage.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private static class OtpEntry {
        private final String otp;
        private final Instant expiresAt;
        private final AtomicInteger attempts = new AtomicInteger(0);

        private OtpEntry(String otp, Instant expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
