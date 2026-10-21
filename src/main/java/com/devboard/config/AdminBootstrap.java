
package com.devboard.config;

import com.devboard.entity.Role;
import com.devboard.entity.User;
import com.devboard.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;

@Configuration
public class AdminBootstrap {

    @Bean
    CommandLineRunner createInitialUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,

            @Value("${DEVBOARD_ADMIN_NAME:}") String adminName,
            @Value("${DEVBOARD_ADMIN_EMAIL:}") String adminEmail,
            @Value("${DEVBOARD_ADMIN_PASSWORD:}") String adminPassword,

            @Value("${DEVBOARD_PM_NAME:}") String pmName,
            @Value("${DEVBOARD_PM_EMAIL:}") String pmEmail,
            @Value("${DEVBOARD_PM_PASSWORD:}") String pmPassword
    ) {
        return args -> {
            createUserIfConfigured(
                    userRepository, passwordEncoder,
                    adminName, adminEmail, adminPassword, Role.ADMIN
            );

            createUserIfConfigured(
                    userRepository, passwordEncoder,
                    pmName, pmEmail, pmPassword, Role.PROJECT_MANAGER
            );
        };
    }

    private void createUserIfConfigured(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String name,
            String email,
            String password,
            Role role
    ) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            System.out.println(role + " bootstrap skipped: credentials not configured.");
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            System.out.println(role + " bootstrap skipped: email already exists.");
            return;
        }

        boolean roleAlreadyExists = userRepository.findAll().stream()
                .anyMatch(user -> user.getRole() == role);

        if (roleAlreadyExists) {
            System.out.println(role + " bootstrap skipped: this role already exists.");
            return;
        }

        User user = new User();
        user.setName(name.trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setActive(true);

        userRepository.save(user);

        System.out.println("Initial " + role + " account created.");
    }
}
