package com.app.pki_backend.service.implementations;

import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.entity.certificates.CertificateStatus;
import com.app.pki_backend.entity.certificates.CertificateType;
import com.app.pki_backend.repository.CertificateRepository;
import com.app.pki_backend.util.PEMConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Сервис для валидации сертификатов и цепочек сертификатов
 */
@Service
public class CertificateChainValidationService {

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private PEMConverter pemConverter;

    /**
     * ГЛАВНАЯ ВАЛИДАЦИЯ: проверяет можно ли использовать issuer для подписи нового сертификата
     */
    public void validateIssuerBeforeSigning(Certificate issuer,
                                            LocalDateTime newCertNotBefore,
                                            LocalDateTime newCertNotAfter) {

        System.out.println("🔍 Validating issuer certificate ID=" + issuer.getId());

        // 1. Проверка что issuer является CA
        if (issuer.getType() == CertificateType.END_ENTITY) {
            throw new IllegalStateException(
                    "End-Entity certificate " + issuer.getId() + " cannot issue other certificates. " +
                            "Only ROOT_CA and INTERMEDIATE_CA can be used as issuers."
            );
        }

        // 2. Проверка статуса issuer
        if (issuer.getStatus() == CertificateStatus.REVOKED) {
            throw new IllegalStateException(
                    "Cannot use revoked certificate " + issuer.getId() + " as issuer"
            );
        }

        if (issuer.getStatus() == CertificateStatus.EXPIRED) {
            throw new IllegalStateException(
                    "Cannot use expired certificate " + issuer.getId() + " as issuer"
            );
        }

        // 3. Проверка срока действия issuer
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(issuer.getValidFrom()) || now.isAfter(issuer.getValidTo())) {
            throw new IllegalStateException(
                    "Issuer certificate " + issuer.getId() + " is not currently valid. " +
                            "Valid from " + issuer.getValidFrom() + " to " + issuer.getValidTo()
            );
        }

        // 4. Проверка что новый сертификат не выходит за рамки issuer
        if (newCertNotBefore.isBefore(issuer.getValidFrom())) {
            throw new IllegalArgumentException(
                    "New certificate cannot start (" + newCertNotBefore +
                            ") before issuer validity (" + issuer.getValidFrom() + ")"
            );
        }

        if (newCertNotAfter.isAfter(issuer.getValidTo())) {
            throw new IllegalArgumentException(
                    "New certificate cannot expire (" + newCertNotAfter +
                            ") after issuer expiry (" + issuer.getValidTo() + ")"
            );
        }

        // 5. Валидация всей цепочки до Root
        List<Certificate> chain = buildChainToRoot(issuer);
        validateChain(chain);

