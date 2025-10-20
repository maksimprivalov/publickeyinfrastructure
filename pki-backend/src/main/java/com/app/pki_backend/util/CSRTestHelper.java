package com.app.pki_backend.util;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

/**
 * Helper utility for generating CSR in tests
 */
public class CSRTestHelper {

    static {
        // Ensure BouncyCastle is registered for tests
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Generate a key pair for testing
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    /**
     * Generate CSR with basic subject
     */
    public static String generateCSR(String commonName, String organization, String country)
            throws NoSuchAlgorithmException, OperatorCreationException, IOException {

        KeyPair keyPair = generateKeyPair();

        // Build subject DN
        X500Name subject = new X500Name(
                "CN=" + commonName +
                        ", O=" + organization +
                        ", C=" + country
        );

        // Build CSR
        PKCS10CertificationRequestBuilder csrBuilder =
                new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());

        // Sign CSR with private key
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                .build(keyPair.getPrivate());

        PKCS10CertificationRequest csr = csrBuilder.build(signer);

        // Convert to PEM format
        return toPEM(csr);
    }

    /**
     * Generate CSR for Intermediate CA with proper extensions
     */
    public static String generateIntermediateCACSR(String commonName, String organization)
            throws Exception {

        KeyPair keyPair = generateKeyPair();

        X500Name subject = new X500Name(
                "CN=" + commonName +
                        ", O=" + organization +
                        ", C=RS"
        );

        PKCS10CertificationRequestBuilder csrBuilder =
                new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());

        // No extensions needed for CSR - they will be added by CA

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                .build(keyPair.getPrivate());

        return toPEM(csrBuilder.build(signer));
    }

    /**
     * Generate CSR for End-Entity certificate
     */
    public static String generateEndEntityCSR(String commonName, String organization, String email)
            throws Exception {

        KeyPair keyPair = generateKeyPair();

        X500Name subject = new X500Name(
                "CN=" + commonName +
                        ", O=" + organization +
                        ", E=" + email +
                        ", C=RS"
        );

        PKCS10CertificationRequestBuilder csrBuilder =
                new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                .build(keyPair.getPrivate());

        return toPEM(csrBuilder.build(signer));
    }

    /**
     * Convert CSR to PEM format string
     */
    private static String toPEM(PKCS10CertificationRequest csr) throws IOException {
        StringWriter sw = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(sw)) {
            pemWriter.writeObject(csr);
            pemWriter.flush();
        }
        return sw.toString();
    }
}