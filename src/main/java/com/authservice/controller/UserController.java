package com.authservice.controller;

import com.authservice.dto.UserResponse;
import com.authservice.entity.User;
import com.authservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Get authenticated user info
     * @param authentication Current authentication
     * @return UserResponse with user data
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        log.debug("Received request for authenticated user info");

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userId = authentication.getPrincipal().toString();
        UserResponse response = userService.findById(UUID.fromString(userId));

        return ResponseEntity.ok(response);
    }
}
