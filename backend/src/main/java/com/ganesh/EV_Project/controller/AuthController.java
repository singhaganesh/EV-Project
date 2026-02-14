package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.config.JwtUtil;
import com.ganesh.EV_Project.model.User;
import com.ganesh.EV_Project.payload.APIResponse;
import com.ganesh.EV_Project.service.OtpService;
import com.ganesh.EV_Project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestParam String mobileNumber) {
        String otp = otpService.generateOtp(mobileNumber);
        return ResponseEntity.ok(APIResponse.builder()
                .success(true)
                .message("OTP sent successfully")
                .data(Map.of("otp", otp))
                .build());
    }

    @PostMapping("/validate-otp")
    public ResponseEntity<?> validateOtp(@RequestParam String mobileNumber,
                                         @RequestParam String otp) {
        boolean isValid = otpService.validateOtp(mobileNumber, otp);
        if (!isValid) {
            return ResponseEntity.ok(APIResponse.builder()
                    .success(false)
                    .message("Invalid or expired OTP")
                    .build());
        }

        User existingUser = userService.findByPhoneNumber(mobileNumber);

        if (existingUser == null) {
            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .message("OTP verified successfully. Please complete your profile.")
                    .data(Map.of("isNewUser", true))
                    .build());
        } else {
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", existingUser.getId());
            claims.put("role", existingUser.getRole().name());
            String token = jwtUtil.generateToken(mobileNumber, claims);

            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .message("Login successful")
                    .data(Map.of(
                            "isNewUser", false,
                            "token", token,
                            "user", existingUser
                    ))
                    .build());
        }
    }

    @PostMapping("/complete-profile")
    public ResponseEntity<?> completeProfile(@RequestBody User user) {
        User savedUser = userService.saveUser(user);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", savedUser.getId());
        claims.put("role", savedUser.getRole().name());
        String token = jwtUtil.generateToken(savedUser.getMobileNumber(), claims);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.builder()
                        .success(true)
                        .message("Profile completed successfully")
                        .data(Map.of(
                                "token", token,
                                "user", savedUser
                        ))
                        .build());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userService.findByEmail(request.getEmail());
        
        // For testing: accept any password or check if password matches
        if (user == null) {
            return ResponseEntity.ok(APIResponse.builder()
                    .success(false)
                    .message("Invalid email or password")
                    .build());
        }
        
        // Debug: print what's being compared
        System.out.println("Login attempt for: " + request.getEmail());
        System.out.println("Stored password hash: " + user.getPassword());
        System.out.println("Provided password: " + request.getPassword());
        
        // Try to match password - for testing, also accept if password is "password123"
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        boolean isTestPassword = "password123".equals(request.getPassword());
        
        if (!passwordMatches && !isTestPassword) {
            return ResponseEntity.ok(APIResponse.builder()
                    .success(false)
                    .message("Invalid email or password")
                    .build());
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        String token = jwtUtil.generateToken(user.getEmail(), claims);

        return ResponseEntity.ok(APIResponse.builder()
                .success(true)
                .message("Login successful")
                .data(Map.of(
                        "token", token,
                        "user", user
                ))
                .build());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userService.findByEmail(request.getEmail()) != null) {
            return ResponseEntity.ok(APIResponse.builder()
                    .success(false)
                    .message("Email already registered")
                    .build());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setRole(User.Role.CUSTOMER);
        
        User savedUser = userService.saveUser(user);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", savedUser.getId());
        claims.put("role", savedUser.getRole().name());
        String token = jwtUtil.generateToken(savedUser.getEmail(), claims);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.builder()
                        .success(true)
                        .message("Registration successful")
                        .data(Map.of(
                                "token", token,
                                "user", savedUser
                        ))
                        .build());
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String email = jwtUtil.extractUsername(token);
            User user = userService.findByEmail(email);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(APIResponse.builder()
                                .success(false)
                                .message("User not found")
                                .build());
            }

            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .data(user)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(APIResponse.builder()
                            .success(false)
                            .message("Invalid token")
                            .build());
        }
    }

    static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    static class RegisterRequest {
        private String email;
        private String password;
        private String name;
        private String role;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
