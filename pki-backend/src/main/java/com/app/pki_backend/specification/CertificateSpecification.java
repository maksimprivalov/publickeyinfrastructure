package com.app.pki_backend.specification;

import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.entity.certificates.CertificateStatus;
import com.app.pki_backend.entity.certificates.CertificateType;
import org.springframework.data.jpa.domain.Specification;

public class CertificateSpecification {

    public static Specification<Certificate> hasStatus(CertificateStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Certificate> hasType(CertificateType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Certificate> hasOrganization(String organization) {
        return (root, query, cb) -> organization == null ? null : cb.equal(root.get("organization"), organization);
    }
}
