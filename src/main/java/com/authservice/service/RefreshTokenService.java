package com.authservice.service;

import com.authservice.entity.RefreshToken;
import com.authservice.entity.User;
import com.authservice.exception.InvalidTokenException;
import com.authservice.repository.RefreshTokenRepository;
import com.authservice.security.JwtProvider;
import com.authservice.security.TokenHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasher tokenHasher;
    private final JwtProvider jwtProvider;

    @Value("${jwt.refresh-token-expiration:2592000000}")
    private long refreshTokenExpirationMs;

    @Transactional
    public String createRefreshToken(User user, String plainToken, String ipAddress, String userAgent) {
        // Hash the token before storing
        String tokenHash = tokenHasher.hash(plainToken);

        RefreshToken refreshToken = RefreshToken.builder()
            .user(user)
            .tokenHash(tokenHash)
            .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();

        refreshTokenRepository.save(refreshToken);
        log.debug("Created refresh token for user: {}", user.getId());
        return plainToken; // Return plain token to send to client
    }

    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String plainToken) {
        String tokenHash = tokenHasher.hash(plainToken);
        
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new InvalidTokenException("Refresh token not found or invalid"));

        if (refreshToken.isExpired()) {
            log.warn("Refresh token expired for user: {}", refreshToken.getUser().getId());
            throw new InvalidTokenException("Refresh token expired");
        }

        if (refreshToken.isRevoked()) {
            log.warn("Refresh token revoked for user: {}", refreshToken.getUser().getId());
            throw new InvalidTokenException("Refresh token revoked");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeRefreshToken(String plainToken) {
        String tokenHash = tokenHasher.hash(plainToken);
        
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);
        log.debug("Revoked refresh token for user: {}", refreshToken.getUser().getId());
    }

    @Transactional
    public void revokeAllUserTokens(com.authservice.entity.User user) {
        int revokedCount = refreshTokenRepository.deleteByUserIdAndRevokedAtIsNull(user.getId());
        log.debug("Revoked {} active refresh tokens for user: {}", revokedCount, user.getId());
    }
}
