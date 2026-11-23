package com.abhijeet.employee_management_system.config;

import com.abhijeet.employee_management_system.model.User;
import com.abhijeet.employee_management_system.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Creates the first admin account on startup, only if no users exist yet.
 * Replaces the old hardcoded admin/admin123 login — no password is ever
 * hardcoded here: it's taken from ADMIN_DEFAULT_PASSWORD if set, or a random
 * one is generated and printed to the startup log exactly once (not stored
 * anywhere in plaintext).
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_DEFAULT_PASSWORD:}")
    private String configuredPassword;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return; // at least one account already exists — nothing to bootstrap
        }

        String password = configuredPassword;
        boolean generated = false;
        if (password == null || password.isBlank()) {
            password = generateRandomPassword();
            generated = true;
        }

        User admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(admin);

        if (generated) {
            log.warn("No users existed yet, so a first admin account was created.");
            log.warn("Username: admin");
            log.warn("Password (shown once, not stored anywhere in plaintext): {}", password);
            log.warn("Log in once and consider registering a personal account via POST /api/auth/register.");
        } else {
            log.info("Created initial admin account 'admin' using ADMIN_DEFAULT_PASSWORD.");
        }
    }

    private String generateRandomPassword() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
