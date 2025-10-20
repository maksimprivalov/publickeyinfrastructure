package com.app.pki_backend.dto.certificate;

import com.app.pki_backend.entity.certificates.CertificateStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CertificateDTO {

    private Long id;
    private BigInteger serialNumber;
    private String subject; // X500Name in string format

    private Long issuerId; // X500Name in string format

    private String publicKey;
    private String encryptedPrivateKey; // EncryptedPrivateKeyInfo in string format
    private String certificateData; // PEM format certificate data
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private String type;
    private CertificateStatus status = CertificateStatus.ACTIVE;
    private Long ownerId;
    private List<Long> issuedCertificatesIds; // Certificates issued by this certificate
    private String extensions; // JSON string representing extensions

    private String organization;
}
