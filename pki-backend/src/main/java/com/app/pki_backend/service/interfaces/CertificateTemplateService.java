package com.app.pki_backend.service.interfaces;

import com.app.pki_backend.entity.certificates.CertificateSigningRequest;
import com.app.pki_backend.entity.certificates.CertificateTemplate;

/**
 * Service interface for certificate template management.
 * Handles template creation, validation, and application to certificates.
 */

public interface CertificateTemplateService {

    CertificateTemplate findById(Long id);
    CertificateTemplate createTemplate(CertificateTemplate template);
    java.util.List<CertificateTemplate> getAllTemplates();
    void deleteTemplate(Integer id);

    CertificateSigningRequest applyTemplate(CertificateSigningRequest csr, CertificateTemplate template);

    boolean validateAgainstTemplate(CertificateSigningRequest csr, CertificateTemplate template);
}
