package com.authservice.service;

import com.authservice.entity.RefreshToken;
import com.authservice.entity.User;
import com.authservice.exception.InvalidTokenException;
import com.authservice.repository.RefreshTokenRepository;
import com.authservice.security.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenHasher tokenHasher;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;
    private String plainToken;
    private String tokenHash;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(UUID.randomUUID())
            .email("test@email.com")
            .name("Test User")
            .build();

        plainToken = "plain_refresh_token_value";
        tokenHash = "hashed_refresh_token_value";
    }

    @Test
    void testCreateRefreshToken_Success() {
        when(tokenHasher.hash(plainToken)).thenReturn(tokenHash);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(UUID.randomUUID());
            return token;
        });

        String result = refreshTokenService.createRefreshToken(testUser, plainToken, "192.168.1.1", "Mozilla/5.0");

        assertEquals(plainToken, result);
        verify(tokenHasher).hash(plainToken);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void testValidateRefreshToken_Success() {
        RefreshToken validToken = RefreshToken.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .tokenHash(tokenHash)
            .expiresAt(Instant.now().plusSeconds(3600))
            .revokedAt(null)
            .build();

        when(tokenHasher.hash(plainToken)).thenReturn(tokenHash);
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(validToken));

        RefreshToken result = refreshTokenService.validateRefreshToken(plainToken);

        assertNotNull(result);
        assertEquals(validToken.getId(), result.getId());
        assertEquals(testUser.getId(), result.getUser().getId());
        verify(refreshTokenRepository).findByTokenHash(tokenHash);
    }

    @Test
    void testValidateRefreshToken_TokenNotFound() {
        when(tokenHasher.hash(plainToken)).thenReturn(tokenHash);
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> 
            refreshTokenService.validateRefreshToken(plainToken)
        );
    }

    @Test
    void testValidateRefreshToken_TokenExpired() {
        RefreshToken expiredToken = RefreshToken.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .tokenHash(tokenHash)
            .expiresAt(Instant.now().minusSeconds(3600))
            .revokedAt(null)
            .build();

        when(tokenHasher.hash(plainToken)).thenReturn(tokenHash);
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredToken));

        assertThrows(InvalidTokenException.class, () -> 
            refreshTokenService.validateRefreshToken(plainToken)
        );
    }

    @Test
    void testValidateRefreshToken_TokenRevoked() {
        RefreshToken revokedToken = RefreshToken.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .tokenHash(tokenHash)
            .expiresAt(Instant.now().plusSeconds(3600))
            .revokedAt(Instant.now().minusSeconds(60))
            .build();

        when(tokenHasher.hash(plainToken)).thenReturn(tokenHash);
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(revokedToken));

        assertThrows(InvalidTokenException.class, () -> 
            refreshTokenService.validateRefreshToken(plainToken)
        );
    }

    @Test
    void testRevokeRefreshToken_Success() {
        RefreshToken token = RefreshToken.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .tokenHash(tokenHash)
            .expiresAt(Instant.now().plusSeconds(3600))
            .revokedAt(null)
            .build();

        when(tokenHasher.hash(plainToken)).thenReturn(tokenHash);
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(token);

        refreshTokenService.revokeRefreshToken(plainToken);

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void testRevokeRefreshToken_NotFound() {
        when(tokenHasher.hash(plainToken)).thenReturn(tokenHash);
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> 
            refreshTokenService.revokeRefreshToken(plainToken)
        );
    }

    @Test
    void testRevokeAllUserTokens_Success() {
        when(refreshTokenRepository.deleteByUserIdAndRevokedAtIsNull(testUser.getId())).thenReturn(3);

        refreshTokenService.revokeAllUserTokens(testUser);

        verify(refreshTokenRepository).deleteByUserIdAndRevokedAtIsNull(testUser.getId());
    }

    @Test
    void testRefreshTokenEntity_IsValid() {
        RefreshToken validToken = RefreshToken.builder()
            .user(testUser)
            .tokenHash(tokenHash)
            .expiresAt(Instant.now().plusSeconds(3600))
            .revokedAt(null)
            .build();

        assertTrue(validToken.isValid());
    }

    @Test
    void testRefreshTokenEntity_NotValid_Expired() {
        RefreshToken expiredToken = RefreshToken.builder()
            .user(testUser)
            .tokenHash(tokenHash)
            .expiresAt(Instant.now().minusSeconds(3600))
            .revokedAt(null)
            .build();

        assertFalse(expiredToken.isValid());
    }

    @Test
    void testRefreshTokenEntity_NotValid_Revoked() {
        RefreshToken revokedToken = RefreshToken.builder()
            .user(testUser)
            .tokenHash(tokenHash)
            .expiresAt(Instant.now().plusSeconds(3600))
            .revokedAt(Instant.now().minusSeconds(60))
            .build();

        assertFalse(revokedToken.isValid());
    }
}
