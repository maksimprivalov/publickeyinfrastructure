package com.app.pki_backend.service.implementations;

import com.app.pki_backend.service.interfaces.CryptographyService;
import com.app.pki_backend.service.interfaces.MasterKeyService;
import com.app.pki_backend.service.interfaces.PrivateKeyService;
import com.app.pki_backend.repository.CertificateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of MasterKeyService for managing master keys.
 * In production, this should integrate with HSM or secure key vault.
 */
@Service
public class MasterKeyServiceImpl implements MasterKeyService {

    @Autowired
    private CryptographyService cryptographyService;

    @Autowired
    private PrivateKeyService privateKeyService;

    @Autowired
    private CertificateRepository certificateRepository;

    @Value("${pki.master-key.current-id:default}")
    private String currentMasterKeyId;

    // In-memory storage for demo purposes
    // In production, use HSM or secure key vault
    private final Map<String, SecretKey> masterKeyStorage = new ConcurrentHashMap<>();
    private SecretKey currentMasterKey;

    @Override
    public SecretKey generateMasterKey() {
        return cryptographyService.generateAESKey();
    }

    @Override
    public SecretKey getCurrentMasterKey() {
        if (currentMasterKey == null) {
            initializeMasterKey();
        }
        return currentMasterKey;
    }

    @Override
    public void storeMasterKey(SecretKey masterKey, String keyId) {
        // In production, this should store in HSM or secure key vault
        masterKeyStorage.put(keyId, masterKey);

        if (keyId.equals(currentMasterKeyId)) {
            currentMasterKey = masterKey;
        }
    }

    @Override
    public SecretKey retrieveMasterKey(String keyId) {
        SecretKey key = masterKeyStorage.get(keyId);
        if (key == null) {
            throw new IllegalArgumentException("Master key not found for ID: " + keyId);
        }
        return key;
    }

    @Override
    public SecretKey rotateMasterKey() {
        try {
            // Generate new master key
            SecretKey newMasterKey = generateMasterKey();
            String newKeyId = "master-key-" + System.currentTimeMillis();

            // Get old master key
            SecretKey oldMasterKey = getCurrentMasterKey();

            // Re-encrypt all private keys with new master key
            certificateRepository.findAll().forEach(certificate -> {
                if (privateKeyService.hasPrivateKey(certificate)) {
                    privateKeyService.reEncryptPrivateKey(certificate, oldMasterKey, newMasterKey);
                }
            });

            // Store new master key
            storeMasterKey(newMasterKey, newKeyId);
            currentMasterKeyId = newKeyId;
            currentMasterKey = newMasterKey;

            return newMasterKey;

        } catch (Exception e) {
            throw new RuntimeException("Failed to rotate master key", e);
        }
    }

    @Override
    public boolean isMasterKeyAvailable() {
        try {
            return getCurrentMasterKey() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Initialize master key on startup.
     * In production, this should retrieve from secure storage.
     */
    private void initializeMasterKey() {
        try {
            // Try to retrieve existing master key
            if (masterKeyStorage.containsKey(currentMasterKeyId)) {
                currentMasterKey = masterKeyStorage.get(currentMasterKeyId);
            } else {
                // Generate new master key if none exists
                currentMasterKey = generateMasterKey();
                storeMasterKey(currentMasterKey, currentMasterKeyId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize master key", e);
        }
    }
}
