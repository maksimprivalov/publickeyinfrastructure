package com.app.pki_backend.util;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

/**
 * Utility class for PEM format conversions
 */
@Component
public class PEMConverter {

    @Autowired
    private CSRValidator csrValidator;

    /**
     * Convert public key to PEM format
     */
    public String publicKeyToPEM(PublicKey publicKey) {
        try (StringWriter stringWriter = new StringWriter();
             JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {

            pemWriter.writeObject(publicKey);
            pemWriter.flush();
            return stringWriter.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert public key to PEM", e);
        }
    }

    /**
     * Convert certificate to PEM format
     */
    public String certificateToPEM(X509Certificate certificate) {
        try (StringWriter stringWriter = new StringWriter();
             JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {

            pemWriter.writeObject(certificate);
            pemWriter.flush();
            return stringWriter.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert certificate to PEM", e);
        }
    }

    /**
     * Parse CSR from PEM format
     */
    public PKCS10CertificationRequest parseCSR(String csrPEM) {
        try {
            // 1️⃣ Проверка на null и пустую строку
            if (csrPEM == null || csrPEM.trim().isEmpty()) {
                throw new IllegalArgumentException("CSR content is null or empty");
            }

            // 2️⃣ Нормализация CSR (удаление лишних пробелов, переносов)
            String normalizedCSR = csrPEM.trim()
                    .replaceAll("\\r\\n", "\n")  // Windows -> Unix
                    .replaceAll("\\r", "\n");     // Old Mac -> Unix

            // 3️⃣ Проверка формата PEM
            if (!normalizedCSR.contains("BEGIN CERTIFICATE REQUEST") &&
                    !normalizedCSR.contains("BEGIN NEW CERTIFICATE REQUEST")) {
                throw new IllegalArgumentException(
                        "Invalid CSR format: missing PEM header. " +
                                "Expected '-----BEGIN CERTIFICATE REQUEST-----'"
                );
            }

            // 4️⃣ Логирование для отладки
            System.out.println("=== CSR Parsing Debug ===");
            System.out.println("CSR Length: " + normalizedCSR.length());
            System.out.println("First 100 chars: " +
                    normalizedCSR.substring(0, Math.min(100, normalizedCSR.length())));
            System.out.println("Last 100 chars: " +
                    normalizedCSR.substring(Math.max(0, normalizedCSR.length() - 100)));

            // 5️⃣ Парсинг CSR
            try (StringReader stringReader = new StringReader(normalizedCSR);
                 PEMParser pemParser = new PEMParser(stringReader)) {

                Object parsedObject = pemParser.readObject();

                if (parsedObject == null) {
                    throw new IllegalArgumentException(
                            "PEMParser returned null - invalid CSR content"
                    );
                }

                if (parsedObject instanceof PKCS10CertificationRequest) {
                    PKCS10CertificationRequest csr = (PKCS10CertificationRequest) parsedObject;

                    // 6️⃣ Валидация CSR
                    csrValidator.validateCSRSignature(csr);

                    return csr;
                } else {
                    throw new IllegalArgumentException(
                            "Invalid CSR format. Expected PKCS10CertificationRequest, got: " +
                                    parsedObject.getClass().getName()
                    );
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse CSR: " + e.getMessage(), e);
        }
    }

    /**
     * Parse certificate from PEM format
     */
    public X509Certificate parseCertificate(String certificatePEM) {
        try (StringReader stringReader = new StringReader(certificatePEM);
             PEMParser pemParser = new PEMParser(stringReader)) {

            Object parsedObject = pemParser.readObject();
            if (parsedObject instanceof X509CertificateHolder) {
                X509CertificateHolder certHolder = (X509CertificateHolder) parsedObject;
                return new JcaX509CertificateConverter().getCertificate(certHolder);
            } else {
                throw new IllegalArgumentException("Invalid certificate format");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse certificate", e);
        }
    }
}
