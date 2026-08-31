package com.authservice.controller;

import com.authservice.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("auth_service_test")
        .withUsername("auth_user")
        .withPassword("auth_password");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequest validRegisterRequest;

    @BeforeEach
    void setUp() {
        validRegisterRequest = RegisterRequest.builder()
            .name("João Silva")
            .email("joao@example.com")
            .password("ValidPass123!")
            .build();
    }

    @Test
    @DisplayName("Should register user successfully and return 201")
    void testRegisterUserSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.name", equalTo("João Silva")))
            .andExpect(jsonPath("$.email", equalTo("joao@example.com")))
            .andExpect(jsonPath("$.emailVerified", equalTo(false)))
            .andExpect(jsonPath("$.status", equalTo("PENDING_VERIFICATION")))
            .andExpect(jsonPath("$.roles", hasItem("USER")))
            .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("Should reject registration with invalid email")
    void testRegisterWithInvalidEmail() throws Exception {
        // Arrange
        RegisterRequest invalidRequest = RegisterRequest.builder()
            .name("João Silva")
            .email("invalid-email")
            .password("ValidPass123!")
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.email", notNullValue()));
    }

    @Test
    @DisplayName("Should reject registration with weak password")
    void testRegisterWithWeakPassword() throws Exception {
        // Arrange
        RegisterRequest weakPasswordRequest = RegisterRequest.builder()
            .name("João Silva")
            .email("joao@example.com")
            .password("weak")
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(weakPasswordRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.password", notNullValue()));
    }

    @Test
    @DisplayName("Should reject registration without name")
    void testRegisterWithoutName() throws Exception {
        // Arrange
        RegisterRequest noNameRequest = RegisterRequest.builder()
            .email("joao@example.com")
            .password("ValidPass123!")
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(noNameRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.name", notNullValue()));
    }

    @Test
    @DisplayName("Should reject duplicate email registration")
    void testRegisterWithDuplicateEmail() throws Exception {
        // Arrange - Register first user
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
            .andExpect(status().isCreated());

        // Act & Assert - Try to register with same email
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error", equalTo("CONFLICT")))
            .andExpect(jsonPath("$.message", containsString("Email already registered")));
    }

    @Test
    @DisplayName("Should normalize email to lowercase")
    void testEmailNormalization() throws Exception {
        // Arrange
        RegisterRequest uppercaseEmailRequest = RegisterRequest.builder()
            .name("João Silva")
            .email("JOAO@EXAMPLE.COM")
            .password("ValidPass123!")
            .build();

        // Act
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(uppercaseEmailRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        // Assert
        String response = result.getResponse().getContentAsString();
        assertThat(response).contains("\"email\":\"joao@example.com\"");
    }

    @Test
    @DisplayName("Should reject registration with short password")
    void testRegisterWithShortPassword() throws Exception {
        // Arrange
        RegisterRequest shortPasswordRequest = RegisterRequest.builder()
            .name("João Silva")
            .email("joao@example.com")
            .password("Pass1!")
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(shortPasswordRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.password", notNullValue()));
    }
}
