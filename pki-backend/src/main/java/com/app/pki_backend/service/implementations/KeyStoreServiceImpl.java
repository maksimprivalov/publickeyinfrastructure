package com.app.pki_backend.service.implementations;

import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.service.interfaces.KeyStoreService;
import com.app.pki_backend.service.interfaces.PrivateKeyService;
import com.app.pki_backend.service.interfaces.MasterKeyService;
import com.app.pki_backend.util.PEMConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of KeyStoreService for managing KeyStores and certificate packaging.
 */
@Service
public class KeyStoreServiceImpl implements KeyStoreService {

    @Autowired
    private PrivateKeyService privateKeyService;

    @Autowired
    private MasterKeyService masterKeyService;

    @Autowired
    private PEMConverter pemConverter;

    private static final String PKCS12_TYPE = "PKCS12";
    private static final String JKS_TYPE = "JKS";

    @Override
    public KeyStore createPKCS12KeyStore(Certificate certificate, PrivateKey privateKey,
                                         String keystorePassword, String keyPassword) {
        try {
            // Create new PKCS#12 keystore
            KeyStore keyStore = KeyStore.getInstance(PKCS12_TYPE);
            keyStore.load(null, null); // Initialize empty keystore

            // Convert certificate to X509Certificate
            X509Certificate x509Cert = pemConverter.parseCertificate(certificate.getCertificateData());

            // Generate alias for the certificate
            String alias = generateCertificateAlias(certificate);

            // Create certificate chain with single certificate
            java.security.cert.Certificate[] certChain = {x509Cert};

            // Store private key and certificate in keystore
            keyStore.setKeyEntry(alias, privateKey, keyPassword.toCharArray(), certChain);

            return keyStore;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create PKCS#12 keystore for certificate: " + certificate.getId(), e);
        }
    }

    @Override
    public KeyStore createPKCS12KeyStoreWithChain(Certificate certificate, PrivateKey privateKey,
                                                  List<X509Certificate> certificateChain,
                                                  String keystorePassword, String keyPassword) {
        try {
            // Create new PKCS#12 keystore
            KeyStore keyStore = KeyStore.getInstance(PKCS12_TYPE);
            keyStore.load(null, null);

            // Generate alias for the end-entity certificate
            String alias = generateCertificateAlias(certificate);

            // Convert list to array for keystore
            java.security.cert.Certificate[] certChainArray =
                certificateChain.toArray(new java.security.cert.Certificate[0]);

            // Store private key and full certificate chain
            keyStore.setKeyEntry(alias, privateKey, keyPassword.toCharArray(), certChainArray);

            // Also store each CA certificate as trusted certificate
            for (int i = 1; i < certificateChain.size(); i++) {
                X509Certificate caCert = certificateChain.get(i);
                String caAlias = "ca-" + i + "-" + extractCommonName(caCert.getSubjectX500Principal().getName());
                keyStore.setCertificateEntry(caAlias, caCert);
            }

            return keyStore;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create PKCS#12 keystore with chain for certificate: " + certificate.getId(), e);
        }
    }

    @Override
    public KeyStore createJKSKeyStore(Certificate certificate, PrivateKey privateKey,
                                      String keystorePassword, String keyPassword) {
        try {
            // Create new JKS keystore
            KeyStore keyStore = KeyStore.getInstance(JKS_TYPE);
            keyStore.load(null, null);

            // Convert certificate to X509Certificate
            X509Certificate x509Cert = pemConverter.parseCertificate(certificate.getCertificateData());

            // Generate alias for the certificate
            String alias = generateCertificateAlias(certificate);

            // Create certificate chain
            java.security.cert.Certificate[] certChain = {x509Cert};

            // Store private key and certificate
            keyStore.setKeyEntry(alias, privateKey, keyPassword.toCharArray(), certChain);

            return keyStore;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create JKS keystore for certificate: " + certificate.getId(), e);
        }
    }

    @Override
    public KeyStore createTrustStore(List<Certificate> certificates, String keystorePassword) {
        try {
            // Create new PKCS#12 truststore (only certificates, no private keys)
            KeyStore trustStore = KeyStore.getInstance(PKCS12_TYPE);
            trustStore.load(null, null);

            // Add each certificate as trusted certificate
            for (Certificate cert : certificates) {
                X509Certificate x509Cert = pemConverter.parseCertificate(cert.getCertificateData());
                String alias = generateCertificateAlias(cert);
                trustStore.setCertificateEntry(alias, x509Cert);
            }

            return trustStore;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create truststore", e);
        }
    }

