package com.app.pki_backend.repository;

import com.app.pki_backend.entity.certificates.CertificateTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, Long> {
}
