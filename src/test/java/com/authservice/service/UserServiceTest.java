package com.authservice.service;

import com.authservice.dto.RegisterRequest;
import com.authservice.dto.UserResponse;
import com.authservice.entity.Role;
import com.authservice.entity.User;
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
@DisplayName("UserService Tests")
class UserServiceTest {

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

    private RegisterRequest validRegisterRequest;
    private Role userRole;

    @BeforeEach
    void setUp() {
        validRegisterRequest = RegisterRequest.builder()
            .name("João Silva")
            .email("joao@example.com")
            .password("ValidPass123!")
            .build();

        userRole = Role.builder()
            .id(UUID.randomUUID())
            .name("USER")
            .description("Standard user role")
            .build();
    }

    @Test
    @DisplayName("Should register user successfully")
    void testRegisterUserSuccessfully() {
        // Arrange
        when(userRepository.existsByEmail("joao@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword123");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));

        User savedUser = User.builder()
            .id(UUID.randomUUID())
            .name(validRegisterRequest.getName())
            .email(validRegisterRequest.getEmail())
            .passwordHash("hashedPassword123")
            .status(User.UserStatus.PENDING_VERIFICATION)
            .emailVerified(false)
            .roles(Set.of(userRole))
            .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponse response = userService.register(validRegisterRequest);

        // Assert
        assertNotNull(response);
        assertEquals(savedUser.getName(), response.getName());
        assertEquals(savedUser.getEmail(), response.getEmail());
        assertFalse(response.getEmailVerified());
        assertEquals("PENDING_VERIFICATION", response.getStatus());
        assertTrue(response.getRoles().contains("USER"));

        verify(userRepository, times(1)).existsByEmail("joao@example.com");
        verify(passwordEncoder, times(1)).encode(validRegisterRequest.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject registration with existing email")
    void testRegisterWithExistingEmail() {
        // Arrange
        when(userRepository.existsByEmail("joao@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.register(validRegisterRequest);
        });

        verify(userRepository, times(1)).existsByEmail("joao@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject registration with weak password")
    void testRegisterWithWeakPassword() {
        // Arrange
        RegisterRequest weakPasswordRequest = RegisterRequest.builder()
            .name("João Silva")
            .email("joao@example.com")
            .password("weak")
            .build();

        when(userRepository.existsByEmail("joao@example.com")).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidPasswordException.class, () -> {
            userService.register(weakPasswordRequest);
        });

        verify(userRepository, times(1)).existsByEmail("joao@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should normalize email to lowercase")
    void testEmailNormalization() {
        // Arrange
        RegisterRequest requestWithUppercaseEmail = RegisterRequest.builder()
            .name("João Silva")
            .email("JOAO@EXAMPLE.COM")
            .password("ValidPass123!")
            .build();

        when(userRepository.existsByEmail("joao@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword123");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));

        User savedUser = User.builder()
            .id(UUID.randomUUID())
            .name(requestWithUppercaseEmail.getName())
            .email("joao@example.com")
            .passwordHash("hashedPassword123")
            .status(User.UserStatus.PENDING_VERIFICATION)
            .emailVerified(false)
            .roles(Set.of(userRole))
            .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponse response = userService.register(requestWithUppercaseEmail);

        // Assert
        assertEquals("joao@example.com", response.getEmail());
    }

    @Test
    @DisplayName("Should reject registration when USER role not found")
    void testRegisterWhenUserRoleNotFound() {
        // Arrange
        when(userRepository.existsByEmail("joao@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword123");
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userService.register(validRegisterRequest);
        });

        verify(userRepository, never()).save(any(User.class));
    }
}
