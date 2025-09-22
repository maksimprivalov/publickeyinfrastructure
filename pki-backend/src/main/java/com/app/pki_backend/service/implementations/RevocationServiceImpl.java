package com.app.pki_backend.service.implementations;

import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.entity.certificates.CertificateStatus;
import com.app.pki_backend.entity.certificates.RevocationReason;
import com.app.pki_backend.entity.certificates.RevokedCertificate;
import com.app.pki_backend.entity.user.User;
import com.app.pki_backend.repository.CertificateRepository;
import com.app.pki_backend.repository.RevokedCertificateRepository;
import com.app.pki_backend.service.interfaces.RevocationService;
import org.bouncycastle.asn1.ocsp.OCSPResponse;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RevocationServiceImpl implements RevocationService {

    private final RevokedCertificateRepository revokedRepository;
    private final CertificateRepository certificateRepository;

    public RevocationServiceImpl(RevokedCertificateRepository revokedRepository,
                                 CertificateRepository certificateRepository) {
        this.revokedRepository = revokedRepository;
        this.certificateRepository = certificateRepository;
    }

    @Override
    public void revokeCertificate(Certificate certificate, String reason, User revokedBy) {
        certificate.setStatus(CertificateStatus.REVOKED);
        certificateRepository.save(certificate);

        RevokedCertificate revoked = new RevokedCertificate();
        revoked.setCertificate(certificate);
        revoked.setRevocationDate(LocalDateTime.now());
        revoked.setReason(RevocationReason.valueOf(reason.toUpperCase()));
        revoked.setRevokedBy(revokedBy);

        revokedRepository.save(revoked);
    }

    @Override
    public byte[] generateCRL(Certificate issuer) {
        // TODO: Реализовать через BouncyCastle (X509v2CRLGenerator)
        return ("CRL for issuer " + issuer.getSerialNumber()).getBytes();
    }

    @Override
    public OCSPResp checkRevocationStatus(String serialNumber) {
        // TODO: позже сделать нормальный OCSP
        return null;
    }

    @Override
    public List<RevokedCertificate> listRevoked() {
        return revokedRepository.findAll();
    }
}
