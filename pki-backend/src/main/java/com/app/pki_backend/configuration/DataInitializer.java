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
import com.app.pki_backend.service.interfaces.PrivateKeyService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.SecretKey;
import java.util.List;

@Configuration
@Order(1)
public class DataInitializer {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private MasterKeyService masterKeyService;

    @Autowired
    private PrivateKeyService privateKeyService;

    @Bean
    CommandLineRunner init(UserRepository userRepository) {
        return args -> {
            System.out.println(" --- PKI System Initialization started... --- ");

            initializeMasterKey();

            initializeUsers(userRepository);

            initializeRootCertificates();

            System.out.println("✅ PKI System Initialization completed successfully!");
            System.out.println("🌐 HTTP available on: http://localhost:8080");
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

            if (!existingRootCerts.isEmpty()) {
                System.out.println("ℹ️ Found existing Root CA certificates, checking if usable...");

                // Try to find a usable Root CA
                Certificate usableRootCA = findUsableRootCA(existingRootCerts);

                if (usableRootCA != null) {
                    System.out.println("✅ Found usable Root CA: " + usableRootCA.getSubject());
                    System.out.println("   Serial: " + usableRootCA.getSerialNumber().toString(16));
                    return; // We have a working Root CA, no need to create new one
                } else {
                    System.out.println("⚠️ No usable Root CA found, cleaning up old certificates...");
                    cleanupUnusableRootCertificates(existingRootCerts);
                }
            }

            System.out.println("🔐 Generating new root certificate using CertificateService...");
            Certificate rootCertificate = certificateService.issueRootCertificate();

            System.out.println("✅ Root certificate generated successfully!");
            System.out.println("   ID: " + rootCertificate.getId());
            System.out.println("   Subject: " + rootCertificate.getSubject());
            System.out.println("   Serial: " + rootCertificate.getSerialNumber().toString(16));
            System.out.println("   Valid from: " + rootCertificate.getValidFrom());
            System.out.println("   Valid to: " + rootCertificate.getValidTo());
            System.out.println("   Status: " + rootCertificate.getStatus());

        } catch (Exception e) {
            System.err.println("❌ Failed to initialize root certificate: " + e.getMessage());
            e.printStackTrace();
            // Don't throw exception to allow application to start even if cert generation fails
        }
    }

    private Certificate findUsableRootCA(List<Certificate> rootCAs) {
        for (Certificate rootCA : rootCAs) {
            try {
                // Test if we can decrypt this Root CA's private key
                if (!masterKeyService.isMasterKeyAvailable()) {
                    continue;
                }

                SecretKey masterKey = masterKeyService.getCurrentMasterKey();
                System.out.println("🔍 Testing Root CA " + rootCA.getId() + "...");

                // Actually try to decrypt the private key to test if it's usable
                privateKeyService.retrievePrivateKey(rootCA, masterKey);

                System.out.println("✅ Root CA " + rootCA.getId() + " is usable!");
                return rootCA; // This one works!

            } catch (Exception e) {
                System.out.println("⚠️ Cannot decrypt Root CA " + rootCA.getId() + ": " + e.getMessage());
                // Continue to next Root CA
            }
        }

        return null; // No usable Root CA found
    }

    private void cleanupUnusableRootCertificates(List<Certificate> unusableRootCerts) {
        System.out.println("🧹 Cleaning up " + unusableRootCerts.size() + " unusable Root CA certificates...");

        for (Certificate cert : unusableRootCerts) {
            try {
                // Mark as revoked instead of deleting to maintain audit trail
                cert.setStatus(com.app.pki_backend.entity.certificates.CertificateStatus.REVOKED);
                certificateRepository.save(cert);

                System.out.println("   ♻️ Marked Root CA " + cert.getId() + " as REVOKED");

            } catch (Exception e) {
                System.err.println("   ❌ Failed to cleanup Root CA " + cert.getId() + ": " + e.getMessage());
            }
        }

        System.out.println("✅ Cleanup completed");
    }
}