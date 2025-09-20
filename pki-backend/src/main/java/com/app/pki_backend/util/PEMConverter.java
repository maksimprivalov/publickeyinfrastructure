package com.app.pki_backend.util;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.StringReader;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509Certificate;

/**
 * Utility class for PEM format conversions
 */
public class PEMConverter {

    static {
        // Ensure BouncyCastle provider is loaded
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Convert X509Certificate to PEM format
     */
    public static String certificateToPem(X509Certificate certificate) {
        try {
            StringWriter sw = new StringWriter();
            try (JcaPEMWriter pemWriter = new JcaPEMWriter(sw)) {
                pemWriter.writeObject(certificate);
            }
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert certificate to PEM", e);
        }
    }

    /**
     * Convert PEM string to X509Certificate
     */
    public static X509Certificate pemToCertificate(String pemData) {
        try {
            StringReader sr = new StringReader(pemData);
            try (PEMParser pemParser = new PEMParser(sr)) {
                Object pemObject = pemParser.readObject();

                if (pemObject instanceof X509CertificateHolder) {
                    return new JcaX509CertificateConverter()
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .getCertificate((X509CertificateHolder) pemObject);
                } else {
                    throw new IllegalArgumentException("Invalid PEM certificate format");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse PEM certificate", e);
        }
    }

    /**
     * Convert PrivateKey to PEM format
     */
    public static String privateKeyToPem(PrivateKey privateKey) {
        try {
            StringWriter sw = new StringWriter();
            try (JcaPEMWriter pemWriter = new JcaPEMWriter(sw)) {
                pemWriter.writeObject(privateKey);
            }
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert private key to PEM", e);
        }
    }

    /**
     * Convert PEM string to PrivateKey
     */
    public static PrivateKey pemToPrivateKey(String pemData) {
        try {
            StringReader sr = new StringReader(pemData);
            try (PEMParser pemParser = new PEMParser(sr)) {
                Object pemObject = pemParser.readObject();

                JcaPEMKeyConverter converter = new JcaPEMKeyConverter()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME);

                if (pemObject instanceof org.bouncycastle.openssl.PEMKeyPair) {
                    org.bouncycastle.openssl.PEMKeyPair keyPair =
                        (org.bouncycastle.openssl.PEMKeyPair) pemObject;
                    return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
                } else if (pemObject instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo) {
                    return converter.getPrivateKey(
                        (org.bouncycastle.asn1.pkcs.PrivateKeyInfo) pemObject);
                } else {
                    throw new IllegalArgumentException("Invalid PEM private key format");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse PEM private key", e);
        }
    }

    /**
     * Convert PublicKey to PEM format
     */
    public static String publicKeyToPem(PublicKey publicKey) {
        try {
            StringWriter sw = new StringWriter();
            try (JcaPEMWriter pemWriter = new JcaPEMWriter(sw)) {
                pemWriter.writeObject(publicKey);
            }
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert public key to PEM", e);
        }
    }

    /**
     * Convert PEM string to PublicKey
     */
    public static PublicKey pemToPublicKey(String pemData) {
        try {
            StringReader sr = new StringReader(pemData);
            try (PEMParser pemParser = new PEMParser(sr)) {
                Object pemObject = pemParser.readObject();

                JcaPEMKeyConverter converter = new JcaPEMKeyConverter()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME);

                if (pemObject instanceof org.bouncycastle.asn1.x509.SubjectPublicKeyInfo) {
                    return converter.getPublicKey(
                        (org.bouncycastle.asn1.x509.SubjectPublicKeyInfo) pemObject);
                } else if (pemObject instanceof org.bouncycastle.openssl.PEMKeyPair) {
                    org.bouncycastle.openssl.PEMKeyPair keyPair =
                        (org.bouncycastle.openssl.PEMKeyPair) pemObject;
                    return converter.getPublicKey(keyPair.getPublicKeyInfo());
                } else {
                    throw new IllegalArgumentException("Invalid PEM public key format");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse PEM public key", e);
        }
    }

    /**
     * Validate PEM format (basic check)
     */
    public static boolean isValidPemFormat(String pemData) {
        if (pemData == null || pemData.trim().isEmpty()) {
            return false;
        }

        String trimmed = pemData.trim();
        return trimmed.startsWith("-----BEGIN") && trimmed.endsWith("-----");
    }

    /**
     * Extract PEM type from header (e.g., "CERTIFICATE", "PRIVATE KEY", etc.)
     */
    public static String extractPemType(String pemData) {
        if (!isValidPemFormat(pemData)) {
            throw new IllegalArgumentException("Invalid PEM format");
        }

        String firstLine = pemData.trim().split("\n")[0];
        if (firstLine.startsWith("-----BEGIN ") && firstLine.endsWith("-----")) {
            return firstLine.substring(11, firstLine.length() - 5);
        }

        throw new IllegalArgumentException("Cannot extract PEM type");
    }
}
