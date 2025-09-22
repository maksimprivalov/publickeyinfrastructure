package com.app.pki_backend.controller;

import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.entity.certificates.RevokedCertificate;
import com.app.pki_backend.entity.user.User;
import com.app.pki_backend.service.implementations.CertificateServiceImpl;
import com.app.pki_backend.service.implementations.RevocationServiceImpl;
import com.app.pki_backend.service.interfaces.UserService;
import com.app.pki_backend.util.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/revocations")
public class RevocationController {


    private final RevocationServiceImpl revocationService;
    private final CertificateServiceImpl certificateService;
    private final TokenUtils tokenUtils;
    private final UserService userService;
    @Autowired
    public RevocationController(RevocationServiceImpl revocationService,
                                CertificateServiceImpl certificateService,
                                TokenUtils tokenUtils, UserService userService) {
        this.revocationService = revocationService;
        this.certificateService = certificateService;
        this.tokenUtils = tokenUtils;
        this.userService = userService;
    }

    // === POST revoke certificate ===
    @PostMapping("/{id}/revoke")
    public ResponseEntity<String> revokeCertificate(
            @PathVariable Long id,
            @RequestParam String reason,
            HttpServletRequest request) {

        String token = tokenUtils.getToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing token");
        }
        String email = tokenUtils.getUsernameFromToken(token);
        User revokedBy = userService.findByEmail(email);

        Certificate cert = certificateService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));

        revocationService.revokeCertificate(cert, reason, revokedBy);
        return ResponseEntity.ok("Certificate " + id + " revoked with reason: " + reason);
    }

    // === GET list of revoked certs ===
    @GetMapping
    public ResponseEntity<List<RevokedCertificate>> listRevoked() {
        return ResponseEntity.ok(revocationService.listRevoked());
    }

    // === GET CRL ===
    @GetMapping("/crl")
    public ResponseEntity<byte[]> downloadCRL(@RequestParam Long issuerId) {
        Certificate issuer = certificateService.findById(issuerId)
                .orElseThrow(() -> new IllegalArgumentException("Issuer not found"));

        byte[] crl = revocationService.generateCRL(issuer);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=crl_" + issuerId + ".crl")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(crl);
    }
}
