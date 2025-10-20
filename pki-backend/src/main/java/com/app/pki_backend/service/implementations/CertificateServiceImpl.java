package com.app.pki_backend.service.implementations;

import com.app.pki_backend.dto.certificate.CertificateDTO;
import com.app.pki_backend.entity.certificates.CertificateSigningRequest;
import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.entity.certificates.CertificateStatus;
import com.app.pki_backend.entity.certificates.CertificateTemplate;
import com.app.pki_backend.entity.certificates.CertificateType;
import com.app.pki_backend.entity.user.User;
import com.app.pki_backend.repository.CertificateRepository;
import com.app.pki_backend.service.interfaces.CertificateService;
import com.app.pki_backend.service.interfaces.CryptographyService;
import com.app.pki_backend.service.interfaces.PrivateKeyService;
import com.app.pki_backend.service.interfaces.MasterKeyService;
import com.app.pki_backend.specification.CertificateSpecification;
import com.app.pki_backend.util.CSRValidator;
import com.app.pki_backend.util.CertificateBuilder;
import com.app.pki_backend.util.PEMConverter;
import com.app.pki_backend.audit.AuditLogger;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import org.springframework.data.domain.Pageable;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(rollbackFor = Exception.class) // ✅ FIX: Добавлен rollback для всех исключений
public class CertificateServiceImpl implements CertificateService {

    @Autowired
    private CryptographyService cryptographyService;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private PEMConverter pemConverter;

    @Autowired
    private PrivateKeyService privateKeyService;

    @Autowired
    private MasterKeyService masterKeyService;

    @Autowired
    private CertificateTemplateServiceImpl certificateTemplateService;

    @Autowired
    private CertificateChainValidationService chainValidationService;

    @Autowired
    private CSRValidator csrValidator;

    @Autowired
    private AuditLogger auditLogger; // ✅ FIX: Добавлено аудит-логирование

    @Value("${pki.root-ca.subject}")
    private String rootCASubject;

    @Value("${pki.root-ca.validity-years}")
    private int validityYears;

    @Value("${pki.root-ca.key-size}")
    private int keySize;

    @Value("${pki.intermediate-ca.validity-years}")
    private int intermediateValidityYears;

    @Value("${pki.intermediate-ca.path-length:0}") // ✅ FIX: Конфигурируемый PathLength
    private int defaultIntermediatePathLength;

    @Value("${pki.end-entity.validity-years}")
    private int endEntityValidityYears;

    @Override
    public Certificate issueRootCertificate() {
        try {
            auditLogger.log("ISSUE_ROOT_CERTIFICATE", "system"); // ✅ FIX: Аудит

            // 1. Generate a key pair for Root CA
            KeyPair keyPair = cryptographyService.generateKeyPair(keySize);

            // 2. Create X.509 Root CA certificate
            X509Certificate rootCert = buildRootCACertificate(
                    rootCASubject,
                    keyPair.getPublic(),
                    keyPair.getPrivate(),
                    validityYears
            );

            // 3. Create and save a Certificate entity using CertificateBuilder
            Certificate certificate = CertificateBuilder.create()
                    .fromX509Certificate(rootCert, pemConverter, keyPair.getPublic())
                    .validityPeriod(validityYears)
                    .type(CertificateType.ROOT_CA)
                    .status(CertificateStatus.ACTIVE)
                    .organization("PKI Root CA")
                    .issuerCertificate(null) // Self-signed
                    .build();

            Certificate savedCertificate = certificateRepository.saveAndFlush(certificate);

            // 4. Store private key securely using PrivateKeyService
            SecretKey masterKey = masterKeyService.getCurrentMasterKey();
            privateKeyService.storePrivateKey(savedCertificate, keyPair.getPrivate(), masterKey);

            return savedCertificate;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create root certificate", e);
        }
    }

