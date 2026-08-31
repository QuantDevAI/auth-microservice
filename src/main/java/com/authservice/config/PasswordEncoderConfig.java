package com.authservice.config;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    /**
     * Argon2 Password Encoder Bean
     * Uses Argon2id algorithm with recommended parameters for password hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder();
    }

    /**
     * Custom PasswordEncoder implementation using Argon2id
     */
    public static class Argon2PasswordEncoder implements PasswordEncoder {

        private final Argon2 argon2 = Argon2Factory.create(
            Argon2Factory.Argon2Types.ARGON2id,
            16,  // iterations
            65536 // memory in KB (64MB)
        );

        @Override
        public String encode(CharSequence rawPassword) {
            return argon2.hash(2, 65536, 1, rawPassword.toString());
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return argon2.verify(encodedPassword, rawPassword.toString());
        }

        @Override
        public boolean upgradeEncoding(String encodedPassword) {
            // Argon2 parameters are fixed, no need to upgrade
            return false;
        }
    }
}
