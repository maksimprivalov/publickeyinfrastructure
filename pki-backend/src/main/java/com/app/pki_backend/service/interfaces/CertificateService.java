package com.app.pki_backend.service.interfaces;

import com.app.pki_backend.dto.certificate.CertificateSigningRequest;
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
    Certificate issueRootCertificate();

    /**
     * Create intermediate certificate for CA
     * @param csr
     * @param issuer
     * @return
     */
    Certificate issueIntermediateCertificate(CertificateSigningRequest csr, Certificate issuer);

    /**
     * Create end-entity certificate for user or device
     * @param csr
     * @param issuer
     * @return
     */
    Certificate issueEndEntityCertificate(CertificateSigningRequest csr, Certificate issuer);

    /**
     * Issue certificate from CSR data and issuer certificate.
     * @param csrData
     * @param issuer
     * @return
     */
    Certificate issueCertificateFromCSR(byte[] csrData, Certificate issuer);
    List<Certificate> findAll();
    Optional<Certificate> findById(Long id);
    void delete(Long id);
    Certificate issueRootWithTemplate(Long templateId);
    Certificate issueIntermediateWithTemplate(Long templateId, CertificateSigningRequest csr);
    Certificate issueEndEntityWithTemplate(Long templateId, CertificateSigningRequest csr);
    List<Certificate> findAllByOrganization(String organizationName);
    List<Certificate> findAllByOwnerId(Integer ownerId);
}
