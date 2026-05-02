package com.smartcampus.eventmanagement.services;

import org.springframework.stereotype.Service;

/**
 * Service for sending SMS messages.
 * Note: Phone authentication has been replaced by Email OTP for professional branding.
 * This service currently acts as a mock/placeholder.
 */
@Service
public class SmsService {

    public boolean sendSms(String toPhoneNumber, String text) {
        System.out.println("SmsService (MOCK): SMS authentication is disabled in favor of Email OTP.");
        System.out.println("Target: " + toPhoneNumber);
        System.out.println("Message: " + text);
        return true;
    }
}
