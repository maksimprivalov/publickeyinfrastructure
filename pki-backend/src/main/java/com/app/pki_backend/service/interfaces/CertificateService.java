package com.app.pki_backend.service.interfaces;

import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.entity.user.User;
import org.bouncycastle.cert.X509v3CertificateBuilder;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for certificate business logic and lifecycle management.
 * Handles high-level certificate operations and business rules.
 */

public interface CertificateService {

    /**
     * Create root certificate for CA
     * @return Root certificate
     */
    X509Certificate createRootCertificate();


}
