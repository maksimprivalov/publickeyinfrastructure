package com.app.pki_backend.controller;

import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.entity.certificates.CertificateTemplate;
import com.app.pki_backend.service.implementations.CertificateServiceImpl;
import com.app.pki_backend.service.implementations.CertificateTemplateServiceImpl;
import com.app.pki_backend.service.implementations.RevocationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    // === Выпуск сертификатов ===
    @PostMapping("/issue/root")
    public ResponseEntity<Certificate> issueRoot(@RequestBody CertificateTemplate template) {
        Certificate cert = certificateService.issueRoot(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(cert);
    }

    @PostMapping("/issue/intermediate")
    public ResponseEntity<Certificate> issueIntermediate(@RequestBody CertificateTemplate template) {
        Certificate cert = certificateService.issueIntermediate(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(cert);
    }

    @PostMapping("/issue/ee")
    public ResponseEntity<Certificate> issueEndEntity(@RequestBody CertificateTemplate template) {
        Certificate cert = certificateService.issueEndEntity(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(cert);
    }

    // === downloading certificates  ===
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable Integer id) {
        byte[] fileBytes = certificateService.exportAsPkcs12(id); // export PKCS12
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=certificate_" + id + ".p12");
        return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
    }

    // === get all certificates ===
    @GetMapping
    public ResponseEntity<List<Certificate>> listCertificates() {
        List<Certificate> certs = certificateService.findAll();
        return ResponseEntity.ok(certs);
    }

    // === certificate cancellation ===
    @PostMapping("/{id}/revoke")
    public ResponseEntity<String> revokeCertificate(@PathVariable Integer id,
                                                    @RequestParam String reason) {
        revocationService.revokeCertificate(id, reason);
        return ResponseEntity.ok("Certificate " + id + " revoked for reason: " + reason);
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