    @Override
    public Certificate issueIntermediateCertificate(CertificateSigningRequest csr, Certificate issuer) {
        try {
            // ✅ FIX: Валидация ПЕРЕД созданием сертификата
            if (issuer.getType() == CertificateType.END_ENTITY) {
                throw new IllegalArgumentException("End Entity certificate cannot issue Intermediate CA");
            }

            // ✅ FIX: Проверка прав для PathLength
            if (issuer.getType().equals(CertificateType.INTERMEDIATE_CA) &&
                    !chainValidationService.canIssueIntermediateCA(issuer)) {
                throw new IllegalStateException(
                        "Issuer " + issuer.getId() + " cannot issue Intermediate CA certificates " +
                                "(PathLength constraint)"
                );
            }

            PKCS10CertificationRequest pkcs10CSR;

            try (PEMParser p = new PEMParser(new StringReader(csr.getCsrContent()))) {
                Object obj = p.readObject();
                if (!(obj instanceof PKCS10CertificationRequest)) {
                    throw new IllegalArgumentException("Invalid CSR format");
                }
                pkcs10CSR = (PKCS10CertificationRequest) obj;
            }

            if (!csrValidator.validateCSRSignature(pkcs10CSR)) {
                throw new IllegalArgumentException(
                        "Invalid CSR signature. Proof of private key ownership failed."
                );
            }

            X500Name subject = pkcs10CSR.getSubject();

            // ✅ FIX: КРИТИЧНО - Генерируем ключи для CA В СИСТЕМЕ, а не используем из CSR!
            // Intermediate CA должен иметь приватный ключ в системе для подписи других сертификатов
            KeyPair intermediateKeyPair = cryptographyService.generateKeyPair(keySize);
            PublicKey publicKey = intermediateKeyPair.getPublic();

            // Получить приватный ключ издателя для подписи
            PrivateKey issuerPrivateKey = getIssuerPrivateKey(issuer);

            // ✅ FIX: Вычисляем PathLength для нового Intermediate CA
            int newPathLength = calculatePathLengthForNewCA(issuer);

            // ✅ FIX: Валидация ПЕРЕД созданием сертификата
            LocalDateTime notBefore = LocalDateTime.now();
            LocalDateTime notAfter = notBefore.plusYears(intermediateValidityYears);

            chainValidationService.validateIssuerBeforeSigning(
                    issuer,
                    notBefore,
                    notAfter
            );

            // Создание Intermediate CA сертификата
            X509Certificate intermediateCert = buildIntermediateCACertificate(
                    subject.toString(),
                    publicKey,
                    issuerPrivateKey,
                    issuer,
                    intermediateValidityYears,
                    newPathLength // ✅ FIX: Передаем вычисленный PathLength
            );

            // Создание и сохранение Certificate entity
            Certificate certificate = CertificateBuilder.create()
                    .fromX509Certificate(intermediateCert, pemConverter, publicKey)
                    .validityPeriod(intermediateValidityYears)
                    .type(CertificateType.INTERMEDIATE_CA)
                    .status(CertificateStatus.ACTIVE)
                    .organization(extractOrganizationFromSubject(subject.toString()))
                    .issuerCertificate(issuer)
                    .owner(csr.getRequestedBy())
                    .build();

            Certificate savedCertificate = certificateRepository.saveAndFlush(certificate);

            // ✅ FIX: СОХРАНИТЬ приватный ключ для Intermediate CA
            SecretKey masterKey = masterKeyService.getCurrentMasterKey();
            privateKeyService.storePrivateKey(
                    savedCertificate,
                    intermediateKeyPair.getPrivate(),
                    masterKey
            );

            // ✅ FIX: Аудит-лог
            auditLogger.log(
                    "ISSUE_INTERMEDIATE_CA id=" + savedCertificate.getId() +
                            " issuer=" + issuer.getId(),
                    csr.getRequestedBy() != null ? csr.getRequestedBy().getEmail() : "unknown"
            );

            return savedCertificate;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create intermediate certificate", e);
        }
    }

