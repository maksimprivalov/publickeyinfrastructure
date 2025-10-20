package com.app.pki_backend.service.implementations;

import com.app.pki_backend.entity.certificates.CertificateSigningRequest;
import com.app.pki_backend.entity.certificates.CertificateTemplate;
import com.app.pki_backend.repository.CertificateTemplateRepository;
import com.app.pki_backend.service.interfaces.CertificateTemplateService;
import com.app.pki_backend.util.PEMConverter;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CertificateTemplateServiceImpl implements CertificateTemplateService {
    private final PEMConverter pemConverter;
    private final CertificateTemplateRepository templateRepository;

    @Autowired
    public CertificateTemplateServiceImpl(PEMConverter pemConverter,
                                          CertificateTemplateRepository templateRepository) {
        this.pemConverter = pemConverter;
        this.templateRepository = templateRepository;
    }

    @Override
    public CertificateTemplate findById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with id=" + id));
    }

    @Override
    public CertificateTemplate createTemplate(CertificateTemplate template) {
        return templateRepository.save(template);
    }

    @Override
    public List<CertificateTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }

    @Override
    public void deleteTemplate(Integer id) {
        CertificateTemplate template = templateRepository.findById(id.longValue())
                .orElseThrow(() -> new IllegalArgumentException("Template not found with id=" + id));
        templateRepository.delete(template);
    }

    @Override
    public CertificateSigningRequest applyTemplate(CertificateSigningRequest csr,
                                                   CertificateTemplate template) {
        // Применяем настройки шаблона к CSR
        // В реальной системе здесь можно модифицировать CSR, добавляя дефолтные значения
        csr.setRejectionReason("Applied template: " + template.getName());
        return csr;
    }

    @Override
    public boolean validateAgainstTemplate(CertificateSigningRequest csr,
                                           CertificateTemplate template) {
        return validateAgainstTemplate(csr, template, null);
    }

    /**
     * ✅ FIX: Улучшенная валидация с поддержкой TTL
     *
     * @param csr CSR для валидации
     * @param template Шаблон для валидации
     * @param requestedTtlDays Запрошенное время жизни в днях (nullable)
     * @return true если CSR соответствует шаблону
     */
    public boolean validateAgainstTemplate(CertificateSigningRequest csr,
                                           CertificateTemplate template,
                                           Integer requestedTtlDays) {
        try {
            PKCS10CertificationRequest pkcs10CSR = pemConverter.parseCSR(csr.getCsrContent());
            JcaPKCS10CertificationRequest jcaCSR = new JcaPKCS10CertificationRequest(pkcs10CSR);

            X500Name subject = pkcs10CSR.getSubject();
            String subjectStr = subject.toString();

            // 1. Валидация CN по регулярному выражению
            if (template.getCnRegex() != null && !template.getCnRegex().isEmpty()) {
                String cn = extractCN(subject);
                if (cn == null || !cn.matches(template.getCnRegex())) {
                    System.err.println("❌ CN validation failed: " + cn +
                            " does not match regex: " + template.getCnRegex());
                    return false;
                }
                System.out.println("✅ CN validation passed: " + cn);
            }

            // 2. Валидация SAN по регулярному выражению
            if (template.getSanRegex() != null && !template.getSanRegex().isEmpty()) {
                Extensions extensions = pkcs10CSR.getRequestedExtensions();

                if (extensions != null) {
                    org.bouncycastle.asn1.x509.Extension sanExt =
                            extensions.getExtension(Extension.subjectAlternativeName);

                    if (sanExt != null) {
                        GeneralNames gns = GeneralNames.getInstance(sanExt.getParsedValue());
                        StringBuilder sanBuilder = new StringBuilder();

                        for (GeneralName gn : gns.getNames()) {
                            if (sanBuilder.length() > 0) {
                                sanBuilder.append(",");
                            }
                            sanBuilder.append(gn.getName().toString());
                        }

                        String sanStr = sanBuilder.toString();

                        if (!sanStr.matches(template.getSanRegex())) {
                            System.err.println("❌ SAN validation failed: " + sanStr +
                                    " does not match regex: " + template.getSanRegex());
                            return false;
                        }
                        System.out.println("✅ SAN validation passed: " + sanStr);
                    }
                }
            }

            // 3. ✅ FIX: Валидация TTL
            if (template.getMaxTtlDays() != null) {
                if (requestedTtlDays == null) {
                    System.err.println("⚠️ Warning: No TTL specified in request, " +
                            "but template requires max TTL of " + template.getMaxTtlDays() + " days");
                    // Можно либо отклонить, либо использовать дефолтное значение
                    // Для строгой валидации - отклоняем:
                    return false;
                }

                if (requestedTtlDays > template.getMaxTtlDays()) {
                    System.err.println("❌ TTL validation failed: requested " + requestedTtlDays +
                            " days exceeds maximum " + template.getMaxTtlDays() + " days");
                    return false;
                }
                System.out.println("✅ TTL validation passed: " + requestedTtlDays +
                        " <= " + template.getMaxTtlDays());
            }

            // 4. Валидация Key Usage (опционально)
            if (template.getDefaultKeyUsage() != null && !template.getDefaultKeyUsage().isEmpty()) {
                // В будущем можно проверять соответствие Key Usage из CSR
                // с требуемыми значениями из шаблона
                System.out.println("ℹ️ Template defines Key Usage: " + template.getDefaultKeyUsage());
            }

            // 5. Валидация Extended Key Usage (опционально)
            if (template.getDefaultExtendedKeyUsage() != null &&
                    !template.getDefaultExtendedKeyUsage().isEmpty()) {
                System.out.println("ℹ️ Template defines Extended Key Usage: " +
                        template.getDefaultExtendedKeyUsage());
            }

            System.out.println("✅ All template validations passed");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Template validation error: " + e.getMessage());
            throw new RuntimeException("Failed to validate CSR against template", e);
        }
    }

    /**
     * ✅ FIX: Извлечение Common Name из Subject
     */
    private String extractCN(X500Name subject) {
        String subjectStr = subject.toString();

        // Парсинг CN из Distinguished Name
        String[] parts = subjectStr.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("CN=")) {
                return part.substring(3);
            }
        }

        return null;
    }

    /**
     * ✅ FIX: Извлечение Organization из Subject
     */
    private String extractO(X500Name subject) {
        String subjectStr = subject.toString();

        String[] parts = subjectStr.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("O=")) {
                return part.substring(2);
            }
        }

        return null;
    }

    /**
     * ✅ FIX: Получить рекомендуемый TTL из шаблона
     */
    public Integer getRecommendedTTL(CertificateTemplate template) {
        if (template.getMaxTtlDays() != null) {
            // Возвращаем максимальное значение как рекомендуемое
            return template.getMaxTtlDays();
        }
        return null;
    }

    /**
     * ✅ FIX: Проверка соответствия организации
     */
    public boolean validateOrganization(CertificateSigningRequest csr, String expectedOrg) {
        try {
            PKCS10CertificationRequest pkcs10CSR = pemConverter.parseCSR(csr.getCsrContent());
            X500Name subject = pkcs10CSR.getSubject();

            String actualOrg = extractO(subject);

            if (actualOrg == null) {
                System.err.println("❌ No organization found in CSR");
                return false;
            }

            if (!actualOrg.equals(expectedOrg)) {
                System.err.println("❌ Organization mismatch: expected '" + expectedOrg +
                        "', found '" + actualOrg + "'");
                return false;
            }

            System.out.println("✅ Organization validation passed: " + actualOrg);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Organization validation error: " + e.getMessage());
            return false;
        }
    }
}