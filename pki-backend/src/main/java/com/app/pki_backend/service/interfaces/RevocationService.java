package com.app.pki_backend.service.interfaces;

import com.app.pki_backend.entity.certificates.Certificate;
import org.bouncycastle.asn1.ocsp.OCSPResponse;

/**
 * Service interface for certificate revocation management.
 * Handles certificate revocation, CRL generation, and OCSP responses.
 */

public interface RevocationService {

    /**
     * Revoke certificate.
     * @param certificate
     * @param reason
     */
    void revokeCertificate(Certificate certificate, String reason);

    /**
     * Generate CRL for issuer certificate.
     * @param issuer
     * @return
     */
    byte[] generateCRL(Certificate issuer);

    /**
     * Check revocation status of certificate.
     * @param serialNumber
     * @return
     */
    OCSPResponse checkRevocationStatus(String serialNumber);
}
