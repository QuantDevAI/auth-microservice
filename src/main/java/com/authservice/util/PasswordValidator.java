package com.authservice.util;

import java.util.regex.Pattern;

public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 255;

    // Minimum requirements for password strength
    private static final Pattern HAS_UPPER = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWER = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("[0-9]");
    private static final Pattern HAS_SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>?]");

    /**
     * Validates password against security requirements
     * Requirements:
     * - Length: 8-255 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character
     */
    public static boolean isValid(String password) {
        if (password == null) {
            return false;
        }

        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return false;
        }

        return HAS_UPPER.matcher(password).find() &&
               HAS_LOWER.matcher(password).find() &&
               HAS_DIGIT.matcher(password).find() &&
               HAS_SPECIAL.matcher(password).find();
    }

    public static String getPasswordRequirements() {
        return "Password must contain at least 8 characters, including at least one uppercase letter, " +
               "one lowercase letter, one digit, and one special character.";
    }
}
