package com.smartcampus.eventmanagement.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpServiceTests {

    @Test
    void generatedOtpValidatesOnce() {
        OtpService otpService = new OtpService(5, 5);
        String email = "test@veltech.edu.in";

        String otp = otpService.generateOtp(email);

        assertTrue(otp.matches("\\d{6}"));
        assertTrue(otpService.validateOtp(email, otp));
        assertFalse(otpService.validateOtp(email, otp));
    }

    @Test
    void otpIsClearedAfterTooManyFailedAttempts() {
        OtpService otpService = new OtpService(5, 2);
        String email = "test@veltech.edu.in";
        String otp = otpService.generateOtp(email);
        String wrongOtp = otp.equals("000000") ? "111111" : "000000";

        assertFalse(otpService.validateOtp(email, wrongOtp));
        assertFalse(otpService.validateOtp(email, wrongOtp));
        assertFalse(otpService.validateOtp(email, otp));
    }
}