    @Override
    public Certificate issueEndEntityCertificate(CertificateSigningRequest csr, Certificate issuer) {
        try {
            // ✅ FIX: Валидация ПЕРЕД созданием сертификата
            if (issuer.getType() == CertificateType.END_ENTITY) {
                throw new IllegalArgumentException("End Entity certificate cannot issue other certificates");
            }

            PKCS10CertificationRequest pkcs10CSR;

            try (PEMParser p = new PEMParser(new StringReader(csr.getCsrContent()))) {
                Object obj = p.readObject();
                if (!(obj instanceof PKCS10CertificationRequest)) {
                    throw new IllegalArgumentException("Invalid CSR format");
                }
                pkcs10CSR = (PKCS10CertificationRequest) obj;
            }

            if (!csrValidator.validateCSRSignature(pkcs10CSR)) {
                throw new IllegalArgumentException(
                        "Invalid CSR signature. Proof of private key ownership failed."
                );
            }

            JcaPKCS10CertificationRequest jcaCSR = new JcaPKCS10CertificationRequest(pkcs10CSR);
            PublicKey publicKey = jcaCSR.getPublicKey();
            X500Name subject = pkcs10CSR.getSubject();

            // Получить приватный ключ издателя
            PrivateKey issuerPrivateKey = getIssuerPrivateKey(issuer);

            // ✅ FIX: Валидация ПЕРЕД созданием сертификата
            LocalDateTime notBefore = LocalDateTime.now();
            LocalDateTime notAfter = notBefore.plusYears(endEntityValidityYears);

            chainValidationService.validateIssuerBeforeSigning(
                    issuer,
                    notBefore,
                    notAfter
            );

            // Создание End Entity сертификата
            X509Certificate endEntityCert = buildEndEntityCertificate(
                    subject.toString(),
                    publicKey,
                    issuerPrivateKey,
                    issuer,
                    endEntityValidityYears
            );

            // Создание и сохранение Certificate entity
            Certificate certificate = CertificateBuilder.create()
                    .fromX509Certificate(endEntityCert, pemConverter, publicKey)
                    .validityPeriod(endEntityValidityYears)
                    .type(CertificateType.END_ENTITY)
                    .status(CertificateStatus.ACTIVE)
                    .organization(extractOrganizationFromSubject(subject.toString()))
                    .issuerCertificate(issuer)
                    .owner(csr.getRequestedBy())
                    .build();

            Certificate savedCertificate = certificateRepository.saveAndFlush(certificate);

            // End Entity НЕ хранит приватный ключ в системе (пользователь сам хранит)

            // ✅ FIX: Аудит-лог
            auditLogger.log(
                    "ISSUE_END_ENTITY id=" + savedCertificate.getId() +
                            " issuer=" + issuer.getId(),
                    csr.getRequestedBy() != null ? csr.getRequestedBy().getEmail() : "unknown"
            );

            return savedCertificate;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create end entity certificate", e);
        }
    }

    // ✅ FIX: Новый метод для вычисления PathLength
    private int calculatePathLengthForNewCA(Certificate issuer) {
        try {
            if (issuer.getType() == CertificateType.ROOT_CA) {
                // Root CA может выпустить Intermediate с настраиваемым PathLength
                return defaultIntermediatePathLength;
            }

            // Для Intermediate CA: PathLength нового CA = PathLength родителя - 1
            X509Certificate x509Issuer = pemConverter.parseCertificate(issuer.getCertificateData());
            int issuerPathLength = x509Issuer.getBasicConstraints();

            if (issuerPathLength == -1) {
                throw new IllegalStateException("Issuer is not a CA certificate");
            }

            if (issuerPathLength == 0) {
                throw new IllegalStateException("Issuer cannot issue more CA certificates (pathLength=0)");
            }

            // Новый PathLength = родительский - 1, минимум 0
            return Math.max(0, issuerPathLength - 1);

        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate pathLength for new CA", e);
        }
    }

    // === Private methods for building different types of certificates ===

