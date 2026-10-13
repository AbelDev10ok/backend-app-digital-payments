package com.app.digital.payments.digital_pyments.configuration.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupSecurityCheck implements ApplicationRunner {

    private final String activeProfiles;

    public StartupSecurityCheck(@Value("${spring.profiles.active:}") String activeProfiles) {
        this.activeProfiles = activeProfiles;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProd()) {
            return;
        }

        String jwtSecret = System.getenv("JWT_SECRET");
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "Falta JWT_SECRET. En produccion es obligatoria (al menos 64 caracteres).");
        }

        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbPassword == null || dbPassword.isBlank()) {
            throw new IllegalStateException("Falta DB_PASSWORD. En produccion es obligatoria.");
        }
    }

    private boolean isProd() {
        if (activeProfiles == null || activeProfiles.isBlank()) {
            return false;
        }
        return Arrays.stream(activeProfiles.split(","))
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile.trim()));
    }
}
