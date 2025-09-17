package com.app.pki_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificate_signing_requests")
public class CertificateSigningRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String csrContent; // CSR в PEM формате

    @ManyToOne
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @ManyToOne
    @JoinColumn(name = "selected_ca_id")
    private Certificate selectedCA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CSRStatus status = CSRStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime processedAt;

    @ManyToOne
    @JoinColumn(name = "issued_certificate_id")
    private Certificate issuedCertificate; // Ссылка на выпущенный сертификат

    @Column(columnDefinition = "TEXT")
    private String rejectionReason; // Причина отклонения, если статус REJECTED

    // Конструкторы
    public CertificateSigningRequest() {}

    // Getters и Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCsrContent() { return csrContent; }
    public void setCsrContent(String csrContent) { this.csrContent = csrContent; }

    public User getRequestedBy() { return requestedBy; }
    public void setRequestedBy(User requestedBy) { this.requestedBy = requestedBy; }

    public Certificate getSelectedCA() { return selectedCA; }
    public void setSelectedCA(Certificate selectedCA) { this.selectedCA = selectedCA; }

    public CSRStatus getStatus() { return status; }
    public void setStatus(CSRStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public Certificate getIssuedCertificate() { return issuedCertificate; }
    public void setIssuedCertificate(Certificate issuedCertificate) { this.issuedCertificate = issuedCertificate; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}

enum CSRStatus {
    PENDING,
    APPROVED,
    REJECTED
}
