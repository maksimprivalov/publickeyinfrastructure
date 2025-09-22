package com.app.pki_backend.dto.keystore;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for certificate chain information response.
 */
public class CertificateChainInfo {

    private Long certificateId;
    private int chainLength;
    private List<CertificateInfo> certificateChain;

    public CertificateChainInfo() {}

    public CertificateChainInfo(Long certificateId, int chainLength, List<CertificateInfo> certificateChain) {
        this.certificateId = certificateId;
        this.chainLength = chainLength;
        this.certificateChain = certificateChain;
    }

    // Getters and setters
    public Long getCertificateId() { return certificateId; }
    public void setCertificateId(Long certificateId) { this.certificateId = certificateId; }

    public int getChainLength() { return chainLength; }
    public void setChainLength(int chainLength) { this.chainLength = chainLength; }

    public List<CertificateInfo> getCertificateChain() { return certificateChain; }
    public void setCertificateChain(List<CertificateInfo> certificateChain) { this.certificateChain = certificateChain; }

    /**
     * Inner class for individual certificate information in the chain.
     */
    public static class CertificateInfo {
        private String subject;
        private String issuer;
        private String serialNumber;
        private String notBefore;
        private String notAfter;
        private boolean isCA;

        public CertificateInfo() {}

        public CertificateInfo(String subject, String issuer, String serialNumber,
                             String notBefore, String notAfter, boolean isCA) {
            this.subject = subject;
            this.issuer = issuer;
            this.serialNumber = serialNumber;
            this.notBefore = notBefore;
            this.notAfter = notAfter;
            this.isCA = isCA;
        }

        // Getters and setters
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }

        public String getSerialNumber() { return serialNumber; }
        public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

        public String getNotBefore() { return notBefore; }
        public void setNotBefore(String notBefore) { this.notBefore = notBefore; }

        public String getNotAfter() { return notAfter; }
        public void setNotAfter(String notAfter) { this.notAfter = notAfter; }

        public boolean isCA() { return isCA; }
        public void setCA(boolean CA) { isCA = CA; }
    }
}