    @Override
    public byte[] exportKeyStore(KeyStore keyStore, String password) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            keyStore.store(outputStream, password.toCharArray());
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to export keystore", e);
        }
    }

    @Override
    public KeyStore importKeyStore(InputStream keystoreData, String password, String keystoreType) {
        try {
            KeyStore keyStore = KeyStore.getInstance(keystoreType);
            keyStore.load(keystoreData, password.toCharArray());
            return keyStore;

        } catch (Exception e) {
            throw new RuntimeException("Failed to import keystore", e);
        }
    }

    @Override
    public List<X509Certificate> buildCertificateChain(Certificate certificate) {
        try {
            List<X509Certificate> chain = new ArrayList<>();

            // Start with the given certificate
            Certificate currentCert = certificate;

            while (currentCert != null) {
                // Convert to X509Certificate and add to chain
                X509Certificate x509Cert = pemConverter.parseCertificate(currentCert.getCertificateData());
                chain.add(x509Cert);

                // Move to issuer certificate
                currentCert = currentCert.getIssuerCertificate();

                // Break if this is a self-signed root certificate
                if (currentCert != null && currentCert.getId().equals(certificate.getId())) {
                    break;
                }
            }

            return chain;

        } catch (Exception e) {
            throw new RuntimeException("Failed to build certificate chain for certificate: " + certificate.getId(), e);
        }
    }

    @Override
    public boolean validateKeyStore(KeyStore keyStore, String password) {
        try {
            // Try to load the keystore with the given password
            keyStore.load(null, password.toCharArray());

            // Check if keystore has at least one entry
            return keyStore.size() > 0;

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String generateCertificateAlias(Certificate certificate) {
        try {
            // Extract Common Name from certificate subject
            String subject = certificate.getSubject();
            String commonName = extractCommonName(subject);

            // Create alias: type-commonname-serialnumber
            String type = certificate.getType().toString().toLowerCase().replace("_", "-");
            String serialHex = certificate.getSerialNumber().toString(16);

            return String.format("%s-%s-%s", type, commonName, serialHex);

        } catch (Exception e) {
            // Fallback alias if extraction fails
            return "cert-" + certificate.getId();
        }
    }

    /**
     * Helper method to extract Common Name from Distinguished Name.
     */
    private String extractCommonName(String distinguishedName) {
        try {
            if (distinguishedName.contains("CN=")) {
                String[] parts = distinguishedName.split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("CN=")) {
                        return part.substring(3).replaceAll("[^a-zA-Z0-9]", "");
                    }
                }
            }
            return "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Create keystore for certificate download with automatic private key retrieval.
     */
    public KeyStore createDownloadKeyStore(Certificate certificate, String keystorePassword,
                                          String keyPassword, String format) {
        try {
            // Retrieve private key using master key
            SecretKey masterKey = masterKeyService.getCurrentMasterKey();
            PrivateKey privateKey = privateKeyService.retrievePrivateKey(certificate, masterKey);

            // Build full certificate chain
            List<X509Certificate> certificateChain = buildCertificateChain(certificate);

            // Create keystore based on requested format
            if (PKCS12_TYPE.equalsIgnoreCase(format)) {
                return createPKCS12KeyStoreWithChain(certificate, privateKey, certificateChain,
                                                   keystorePassword, keyPassword);
            } else if (JKS_TYPE.equalsIgnoreCase(format)) {
                return createJKSKeyStore(certificate, privateKey, keystorePassword, keyPassword);
            } else {
                throw new IllegalArgumentException("Unsupported keystore format: " + format);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to create download keystore for certificate: " + certificate.getId(), e);
        }
    }

    /**
     * Create CA truststore containing all CA certificates in the system.
     */
    public KeyStore createCATrustStore(List<Certificate> caCertificates, String password) {
        // Filter only CA certificates (ROOT_CA and INTERMEDIATE_CA)
        List<Certificate> filteredCerts = caCertificates.stream()
                .filter(cert -> cert.getType().name().contains("CA"))
                .toList();

        return createTrustStore(filteredCerts, password);
    }
}
