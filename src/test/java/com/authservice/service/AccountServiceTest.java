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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenHasher tokenHasher;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    @Test
    void verifyEmailActivatesPendingUserAndConsumesToken() {
        User user = user(User.UserStatus.PENDING_VERIFICATION, false);
        EmailVerificationToken token = EmailVerificationToken.builder()
            .id(UUID.randomUUID())
            .user(user)
            .tokenHash("hash")
                .expiresAt(Instant.now().plusSeconds(3600))
            .build();
        when(tokenHasher.hash("verification-token")).thenReturn("hash");
        when(emailVerificationTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

        accountService.verifyEmail("verification-token");

        assertTrue(user.getEmailVerified());
        assertEquals(User.UserStatus.ACTIVE, user.getStatus());
        assertTrue(token.isUsed());
        verify(userRepository).save(user);
        verify(emailVerificationTokenRepository).save(token);
    }

    @Test
    void verifyEmailRejectsExpiredToken() {
        EmailVerificationToken token = EmailVerificationToken.builder()
            .user(user(User.UserStatus.PENDING_VERIFICATION, false))
            .tokenHash("hash")
            .expiresAt(Instant.now().minusSeconds(1))
            .build();
        when(tokenHasher.hash("expired")).thenReturn("hash");
        when(emailVerificationTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

        assertThrows(InvalidTokenException.class, () -> accountService.verifyEmail("expired"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void forgotPasswordDoesNotRevealUnknownEmail() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        accountService.requestPasswordReset(ForgotPasswordRequest.builder()
            .email(" MISSING@EXAMPLE.COM ")
            .build());

        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void resetPasswordUpdatesPasswordConsumesTokenAndRevokesSessions() {
        User user = user(User.UserStatus.ACTIVE, true);
        PasswordResetToken token = PasswordResetToken.builder()
            .user(user)
            .tokenHash("hash")
                .expiresAt(Instant.now().plusSeconds(3600))
            .build();
        when(tokenHasher.hash("reset-token")).thenReturn("hash");
        when(passwordResetTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("new-hash");

        accountService.resetPassword(ResetPasswordRequest.builder()
            .token("reset-token")
            .newPassword("NewPassword123!")
            .build());

        assertEquals("new-hash", user.getPasswordHash());
        assertTrue(token.isUsed());
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(token);
        verify(refreshTokenService).revokeAllUserTokens(user);
    }

    @Test
    void resetPasswordRejectsWeakPasswordBeforeChangingUser() {
        User user = user(User.UserStatus.ACTIVE, true);
        PasswordResetToken token = PasswordResetToken.builder()
            .user(user)
            .tokenHash("hash")
                .expiresAt(Instant.now().plusSeconds(3600))
            .build();
        when(tokenHasher.hash("reset-token")).thenReturn("hash");
        when(passwordResetTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

        assertThrows(InvalidPasswordException.class, () -> accountService.resetPassword(
            ResetPasswordRequest.builder().token("reset-token").newPassword("weak").build()));
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAllUserTokens(any());
    }

    private User user(User.UserStatus status, boolean emailVerified) {
        return User.builder()
            .id(UUID.randomUUID())
            .name("Test User")
            .email("test@example.com")
            .passwordHash("old-hash")
            .status(status)
            .emailVerified(emailVerified)
            .build();
    }
}
