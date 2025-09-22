package com.app.pki_backend.service.implementations;

import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.service.interfaces.RevocationService;
import org.bouncycastle.asn1.ocsp.OCSPResponse;
import org.springframework.stereotype.Service;

@Service
public class RevocationServiceImpl implements RevocationService {

    @Override
    public void revokeCertificate(Certificate certificate, String reason) {

    }

    @Override
    public byte[] generateCRL(Certificate issuer) {
        return new byte[0];
    }

    @Override
    public OCSPResponse checkRevocationStatus(String serialNumber) {
        return null;
    }
}
