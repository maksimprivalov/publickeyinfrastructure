package com.app.pki_backend.entity.certificates;

import com.app.pki_backend.entity.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * ✅ FIXED: Добавлена поддержка TTL (Time To Live)
 */
@Entity
@Table(name = "certificate_signing_requests")
public class CertificateSigningRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotNull(message = "CSR content is required")
    private String csrContent; // CSR in PEM format

    @ManyToOne
    @JoinColumn(name = "requested_by", nullable = false)
    @NotNull(message = "Requesting user is required")
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
    private Certificate issuedCertificate; // Id of issued certificate if status is ISSUED

    @Column(columnDefinition = "TEXT")
    private String rejectionReason; // Rejection reason if status is REJECTED

    // ✅ FIX: Добавлены новые поля

    /**
     * Запрошенное время жизни сертификата в днях
     */
    @Column(name = "requested_ttl_days")
    @Min(value = 1, message = "TTL must be at least 1 day")
    @Max(value = 7300, message = "TTL cannot exceed 20 years (7300 days)")
    private Integer requestedTtlDays;

    /**
     * Запрошенное время жизни в годах (для удобства)
     */
    @Column(name = "requested_ttl_years")
    @Min(value = 1, message = "TTL must be at least 1 year")
    @Max(value = 20, message = "TTL cannot exceed 20 years")
    private Integer requestedTtlYears;

    /**
     * ID шаблона, который был использован (если применимо)
     */
    @Column(name = "template_id")
    private Long templateId;

    /**
     * Флаг: генерировать ключи автоматически или использовать из CSR
     */
    @Column(name = "auto_generate_keys")
    private Boolean autoGenerateKeys = false;

    public CertificateSigningRequest() {}

    // ============================================================================
    // Getters and Setters
    // ============================================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCsrContent() {
        return csrContent;
    }

    public void setCsrContent(String csrContent) {
        this.csrContent = csrContent;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(User requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Certificate getSelectedCA() {
        return selectedCA;
    }

    public void setSelectedCA(Certificate selectedCA) {
        this.selectedCA = selectedCA;
    }

    public CSRStatus getStatus() {
        return status;
    }

    public void setStatus(CSRStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public Certificate getIssuedCertificate() {
        return issuedCertificate;
    }

    public void setIssuedCertificate(Certificate issuedCertificate) {
        this.issuedCertificate = issuedCertificate;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    // ✅ FIX: Новые getters/setters

    public Integer getRequestedTtlDays() {
        return requestedTtlDays;
    }

    public void setRequestedTtlDays(Integer requestedTtlDays) {
        this.requestedTtlDays = requestedTtlDays;
        // Автоматически обновляем годы
        if (requestedTtlDays != null) {
            this.requestedTtlYears = requestedTtlDays / 365;
        }
    }

    public Integer getRequestedTtlYears() {
        return requestedTtlYears;
    }

    public void setRequestedTtlYears(Integer requestedTtlYears) {
        this.requestedTtlYears = requestedTtlYears;
        // Автоматически обновляем дни
        if (requestedTtlYears != null) {
            this.requestedTtlDays = requestedTtlYears * 365;
        }
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Boolean getAutoGenerateKeys() {
        return autoGenerateKeys;
    }

    public void setAutoGenerateKeys(Boolean autoGenerateKeys) {
        this.autoGenerateKeys = autoGenerateKeys;
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    /**
     * Получить TTL в днях (приоритет имеет requestedTtlDays)
     */
    public Integer getTtlInDays() {
        if (requestedTtlDays != null) {
            return requestedTtlDays;
        }
        if (requestedTtlYears != null) {
            return requestedTtlYears * 365;
        }
        return null;
    }

    /**
     * Проверка валидности CSR
     */
    public boolean isValid() {
        return csrContent != null && !csrContent.trim().isEmpty() &&
                requestedBy != null &&
                selectedCA != null;
    }

    /**
     * Проверка что CSR ожидает обработки
     */
    public boolean isPending() {
        return status == CSRStatus.PENDING;
    }

    /**
     * Проверка что CSR одобрен
     */
    public boolean isApproved() {
        return status == CSRStatus.APPROVED;
    }

    /**
     * Проверка что CSR отклонен
     */
    public boolean isRejected() {
        return status == CSRStatus.REJECTED;
    }

    @Override
    public String toString() {
        return "CertificateSigningRequest{" +
                "id=" + id +
                ", requestedBy=" + (requestedBy != null ? requestedBy.getEmail() : "null") +
                ", selectedCA=" + (selectedCA != null ? selectedCA.getId() : "null") +
                ", status=" + status +
                ", requestedTtlDays=" + requestedTtlDays +
                ", templateId=" + templateId +
                ", autoGenerateKeys=" + autoGenerateKeys +
                '}';
    }
}