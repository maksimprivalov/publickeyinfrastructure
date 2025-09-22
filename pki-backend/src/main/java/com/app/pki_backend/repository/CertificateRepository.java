package com.app.pki_backend.repository;

import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.entity.certificates.CertificateStatus;
import com.app.pki_backend.entity.certificates.CertificateType;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long>, JpaSpecificationExecutor<Certificate> {

    Optional<Certificate> findByTypeAndStatus(CertificateType type, CertificateStatus status);

    Optional<Certificate> findBySerialNumber(BigInteger serialNumber);

    List<Certificate> findByOrganization(String organization);

    List<Certificate> findByOwnerId(Integer ownerId);

    @Query("SELECT c FROM Certificate c WHERE c.type IN (com.app.pki_backend.entity.certificates.CertificateType.ROOT_CA, com.app.pki_backend.entity.certificates.CertificateType.INTERMEDIATE_CA) AND c.status = com.app.pki_backend.entity.certificates.CertificateStatus.ACTIVE")
    List<Certificate> findActiveCaCertificates();


    @Query("SELECT c FROM Certificate c WHERE c.validTo <= :expirationDate AND c.status = 'ACTIVE'")
    List<Certificate> findExpiringCertificates(@Param("expirationDate") LocalDateTime expirationDate);

    List<Certificate> findByType(CertificateType type);

    List<Certificate> findByStatus(CertificateStatus status);

    List<Certificate> findByIssuerCertificateId(Long issuerCertificateId);

    boolean existsBySerialNumber(BigInteger serialNumber);

    Page<Certificate> findAll(Specification<Certificate> and, Pageable pageable);
}
