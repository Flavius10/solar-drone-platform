package com.solar.backend.controller;

import com.solar.backend.model.UserAccount;
import com.solar.backend.model.dto.JwtResponseDTO;
import com.solar.backend.model.dto.LoginRequestDTO;
import com.solar.backend.model.dto.RegisterRequestDTO;
import com.solar.backend.repository.PasswordResetTokenRepository;
import com.solar.backend.repository.UserRepository;
import com.solar.backend.security.JwtUtil;
import com.solar.backend.service.AuditLogService;
import com.solar.backend.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.solar.backend.model.PasswordResetToken;
import com.solar.backend.model.dto.ForgotPasswordRequestDTO;
import com.solar.backend.model.dto.ResetPasswordConfirmDTO;
import com.solar.backend.model.dto.UpdateEmailRequestDTO;
import com.solar.backend.model.dto.UserSummaryDTO;
import com.solar.backend.repository.PasswordResetTokenRepository;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          AuditLogService auditLogService,
                          EmailService emailService,
                          PasswordResetTokenRepository passwordResetTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {
        UserAccount userAccount = userRepository.findByUsername(request.getUsername());

        if (userAccount != null && userAccount.getLockedUntil() != null
                && userAccount.getLockedUntil().isAfter(java.time.LocalDateTime.now())) {
            auditLogService.log(userAccount.getUsername(), "LOGIN_BLOCKED",
                    "Login attempt while account locked until " + userAccount.getLockedUntil());
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body("Error: Account is locked due to too many failed attempts. Try again later.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            if (userAccount != null) {
                int attempts = userAccount.getFailedLoginAttempts() + 1;
                userAccount.setFailedLoginAttempts(attempts);

                if (attempts >= MAX_FAILED_ATTEMPTS) {
                    userAccount.setLockedUntil(java.time.LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
                    userAccount.setFailedLoginAttempts(0);
                    auditLogService.log(userAccount.getUsername(), "ACCOUNT_LOCKED",
                            "Account locked after " + MAX_FAILED_ATTEMPTS + " failed login attempts.");
                }
                userRepository.save(userAccount);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Invalid username or password.");
        }

        if (userAccount.getFailedLoginAttempts() != 0 || userAccount.getLockedUntil() != null) {
            userAccount.setFailedLoginAttempts(0);
            userAccount.setLockedUntil(null);
            userRepository.save(userAccount);
        }

        String token = jwtUtil.generateToken(userAccount.getUsername(), userAccount.getRole(), userAccount.getSubscriptionTier());

        auditLogService.log(userAccount.getUsername(), "LOGIN", "User logged in with role " + userAccount.getRole());
        return ResponseEntity.ok(new JwtResponseDTO(token, userAccount.getUsername(), userAccount.getRole(), userAccount.getSubscriptionTier()));
    }

    @PostMapping("/register-admin")
    public ResponseEntity<?> registerAdmin(@RequestBody RegisterRequestDTO request) {
        boolean noUsersYet = userRepository.count() == 0;

        if (!noUsersYet && !callerIsAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Error: Only an existing ADMIN can create new accounts.");
        }

        if (userRepository.findByUsername(request.getUsername()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error: Username already exists.");
        }

        String role = (request.getRole() != null && !request.getRole().isBlank())
                ? request.getRole().toUpperCase() : "ADMIN";
        String tier = (request.getSubscriptionTier() != null && !request.getSubscriptionTier().isBlank())
                ? request.getSubscriptionTier().toUpperCase() : "BASIC";

        if (noUsersYet) {
            role = "ADMIN";
        }

        UserAccount userAccount = new UserAccount();
        userAccount.setUsername(request.getUsername());
        userAccount.setPassword(passwordEncoder.encode(request.getPassword()));
        userAccount.setRole(role);
        userAccount.setSubscriptionTier(tier);

        userRepository.save(userAccount);

        auditLogService.log(auditLogService.getCurrentUsername(), "CREATE_USER",
                "Created user '" + userAccount.getUsername() + "' with role " + role + " and tier " + tier);
        return ResponseEntity.status(HttpStatus.CREATED).body("User created successfully with role " + role + ".");
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers() {
        List<UserSummaryDTO> users = userRepository.findAll().stream()
                .map(u -> new UserSummaryDTO(u.getId(), u.getUsername(), u.getRole(), u.getSubscriptionTier(), u.getEmail()))
                .toList();
        return ResponseEntity.ok(users);
    }

    private boolean callerIsAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    @PutMapping("/update-email")
    public ResponseEntity<?> updateEmail(@RequestBody UpdateEmailRequestDTO request) {
        String currentUsername = auditLogService.getCurrentUsername();
        if ("SYSTEM".equals(currentUsername)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Must be logged in.");
        }
        UserAccount userAccount = userRepository.findByUsername(currentUsername);
        userAccount.setEmail(request.getEmail());
        userRepository.save(userAccount);
        return ResponseEntity.ok("Email updated successfully.");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequestDTO request) {
        UserAccount userAccount = userRepository.findByUsername(request.getUsername());

        if (userAccount != null && userAccount.getEmail() != null && !userAccount.getEmail().isBlank()) {
            String token = java.util.UUID.randomUUID().toString();

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setUsername(userAccount.getUsername());
            resetToken.setExpiryDate(java.time.LocalDateTime.now().plusMinutes(30));
            resetToken.setUsed(false);
            passwordResetTokenRepository.save(resetToken);

            String resetLink = "http://localhost:4200/reset-password?token=" + token;
            emailService.sendPasswordResetEmail(userAccount.getEmail(), userAccount.getUsername(), resetLink);

            auditLogService.log(userAccount.getUsername(), "PASSWORD_RESET_REQUESTED", "Password reset email sent.");
        }

        return ResponseEntity.ok("If an account with that username exists and has an email set, a reset link has been sent.");
    }

    @PostMapping("/reset-password-confirm")
    public ResponseEntity<?> resetPasswordConfirm(@RequestBody ResetPasswordConfirmDTO request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken());

        if (resetToken == null || resetToken.isUsed() || resetToken.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Invalid or expired reset link.");
        }

        UserAccount userAccount = userRepository.findByUsername(resetToken.getUsername());
        if (userAccount == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: User not found.");
        }

        userAccount.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userAccount.setFailedLoginAttempts(0);
        userAccount.setLockedUntil(null);
        userRepository.save(userAccount);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        auditLogService.log(userAccount.getUsername(), "PASSWORD_RESET_COMPLETED", "Password reset via email link.");

        return ResponseEntity.ok("Password has been reset successfully. You can now log in.");
    }
}