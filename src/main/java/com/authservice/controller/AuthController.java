package com.authservice.controller;

import com.authservice.dto.LoginRequest;
import com.authservice.dto.LoginResponse;
import com.authservice.dto.ForgotPasswordRequest;
import com.authservice.dto.RefreshRequest;
import com.authservice.dto.RegisterRequest;
import com.authservice.dto.ResetPasswordRequest;
import com.authservice.dto.UserResponse;
import com.authservice.dto.VerifyEmailRequest;
import com.authservice.entity.RefreshToken;
import com.authservice.entity.User;
import com.authservice.security.JwtProvider;
import com.authservice.service.RefreshTokenService;
import com.authservice.service.AccountService;
import com.authservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final AccountService accountService;

    /**
     * Register a new user
     * @param request Registration request with name, email, and password
     * @return UserResponse with registered user data
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Received registration request for email: {}", request.getEmail());
        UserResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        accountService.verifyEmail(request.getToken());
        return ResponseEntity.ok("Email verified successfully");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        accountService.requestPasswordReset(request);
        return ResponseEntity.ok("If the email exists, password reset instructions will be sent");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        accountService.resetPassword(request);
        return ResponseEntity.ok("Password reset successfully");
    }

    /**
     * Login user with email and password
     * @param request Login request with email and password
     * @param servletRequest HTTP request for IP and User-Agent
     * @return LoginResponse with access token and refresh token
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        log.info("Received login request for email: {}", request.getEmail());
        
        User user = userService.login(request, servletRequest);
        
        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshTokenPlain = jwtProvider.generateRefreshToken(user);
        
        // Extract IP and User-Agent
        String ipAddress = getClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");
        
        // Store refresh token in database
        refreshTokenService.createRefreshToken(user, refreshTokenPlain, ipAddress, userAgent);
        
        long expiresIn = jwtProvider.getAccessTokenExpirationMs();

        LoginResponse response = LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshTokenPlain)
            .tokenType("Bearer")
            .expiresIn(expiresIn)
            .build();

        log.info("User logged in successfully: {}", user.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token using refresh token
     * @param request Refresh request with refresh token
     * @return LoginResponse with new access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest servletRequest) {
        log.debug("Received refresh token request");
        
        // Validate refresh token
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();
        
        // Revoke old refresh token
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());
        
        // Generate new tokens
        String newAccessToken = jwtProvider.generateAccessToken(user);
        String newRefreshTokenPlain = jwtProvider.generateRefreshToken(user);
        
        // Store new refresh token in database
        String ipAddress = getClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");
        refreshTokenService.createRefreshToken(user, newRefreshTokenPlain, ipAddress, userAgent);
        
        long expiresIn = jwtProvider.getAccessTokenExpirationMs();

        LoginResponse response = LoginResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshTokenPlain)
            .tokenType("Bearer")
            .expiresIn(expiresIn)
            .build();

        log.info("Refresh token rotated for user: {}", user.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Logout user by revoking refresh token
     * @param authentication Current user authentication
     * @param request HTTP request to extract refresh token
     * @return 200 OK
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication, HttpServletRequest request) {
        if (authentication != null && authentication.isAuthenticated()) {
            UUID userId = UUID.fromString(authentication.getPrincipal().toString());
            User user = userService.findUserById(userId);
            
            // Revoke all refresh tokens for this user
            refreshTokenService.revokeAllUserTokens(user);
            
            log.info("User logged out: {}", userId);
        }
        
        return ResponseEntity.ok().build();
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}

