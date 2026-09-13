package com.authservice.service;

import com.authservice.dto.ForgotPasswordRequest;
import com.authservice.dto.ResetPasswordRequest;
import com.authservice.entity.EmailVerificationToken;
import com.authservice.entity.PasswordResetToken;
import com.authservice.entity.User;
import com.authservice.exception.InvalidPasswordException;
import com.authservice.exception.InvalidTokenException;
import com.authservice.repository.EmailVerificationTokenRepository;
import com.authservice.repository.PasswordResetTokenRepository;
import com.authservice.repository.UserRepository;
import com.authservice.security.TokenHasher;
import com.authservice.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {
    private static final int TOKEN_BYTES = 32;

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final TokenHasher tokenHasher;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${account.email-verification-expiration:86400000}")
    private long emailVerificationExpirationMs;

    @Value("${account.password-reset-expiration:3600000}")
    private long passwordResetExpirationMs;

    @Transactional
    public String createEmailVerificationToken(User user) {
        String plainToken = generateToken();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
            .user(user)
            .tokenHash(tokenHasher.hash(plainToken))
            .expiresAt(Instant.now().plusMillis(emailVerificationExpirationMs))
            .build();

        emailVerificationTokenRepository.save(verificationToken);
        log.debug("Created email verification token for user: {}", user.getId());
        return plainToken;
    }

    @Transactional
    public void verifyEmail(String plainToken) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository
            .findByTokenHash(tokenHasher.hash(plainToken))
            .orElseThrow(() -> new InvalidTokenException("Verification token is invalid"));

        if (!verificationToken.isValid()) {
            throw new InvalidTokenException("Verification token is expired or already used");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        if (user.isPendingVerification()) {
            user.setStatus(User.UserStatus.ACTIVE);
        }
        verificationToken.markAsUsed();
        userRepository.save(user);
        emailVerificationTokenRepository.save(verificationToken);
        log.info("Email verified for user: {}", user.getId());
    }

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            String plainToken = generateToken();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHasher.hash(plainToken))
                .expiresAt(Instant.now().plusMillis(passwordResetExpirationMs))
                .build();
            passwordResetTokenRepository.save(resetToken);
            log.debug("Created password reset token for user: {}", user.getId());
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository
            .findByTokenHash(tokenHasher.hash(request.getToken()))
            .orElseThrow(() -> new InvalidTokenException("Password reset token is invalid"));

        if (!resetToken.isValid()) {
            throw new InvalidTokenException("Password reset token is expired or already used");
        }

        if (!PasswordValidator.isValid(request.getNewPassword())) {
            throw new InvalidPasswordException(PasswordValidator.getPasswordRequirements());
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        resetToken.markAsUsed();
        userRepository.save(user);
        passwordResetTokenRepository.save(resetToken);
        refreshTokenService.revokeAllUserTokens(user);
        log.info("Password reset completed for user: {}", user.getId());
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
