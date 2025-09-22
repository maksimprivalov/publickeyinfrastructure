package com.app.pki_backend.service.interfaces;

import com.app.pki_backend.entity.certificates.Certificate;

import javax.crypto.SecretKey;
import java.security.PrivateKey;

/**
 * Service interface for managing private keys and key encryption operations.
 * Handles secure storage, retrieval, and decryption of private keys.
 */
public interface PrivateKeyService {

    /**
     * Store encrypted private key for a certificate.
     * @param certificate Certificate entity to store private key for
     * @param privateKey Private key to encrypt and store
     * @param masterKey Master key for encryption
     */
    void storePrivateKey(Certificate certificate, PrivateKey privateKey, SecretKey masterKey);

    /**
     * Retrieve and decrypt private key for a certificate.
     * @param certificate Certificate entity to retrieve private key for
     * @param masterKey Master key for decryption
     * @return Decrypted private key
     */
    PrivateKey retrievePrivateKey(Certificate certificate, SecretKey masterKey);

    /**
     * Check if private key exists for a certificate.
     * @param certificate Certificate to check
     * @return true if private key exists, false otherwise
     */
    boolean hasPrivateKey(Certificate certificate);

    /**
     * Delete private key for a certificate (for security or revocation).
     * @param certificate Certificate to delete private key for
     */
    void deletePrivateKey(Certificate certificate);

    /**
     * Re-encrypt private key with new master key (for key rotation).
     * @param certificate Certificate to re-encrypt private key for
     * @param oldMasterKey Old master key
     * @param newMasterKey New master key
     */
    void reEncryptPrivateKey(Certificate certificate, SecretKey oldMasterKey, SecretKey newMasterKey);
}
