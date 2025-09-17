package com.app.pki_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "revoked_certificates")
public class RevokedCertificate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "certificate_id", nullable = false)
    private Certificate certificate;

    @Column(nullable = false)
    private LocalDateTime revocationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RevocationReason reason;

    @ManyToOne
    @JoinColumn(name = "revoked_by", nullable = false)
    private User revokedBy;

    // Конструкторы
    public RevokedCertificate() {}

    // Getters и Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Certificate getCertificate() { return certificate; }
    public void setCertificate(Certificate certificate) { this.certificate = certificate; }

    public LocalDateTime getRevocationDate() { return revocationDate; }
    public void setRevocationDate(LocalDateTime revocationDate) { this.revocationDate = revocationDate; }

    public RevocationReason getReason() { return reason; }
    public void setReason(RevocationReason reason) { this.reason = reason; }

    public User getRevokedBy() { return revokedBy; }
    public void setRevokedBy(User revokedBy) { this.revokedBy = revokedBy; }
}

enum RevocationReason {
    UNSPECIFIED,
    KEY_COMPROMISE,
    CA_COMPROMISE,
    AFFILIATION_CHANGED,
    SUPERSEDED,
    CESSATION_OF_OPERATION,
    CERTIFICATE_HOLD,
    REMOVE_FROM_CRL,
    PRIVILEGE_WITHDRAWN,
    AA_COMPROMISE
}
