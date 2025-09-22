package com.app.pki_backend.controller;

import com.app.pki_backend.dto.certificate.CertificateSigningRequest;
import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.entity.certificates.CertificateTemplate;
import com.app.pki_backend.entity.user.User;
import com.app.pki_backend.service.implementations.CertificateServiceImpl;
import com.app.pki_backend.service.implementations.CertificateTemplateServiceImpl;
import com.app.pki_backend.service.implementations.RevocationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/certificates")
public class CertificateController {

    private final CertificateServiceImpl certificateService;
    private final RevocationServiceImpl revocationService;
    private final CertificateTemplateServiceImpl templateService;

    @Autowired
    public CertificateController(CertificateServiceImpl certificateService,
                                 RevocationServiceImpl revocationService,
                                 CertificateTemplateServiceImpl templateService) {
        this.certificateService = certificateService;
        this.revocationService = revocationService;
        this.templateService = templateService;
    }

    @GetMapping
    public ResponseEntity<List<Certificate>> getAllCertificates(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();

        List<Certificate> certificates;

        switch (currentUser.getRole().toUpperCase()) {
            case "ADMIN":
                certificates = certificateService.findAll();
                break;
            case "CAUSER":
                certificates = certificateService.findAllByOrganization(currentUser.getOrganizationName());
                break;
            case "USER":
            default:
                certificates = certificateService.findAllByOwnerId(currentUser.getId());
                break;
        }

        return ResponseEntity.ok(certificates);
    }
    // === GET certificate by id ===
    @GetMapping("/{id}")
    public ResponseEntity<Certificate> getCertificateById(@PathVariable Long id) {
        return certificateService.findById(id)
                .map(cert -> ResponseEntity.ok(cert))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // === DELETE certificate ===
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCertificate(@PathVariable Long id) {
        if (certificateService.findById(id).isPresent()) {
            certificateService.delete(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    @PostMapping("/issue/root")
    public ResponseEntity<Certificate> issueRoot() {
        Certificate cert = certificateService.issueRootCertificate();
        return ResponseEntity.status(HttpStatus.CREATED).body(cert);
    }

    @PostMapping("/issue/intermediate/{issuerId}")
    public ResponseEntity<Certificate> issueIntermediate(
            @PathVariable Long issuerId,
            @RequestBody CertificateSigningRequest csr) {

        Certificate issuer = certificateService.findById(issuerId)
                .orElseThrow(() -> new IllegalArgumentException("Issuer not found"));

        Certificate cert = certificateService.issueIntermediateCertificate(csr, issuer);
        return ResponseEntity.status(HttpStatus.CREATED).body(cert);
    }

    @PostMapping("/issue/ee/{issuerId}")
    public ResponseEntity<Certificate> issueEndEntity(
            @PathVariable Long issuerId,
            @RequestBody CertificateSigningRequest csr) {

        Certificate issuer = certificateService.findById(issuerId)
                .orElseThrow(() -> new IllegalArgumentException("Issuer not found"));

        Certificate cert = certificateService.issueEndEntityCertificate(csr, issuer);
        return ResponseEntity.status(HttpStatus.CREATED).body(cert);
    }
    @PostMapping("/issue/root/template/{templateId}")
    public ResponseEntity<Certificate> issueRootWithTemplate(@PathVariable Long templateId) {
        Certificate cert = certificateService.issueRootWithTemplate(templateId);
        return ResponseEntity.status(HttpStatus.CREATED).body(cert);
    }

    @PostMapping("/issue/intermediate/template/{templateId}")
    public ResponseEntity<Certificate> issueIntermediateWithTemplate(
            @PathVariable Long templateId,
            @RequestBody CertificateSigningRequest csr) {

        Certificate cert = certificateService.issueIntermediateWithTemplate(templateId, csr);
        return ResponseEntity.status(HttpStatus.CREATED).body(cert);
    }

    @PostMapping("/issue/ee/template/{templateId}")
    public ResponseEntity<Certificate> issueEndEntityWithTemplate(
            @PathVariable Long templateId,
            @RequestBody CertificateSigningRequest csr) {

        Certificate cert = certificateService.issueEndEntityWithTemplate(templateId, csr);
        return ResponseEntity.status(HttpStatus.CREATED).body(cert);
    }

    // === work with templates ===
    @PostMapping("/templates")
    public ResponseEntity<CertificateTemplate> createTemplate(@RequestBody CertificateTemplate template) {
        CertificateTemplate saved = templateService.createTemplate(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/templates")
    public ResponseEntity<List<CertificateTemplate>> listTemplates() {
        List<CertificateTemplate> templates = templateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<String> deleteTemplate(@PathVariable Integer id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok("Template " + id + " deleted.");
    }
}
