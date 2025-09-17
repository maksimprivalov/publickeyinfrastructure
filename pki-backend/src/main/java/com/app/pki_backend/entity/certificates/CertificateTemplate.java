package com.app.pki_backend.entity.certificates;

import com.app.pki_backend.entity.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificate_templates")
public class CertificateTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "ca_issuer_id", nullable = false)
    private Certificate caIssuer;

    @Column(name = "cn_regex")
    private String cnRegex; // regex for CN

    @Column(name = "san_regex")
    private String sanRegex; // regex for SAN

    @Column(name = "max_ttl_days")
    private Integer maxTtlDays; // Max TTL in days (time to live)

    @Column(name = "default_key_usage", columnDefinition = "TEXT")
    private String defaultKeyUsage; // JSON string

    @Column(name = "default_extended_key_usage", columnDefinition = "TEXT")
    private String defaultExtendedKeyUsage; // JSON string

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner; // CA user, created this template

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public CertificateTemplate() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Certificate getCaIssuer() { return caIssuer; }
    public void setCaIssuer(Certificate caIssuer) { this.caIssuer = caIssuer; }

    public String getCnRegex() { return cnRegex; }
    public void setCnRegex(String cnRegex) { this.cnRegex = cnRegex; }

    public String getSanRegex() { return sanRegex; }
    public void setSanRegex(String sanRegex) { this.sanRegex = sanRegex; }

    public Integer getMaxTtlDays() { return maxTtlDays; }
    public void setMaxTtlDays(Integer maxTtlDays) { this.maxTtlDays = maxTtlDays; }

    public String getDefaultKeyUsage() { return defaultKeyUsage; }
    public void setDefaultKeyUsage(String defaultKeyUsage) { this.defaultKeyUsage = defaultKeyUsage; }

    public String getDefaultExtendedKeyUsage() { return defaultExtendedKeyUsage; }
    public void setDefaultExtendedKeyUsage(String defaultExtendedKeyUsage) { this.defaultExtendedKeyUsage = defaultExtendedKeyUsage; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
