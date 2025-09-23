package com.app.pki_backend.service.implementations;

import com.app.pki_backend.dto.certificate.CertificateSigningRequest;
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
import com.app.pki_backend.util.CertificateBuilder;
import com.app.pki_backend.util.PEMConverter;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
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
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
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

    @Value("${pki.root-ca.subject}")
    private String rootCASubject;

    @Value("${pki.root-ca.validity-years}")
    private int validityYears;

    @Value("${pki.root-ca.key-size}")
    private int keySize;

    @Value("${pki.intermediate-ca.validity-years}")
    private int intermediateValidityYears;

    @Value("${pki.end-entity.validity-years}")
    private int endEntityValidityYears;

    @Override
    public Certificate issueRootCertificate() {
        try {
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

            Certificate savedCertificate = certificateRepository.save(certificate);

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
            // 1. Verify that issuer is Root CA or another Intermediate CA
            if (issuer.getType() == CertificateType.END_ENTITY) {
                throw new IllegalArgumentException("End Entity certificate cannot issue Intermediate CA");
            }

            // 2. Parse CSR
            PKCS10CertificationRequest pkcs10CSR = pemConverter.parseCSR(csr.getCsrContent());
            JcaPKCS10CertificationRequest jcaCSR = new JcaPKCS10CertificationRequest(pkcs10CSR);
            PublicKey publicKey = jcaCSR.getPublicKey();
            X500Name subject = pkcs10CSR.getSubject();

            // 3. Get issuer's private key for signing
            PrivateKey issuerPrivateKey = getIssuerPrivateKey(issuer);

            // 4. Create Intermediate CA certificate
            X509Certificate intermediateCert = buildIntermediateCACertificate(
                    subject.toString(),
                    publicKey,
                    issuerPrivateKey,
                    issuer,
                    intermediateValidityYears
            );

            // 5. Create and save Certificate entity using CertificateBuilder
            Certificate certificate = CertificateBuilder.create()
                    .fromX509Certificate(intermediateCert, pemConverter, publicKey)
                    .validityPeriod(intermediateValidityYears)
                    .type(CertificateType.INTERMEDIATE_CA)
                    .status(CertificateStatus.ACTIVE)
                    .organization(extractOrganizationFromSubject(subject.toString()))
                    .issuerCertificate(issuer)
                    .owner(csr.getRequestedBy())
                    .build();

            Certificate savedCertificate = certificateRepository.save(certificate);

            return savedCertificate;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create intermediate certificate", e);
        }
    }

    @Override
    public Certificate issueEndEntityCertificate(CertificateSigningRequest csr, Certificate issuer) {
        try {
            // 1. Verify that issuer is a CA
            if (issuer.getType() == CertificateType.END_ENTITY) {
                throw new IllegalArgumentException("End Entity certificate cannot issue other certificates");
            }

            // 2. Parse CSR
            PKCS10CertificationRequest pkcs10CSR = pemConverter.parseCSR(csr.getCsrContent());
            JcaPKCS10CertificationRequest jcaCSR = new JcaPKCS10CertificationRequest(pkcs10CSR);
            PublicKey publicKey = jcaCSR.getPublicKey();
            X500Name subject = pkcs10CSR.getSubject();

            // 3. Get issuer's private key for signing
            PrivateKey issuerPrivateKey = getIssuerPrivateKey(issuer);

            // 4. Create End Entity certificate
            X509Certificate endEntityCert = buildEndEntityCertificate(
                    subject.toString(),
                    publicKey,
                    issuerPrivateKey,
                    issuer,
                    endEntityValidityYears
            );

            // 5. Create and save Certificate entity using CertificateBuilder
            Certificate certificate = CertificateBuilder.create()
                    .fromX509Certificate(endEntityCert, pemConverter, publicKey)
                    .validityPeriod(endEntityValidityYears)
                    .type(CertificateType.END_ENTITY)
                    .status(CertificateStatus.ACTIVE)
                    .organization(extractOrganizationFromSubject(subject.toString()))
                    .issuerCertificate(issuer)
                    .owner(csr.getRequestedBy())
                    .build();

            Certificate savedCertificate = certificateRepository.save(certificate);

            return savedCertificate;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create end entity certificate", e);
        }
    }

    @Override
    public Certificate issueCertificateFromCSR(byte[] csrData, Certificate issuer) {
        try {
            // Convert byte[] to PEM string and create CSR object
            String csrPEM = new String(csrData);
            CertificateSigningRequest csr = new CertificateSigningRequest();
            csr.setCsrContent(csrPEM);

            // Determine certificate type based on CSR and issue corresponding certificate
            PKCS10CertificationRequest pkcs10CSR = pemConverter.parseCSR(csrPEM);

            // By default, issue End Entity certificate
            // In real system, type should be determined based on CSR attributes or policies
            return issueEndEntityCertificate(csr, issuer);

        } catch (Exception e) {
            throw new RuntimeException("Failed to issue certificate from CSR", e);
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

        addIntermediateCAExtensions(certBuilder);

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
                new BasicConstraints(true) // isCA = true
        );

        // Key Usage - certificate and CRL signing
        certBuilder.addExtension(
                Extension.keyUsage,
                true, // critical
                new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign)
        );
    }

    private void addIntermediateCAExtensions(X509v3CertificateBuilder certBuilder) throws Exception {
        // Basic Constraints - this is Intermediate CA with path length constraint
        certBuilder.addExtension(
                Extension.basicConstraints,
                true, // critical
                new BasicConstraints(0) // pathLenConstraint = 0 (cannot issue other CAs)
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
            // Check if master key is available
            if (!masterKeyService.isMasterKeyAvailable()) {
                throw new IllegalStateException("Master key is not available for private key decryption");
            }

            // Check if private key exists for the issuer certificate
            if (!privateKeyService.hasPrivateKey(issuerCert)) {
                throw new IllegalStateException("No private key found for issuer certificate: " + issuerCert.getId());
            }

            // Get current master key
            SecretKey masterKey = masterKeyService.getCurrentMasterKey();

            // Retrieve and decrypt private key
            return privateKeyService.retrievePrivateKey(issuerCert, masterKey);

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve issuer private key for certificate: " + issuerCert.getId(), e);
        }
    }

    private String extractOrganizationFromSubject(String subject) {
        // Simple extraction of organization from subject DN
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
    public Certificate issueServerCertificate(String serverName, Certificate issuer) {
        try {
            // Generate key pair for server
            KeyPair keyPair = cryptographyService.generateKeyPair(keySize);

            // Create server certificate
            X509Certificate serverCert = buildServerCertificate(
                    "CN=" + serverName + ", O=PKI Server, C=RS",
                    keyPair.getPublic(),
                    getIssuerPrivateKey(issuer),
                    issuer,
                    endEntityValidityYears
            );

            // Save certificate
            Certificate certificate = CertificateBuilder.create()
                    .fromX509Certificate(serverCert, pemConverter, keyPair.getPublic())
                    .validityPeriod(endEntityValidityYears)
                    .type(CertificateType.END_ENTITY)
                    .status(CertificateStatus.ACTIVE)
                    .organization("PKI Server")
                    .issuerCertificate(issuer)
                    .build();

            Certificate savedCertificate = certificateRepository.save(certificate);

            // Store private key
            SecretKey masterKey = masterKeyService.getCurrentMasterKey();
            privateKeyService.storePrivateKey(savedCertificate, keyPair.getPrivate(), masterKey);

            return savedCertificate;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create server certificate", e);
        }
    }

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

            keyStore.setKeyEntry("key", privateKey, password.toCharArray(), new java.security.cert.Certificate[]{x509Cert});

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            keyStore.store(baos, password.toCharArray());

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to export PKCS12", e);
        }
    }
    public Page<Certificate> search(CertificateStatus status, CertificateType type, String organization, Pageable pageable) { return certificateRepository.findAll( Specification.where(CertificateSpecification.hasStatus(status)) .and(CertificateSpecification.hasType(type)) .and(CertificateSpecification.hasOrganization(organization)), pageable ); }
//    @Override
//    public User findUserByEmail(String email) {
//        return user;
//    }

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
        // Basic Constraints - not a CA
        certBuilder.addExtension(
                Extension.basicConstraints,
                true,
                new BasicConstraints(false)
        );

        // Key Usage for server authentication
        certBuilder.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment)
        );

        // Extended Key Usage for server authentication
        certBuilder.addExtension(
                Extension.extendedKeyUsage,
                false,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth)
        );

    }
}
