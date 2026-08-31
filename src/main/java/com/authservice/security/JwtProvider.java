package com.authservice.security;

import com.authservice.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration:900000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:2592000000}")
    private long refreshTokenExpiration;

    private static final String ISSUER = "auth-service";
    private static final String AUTHORITIES_CLAIM = "roles";

    /**
     * Generate Access Token for user
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(accessTokenExpiration, ChronoUnit.MILLIS);

        String roles = user.getRoles().stream()
            .map(role -> "ROLE_" + role.getName())
            .collect(Collectors.joining(","));

        return Jwts.builder()
            .subject(user.getId().toString())
            .issuer(ISSUER)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiryDate))
            .claim(AUTHORITIES_CLAIM, roles)
            .claim("email", user.getEmail())
            .claim("name", user.getName())
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact();
    }

    /**
     * Generate Refresh Token for user
     */
    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(refreshTokenExpiration, ChronoUnit.MILLIS);

        return Jwts.builder()
            .subject(user.getId().toString())
            .issuer(ISSUER)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiryDate))
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact();
    }

    /**
     * Extract user ID from token
     */
    public String getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * Extract email from token
     */
    public String getEmailFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return (String) claims.get("email");
    }

    /**
     * Extract roles from token
     */
    public String getRolesFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return (String) claims.get(AUTHORITIES_CLAIM);
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (SecurityException ex) {
            log.debug("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.debug("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.debug("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.debug("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.debug("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        }
    }

    /**
     * Get all claims from token
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    /**
     * Get signing key
     */
    private SecretKey getSigningKey() {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new RuntimeException("JWT_SECRET environment variable is not set");
        }
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Get access token expiration in milliseconds
     */
    public long getAccessTokenExpirationMs() {
        return accessTokenExpiration;
    }
}
