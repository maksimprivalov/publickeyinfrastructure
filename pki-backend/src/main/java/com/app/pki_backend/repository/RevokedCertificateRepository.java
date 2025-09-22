package com.app.pki_backend.repository;

import org.springframework.stereotype.Repository;

import com.app.pki_backend.entity.certificates.RevokedCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface RevokedCertificateRepository extends JpaRepository<RevokedCertificate, Long> {
}
