package com.app.pki_backend.service.implementations;

import com.app.pki_backend.dto.certificate.CertificateSigningRequest;
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
    public CertificateTemplateServiceImpl(PEMConverter pemConverter, CertificateTemplateRepository templateRepository) {
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
    public CertificateSigningRequest applyTemplate(CertificateSigningRequest csr, CertificateTemplate template) {
        csr.setRejectionReason("Applied template: " + template.getId());
        return csr;
    }

    @Override
    public boolean validateAgainstTemplate(CertificateSigningRequest csr, CertificateTemplate template) {
        try {
            PKCS10CertificationRequest pkcs10CSR = pemConverter.parseCSR(csr.getCsrContent());
            JcaPKCS10CertificationRequest jcaCSR = new JcaPKCS10CertificationRequest(pkcs10CSR);

            X500Name subject = pkcs10CSR.getSubject();
            String subjectStr = subject.toString();

            if (template.getCnRegex() != null && !subjectStr.matches(template.getCnRegex())) {
                return false;
            }

            Extensions extensions = pkcs10CSR.getRequestedExtensions();
            if (extensions != null && extensions.getExtension(Extension.subjectAlternativeName) != null) {
                GeneralNames gns = GeneralNames.getInstance(
                        extensions.getExtension(Extension.subjectAlternativeName).getParsedValue()
                );
                StringBuilder sanBuilder = new StringBuilder();
                for (GeneralName gn : gns.getNames()) {
                    sanBuilder.append(gn.getName().toString()).append(",");
                }
                String sanStr = sanBuilder.toString();

                if (template.getSanRegex() != null && !sanStr.matches(template.getSanRegex())) {
                    return false;
                }
            }

            if (template.getMaxTtlDays() != null) {
                int requestedTtlDays = template.getMaxTtlDays(); // или брать из внешнего DTO
                if (requestedTtlDays > template.getMaxTtlDays()) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate CSR against template", e);
        }
    }
}

