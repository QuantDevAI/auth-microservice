package com.authservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Value("${info.app.name:auth-service}")
    private String appName;

    @Value("${info.app.version:1.0.0}")
    private String appVersion;

    @Value("${info.app.environment:${SPRING_PROFILES_ACTIVE:default}}")
    private String environment;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(buildHealthPayload());
    }

    @GetMapping("/api/v1/health")
    public ResponseEntity<Map<String, Object>> apiHealth() {
        return ResponseEntity.ok(buildHealthPayload());
    }

    private Map<String, Object> buildHealthPayload() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", appName);
        response.put("version", appVersion);
        response.put("environment", environment);
        return response;
    }
}
