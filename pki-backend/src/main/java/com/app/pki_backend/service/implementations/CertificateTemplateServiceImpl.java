package com.app.pki_backend.service.implementations;

import com.app.pki_backend.dto.certificate.CertificateSigningRequest;
import com.app.pki_backend.entity.certificates.CertificateTemplate;
import com.app.pki_backend.service.interfaces.CertificateTemplateService;
import org.springframework.stereotype.Service;

@Service
public class CertificateTemplateServiceImpl implements CertificateTemplateService {

    @Override
    public CertificateSigningRequest applyTemplate(CertificateSigningRequest csr, CertificateTemplate template) {
        return null;
    }

    @Override
    public boolean validateAgainstTemplate(CertificateSigningRequest csr, CertificateTemplate template) {
        return false;
    }
}
