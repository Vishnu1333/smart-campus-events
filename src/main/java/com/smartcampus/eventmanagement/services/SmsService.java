package com.smartcampus.eventmanagement.services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromPhoneNumber;

    private boolean isMockMode = false;

    @PostConstruct
    public void init() {
        if (isBlank(accountSid) || accountSid.contains("mock_account_sid")
                || isBlank(authToken) || authToken.contains("mock_auth_token")
                || isBlank(fromPhoneNumber)) {
            System.out.println("SmsService: Running in MOCK mode. SMS will only be printed to console.");
            isMockMode = true;
        } else {
            Twilio.init(accountSid, authToken);
        }
    }

    public boolean sendSms(String toPhoneNumber, String text) {
        if (isBlank(toPhoneNumber) || isBlank(text)) {
            System.err.println("SMS not sent: phone number and message are required.");
            return false;
        }

        if (isMockMode) {
            System.out.println("==================================================");
            System.out.println("MOCK SMS SENT TO: " + toPhoneNumber);
            System.out.println("MESSAGE: " + text);
            System.out.println("==================================================");
            return true;
        }

        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(fromPhoneNumber),
                    text
            ).create();
            System.out.println("SMS sent successfully, SID: " + message.getSid());
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send SMS: " + e.getMessage());
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
