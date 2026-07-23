package org.example.librarymanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtSecretValidator {
    @Value("${jwt.secret}")
    public void validateJwtSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "[STARTUP FAILURE] jwt.secret is blank. " +
                    "Set the JWT_SECRET environment variable before starting the application.");
        }
    }
}
