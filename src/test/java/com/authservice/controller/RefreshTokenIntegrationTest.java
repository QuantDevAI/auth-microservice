package com.authservice.controller;

import com.authservice.dto.RefreshRequest;
import com.authservice.entity.RefreshToken;
import com.authservice.entity.Role;
import com.authservice.entity.User;
import com.authservice.exception.InvalidTokenException;
import com.authservice.repository.RefreshTokenRepository;
import com.authservice.repository.RoleRepository;
import com.authservice.repository.UserRepository;
import com.authservice.security.TokenHasher;
import com.authservice.service.RefreshTokenService;
import com.authservice.service.UserService;
import com.authservice.util.PasswordValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class RefreshTokenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TokenHasher tokenHasher;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role userRole;
    private User testUser;
    private String validAccessToken;
    private String validRefreshToken;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        userRole = roleRepository.findByName("USER")
            .orElseGet(() -> roleRepository.save(Role.builder().name("USER").description("User role").build()));

        testUser = User.builder()
            .name("Test User")
            .email("test@email.com")
            .passwordHash(passwordEncoder.encode("Password123!"))
            .status(User.UserStatus.ACTIVE)
            .emailVerified(true)
            .roles(Set.of(userRole))
            .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testRefreshToken_Success() throws Exception {
        // First, login to get tokens
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "test@email.com");
        loginRequest.put("password", "Password123!");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();

        // Now refresh the token
        Map<String, String> refreshRequest = new HashMap<>();
        refreshRequest.put("refreshToken", refreshToken);

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(refreshRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").isNumber())
            .andReturn();

        String refreshResponse = refreshResult.getResponse().getContentAsString();
        String newRefreshToken = objectMapper.readTree(refreshResponse).get("refreshToken").asText();

        // Old token should be revoked, new token should be stored
        RefreshToken oldTokenRecord = refreshTokenRepository.findByTokenHash(tokenHasher.hash(refreshToken))
            .orElse(null);
        RefreshToken newTokenRecord = refreshTokenRepository.findByTokenHash(tokenHasher.hash(newRefreshToken))
            .orElse(null);

        assertNotNull(newTokenRecord);
        assertNotNull(oldTokenRecord);
        assertNotNull(oldTokenRecord.getRevokedAt());
    }

    @Test
    void testRefreshToken_InvalidToken() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("refreshToken", "invalid_token_that_does_not_exist");

        mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void testRefreshToken_MissingToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new HashMap<>())))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testLogout_Success() throws Exception {
        // Login first
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "test@email.com");
        loginRequest.put("password", "Password123!");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(loginResponse).get("accessToken").asText();

        // Logout
        mockMvc.perform(post("/api/v1/auth/logout")
            .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk());

        // Verify all user tokens are revoked
        long activeTokenCount = refreshTokenRepository.findAll().stream()
            .filter(token -> token.getUser().getId().equals(testUser.getId()) && token.getRevokedAt() == null)
            .count();

        assertEquals(0, activeTokenCount);
    }

    @Test
    void testLogout_WithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isOk());
    }

    @Test
    void testTokenRotation_OldTokenCannotBeReused() throws Exception {
        // Login
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "test@email.com");
        loginRequest.put("password", "Password123!");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        String originalRefreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();

        // Refresh once
        Map<String, String> refreshRequest1 = new HashMap<>();
        refreshRequest1.put("refreshToken", originalRefreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(refreshRequest1)))
            .andExpect(status().isOk());

        // Try to refresh with the old token again (should fail because it was revoked)
        Map<String, String> refreshRequest2 = new HashMap<>();
        refreshRequest2.put("refreshToken", originalRefreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(refreshRequest2)))
            .andExpect(status().isUnauthorized());
    }
}
