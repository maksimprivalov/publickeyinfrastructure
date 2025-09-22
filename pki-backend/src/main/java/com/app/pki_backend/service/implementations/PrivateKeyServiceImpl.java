package com.app.pki_backend.service.implementations;

import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.repository.CertificateRepository;
import com.app.pki_backend.service.interfaces.CryptographyService;
import com.app.pki_backend.service.interfaces.PrivateKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.security.PrivateKey;

/**
 * Implementation of PrivateKeyService for managing private keys and key encryption operations.
 */
@Service
@Transactional
public class PrivateKeyServiceImpl implements PrivateKeyService {

    @Autowired
    private CryptographyService cryptographyService;

    @Autowired
    private CertificateRepository certificateRepository;

    @Override
    public void storePrivateKey(Certificate certificate, PrivateKey privateKey, SecretKey masterKey) {
        try {
            // Encrypt private key with master key
            String encryptedPrivateKey = cryptographyService.encryptPrivateKey(privateKey, masterKey);

            // Store encrypted private key in certificate entity
            certificate.setEncryptedPrivateKey(encryptedPrivateKey);

            // Save certificate with encrypted private key
            certificateRepository.save(certificate);

        } catch (Exception e) {
            throw new RuntimeException("Failed to store private key for certificate: " + certificate.getId(), e);
        }
    }

    @Override
    public PrivateKey retrievePrivateKey(Certificate certificate, SecretKey masterKey) {
        try {
            // Check if private key exists
            if (!hasPrivateKey(certificate)) {
                throw new IllegalStateException("No private key found for certificate: " + certificate.getId());
            }

            // Get encrypted private key from certificate
            String encryptedPrivateKey = certificate.getEncryptedPrivateKey();

            // Decrypt private key using master key
            return cryptographyService.decryptPrivateKey(encryptedPrivateKey, masterKey);

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve private key for certificate: " + certificate.getId(), e);
        }
    }

    @Override
    public boolean hasPrivateKey(Certificate certificate) {
        return certificate != null &&
               certificate.getEncryptedPrivateKey() != null &&
               !certificate.getEncryptedPrivateKey().trim().isEmpty();
    }

    @Override
    public void deletePrivateKey(Certificate certificate) {
        try {
            // Remove encrypted private key from certificate
            certificate.setEncryptedPrivateKey(null);

            // Save certificate without private key
            certificateRepository.save(certificate);

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete private key for certificate: " + certificate.getId(), e);
        }
    }

    @Override
    public void reEncryptPrivateKey(Certificate certificate, SecretKey oldMasterKey, SecretKey newMasterKey) {
        try {
            // Retrieve private key with old master key
            PrivateKey privateKey = retrievePrivateKey(certificate, oldMasterKey);

            // Re-encrypt with new master key
            String newEncryptedPrivateKey = cryptographyService.encryptPrivateKey(privateKey, newMasterKey);

            // Update certificate with new encrypted private key
            certificate.setEncryptedPrivateKey(newEncryptedPrivateKey);

            // Save certificate
            certificateRepository.save(certificate);

        } catch (Exception e) {
            throw new RuntimeException("Failed to re-encrypt private key for certificate: " + certificate.getId(), e);
        }
    }
}
