
package com.devboard.config;

import com.devboard.entity.Role;
import com.devboard.entity.User;
import com.devboard.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class AdminBootstrap {

    @Bean
    CommandLineRunner createInitialAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${DEVBOARD_ADMIN_NAME:}") String adminName,
            @Value("${DEVBOARD_ADMIN_EMAIL:}") String adminEmail,
            @Value("${DEVBOARD_ADMIN_PASSWORD:}") String adminPassword
    ) {
        return args -> {
            if (adminEmail.isBlank()
                    || adminPassword.isBlank()
                    || adminName.isBlank()) {
                System.out.println(
                    "Initial Admin bootstrap skipped: credentials not configured."
                );
                return;
            }

            if (userRepository.existsByEmailIgnoreCase(adminEmail.trim())) {
                System.out.println(
                    "Initial Admin bootstrap skipped: account already exists."
                );
                return;
            }

            if (userRepository.existsByEmailIgnoreCase(adminEmail.trim())) {
                return;
            }

            

            if (userRepository.findAll().stream()
                    .anyMatch(user -> user.getRole() == Role.ADMIN)) {
                System.out.println(
                    "Initial Admin bootstrap skipped: an Admin already exists."
                );
                return;
            }

            User admin = new User();
            admin.setName(adminName.trim());
            admin.setEmail(adminEmail.trim().toLowerCase());
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setActive(true);

            userRepository.save(admin);

            System.out.println("Initial Admin account created.");
        };
    }

   
}
