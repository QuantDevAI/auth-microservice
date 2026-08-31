package com.authservice.security;

import com.authservice.entity.Role;
import com.authservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("JwtProvider Tests")
class JwtProviderTest {

    @Autowired
    private JwtProvider jwtProvider;

    private User user;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = Role.builder()
            .id(UUID.randomUUID())
            .name("USER")
            .description("Standard user")
            .build();

        user = User.builder()
            .id(UUID.randomUUID())
            .name("João Silva")
            .email("joao@example.com")
            .passwordHash("hashedPassword123")
            .status(User.UserStatus.ACTIVE)
            .emailVerified(true)
            .roles(Set.of(userRole))
            .build();
    }

    @Test
    @DisplayName("Should generate access token")
    void testGenerateAccessToken() {
        // Act
        String token = jwtProvider.generateAccessToken(user);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtProvider.validateToken(token));
    }

    @Test
    @DisplayName("Should generate refresh token")
    void testGenerateRefreshToken() {
        // Act
        String token = jwtProvider.generateRefreshToken(user);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtProvider.validateToken(token));
    }

    @Test
    @DisplayName("Should extract user ID from token")
    void testExtractUserIdFromToken() {
        // Arrange
        String token = jwtProvider.generateAccessToken(user);

        // Act
        String userId = jwtProvider.getUserIdFromToken(token);

        // Assert
        assertEquals(user.getId().toString(), userId);
    }

    @Test
    @DisplayName("Should extract email from token")
    void testExtractEmailFromToken() {
        // Arrange
        String token = jwtProvider.generateAccessToken(user);

        // Act
        String email = jwtProvider.getEmailFromToken(token);

        // Assert
        assertEquals(user.getEmail(), email);
    }

    @Test
    @DisplayName("Should extract roles from token")
    void testExtractRolesFromToken() {
        // Arrange
        String token = jwtProvider.generateAccessToken(user);

        // Act
        String roles = jwtProvider.getRolesFromToken(token);

        // Assert
        assertNotNull(roles);
        assertTrue(roles.contains("ROLE_USER"));
    }

    @Test
    @DisplayName("Should validate valid token")
    void testValidateValidToken() {
        // Arrange
        String token = jwtProvider.generateAccessToken(user);

        // Act
        boolean isValid = jwtProvider.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject invalid token")
    void testValidateInvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act
        boolean isValid = jwtProvider.validateToken(invalidToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should check if token is not expired")
    void testIsTokenNotExpired() {
        // Arrange
        String token = jwtProvider.generateAccessToken(user);

        // Act
        boolean isExpired = jwtProvider.isTokenExpired(token);

        // Assert
        assertFalse(isExpired);
    }

    @Test
    @DisplayName("Should get access token expiration in milliseconds")
    void testGetAccessTokenExpirationMs() {
        // Act
        long expirationMs = jwtProvider.getAccessTokenExpirationMs();

        // Assert
        assertTrue(expirationMs > 0);
        assertEquals(900000L, expirationMs); // 15 minutes
    }

    @Test
    @DisplayName("Should handle multiple roles in token")
    void testMultipleRolesInToken() {
        // Arrange
        Role adminRole = Role.builder()
            .id(UUID.randomUUID())
            .name("ADMIN")
            .build();
        user.addRole(adminRole);

        // Act
        String token = jwtProvider.generateAccessToken(user);
        String roles = jwtProvider.getRolesFromToken(token);

        // Assert
        assertTrue(roles.contains("ROLE_USER"));
        assertTrue(roles.contains("ROLE_ADMIN"));
    }
}