    private X509Certificate buildRootCACertificate(
            String subjectDN,
            PublicKey publicKey,
            PrivateKey privateKey,
            int validityYears) throws Exception {

        X500Name subject = new X500Name(subjectDN);
        X500Name issuer = subject; // Self-signed for Root CA

        BigInteger serialNumber = cryptographyService.generateSerialNumber();
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() +
                (long) validityYears * 365 * 24 * 60 * 60 * 1000L);

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                serialNumber,
                notBefore,
                notAfter,
                subject,
                publicKey
        );

        addRootCAExtensions(certBuilder);

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                .build(privateKey);

        return new JcaX509CertificateConverter()
                .getCertificate(certBuilder.build(signer));
    }

    private X509Certificate buildIntermediateCACertificate(
            String subjectDN,
            PublicKey publicKey,
            PrivateKey issuerPrivateKey,
            Certificate issuerCert,
            int validityYears,
            int pathLength) throws Exception { // ✅ FIX: Добавлен параметр pathLength

        X500Name subject = new X500Name(subjectDN);
        X500Name issuer = new X500Name(issuerCert.getSubject());

        BigInteger serialNumber = cryptographyService.generateSerialNumber();
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() +
                (long) validityYears * 365 * 24 * 60 * 60 * 1000L);

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                serialNumber,
                notBefore,
                notAfter,
                subject,
                publicKey
        );

        addIntermediateCAExtensions(certBuilder, pathLength); // ✅ FIX: Передаем pathLength

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                .build(issuerPrivateKey);

        return new JcaX509CertificateConverter()
                .getCertificate(certBuilder.build(signer));
    }

    private X509Certificate buildEndEntityCertificate(
            String subjectDN,
            PublicKey publicKey,
            PrivateKey issuerPrivateKey,
            Certificate issuerCert,
            int validityYears) throws Exception {

        X500Name subject = new X500Name(subjectDN);
        X500Name issuer = new X500Name(issuerCert.getSubject());

        BigInteger serialNumber = cryptographyService.generateSerialNumber();
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() +
                (long) validityYears * 365 * 24 * 60 * 60 * 1000L);

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                serialNumber,
                notBefore,
                notAfter,
                subject,
                publicKey
        );

        addEndEntityExtensions(certBuilder);

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                .build(issuerPrivateKey);

        return new JcaX509CertificateConverter()
                .getCertificate(certBuilder.build(signer));
    }

    // === Methods for adding certificate extensions ===

    private void addRootCAExtensions(X509v3CertificateBuilder certBuilder) throws Exception {
        // Basic Constraints - this is Root CA
        certBuilder.addExtension(
                Extension.basicConstraints,
                true, // critical
                new BasicConstraints(true) // isCA = true, pathLength unlimited
        );

        // Key Usage - certificate and CRL signing
        certBuilder.addExtension(
                Extension.keyUsage,
                true, // critical
                new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign)
        );
    }

    private void addIntermediateCAExtensions(
            X509v3CertificateBuilder certBuilder,
            int pathLength) throws Exception { // ✅ FIX: Добавлен параметр pathLength

        // Basic Constraints - this is Intermediate CA with configurable path length
        certBuilder.addExtension(
                Extension.basicConstraints,
                true, // critical
                new BasicConstraints(pathLength) // ✅ FIX: Конфигурируемый pathLength
        );

        // Key Usage - certificate and CRL signing
        certBuilder.addExtension(
                Extension.keyUsage,
                true, // critical
                new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign)
        );
    }

    private void addEndEntityExtensions(X509v3CertificateBuilder certBuilder) throws Exception {
        // Basic Constraints - this is NOT a CA
        certBuilder.addExtension(
                Extension.basicConstraints,
                true, // critical
                new BasicConstraints(false) // isCA = false
        );

        // Key Usage - digital signature and encryption
        certBuilder.addExtension(
                Extension.keyUsage,
                true, // critical
                new KeyUsage(KeyUsage.digitalSignature |
                        KeyUsage.keyEncipherment |
                        KeyUsage.dataEncipherment)
        );

        // Extended Key Usage - client and server authentication
        certBuilder.addExtension(
                Extension.extendedKeyUsage,
                false, // not critical
                new ExtendedKeyUsage(new KeyPurposeId[]{
                        KeyPurposeId.id_kp_clientAuth,
                        KeyPurposeId.id_kp_serverAuth
                })
        );
    }

    // === Helper methods ===

    private PrivateKey getIssuerPrivateKey(Certificate issuerCert) {
        try {
            if (!masterKeyService.isMasterKeyAvailable()) {
                throw new IllegalStateException("Master key is not available for private key decryption");
            }

            if (!privateKeyService.hasPrivateKey(issuerCert)) {
                throw new IllegalStateException("No private key found for issuer certificate: " + issuerCert.getId());
            }

            SecretKey masterKey = masterKeyService.getCurrentMasterKey();
            return privateKeyService.retrievePrivateKey(issuerCert, masterKey);

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve issuer private key for certificate: " + issuerCert.getId(), e);
        }
    }

    private String extractOrganizationFromSubject(String subject) {
        if (subject.contains("O=")) {
            String[] parts = subject.split(",");
            for (String part : parts) {
                part = part.trim();
                if (part.startsWith("O=")) {
                    return part.substring(2);
                }
            }
        }
        return "Unknown Organization";
    }

    @Override
    public Certificate issueCertificateFromCSR(byte[] csrData, Certificate issuer) {
        try {
            String csrPEM = new String(csrData);
            CertificateSigningRequest csr = new CertificateSigningRequest();
            csr.setCsrContent(csrPEM);

            // ✅ FIX: Определяем тип сертификата из CSR
            PKCS10CertificationRequest pkcs10CSR = pemConverter.parseCSR(csrPEM);

            // Проверяем BasicConstraints в CSR для определения типа
            CertificateType certType = determineCertificateType(pkcs10CSR);

            if (certType == CertificateType.INTERMEDIATE_CA) {
                return issueIntermediateCertificate(csr, issuer);
            } else {
                return issueEndEntityCertificate(csr, issuer);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to issue certificate from CSR", e);
        }
    }

    // ✅ FIX: Новый метод для определения типа сертификата
    private CertificateType determineCertificateType(PKCS10CertificationRequest csr) {
        try {
            org.bouncycastle.asn1.x509.Extensions extensions = csr.getRequestedExtensions();

            if (extensions != null) {
                org.bouncycastle.asn1.x509.Extension bcExt = extensions.getExtension(Extension.basicConstraints);

                if (bcExt != null) {
                    BasicConstraints bc = BasicConstraints.getInstance(bcExt.getParsedValue());
                    if (bc.isCA()) {
                        return CertificateType.INTERMEDIATE_CA;
                    }
                }
            }

            return CertificateType.END_ENTITY;

        } catch (Exception e) {
            return CertificateType.END_ENTITY; // Default to end entity
        }
    }

    @Override
    public Certificate issueServerCertificate(String serverName, Certificate issuer) {
        try {
            KeyPair keyPair = cryptographyService.generateKeyPair(keySize);

            // ✅ FIX: Валидация перед созданием
            LocalDateTime notBefore = LocalDateTime.now();
            LocalDateTime notAfter = notBefore.plusYears(endEntityValidityYears);

            chainValidationService.validateIssuerBeforeSigning(issuer, notBefore, notAfter);

            X509Certificate serverCert = buildServerCertificate(
                    "CN=" + serverName + ", O=PKI Server, C=RS",
                    keyPair.getPublic(),
                    getIssuerPrivateKey(issuer),
                    issuer,
                    endEntityValidityYears
            );

            Certificate certificate = CertificateBuilder.create()
                    .fromX509Certificate(serverCert, pemConverter, keyPair.getPublic())
                    .validityPeriod(endEntityValidityYears)
                    .type(CertificateType.END_ENTITY)
                    .status(CertificateStatus.ACTIVE)
                    .organization("PKI Server")
                    .issuerCertificate(issuer)
                    .build();

            Certificate savedCertificate = certificateRepository.saveAndFlush(certificate);

            SecretKey masterKey = masterKeyService.getCurrentMasterKey();
            privateKeyService.storePrivateKey(savedCertificate, keyPair.getPrivate(), masterKey);

            auditLogger.log("ISSUE_SERVER_CERT id=" + savedCertificate.getId(), "system");

            return savedCertificate;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create server certificate", e);
        }
    }

    private X509Certificate buildServerCertificate(
            String subjectDN,
            PublicKey publicKey,
            PrivateKey issuerPrivateKey,
            Certificate issuerCert,
            int validityYears) throws Exception {

        X500Name subject = new X500Name(subjectDN);
        X500Name issuer = new X500Name(issuerCert.getSubject());

        BigInteger serialNumber = cryptographyService.generateSerialNumber();
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() +
                (long) validityYears * 365 * 24 * 60 * 60 * 1000L);

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                serialNumber,
                notBefore,
                notAfter,
                subject,
                publicKey
        );

        addServerExtensions(certBuilder);

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                .build(issuerPrivateKey);

        return new JcaX509CertificateConverter()
                .getCertificate(certBuilder.build(signer));
    }

    private void addServerExtensions(X509v3CertificateBuilder certBuilder) throws Exception {
        certBuilder.addExtension(
                Extension.basicConstraints,
                true,
                new BasicConstraints(false)
        );

        certBuilder.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment)
        );

        certBuilder.addExtension(
                Extension.extendedKeyUsage,
                false,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth)
        );
    }

    // === Остальные методы без изменений ===

    @Override
    public List<Certificate> findAll() {
        return certificateRepository.findAll();
    }

    @Override
    public Optional<Certificate> findById(Long id) {
        return certificateRepository.findById(id);
    }

    @Override
    public void delete(Long id) {
        auditLogger.log("DELETE_CERTIFICATE id=" + id, "system");
        certificateRepository.deleteById(id);
    }

    @Override
    public Certificate issueRootWithTemplate(Long templateId) {
        CertificateTemplate template = certificateTemplateService.findById(templateId);
        return issueRootCertificate();
    }

    @Override
    public Certificate issueIntermediateWithTemplate(Long templateId, CertificateSigningRequest csr) {
        CertificateTemplate template = certificateTemplateService.findById(templateId);
        Certificate issuer = template.getCaIssuer();

        if (!certificateTemplateService.validateAgainstTemplate(csr, template)) {
            throw new IllegalArgumentException("CSR does not match template policy");
        }
        csr = certificateTemplateService.applyTemplate(csr, template);

        return issueIntermediateCertificate(csr, issuer);
    }

    @Override
    public Certificate issueEndEntityWithTemplate(Long templateId, CertificateSigningRequest csr) {
        CertificateTemplate template = certificateTemplateService.findById(templateId);
        Certificate issuer = template.getCaIssuer();

        if (!certificateTemplateService.validateAgainstTemplate(csr, template)) {
            throw new IllegalArgumentException("CSR does not match template policy");
        }
        csr = certificateTemplateService.applyTemplate(csr, template);

        return issueEndEntityCertificate(csr, issuer);
    }

    @Override
    public List<Certificate> findAllByOrganization(String organizationName) {
        return certificateRepository.findByOrganization(organizationName);
    }

    @Override
    public List<Certificate> findAllByOwnerId(Integer ownerId) {
        return certificateRepository.findByOwnerId(ownerId);
    }

    @Override
    public byte[] exportAsPkcs12(Long certId, String password) {
        try {
            Certificate cert = certificateRepository.findById(certId)
                    .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));

            SecretKey masterKey = masterKeyService.getCurrentMasterKey();
            PrivateKey privateKey = privateKeyService.retrievePrivateKey(cert, masterKey);

            X509Certificate x509Cert = pemConverter.parseCertificate(cert.getCertificateData());

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);

            keyStore.setKeyEntry("key", privateKey, password.toCharArray(),
                    new java.security.cert.Certificate[]{x509Cert});

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            keyStore.store(baos, password.toCharArray());

            auditLogger.log("EXPORT_PKCS12 certId=" + certId, "system");

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to export PKCS12", e);
        }
    }

    @Override
    public Page<Certificate> search(CertificateStatus status, CertificateType type,
                                    String organization, Pageable pageable) {
        return certificateRepository.findAll(
                Specification.where(CertificateSpecification.hasStatus(status))
                        .and(CertificateSpecification.hasType(type))
                        .and(CertificateSpecification.hasOrganization(organization)),
                pageable
        );
    }

    public List<CertificateDTO> getAllCertificatesByOwner(Long ownerId) {
        List<Certificate> certificates = certificateRepository.findByOwnerId(Math.toIntExact(ownerId));
        return certificates.stream().map(this::fromEntity).toList();
    }

    private CertificateDTO fromEntity(Certificate certificate) {
        CertificateDTO dto = new CertificateDTO();
        dto.setId(certificate.getId());
        dto.setSubject(certificate.getSubject());
        dto.setIssuerId(certificate.getIssuerCertificate().getId());
        dto.setValidFrom(certificate.getValidFrom());
        dto.setValidTo(certificate.getValidTo());
        dto.setType(certificate.getType().toString());
        dto.setStatus(certificate.getStatus());
        dto.setOrganization(certificate.getOrganization());
        return dto;
    }
    public Path generateHttpsKeystore(String serverName, Long issuerId, String password) {
        try {
            // 1️⃣ Проверяем, есть ли активный серверный сертификат под нужный CN
            String expectedSubject = "CN=" + serverName + ", O=PKI Server, C=RS";
            List<Certificate> existing = certificateRepository.findByType(CertificateType.END_ENTITY)
                    .stream()
                    .filter(c -> c.getStatus() == CertificateStatus.ACTIVE)
                    .filter(c -> expectedSubject.equals(c.getSubject()))
                    .toList();

            Certificate serverCert;
            if (!existing.isEmpty()) {
                serverCert = existing.get(0);
                System.out.println("✅ Found existing server certificate for HTTPS: " + serverCert.getSubject());
            } else {
                // 2️⃣ Выпускаем новый серверный сертификат
                Certificate issuer = certificateRepository.findById(issuerId)
                        .orElseThrow(() -> new IllegalArgumentException("Issuer not found for HTTPS generation"));
                serverCert = issueServerCertificate(serverName, issuer);
                System.out.println("✅ Issued new server certificate for HTTPS: " + serverCert.getSubject());
            }

            // 3️⃣ Экспортируем PKCS#12 c цепочкой сертификатов
            byte[] p12 = exportAsPkcs12WithChain(serverCert.getId(), password);

            // 4️⃣ Сохраняем keystore.p12 в локальную папку
            Path keystoreDir = Paths.get("keystore");
            Files.createDirectories(keystoreDir);
            Path p12Path = keystoreDir.resolve("keystore.p12");
            Files.write(p12Path, p12, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println("✅ HTTPS keystore generated at: " + p12Path.toAbsolutePath());
            return p12Path;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HTTPS keystore", e);
        }
    }

    /**
     * Экспортирует PKCS#12 файл с цепочкой сертификатов (сервер + CA + Root)
     */
    public byte[] exportAsPkcs12WithChain(Long certId, String password) {
        try {
            Certificate cert = certificateRepository.findById(certId)
                    .orElseThrow(() -> new IllegalArgumentException("Certificate not found for PKCS12 export"));

            SecretKey masterKey = masterKeyService.getCurrentMasterKey();
            PrivateKey privateKey = privateKeyService.retrievePrivateKey(cert, masterKey);

            java.security.cert.Certificate[] chain = buildCertificateChain(cert);

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            keyStore.setKeyEntry("pki-server", privateKey, password.toCharArray(), chain);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            keyStore.store(baos, password.toCharArray());
            auditLogger.log("EXPORT_PKCS12_WITH_CHAIN certId=" + certId, "system");

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export PKCS12 with chain", e);
        }
    }

    /**
     * Собирает цепочку сертификатов от конечного до Root CA.
     */
    private java.security.cert.Certificate[] buildCertificateChain(Certificate leaf) throws Exception {
        List<java.security.cert.Certificate> chain = new java.util.ArrayList<>();
        chain.add(pemConverter.parseCertificate(leaf.getCertificateData()));

        Certificate current = leaf.getIssuerCertificate();
        while (current != null) {
            chain.add(pemConverter.parseCertificate(current.getCertificateData()));
            current = current.getIssuerCertificate();
        }
        return chain.toArray(new java.security.cert.Certificate[0]);
    }
}