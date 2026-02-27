package com.interviewprep.interviewservice.controller;

import com.interviewprep.interviewservice.entity.AppUser;
import com.interviewprep.interviewservice.repository.AppUserRepository;
import com.interviewprep.interviewservice.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Auth controller – Day 8 (updated Day 15 with register + role).
 * Public endpoints (no JWT required).
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthController(JwtUtil jwtUtil, AppUserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.jwtUtil = jwtUtil;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * POST /api/auth/register
     * Body: { "email": "user@example.com", "password": "user_password" }
     * First user to register gets ADMIN role. Others get USER.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        if (email == null || email.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "'email' required"));
        if (password == null || password.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "'password' required"));

        // Strong password validation
        if (!isValidPassword(password)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters with uppercase, lowercase, number, and special character"));
        }

        if (userRepo.existsByEmail(email))
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));

        String role = userRepo.count() == 0 ? "ADMIN" : "USER";
        AppUser user = AppUser.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password)) // Hash the password
                .role(role)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        userRepo.save(user);

        String token = jwtUtil.generateToken(email + ":" + role);
        return ResponseEntity.ok(Map.of(
                "token", token, "email", email,
                "role", role, "message", "Registered successfully"));
    }

    /**
     * POST /api/auth/login
     * Body: { "email": "user@example.com", "password": "user_password" }
     * Returns JWT. Role is embedded in the token.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        if (email == null || email.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "'email' required"));
        if (password == null || password.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "'password' required"));

        // Look up user from DB
        AppUser user = userRepo.findByEmail(email).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }

        String token = jwtUtil.generateToken(email);
        return ResponseEntity.ok(Map.of(
                "token", token, "email", email,
                "role", user.getRole(),
                "message", "Login successful. Use as: Authorization: Bearer <token>"));
    }

    /** GET /api/auth/validate */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "").trim();
            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.getEmailFromToken(token);
                String role = userRepo.findByEmail(email)
                        .map(AppUser::getRole).orElse("USER");
                return ResponseEntity.ok(Map.of("valid", true, "email", email, "role", role));
            }
            return ResponseEntity.ok(Map.of("valid", false));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("valid", false, "error", e.getMessage()));
        }
    }

    // Helper method to validate strong password
    private boolean isValidPassword(String password) {
        // At least 8 characters, 1 uppercase, 1 lowercase, 1 digit, 1 special character
        return password != null &&
               password.length() >= 8 &&
               password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$");
    }
}