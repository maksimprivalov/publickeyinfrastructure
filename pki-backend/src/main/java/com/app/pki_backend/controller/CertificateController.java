package com.app.pki_backend.controller;

import com.app.pki_backend.dto.certificate.CertificateSigningRequest;
import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.entity.certificates.CertificateStatus;
import com.app.pki_backend.entity.certificates.CertificateTemplate;
import com.app.pki_backend.entity.certificates.CertificateType;
import com.app.pki_backend.entity.user.User;
import com.app.pki_backend.service.implementations.CertificateServiceImpl;
import com.app.pki_backend.service.implementations.CertificateTemplateServiceImpl;
import com.app.pki_backend.service.implementations.RevocationServiceImpl;
import com.app.pki_backend.service.interfaces.UserService;
import com.app.pki_backend.util.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;

@RestController
@RequestMapping(value = "/api/certificates")
public class CertificateController {

    private final CertificateServiceImpl certificateService;
    private final RevocationServiceImpl revocationService;
    private final CertificateTemplateServiceImpl templateService;
    @Autowired
    private TokenUtils tokenUtils;
    @Autowired
    private UserService userService;

    @Autowired
    public CertificateController(CertificateServiceImpl certificateService,
                                 RevocationServiceImpl revocationService,
                                 CertificateTemplateServiceImpl templateService) {
        this.certificateService = certificateService;
        this.revocationService = revocationService;
        this.templateService = templateService;
    }

    @GetMapping
    public ResponseEntity<List<Certificate>> getAllCertificates(HttpServletRequest request) {
        String token = tokenUtils.getToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = tokenUtils.getUsernameFromToken(token);
        User currentUser = userService.findByEmail(email);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

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

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadCertificate(
            @PathVariable Long id,
            @RequestParam(defaultValue = "changeit") String password) {

        byte[] fileBytes = certificateService.exportAsPkcs12(id, password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=certificate_" + id + ".p12");

        return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
    }

    @PostMapping("/csr/upload/{issuerId}")
    public ResponseEntity<Certificate> uploadCsr(
            @PathVariable Long issuerId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        try {
            String jwtToken = tokenUtils.getToken(request);
            if (jwtToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String email = tokenUtils.getUsernameFromToken(jwtToken);
            User user = userService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Certificate issuer = certificateService.findById(issuerId)
                    .orElseThrow(() -> new IllegalArgumentException("Issuer not found"));

            String csrContent = new String(file.getBytes());

            CertificateSigningRequest csr = new CertificateSigningRequest();
            csr.setCsrContent(csrContent);
            csr.setRequestedBy(user);
            csr.setSelectedCA(issuer);

            Certificate issuedCert = certificateService.issueCertificateFromCSR(file.getBytes(), issuer);

            return ResponseEntity.status(HttpStatus.CREATED).body(issuedCert);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping("/search")
    public ResponseEntity<Page<Certificate>> searchCertificates(
            @RequestParam(required = false) CertificateStatus status,
            @RequestParam(required = false) CertificateType type,
            @RequestParam(required = false) String organization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Certificate> result = certificateService.search(status, type, organization, pageable);
        return ResponseEntity.ok(result);
    }
}
