package com.authservice.service;

import com.authservice.dto.LoginRequest;
import com.authservice.dto.RegisterRequest;
import com.authservice.dto.UserResponse;
import com.authservice.entity.Role;
import com.authservice.entity.User;
import com.authservice.exception.InvalidCredentialsException;
import com.authservice.exception.InvalidPasswordException;
import com.authservice.exception.UserAlreadyExistsException;
import com.authservice.exception.UserNotFoundException;
import com.authservice.repository.RoleRepository;
import com.authservice.repository.UserRepository;
import com.authservice.util.PasswordValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AccountService accountService;

    /**
     * Register a new user
     * @param request Registration request with name, email, and password
     * @return UserResponse with registered user data
     * @throws UserAlreadyExistsException if email already exists
     * @throws InvalidPasswordException if password doesn't meet requirements
     */
    public UserResponse register(RegisterRequest request) {
        log.debug("Processing user registration for email: {}", request.getEmail());


        // Normalize and validate email
        String normalizedEmail = normalizeEmail(request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Registration attempt with existing email: {}", normalizedEmail);
            throw new UserAlreadyExistsException("Email already registered");
        }

        // Validate password strength
        if (!PasswordValidator.isValid(request.getPassword())) {
            log.warn("Registration attempt with weak password for email: {}", normalizedEmail);
            throw new InvalidPasswordException(PasswordValidator.getPasswordRequirements());
        }

        // Hash password
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // Get default USER role
        Role userRole = roleRepository.findByName("USER")
            .orElseThrow(() -> new RuntimeException("Default USER role not found"));

        User user = User.builder()
                .name(request.getName())
                .email(normalizedEmail)
                .passwordHash(passwordHash)
                .status(User.UserStatus.ACTIVE)
                .emailVerified(true)
                .roles(Set.of(userRole))
                .build();


        User savedUser = userRepository.save(user);
        accountService.createEmailVerificationToken(savedUser);
        log.info("User registered successfully: {}", savedUser.getId());

        return mapToUserResponse(savedUser);
    }

    /**
     * Login user with email and password
     * @param request Login request with email and password
     * @param servletRequest HTTP request for IP and User-Agent
     * @return User if credentials are valid
     * @throws InvalidCredentialsException if credentials are invalid
     */
    public User login(LoginRequest request) {
        return login(request, null);
    }

    public User login(LoginRequest request, HttpServletRequest servletRequest) {
        log.debug("Processing login for email: {}", request.getEmail());

        String normalizedEmail = normalizeEmail(request.getEmail());
        
        User user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> {
                log.warn("Login attempt with non-existent email: {}", normalizedEmail);
                return new InvalidCredentialsException("Invalid email or password");
            });

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login attempt with invalid password for email: {}", normalizedEmail);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Check if account is active
        if (!user.isAccountActive()) {
            log.warn("Login attempt with inactive account: {}", normalizedEmail);
            throw new InvalidCredentialsException("Account is not active");
        }

        log.info("User logged in successfully: {}", user.getId());
        return user;
    }

    /**
     * Find user by email
     */
    @Transactional(readOnly = true)
    public UserResponse findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        return mapToUserResponse(user);
    }

    /**
     * Find user by ID
     */
    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        return mapToUserResponse(user);
    }

    /**
     * Find user entity by ID (for internal use)
     */
    @Transactional(readOnly = true)
    public User findUserById(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    /**
     * Normalize email to lowercase
     */
    private String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase() : null;
    }

    /**
     * Map User entity to UserResponse DTO
     */
    private UserResponse mapToUserResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());

        return UserResponse.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .emailVerified(user.getEmailVerified())
            .status(user.getStatus().toString())
            .roles(roleNames)
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .build();
    }
}
