package com.authservice.service;

import com.authservice.dto.LoginRequest;
import com.authservice.dto.RegisterRequest;
import com.authservice.dto.UserResponse;
import com.authservice.entity.Role;
import com.authservice.entity.User;
import com.authservice.exception.InvalidCredentialsException;
import com.authservice.exception.InvalidPasswordException;
import com.authservice.exception.UserAlreadyExistsException;
import com.authservice.repository.RoleRepository;
import com.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Login Tests")
class UserServiceLoginTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private UserService userService;

    private LoginRequest validLoginRequest;
    private User activeUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        validLoginRequest = LoginRequest.builder()
            .email("joao@example.com")
            .password("ValidPass123!")
            .build();

        userRole = Role.builder()
            .id(UUID.randomUUID())
            .name("USER")
            .description("Standard user role")
            .build();

        activeUser = User.builder()
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
    @DisplayName("Should login user successfully with valid credentials")
    void testLoginUserSuccessfully() {
        // Arrange
        when(userRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("ValidPass123!", "hashedPassword123")).thenReturn(true);

        // Act
        User result = userService.login(validLoginRequest);

        // Assert
        assertNotNull(result);
        assertEquals(activeUser.getId(), result.getId());
        assertEquals(activeUser.getEmail(), result.getEmail());
        verify(userRepository, times(1)).findByEmail("joao@example.com");
        verify(passwordEncoder, times(1)).matches("ValidPass123!", "hashedPassword123");
    }

    @Test
    @DisplayName("Should reject login with non-existent email")
    void testLoginWithNonExistentEmail() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        LoginRequest request = LoginRequest.builder()
            .email("nonexistent@example.com")
            .password("ValidPass123!")
            .build();

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            userService.login(request);
        });

        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
    }

    @Test
    @DisplayName("Should reject login with invalid password")
    void testLoginWithInvalidPassword() {
        // Arrange
        when(userRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("WrongPassword123!", "hashedPassword123")).thenReturn(false);

        LoginRequest request = LoginRequest.builder()
            .email("joao@example.com")
            .password("WrongPassword123!")
            .build();

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            userService.login(request);
        });

        verify(passwordEncoder, times(1)).matches("WrongPassword123!", "hashedPassword123");
    }

    @Test
    @DisplayName("Should reject login with inactive account")
    void testLoginWithInactiveAccount() {
        // Arrange
        User inactiveUser = User.builder()
            .id(UUID.randomUUID())
            .name("João Silva")
            .email("joao@example.com")
            .passwordHash("hashedPassword123")
            .status(User.UserStatus.INACTIVE)
            .emailVerified(true)
            .roles(Set.of(userRole))
            .build();

        when(userRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(inactiveUser));
        when(passwordEncoder.matches("ValidPass123!", "hashedPassword123")).thenReturn(true);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            userService.login(validLoginRequest);
        });
    }

    @Test
    @DisplayName("Should reject login with locked account")
    void testLoginWithLockedAccount() {
        // Arrange
        User lockedUser = User.builder()
            .id(UUID.randomUUID())
            .name("João Silva")
            .email("joao@example.com")
            .passwordHash("hashedPassword123")
            .status(User.UserStatus.LOCKED)
            .emailVerified(true)
            .roles(Set.of(userRole))
            .build();

        when(userRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(lockedUser));
        when(passwordEncoder.matches("ValidPass123!", "hashedPassword123")).thenReturn(true);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            userService.login(validLoginRequest);
        });
    }

    @Test
    @DisplayName("Should normalize email during login")
    void testEmailNormalizationDuringLogin() {
        // Arrange
        LoginRequest requestWithUppercaseEmail = LoginRequest.builder()
            .email("JOAO@EXAMPLE.COM")
            .password("ValidPass123!")
            .build();

        when(userRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("ValidPass123!", "hashedPassword123")).thenReturn(true);

        // Act
        User result = userService.login(requestWithUppercaseEmail);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(1)).findByEmail("joao@example.com");
    }
}
