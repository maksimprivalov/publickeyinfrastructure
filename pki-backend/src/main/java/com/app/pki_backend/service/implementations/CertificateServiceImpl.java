package com.app.pki_backend.service.implementations;

import com.app.pki_backend.service.interfaces.CertificateService;
import com.app.pki_backend.service.interfaces.CryptographyService;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;

@Service
public class CertificateServiceImpl implements CertificateService {

    @Autowired
    private CryptographyService cryptographyService;

    @Value("${pki.root-ca.subject}")
    private String rootCASubject;

    @Value("${pki.root-ca.validity-years}")
    private int validityYears;

    @Value("${pki.root-ca.key-size}")
    private int keySize;

    @Override
    public X509Certificate createRootCertificate() {
        try {
            KeyPair keyPair = cryptographyService.generateKeyPair(keySize);

            X509Certificate rootCert = buildRootCACertificate(
                    rootCASubject,
                    keyPair.getPublic(),
                    keyPair.getPrivate(),
                    validityYears
            );

            //TODO: add saving root certificate to DB, saving Public and Private keys

            return rootCert;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create root certificate", e);
        }
    }

    private X509Certificate buildRootCACertificate(
            String subjectDN,
            PublicKey publicKey,
            PrivateKey privateKey,
            int validityYears) throws Exception {

        X500Name subject = new X500Name(subjectDN);
        X500Name issuer = subject; // Self-signed для Root CA

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

    private void addRootCAExtensions(X509v3CertificateBuilder certBuilder) throws Exception {

        certBuilder.addExtension(
                Extension.basicConstraints,
                true, // critical
                new BasicConstraints(true)
        );

        certBuilder.addExtension(
                Extension.keyUsage,
                true, // critical
                new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign)
        );

//        certBuilder.addExtension(
//                Extension.subjectKeyIdentifier,
//                false,
//                cryptographyService.createSubjectKeyIdentifier(
//                        certBuilder.build(new JcaContentSignerBuilder("SHA256WithRSA")
//                                .build(privateKey)).getSubjectPublicKeyInfo()
//                )
//        );

    }

}
