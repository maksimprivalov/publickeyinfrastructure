package com.app.pki_backend.configuration;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

@Configuration
public class SelfSignedHttpsConfig {

    @Bean
    CommandLineRunner selfSignedHttpsInitializer() {
        return args -> {
            try {
                System.out.println("🔐 Generating BouncyCastle self-signed HTTPS certificate...");

                // === 1️⃣ Генерация ключей RSA ===
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(2048, new SecureRandom());
                KeyPair keyPair = keyGen.generateKeyPair();

                // === 2️⃣ Параметры сертификата ===
                X500Name subject = new X500Name("CN=localhost, O=PKI Demo, C=RS");
                BigInteger serial = new BigInteger(64, new SecureRandom());
                Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60);
                Date notAfter = new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000L);

                // === 3️⃣ Создание X.509 сертификата ===
                X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                        subject, serial, notBefore, notAfter, subject, keyPair.getPublic()
                );

                certBuilder.addExtension(
                        Extension.basicConstraints, true, new BasicConstraints(false)
                );
                certBuilder.addExtension(
                        Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment)
                );

                ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                        .build(keyPair.getPrivate());

                X509Certificate certificate = new JcaX509CertificateConverter()
                        .setProvider("BC")
                        .getCertificate(certBuilder.build(signer));

                // === 4️⃣ Создание и сохранение PKCS12 keystore ===
                String password = "changeit";
                KeyStore ks = KeyStore.getInstance("PKCS12");
                ks.load(null, null);
                ks.setKeyEntry("selfsigned", keyPair.getPrivate(),
                        password.toCharArray(),
                        new java.security.cert.Certificate[]{certificate});

                java.nio.file.Path ksDir = java.nio.file.Paths.get("keystore");
                java.nio.file.Files.createDirectories(ksDir);
                try (FileOutputStream fos = new FileOutputStream(ksDir.resolve("selfsigned.p12").toFile())) {
                    ks.store(fos, password.toCharArray());
                }

                // === 5️⃣ Включаем HTTPS для Spring Boot ===
                System.setProperty("server.ssl.enabled", "true");
                System.setProperty("server.ssl.key-store", ksDir.resolve("selfsigned.p12").toAbsolutePath().toString());
                System.setProperty("server.ssl.key-store-password", password);
                System.setProperty("server.ssl.key-store-type", "PKCS12");
                System.setProperty("server.ssl.key-alias", "selfsigned");
                System.setProperty("server.port", "8443");

                System.out.println("✅ HTTPS ready at https://localhost:8443");

            } catch (Exception e) {
                System.err.println("❌ Failed to generate HTTPS certificate: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}
