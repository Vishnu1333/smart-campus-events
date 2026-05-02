package com.smartcampus.eventmanagement.controllers;

import com.smartcampus.eventmanagement.models.User;
import com.smartcampus.eventmanagement.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(@RequestBody AdminLoginRequest request) {
        if ("admin".equals(request.getUsername()) && "admin123".equals(request.getPassword())) {
            return ResponseEntity.ok(Map.of("token", "demo-admin-token"));
        }
        return ResponseEntity.status(401).body(Map.of("message", "Invalid admin credentials"));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(@RequestHeader(value = "Authorization", required = false) String token) {
        if (!"Bearer demo-admin-token".equals(token)) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        List<User> users = userRepository.findAll();
        for (User user : users) {
            user.setPassword(null); 
        }
        return ResponseEntity.ok(users);
    }

    public static class AdminLoginRequest {
        private String username;
        private String password;
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
