package com.app.pki_backend.service.implementations;

import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.service.interfaces.CertificateAuthorityService;
import org.springframework.stereotype.Service;

@Service
public class CertificateAuthorityServiceImpl implements CertificateAuthorityService {

    @Override
    public boolean validateCertificateChain(Certificate certificate) {
        return false;
    }

    @Override
    public boolean isValid(Certificate certificate) {
        return false;
    }

    @Override
    public boolean isRevoked(Certificate certificate) {
        return false;
    }

    @Override
    public boolean verifySignature(Certificate certificate, Certificate issuer) {
        return false;
    }
}
