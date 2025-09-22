package com.app.pki_backend.service.interfaces;

import com.app.pki_backend.dto.certificate.CertificateSigningRequest;
import com.app.pki_backend.entity.certificates.CertificateTemplate;

/**
 * Service interface for certificate template management.
 * Handles template creation, validation, and application to certificates.
 */

public interface CertificateTemplateService {

    /**
     * Apply template to CSR.
     * @param csr
     * @param template
     * @return CertificateSigningRequest with applied template.
     */
    CertificateSigningRequest applyTemplate(CertificateSigningRequest csr, CertificateTemplate template);

    /**
     * Validate CSR against template.
     * @param csr
     * @param template
     * @return true if valid, false otherwise
     */
    boolean validateAgainstTemplate(CertificateSigningRequest csr, CertificateTemplate template);

}
