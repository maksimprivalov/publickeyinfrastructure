package com.app.pki_backend.configuration;

import com.app.pki_backend.entity.user.Admin;
import com.app.pki_backend.entity.user.CAUser;
import com.app.pki_backend.entity.user.User;
import com.app.pki_backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static com.app.pki_backend.controller.UserController.passwordEncoder;

@Configuration
public class DataInitializer {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Bean
    CommandLineRunner init(UserRepository userRepository) {
        return args -> {
            System.out.println("✅ DataInitializer started...");

            // === Admin ===
            if (userRepository.findByEmail("admin@pki.local").isEmpty()) {
                Admin admin = new Admin();
                admin.setEmail("admin@pki.local");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setName("System");
                admin.setSurname("Administrator");
                admin.setOrganizationName("PKI-Org");
                admin.setActive(true);
                userRepository.saveAndFlush(admin);

                System.out.println("✅ Admin created: admin@pki.local / admin123");
            }

            // === CAUser ===
            if (userRepository.findByEmail("causer@pki.local").isEmpty()) {
                CAUser caUser = new CAUser();
                caUser.setEmail("causer@pki.local");
                caUser.setPassword(passwordEncoder.encode("causer123"));
                caUser.setName("CA");
                caUser.setSurname("Operator");
                caUser.setOrganizationName("Test-Org");
                caUser.setActive(true);
                userRepository.saveAndFlush(caUser);

                System.out.println("✅ CAUser created: causer@pki.local / causer123");
            }

            // === Regular User ===
            if (userRepository.findByEmail("user@pki.local").isEmpty()) {
                User user = new User();
                user.setEmail("user@pki.local");
                user.setPassword(passwordEncoder.encode("user123"));
                user.setName("Normal");
                user.setSurname("User");
                user.setOrganizationName("Client-Org");
                user.setActive(true);
                userRepository.saveAndFlush(user);

                System.out.println("✅ Regular User created: user@pki.local / user123");
            }

            System.out.println("📋 Users in DB:");
            userRepository.findAll().forEach(u ->
                    System.out.println(" - " + u.getId() + " | " + u.getEmail() + " | role=" + u.getRole())
            );
        };
    }
}