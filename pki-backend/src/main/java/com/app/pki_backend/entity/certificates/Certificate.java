package com.app.pki_backend.entity;

import jakarta.persistence.*;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "certificates")
public class Certificate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private BigInteger serialNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String subject; // X500Name в строковом формате

    @Column(nullable = false, columnDefinition = "TEXT")
    private String issuer; // X500Name в строковом формате

    @Column(nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Column(columnDefinition = "TEXT")
    private String encryptedPrivateKey; // Зашифрованный приватный ключ

    @Column(nullable = false, columnDefinition = "TEXT")
    private String certificateData; // PEM формат сертификата

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificateType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificateStatus status = CertificateStatus.ACTIVE;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne
    @JoinColumn(name = "issuer_certificate_id")
    private Certificate issuerCertificate; // Ссылка на сертификат-издатель

    @OneToMany(mappedBy = "issuerCertificate", cascade = CascadeType.ALL)
    private List<Certificate> issuedCertificates; // Сертификаты, выпущенные этим CA

    @Column(columnDefinition = "TEXT")
    private String extensions; // JSON строка с расширениями

    @Column(nullable = false)
    private String organization;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Конструкторы
    public Certificate() {}

    // Getters и Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigInteger getSerialNumber() { return serialNumber; }
    public void setSerialNumber(BigInteger serialNumber) { this.serialNumber = serialNumber; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public String getEncryptedPrivateKey() { return encryptedPrivateKey; }
    public void setEncryptedPrivateKey(String encryptedPrivateKey) { this.encryptedPrivateKey = encryptedPrivateKey; }

    public String getCertificateData() { return certificateData; }
    public void setCertificateData(String certificateData) { this.certificateData = certificateData; }

    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }

    public LocalDateTime getValidTo() { return validTo; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }

    public CertificateType getType() { return type; }
    public void setType(CertificateType type) { this.type = type; }

    public CertificateStatus getStatus() { return status; }
    public void setStatus(CertificateStatus status) { this.status = status; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public Certificate getIssuerCertificate() { return issuerCertificate; }
    public void setIssuerCertificate(Certificate issuerCertificate) { this.issuerCertificate = issuerCertificate; }

    public List<Certificate> getIssuedCertificates() { return issuedCertificates; }
    public void setIssuedCertificates(List<Certificate> issuedCertificates) { this.issuedCertificates = issuedCertificates; }

    public String getExtensions() { return extensions; }
    public void setExtensions(String extensions) { this.extensions = extensions; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

enum CertificateType {
    ROOT_CA,
    INTERMEDIATE_CA,
    END_ENTITY
}

enum CertificateStatus {
    ACTIVE,
    REVOKED,
    EXPIRED
}
