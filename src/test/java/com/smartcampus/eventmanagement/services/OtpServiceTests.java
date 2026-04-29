package com.smartcampus.eventmanagement.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpServiceTests {

    @Test
    void generatedOtpValidatesOnce() {
        OtpService otpService = new OtpService(5, 5);
        String phone = "+919876543210";

        String otp = otpService.generateOtp(phone);

        assertTrue(otp.matches("\\d{6}"));
        assertTrue(otpService.validateOtp(phone, otp));
        assertFalse(otpService.validateOtp(phone, otp));
    }

    @Test
    void otpIsClearedAfterTooManyFailedAttempts() {
        OtpService otpService = new OtpService(5, 2);
        String phone = "+919876543210";
        String otp = otpService.generateOtp(phone);
        String wrongOtp = otp.equals("000000") ? "111111" : "000000";

        assertFalse(otpService.validateOtp(phone, wrongOtp));
        assertFalse(otpService.validateOtp(phone, wrongOtp));
        assertFalse(otpService.validateOtp(phone, otp));
    }
}
