package com.authservice.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Entity Tests")
class UserTest {

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
            .status(User.UserStatus.PENDING_VERIFICATION)
            .emailVerified(false)
            .roles(Set.of(userRole))
            .build();
    }

    @Test
    @DisplayName("Should create user with correct properties")
    void testUserCreation() {
        assertNotNull(user.getId());
        assertEquals("João Silva", user.getName());
        assertEquals("joao@example.com", user.getEmail());
        assertEquals("hashedPassword123", user.getPasswordHash());
        assertEquals(User.UserStatus.PENDING_VERIFICATION, user.getStatus());
        assertFalse(user.getEmailVerified());
        assertTrue(user.getRoles().contains(userRole));
    }

    @Test
    @DisplayName("Should add role to user")
    void testAddRole() {
        // Arrange
        Role adminRole = Role.builder()
            .id(UUID.randomUUID())
            .name("ADMIN")
            .build();

        // Act
        user.addRole(adminRole);

        // Assert
        assertEquals(2, user.getRoles().size());
        assertTrue(user.getRoles().contains(adminRole));
    }

    @Test
    @DisplayName("Should remove role from user")
    void testRemoveRole() {
        // Act
        user.removeRole(userRole);

        // Assert
        assertEquals(0, user.getRoles().size());
        assertFalse(user.getRoles().contains(userRole));
    }

    @Test
    @DisplayName("Should check if account is active")
    void testIsAccountActive() {
        // Arrange
        user.setStatus(User.UserStatus.PENDING_VERIFICATION);
        assertFalse(user.isAccountActive());

        user.setStatus(User.UserStatus.ACTIVE);
        assertTrue(user.isAccountActive());
    }

    @Test
    @DisplayName("Should check if account is locked")
    void testIsAccountLocked() {
        // Arrange
        user.setStatus(User.UserStatus.LOCKED);

        // Act & Assert
        assertTrue(user.isAccountLocked());
    }

    @Test
    @DisplayName("Should check if email is verified")
    void testIsEmailVerified() {
        // Arrange
        user.setEmailVerified(false);
        assertFalse(user.isEmailVerified());

        user.setEmailVerified(true);
        assertTrue(user.isEmailVerified());
    }

    @Test
    @DisplayName("Should check if account is pending verification")
    void testIsPendingVerification() {
        // Arrange
        user.setStatus(User.UserStatus.PENDING_VERIFICATION);

        // Act & Assert
        assertTrue(user.isPendingVerification());

        user.setStatus(User.UserStatus.ACTIVE);
        assertFalse(user.isPendingVerification());
    }

    @Test
    @DisplayName("Should update last login timestamp")
    void testUpdateLastLogin() {
        // Arrange
        assertNull(user.getLastLoginAt());

        // Act
        user.updateLastLogin();

        // Assert
        assertNotNull(user.getLastLoginAt());
    }
}
