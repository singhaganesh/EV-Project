package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.config.JwtUtil;
import com.ganesh.EV_Project.dto.TokenRefreshRequest;
import com.ganesh.EV_Project.enums.UserStatus;
import com.ganesh.EV_Project.model.BusinessProfile;
import com.ganesh.EV_Project.model.RefreshToken;
import com.ganesh.EV_Project.model.User;
import com.ganesh.EV_Project.payload.APIResponse;
import com.ganesh.EV_Project.repository.BusinessProfileRepository;
import com.ganesh.EV_Project.service.DocumentStorageService;
import com.ganesh.EV_Project.service.EmailService;
import com.ganesh.EV_Project.service.MfaOtpService;
import com.ganesh.EV_Project.service.OtpService;
import com.ganesh.EV_Project.service.RefreshTokenService;
import com.ganesh.EV_Project.service.UserService;
import com.ganesh.EV_Project.service.LoginAttemptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserService userService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MfaOtpService mfaOtpService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private DocumentStorageService documentStorageService;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @Autowired
    private Environment environment;

    // Dev-only: when true, the generated OTP is returned in the response for
    // local testing. Defaults to false so production never leaks the OTP.
    @Value("${otp.expose-in-response:false}")
    private boolean exposeOtpInResponse;

    private boolean isDevProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    /**
     * Sends an OTP email. In dev the OTP is also returned in the response, so a mail
     * failure is logged but not fatal; in prod email is the only channel, so it throws.
     */
    private void deliverOtp(String email, String otp, String purpose) {
        try {
            emailService.sendOtpEmail(email, otp, purpose);
        } catch (Exception e) {
            if (!exposeOtpInResponse) {
                throw e;
            }
            log.warn("OTP email to {} failed ({}); continuing because OTP is exposed in dev response", email, e.getMessage());
        }
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestParam String mobileNumber) {
        if (loginAttemptService.isBlocked(mobileNumber)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(APIResponse.builder()
                            .success(false)
                            .message("Too many attempts. You are blocked for 15 minutes.")
                            .build());
        }

        String otp = otpService.generateOtp(mobileNumber);
        // Never return the OTP in production responses; only expose under the
        // dev flag for local testing. Real delivery must go via SMS.
        Object data = exposeOtpInResponse ? Map.of("otp", otp) : null;
        return ResponseEntity.ok(APIResponse.builder()
                .success(true)
                .message("OTP sent successfully")
                .data(data)
                .build());
    }

    @PostMapping("/validate-otp")
    public ResponseEntity<?> validateOtp(@RequestParam String mobileNumber,
                                         @RequestParam String otp) {
        if (loginAttemptService.isBlocked(mobileNumber)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(APIResponse.builder()
                            .success(false)
                            .message("Too many attempts. You are blocked for 15 minutes.")
                            .build());
        }

        boolean isValid = otpService.validateOtp(mobileNumber, otp);
        if (!isValid) {
            int remaining = loginAttemptService.getRemainingAttempts(mobileNumber);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.builder()
                    .success(false)
                    .message("Invalid or expired OTP. " + remaining + " attempts remaining.")
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
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(existingUser.getId());

            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .message("Login successful")
                    .data(Map.of(
                            "isNewUser", false,
                            "token", token,
                            "refreshToken", refreshToken.getToken(),
                            "user", existingUser))
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
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.builder()
                        .success(true)
                        .message("Profile completed successfully")
                        .data(Map.of(
                                "token", token,
                                "refreshToken", refreshToken.getToken(),
                                "user", savedUser))
                        .build());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String key = request.getEmail();
        if (loginAttemptService.isBlocked(key)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(APIResponse.builder()
                            .success(false)
                            .message("Too many attempts. You are blocked for 15 minutes.")
                            .build());
        }

        User user = userService.findByEmail(request.getEmail());

        if (user == null) {
            loginAttemptService.loginFailed(key);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.builder()
                    .success(false)
                    .message("Invalid email or password")
                    .build());
        }

        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!passwordMatches) {
            loginAttemptService.loginFailed(key);
            int remaining = loginAttemptService.getRemainingAttempts(key);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.builder()
                    .success(false)
                    .message("Invalid email or password. " + remaining + " attempts remaining.")
                    .build());
        }

        loginAttemptService.loginSucceeded(key);

        // Owners must have completed verification + approval before they can log in.
        if (user.getRole() == User.Role.STATION_OWNER
                && user.getStatus() != UserStatus.APPROVED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(APIResponse.builder()
                    .success(false)
                    .message(statusMessage(user.getStatus()))
                    .build());
        }

        // Owners with MFA enabled: send an email OTP and return a temp-login token
        // instead of the JWT. Admins/customers (and MFA-off owners) log in directly.
        if (user.getRole() == User.Role.STATION_OWNER && Boolean.TRUE.equals(user.getMfaEnabled())) {
            MfaOtpService.MfaChallenge challenge = mfaOtpService.createMfaChallenge(user.getId());
            deliverOtp(user.getEmail(), challenge.otp(), "login");
            Map<String, Object> data = new HashMap<>();
            data.put("mfaRequired", true);
            data.put("tempLoginToken", challenge.tempToken());
            if (exposeOtpInResponse) data.put("otp", challenge.otp());
            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .message("MFA code sent to email.")
                    .data(data)
                    .build());
        }

        return ResponseEntity.ok(APIResponse.builder()
                .success(true)
                .message("Login successful")
                .data(issueTokens(user))
                .build());
    }

    /** Human-readable message for a non-approved owner status. */
    private String statusMessage(UserStatus status) {
        return switch (status) {
            case PENDING_EMAIL_VERIFICATION -> "Email not verified. Please verify your email first.";
            case PENDING_ADMIN_APPROVAL -> "Account pending admin approval.";
            case SUSPENDED -> "Account suspended. Contact support.";
            default -> "Account not active.";
        };
    }

    /** Issues a JWT + refresh token for the user and returns the standard login payload. */
    private Map<String, Object> issueTokens(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        String principal = user.getEmail() != null ? user.getEmail() : user.getMobileNumber();
        String token = jwtUtil.generateToken(principal, claims);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("refreshToken", refreshToken.getToken());
        data.put("user", user);
        return data;
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

        if (request.getRole() != null && request.getRole().equals("PUMP_OWNER")) {
            user.setRole(User.Role.STATION_OWNER);
        } else {
            user.setRole(User.Role.CUSTOMER);
        }

        User savedUser = userService.saveUser(user);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", savedUser.getId());
        claims.put("role", savedUser.getRole().name());
        String token = jwtUtil.generateToken(savedUser.getEmail(), claims);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.builder()
                        .success(true)
                        .message("Registration successful")
                        .data(Map.of(
                                "token", token,
                                "refreshToken", refreshToken.getToken(),
                                "user", savedUser))
                        .build());
    }

    @PostMapping(value = "/register/owner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerOwner(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String companyName,
            @RequestParam String taxId,
            @RequestParam String phoneNumber,
            @RequestParam String bankAccountNumber,
            @RequestParam String bankIfscCode,
            @RequestParam("registrationDoc") MultipartFile registrationDoc,
            @RequestParam("electricityDoc") MultipartFile electricityDoc,
            @RequestParam("bankDoc") MultipartFile bankDoc) {

        if (userService.findByEmail(email) != null) {
            return ResponseEntity.badRequest().body(APIResponse.builder()
                    .success(false)
                    .message("Email already registered")
                    .build());
        }

        // 1. Create the owner account in the PENDING_EMAIL_VERIFICATION state, MFA on.
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(User.Role.STATION_OWNER);
        user.setStatus(UserStatus.PENDING_EMAIL_VERIFICATION);
        user.setMfaEnabled(true);
        User savedUser = userService.saveUser(user);

        // 2. Upload documents to Supabase Storage and persist the business profile.
        BusinessProfile profile = new BusinessProfile();
        profile.setUser(savedUser);
        profile.setCompanyName(companyName);
        profile.setTaxId(taxId);
        profile.setPhoneNumber(phoneNumber);
        profile.setBankAccountNumber(bankAccountNumber);
        profile.setBankIfscCode(bankIfscCode);
        profile.setRegistrationDocPath(
                documentStorageService.upload(savedUser.getId(), "registration_doc", registrationDoc));
        profile.setElectricityDocPath(
                documentStorageService.upload(savedUser.getId(), "electricity_doc", electricityDoc));
        profile.setBankDocPath(
                documentStorageService.upload(savedUser.getId(), "bank_doc", bankDoc));
        businessProfileRepository.save(profile);

        // 3. Generate + email the verification OTP.
        String otp = mfaOtpService.generateRegistrationOtp(savedUser.getId());
        deliverOtp(email, otp, "verification");

        Map<String, Object> data = new HashMap<>();
        data.put("userId", savedUser.getId());
        if (exposeOtpInResponse) data.put("otp", otp);

        return ResponseEntity.ok(APIResponse.builder()
                .success(true)
                .message("Registration initiated. Verification OTP sent to email.")
                .data(data)
                .build());
    }

    @PostMapping("/verify-registration")
    public ResponseEntity<?> verifyRegistration(@RequestBody Map<String, String> body) {
        String userIdStr = body.get("userId");
        String otp = body.get("otp");
        if (userIdStr == null || otp == null) {
            return ResponseEntity.badRequest().body(APIResponse.builder()
                    .success(false)
                    .message("userId and otp are required")
                    .build());
        }

        User user = userService.findById(Long.parseLong(userIdStr));
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.builder()
                    .success(false)
                    .message("User not found")
                    .build());
        }

        if (!mfaOtpService.validateRegistrationOtp(user.getId(), otp)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.builder()
                    .success(false)
                    .message("Invalid or expired OTP")
                    .build());
        }

        // Dev auto-approves so local testing skips the admin step; prod waits for approval.
        user.setStatus(isDevProfile() ? UserStatus.APPROVED : UserStatus.PENDING_ADMIN_APPROVAL);
        userService.updateUser(user);

        return ResponseEntity.ok(APIResponse.builder()
                .success(true)
                .message("Email verified successfully.")
                .data(Map.of("status", user.getStatus().name()))
                .build());
    }

    @PostMapping("/verify-mfa")
    public ResponseEntity<?> verifyMfa(@RequestBody Map<String, String> body) {
        String tempLoginToken = body.get("tempLoginToken");
        String otp = body.get("otp");
        if (tempLoginToken == null || otp == null) {
            return ResponseEntity.badRequest().body(APIResponse.builder()
                    .success(false)
                    .message("tempLoginToken and otp are required")
                    .build());
        }

        Long userId = mfaOtpService.validateMfaOtp(tempLoginToken, otp);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.builder()
                    .success(false)
                    .message("Invalid or expired MFA code")
                    .build());
        }

        User user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.builder()
                    .success(false)
                    .message("User not found")
                    .build());
        }

        return ResponseEntity.ok(APIResponse.builder()
                .success(true)
                .message("MFA verified. Login successful.")
                .data(issueTokens(user))
                .build());
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .<ResponseEntity<?>>map(user -> {
                    Map<String, Object> claims = new HashMap<>();
                    claims.put("userId", user.getId());
                    claims.put("role", user.getRole().name());
                    String principal = user.getMobileNumber() != null ? user.getMobileNumber() : user.getEmail();
                    String token = jwtUtil.generateToken(principal, claims);
                    return ResponseEntity.ok(APIResponse.builder()
                            .success(true)
                            .message("Token refreshed successfully")
                            .data(Map.of(
                                    "token", token,
                                    "refreshToken", requestRefreshToken))
                            .build());
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(APIResponse.builder()
                                .success(false)
                                .message("Invalid or expired refresh token")
                                .build()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(Authentication authentication) {
        String principal = authentication.getName();
        User user = userService.findByPhoneNumber(principal);
        if (user == null) user = userService.findByEmail(principal);

        if (user != null) {
            refreshTokenService.deleteByUserId(user.getId());
        }

        return ResponseEntity.ok(APIResponse.builder()
                .success(true)
                .message("Log out successful")
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            String principal = authentication.getName();
            User user = userService.findByPhoneNumber(principal);
            if (user == null) user = userService.findByEmail(principal);

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
                            .message("Unauthorized")
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
