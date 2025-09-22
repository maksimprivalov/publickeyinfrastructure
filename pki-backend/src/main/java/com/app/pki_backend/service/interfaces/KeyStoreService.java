package com.app.pki_backend.service.interfaces;

import com.app.pki_backend.entity.certificates.Certificate;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Service interface for managing KeyStores and certificate packaging.
 * Handles PKCS#12 and JKS keystore operations for certificate distribution.
 */
public interface KeyStoreService {

    /**
     * Create PKCS#12 keystore containing certificate and its private key.
     * @param certificate Certificate entity from database
     * @param privateKey Associated private key
     * @param keystorePassword Password to protect the keystore
     * @param keyPassword Password to protect the private key
     * @return KeyStore in PKCS#12 format
     */
    KeyStore createPKCS12KeyStore(Certificate certificate, PrivateKey privateKey,
                                  String keystorePassword, String keyPassword);

    /**
     * Create PKCS#12 keystore with full certificate chain.
     * @param certificate End entity certificate
     * @param privateKey Associated private key
     * @param certificateChain Full certificate chain (including CA certificates)
     * @param keystorePassword Password to protect the keystore
     * @param keyPassword Password to protect the private key
     * @return KeyStore in PKCS#12 format with complete chain
     */
    KeyStore createPKCS12KeyStoreWithChain(Certificate certificate, PrivateKey privateKey,
                                           List<X509Certificate> certificateChain,
                                           String keystorePassword, String keyPassword);

    /**
     * Create JKS keystore containing certificate and its private key.
     * @param certificate Certificate entity from database
     * @param privateKey Associated private key
     * @param keystorePassword Password to protect the keystore
     * @param keyPassword Password to protect the private key
     * @return KeyStore in JKS format
     */
    KeyStore createJKSKeyStore(Certificate certificate, PrivateKey privateKey,
                               String keystorePassword, String keyPassword);

    /**
     * Create truststore containing only certificates (no private keys).
     * Used for storing trusted CA certificates.
     * @param certificates List of trusted certificates
     * @param keystorePassword Password to protect the truststore
     * @return KeyStore containing only certificates
     */
    KeyStore createTrustStore(List<Certificate> certificates, String keystorePassword);

    /**
     * Export keystore as byte array for download.
     * @param keyStore KeyStore to export
     * @param password Password for the keystore
     * @return Byte array representation of keystore
     */
    byte[] exportKeyStore(KeyStore keyStore, String password);

    /**
     * Import keystore from byte array or input stream.
     * @param keystoreData Input stream containing keystore data
     * @param password Password for the keystore
     * @param keystoreType Type of keystore (PKCS12, JKS)
     * @return Loaded KeyStore
     */
    KeyStore importKeyStore(InputStream keystoreData, String password, String keystoreType);

    /**
     * Build certificate chain for a given certificate.
     * @param certificate Certificate to build chain for
     * @return List of certificates in the chain (from end-entity to root)
     */
    List<X509Certificate> buildCertificateChain(Certificate certificate);

    /**
     * Validate keystore integrity and password.
     * @param keyStore KeyStore to validate
     * @param password Password to test
     * @return true if keystore is valid and password is correct
     */
    boolean validateKeyStore(KeyStore keyStore, String password);

    /**
     * Get certificate alias for keystore entry.
     * @param certificate Certificate entity
     * @return Alias string for the certificate
     */
    String generateCertificateAlias(Certificate certificate);
}
