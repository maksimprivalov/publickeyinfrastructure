package com.app.pki_backend.util;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
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
        try (StringReader stringReader = new StringReader(csrPEM);
             PEMParser pemParser = new PEMParser(stringReader)) {

            Object parsedObject = pemParser.readObject();
            if (parsedObject instanceof PKCS10CertificationRequest) {
                return (PKCS10CertificationRequest) parsedObject;
            } else {
                throw new IllegalArgumentException("Invalid CSR format");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse CSR", e);
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
