package com.app.pki_backend.service.interfaces;

import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.entity.certificates.RevokedCertificate;
import com.app.pki_backend.entity.user.User;
import org.bouncycastle.asn1.ocsp.OCSPResponse;
import org.bouncycastle.cert.ocsp.OCSPResp;

import java.util.List;

/**
 * Service interface for certificate revocation management.
 * Handles certificate revocation, CRL generation, and OCSP responses.
 */

public interface RevocationService {
    void revokeCertificate(Certificate certificate, String reason, User revokedBy);

    byte[] generateCRL(Certificate issuer);

    OCSPResp checkRevocationStatus(String serialNumber);

    List<RevokedCertificate> listRevoked();
}
