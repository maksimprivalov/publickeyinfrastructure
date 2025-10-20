package com.app.pki_backend.configuration;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.security.Security;

/**
 * Configuration to register BouncyCastle security provider
 * MUST be loaded before any cryptographic operations
 */
@Configuration
public class BouncyCastleConfiguration {

    @PostConstruct
    public void init() {
        // Register BouncyCastle as a security provider
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
            System.out.println("✅ BouncyCastle security provider registered");
        } else {
            System.out.println("ℹ️ BouncyCastle security provider already registered");
        }
    }
}