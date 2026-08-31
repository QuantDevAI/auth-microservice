package com.authservice.controller;

import com.authservice.entity.Role;
import com.authservice.entity.User;
import com.authservice.repository.RoleRepository;
import com.authservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AdminAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        adminRole = roleRepository.findByName("ADMIN")
            .orElseGet(() -> roleRepository.save(Role.builder().name("ADMIN").description("Admin role").build()));

        userRole = roleRepository.findByName("USER")
            .orElseGet(() -> roleRepository.save(Role.builder().name("USER").description("User role").build()));
    }

    @Test
    void adminUserCanAccessAdminEndpoint() throws Exception {
        User adminUser = User.builder()
            .name("Admin User")
            .email("admin@example.com")
            .passwordHash(passwordEncoder.encode("Password123!"))
            .status(User.UserStatus.ACTIVE)
            .emailVerified(true)
            .roles(Set.of(adminRole))
            .build();
        userRepository.save(adminUser);

        String loginBody = "{\"email\":\"admin@example.com\",\"password\":\"Password123!\"}";
        String accessToken = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk());
    }

    @Test
    void regularUserCannotAccessAdminEndpoint() throws Exception {
        User normalUser = User.builder()
            .name("Regular User")
            .email("user@example.com")
            .passwordHash(passwordEncoder.encode("Password123!"))
            .status(User.UserStatus.ACTIVE)
            .emailVerified(true)
            .roles(Set.of(userRole))
            .build();
        userRepository.save(normalUser);

        String loginBody = "{\"email\":\"user@example.com\",\"password\":\"Password123!\"}";
        String accessToken = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isForbidden());
    }
}
