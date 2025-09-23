package com.app.pki_backend.service.interfaces;

import com.app.pki_backend.dto.certificate.CertificateSigningRequest;
import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.entity.certificates.CertificateStatus;
import com.app.pki_backend.entity.certificates.CertificateType;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;
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

    Certificate issueServerCertificate(String serverName, Certificate issuer);

    List<Certificate> findAll();
    Optional<Certificate> findById(Long id);
    void delete(Long id);
    Certificate issueRootWithTemplate(Long templateId);
    Certificate issueIntermediateWithTemplate(Long templateId, CertificateSigningRequest csr);
    Certificate issueEndEntityWithTemplate(Long templateId, CertificateSigningRequest csr);
    List<Certificate> findAllByOrganization(String organizationName);
    List<Certificate> findAllByOwnerId(Integer ownerId);
    byte[] exportAsPkcs12(Long certId, String password);

    Page<Certificate> search(CertificateStatus status, CertificateType type, String organization, Pageable pageable);

//    Optional<User> findById(Integer userId);

//    User findUserByEmail(String email);
}
