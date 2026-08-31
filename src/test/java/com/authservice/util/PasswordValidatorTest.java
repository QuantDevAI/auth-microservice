package com.authservice.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordValidator Tests")
class PasswordValidatorTest {

    @Test
    @DisplayName("Should accept valid password")
    void testValidPassword() {
        String validPassword = "ValidPass123!";
        assertTrue(PasswordValidator.isValid(validPassword));
    }

    @Test
    @DisplayName("Should reject password without uppercase")
    void testPasswordWithoutUppercase() {
        String password = "validpass123!";
        assertFalse(PasswordValidator.isValid(password));
    }

    @Test
    @DisplayName("Should reject password without lowercase")
    void testPasswordWithoutLowercase() {
        String password = "VALIDPASS123!";
        assertFalse(PasswordValidator.isValid(password));
    }

    @Test
    @DisplayName("Should reject password without digit")
    void testPasswordWithoutDigit() {
        String password = "ValidPass!";
        assertFalse(PasswordValidator.isValid(password));
    }

    @Test
    @DisplayName("Should reject password without special character")
    void testPasswordWithoutSpecialChar() {
        String password = "ValidPass123";
        assertFalse(PasswordValidator.isValid(password));
    }

    @Test
    @DisplayName("Should reject password shorter than 8 characters")
    void testPasswordTooShort() {
        String password = "Abc1!";
        assertFalse(PasswordValidator.isValid(password));
    }

    @Test
    @DisplayName("Should reject password longer than 255 characters")
    void testPasswordTooLong() {
        String password = "A" + "a1!" + "x".repeat(253);
        assertFalse(PasswordValidator.isValid(password));
    }

    @Test
    @DisplayName("Should reject null password")
    void testNullPassword() {
        assertFalse(PasswordValidator.isValid(null));
    }

    @Test
    @DisplayName("Should accept multiple special characters")
    void testMultipleSpecialChars() {
        String password = "Valid@Pass#123";
        assertTrue(PasswordValidator.isValid(password));
    }
}
