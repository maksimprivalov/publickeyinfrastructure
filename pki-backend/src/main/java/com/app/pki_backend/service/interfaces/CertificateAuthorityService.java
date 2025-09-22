package com.app.pki_backend.service.interfaces;

import com.app.pki_backend.dto.certificate.CertificateSigningRequest;
import com.app.pki_backend.entity.certificates.Certificate;

/**
 * Service interface for managing Certificate Authorities.
 * Handles CA hierarchy, CA user management, and CA-specific operations.
 */

public interface CertificateAuthorityService {

    /**
     * Validate certificate chain.
     * @param certificate
     * @return true if valid, false otherwise
     */
    boolean validateCertificateChain(Certificate certificate);

    /**
     * Check if the certificate is valid.
     * @param certificate
     * @return true if valid, false otherwise
     */
    boolean isValid(Certificate certificate);

    /**
     * Check if the certificate is revoked.
     * @param certificate
     * @return true if revoked, false otherwise
     */
    boolean isRevoked(Certificate certificate);

    /**
     * Verify certificate signature.
     * @param certificate
     * @param issuer
     * @return true if valid, false otherwise
     */
    boolean verifySignature(Certificate certificate, Certificate issuer);

}
