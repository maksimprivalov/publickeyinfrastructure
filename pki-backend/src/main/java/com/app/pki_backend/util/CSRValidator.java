package com.app.pki_backend.util;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.springframework.stereotype.Component;

import java.security.PublicKey;

/**
 * Утилиты для работы с CSR
 */
@Component
public class CSRValidator {

    /**
     * КРИТИЧНО: Валидация подписи CSR
     * Это доказывает что отправитель владеет приватным ключом
     */
    public boolean validateCSRSignature(PKCS10CertificationRequest csr) {
        try {
            // Извлечь публичный ключ из CSR
            JcaPKCS10CertificationRequest jcaCSR = new JcaPKCS10CertificationRequest(csr);
            PublicKey publicKey = jcaCSR.getPublicKey();

            // Создать verifier с этим публичным ключом
            ContentVerifierProvider verifierProvider =
                    new JcaContentVerifierProviderBuilder()
                            .setProvider("BC")
                            .build(csr.getSubjectPublicKeyInfo());

            // Проверить подпись
            boolean valid = csr.isSignatureValid(verifierProvider);

            if (!valid) {
                System.err.println("❌ CSR signature verification FAILED");
                return false;
            }

            System.out.println("✅ CSR signature is valid");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Error validating CSR signature: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Извлечь Subject из CSR
     */
    public X500Name extractSubject(PKCS10CertificationRequest csr) {
        return csr.getSubject();
    }

    /**
     * Извлечь PublicKey из CSR
     */
    public PublicKey extractPublicKey(PKCS10CertificationRequest csr) throws Exception {
        JcaPKCS10CertificationRequest jcaCSR = new JcaPKCS10CertificationRequest(csr);
        return jcaCSR.getPublicKey();
    }

    /**
     * Валидация CSR по шаблону (если используется)
     */
    public boolean validateCSRAgainstTemplate(PKCS10CertificationRequest csr,
                                              String cnRegex,
                                              String sanRegex) {
        try {
            X500Name subject = csr.getSubject();
            String subjectStr = subject.toString();

            // Проверка CN regex
            if (cnRegex != null && !cnRegex.isEmpty()) {
                if (!subjectStr.matches(cnRegex)) {
                    System.err.println("❌ CSR CN does not match template regex: " + cnRegex);
                    return false;
                }
            }

            // TODO: Проверка SAN regex если необходимо

            return true;

        } catch (Exception e) {
            System.err.println("❌ Error validating CSR against template: " + e.getMessage());
            return false;
        }
    }
}