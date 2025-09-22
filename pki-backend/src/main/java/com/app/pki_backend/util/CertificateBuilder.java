package com.app.pki_backend.util;

import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.entity.certificates.CertificateStatus;
import com.app.pki_backend.entity.certificates.CertificateType;
import com.app.pki_backend.entity.user.User;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;

/**
 * Builder pattern for creating Certificate entities
 */
public class CertificateBuilder {

    private final Certificate certificate;

    public CertificateBuilder() {
        this.certificate = new Certificate();
    }

    public static CertificateBuilder create() {
        return new CertificateBuilder();
    }

    public CertificateBuilder serialNumber(BigInteger serialNumber) {
        this.certificate.setSerialNumber(serialNumber);
        return this;
    }

    public CertificateBuilder subject(String subject) {
        this.certificate.setSubject(subject);
        return this;
    }

    public CertificateBuilder issuer(String issuer) {
        this.certificate.setIssuer(issuer);
        return this;
    }

    public CertificateBuilder publicKey(String publicKey) {
        this.certificate.setPublicKey(publicKey);
        return this;
    }

    public CertificateBuilder encryptedPrivateKey(String encryptedPrivateKey) {
        this.certificate.setEncryptedPrivateKey(encryptedPrivateKey);
        return this;
    }

    public CertificateBuilder certificateData(String certificateData) {
        this.certificate.setCertificateData(certificateData);
        return this;
    }

    public CertificateBuilder validFrom(LocalDateTime validFrom) {
        this.certificate.setValidFrom(validFrom);
        return this;
    }

    public CertificateBuilder validTo(LocalDateTime validTo) {
        this.certificate.setValidTo(validTo);
        return this;
    }

    public CertificateBuilder type(CertificateType type) {
        this.certificate.setType(type);
        return this;
    }

    public CertificateBuilder status(CertificateStatus status) {
        this.certificate.setStatus(status);
        return this;
    }

    public CertificateBuilder organization(String organization) {
        this.certificate.setOrganization(organization);
        return this;
    }

    public CertificateBuilder issuerCertificate(Certificate issuerCertificate) {
        this.certificate.setIssuerCertificate(issuerCertificate);
        return this;
    }

    public CertificateBuilder owner(User owner) {
        this.certificate.setOwner(owner);
        return this;
    }

    public CertificateBuilder fromX509Certificate(X509Certificate x509Cert, PEMConverter pemConverter, PublicKey publicKey) {
        this.certificate.setSerialNumber(new BigInteger(x509Cert.getSerialNumber().toString()));
        this.certificate.setSubject(x509Cert.getSubjectX500Principal().getName());
        this.certificate.setIssuer(x509Cert.getIssuerX500Principal().getName());
        this.certificate.setPublicKey(pemConverter.publicKeyToPEM(publicKey));
        this.certificate.setCertificateData(pemConverter.certificateToPEM(x509Cert));
        return this;
    }

    public CertificateBuilder validityPeriod(int years) {
        LocalDateTime now = LocalDateTime.now();
        this.certificate.setValidFrom(now);
        this.certificate.setValidTo(now.plusYears(years));
        return this;
    }

    public Certificate build() {
        return this.certificate;
    }
}