package com.app.pki_backend.service.interfaces;

import javax.crypto.SecretKey;

/**
 * Service interface for managing master keys used for private key encryption.
 * Handles master key generation, storage, and retrieval.
 */
public interface MasterKeyService {

    /**
     * Generate a new master key for private key encryption.
     * @return Generated master key
     */
    SecretKey generateMasterKey();

    /**
     * Get the current active master key.
     * @return Current master key
     */
    SecretKey getCurrentMasterKey();

    /**
     * Store master key securely (in HSM, key vault, or secure storage).
     * @param masterKey Master key to store
     * @param keyId Unique identifier for the key
     */
    void storeMasterKey(SecretKey masterKey, String keyId);

    /**
     * Retrieve master key by ID.
     * @param keyId Key identifier
     * @return Retrieved master key
     */
    SecretKey retrieveMasterKey(String keyId);

    /**
     * Rotate master key (generate new key and update all encrypted private keys).
     * @return New master key
     */
    SecretKey rotateMasterKey();

    /**
     * Check if master key exists and is accessible.
     * @return true if master key is available, false otherwise
     */
    boolean isMasterKeyAvailable();
}
