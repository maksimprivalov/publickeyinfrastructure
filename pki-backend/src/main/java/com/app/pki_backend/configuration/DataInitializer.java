package com.app.pki_backend.configuration;

import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.entity.certificates.CertificateType;
import com.app.pki_backend.entity.user.Admin;
import com.app.pki_backend.entity.user.CAUser;
import com.app.pki_backend.entity.user.User;
import com.app.pki_backend.repository.CertificateRepository;
import com.app.pki_backend.repository.UserRepository;
import com.app.pki_backend.service.interfaces.CertificateService;
import com.app.pki_backend.service.interfaces.MasterKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

@Configuration
public class DataInitializer {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private MasterKeyService masterKeyService;

    @Bean
    CommandLineRunner init(UserRepository userRepository) {
        return args -> {
            System.out.println("✅ DataInitializer started...");

            // Initialize master key first
            initializeMasterKey();

            // Initialize users
            initializeUsers(userRepository);

            // Initialize root certificates
            initializeRootCertificates();

            System.out.println("✅ DataInitializer completed successfully!");
        };
    }

    private void initializeMasterKey() {
        try {
            if (!masterKeyService.isMasterKeyAvailable()) {
                masterKeyService.generateMasterKey();
                System.out.println("✅ Master key generated");
            } else {
                System.out.println("ℹ️ Master key already exists");
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize master key: " + e.getMessage());
            throw new RuntimeException("Master key initialization failed", e);
        }
    }

    private void initializeUsers(UserRepository userRepository) {
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
    }

    private void initializeRootCertificates() {
        try {
            // Check if root certificates already exist
            List<Certificate> existingRootCerts = certificateRepository.findByType(CertificateType.ROOT_CA);

            if (existingRootCerts.isEmpty()) {
                System.out.println("🔐 Generating root certificate using CertificateService...");

                // Use your existing CertificateService to generate root certificate
                Certificate rootCertificate = certificateService.issueRootCertificate();

                System.out.println("✅ Root certificate generated successfully!");
                System.out.println("   ID: " + rootCertificate.getId());
                System.out.println("   Subject: " + rootCertificate.getSubject());
                System.out.println("   Serial: " + rootCertificate.getSerialNumber().toString(16));
                System.out.println("   Valid from: " + rootCertificate.getValidFrom());
                System.out.println("   Valid to: " + rootCertificate.getValidTo());
                System.out.println("   Status: " + rootCertificate.getStatus());

            } else {
                System.out.println("ℹ️ Root certificate already exists");

                // Display existing root certificates
                System.out.println("📋 Existing Root CAs:");
                existingRootCerts.forEach(cert ->
                        System.out.println(" - " + cert.getSubject() + " | Serial: " + cert.getSerialNumber().toString(16))
                );
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize root certificate: " + e.getMessage());
            e.printStackTrace();
            // Don't throw exception to allow application to start even if cert generation fails
        }
    }
}