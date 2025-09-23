package com.app.pki_backend.repository;

import com.app.pki_backend.entity.ActivationToken;
import com.app.pki_backend.entity.certificates.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long>, JpaSpecificationExecutor<Certificate> {
    Optional<ActivationToken> findByToken(String token);
}