package com.authservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@Slf4j
public class TokenHasher {
    private static final String ALGORITHM = "SHA-256";

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to hash token using algorithm: {}", ALGORITHM, e);
            throw new RuntimeException("Token hashing failed", e);
        }
    }

    public boolean verify(String token, String hash) {
        return hash(token).equals(hash);
    }
}