        System.out.println("✅ Issuer validation passed. Chain length: " + chain.size());
    }

    /**
     * Построить цепочку от сертификата до Root CA
     */
    public List<Certificate> buildChainToRoot(Certificate certificate) {
        List<Certificate> chain = new ArrayList<>();
        Certificate current = certificate;
        int depth = 0;
        int maxDepth = 10; // защита от циклов

        while (current != null && depth < maxDepth) {
            chain.add(current);

            // Если это Root (самоподписанный), останавливаемся
            if (current.getIssuerCertificate() == null) {
                break;
            }

            // Защита от циклических ссылок
            Certificate parent = current.getIssuerCertificate();
            if (chain.contains(parent)) {
                throw new IllegalStateException(
                        "Circular reference detected in certificate chain at ID=" + parent.getId()
                );
            }

            current = parent;
            depth++;
        }

        if (depth >= maxDepth) {
            throw new IllegalStateException("Certificate chain too deep (> " + maxDepth + ")");
        }

        return chain;
    }

    /**
     * Валидация всей цепочки сертификатов
     */
    private void validateChain(List<Certificate> chain) {
        if (chain.isEmpty()) {
            throw new IllegalStateException("Empty certificate chain");
        }

        // 1. Проверка что последний в цепочке - Root CA
        Certificate root = chain.get(chain.size() - 1);
        if (root.getIssuerCertificate() != null) {
            throw new IllegalStateException(
                    "Chain does not end with Root CA. Last cert ID=" + root.getId()
            );
        }

        // 2. Проверка каждого сертификата в цепочке
        for (int i = 0; i < chain.size(); i++) {
            Certificate cert = chain.get(i);

            // Проверка статуса
            if (cert.getStatus() == CertificateStatus.REVOKED) {
                throw new IllegalStateException(
                        "Certificate " + cert.getId() + " in chain is REVOKED"
                );
            }

            // Проверка срока действия
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(cert.getValidFrom()) || now.isAfter(cert.getValidTo())) {
                throw new IllegalStateException(
                        "Certificate " + cert.getId() + " in chain is not currently valid"
                );
            }

            // Проверка подписи (кроме Root)
            if (i < chain.size() - 1) {
                Certificate parent = chain.get(i + 1);
                validateSignature(cert, parent);
            } else {
                // Root - проверка самоподписи
                validateSelfSignature(cert);
            }
        }

        // 3. Проверка PathLength constraints
        validatePathLengthConstraints(chain);
    }

    /**
     * Проверка цифровой подписи сертификата
     */
    private void validateSignature(Certificate cert, Certificate issuer) {
        try {
            X509Certificate x509Cert = pemConverter.parseCertificate(cert.getCertificateData());
            X509Certificate x509Issuer = pemConverter.parseCertificate(issuer.getCertificateData());

            PublicKey issuerPublicKey = x509Issuer.getPublicKey();

            // Проверка подписи
            x509Cert.verify(issuerPublicKey);

            System.out.println("✓ Signature valid: cert " + cert.getId() + " signed by " + issuer.getId());

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Invalid signature for certificate " + cert.getId() +
                            " (issuer=" + issuer.getId() + "): " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Проверка самоподписи Root сертификата
     */
    private void validateSelfSignature(Certificate rootCert) {
        try {
            X509Certificate x509Cert = pemConverter.parseCertificate(rootCert.getCertificateData());
            PublicKey publicKey = x509Cert.getPublicKey();

            // Root должен быть самоподписан
            x509Cert.verify(publicKey);

            System.out.println("✓ Self-signature valid for Root CA " + rootCert.getId());

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Invalid self-signature for Root CA " + rootCert.getId() + ": " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Проверка PathLength constraints
     */
    private void validatePathLengthConstraints(List<Certificate> chain) {
        // Идем от Root вниз
        for (int i = chain.size() - 1; i >= 0; i--) {
            Certificate cert = chain.get(i);

            if (!cert.getType().equals(CertificateType.INTERMEDIATE_CA)) {
                continue; // End-entity сертификаты не имеют PathLength
            }

            try {
                X509Certificate x509Cert = pemConverter.parseCertificate(cert.getCertificateData());
                int pathLength = x509Cert.getBasicConstraints();

                if (pathLength == -1) {
                    throw new IllegalStateException(
                            "CA certificate " + cert.getId() + " has no BasicConstraints"
                    );
                }

                // Сколько CA сертификатов ниже этого?
                int casBelowCount = 0;
                for (int j = i - 1; j >= 0; j--) {
                    if (chain.get(j).getType().equals(CertificateType.INTERMEDIATE_CA)) {
                        casBelowCount++;
                    }
                }

                // PathLength ограничивает количество CA сертификатов ниже
                if (pathLength < casBelowCount && pathLength != Integer.MAX_VALUE) {
                    throw new IllegalStateException(
                            "PathLength constraint violated for certificate " + cert.getId() +
                                    ". Allowed: " + pathLength + ", actual: " + casBelowCount
                    );
                }

                System.out.println("✓ PathLength OK for cert " + cert.getId() +
                        ": allowed=" + pathLength + ", below=" + casBelowCount);

            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Error checking PathLength for certificate " + cert.getId(), e
                );
            }
        }
    }

    /**
     * Вычислить глубину сертификата от Root
     */
    public int calculateDepthFromRoot(Certificate certificate) {
        List<Certificate> chain = buildChainToRoot(certificate);
        return chain.size() - 1; // Root имеет depth=0
    }

    /**
     * Проверить может ли данный CA выпустить ещё один Intermediate CA
     */
    public boolean canIssueIntermediateCA(Certificate issuer) {
        try {
            X509Certificate x509Cert = pemConverter.parseCertificate(issuer.getCertificateData());
            int pathLength = x509Cert.getBasicConstraints();

            if (pathLength == -1 || !issuer.getType().equals(CertificateType.INTERMEDIATE_CA)) {
                return false; // Не CA или нет BasicConstraints
            }

            // Если pathLength = 0, не может выпускать другие CA
            if (pathLength == 0) {
                return false;
            }

            // Если pathLength > 0 или Integer.MAX_VALUE, может выпускать
            return true;

        } catch (Exception e) {
            return false;
        }
    }
}
