package com.app.pki_backend.controller;

import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.entity.certificates.CertificateStatus;
import com.app.pki_backend.entity.certificates.RevokedCertificate;
import com.app.pki_backend.entity.certificates.RevocationReason;
import com.app.pki_backend.entity.user.User;
import com.app.pki_backend.service.implementations.CertificateServiceImpl;
import com.app.pki_backend.service.implementations.RevocationServiceImpl;
import com.app.pki_backend.service.interfaces.UserService;
import com.app.pki_backend.util.TokenUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RevocationController.class)
@AutoConfigureMockMvc(addFilters = false)
class RevocationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean RevocationServiceImpl revocationService;
    @MockBean CertificateServiceImpl certificateService;
    @MockBean TokenUtils tokenUtils;
    @MockBean UserService userService;

    // ===== helpers =====

    private Certificate cert(Long id) {
        Certificate c = new Certificate();
        c.setId(id);
        c.setSerialNumber(new BigInteger("123456789"));
        c.setSubject("CN=test");
        c.setIssuer("CN=issuer");
        c.setPublicKey("PUB");
        c.setCertificateData("PEM");
        c.setValidFrom(LocalDateTime.now().minusDays(1));
        c.setValidTo(LocalDateTime.now().plusDays(365));
        c.setStatus(CertificateStatus.ACTIVE);
        c.setOrganization("OrgA");
        return c;
    }

    private User user(int id, String email) {
        User u = new User();
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, id);
        } catch (Exception ignored) {}
        u.setEmail(email);
        u.setName("Test");
        u.setSurname("User");
        u.setOrganizationName("OrgA");
        u.setActive(true);
        u.setRole("User");
        u.setPassword("pass");
        return u;
    }

    // ===== tests =====

    @Test
    @DisplayName("POST /api/revocations/{id}/revoke — успех")
    void revoke_ok() throws Exception {
        var c = cert(42L);
        var actor = user(7, "actor@pki.local");

        given(tokenUtils.getToken(any())).willReturn("tok");
        given(tokenUtils.getUsernameFromToken("tok")).willReturn("actor@pki.local");
        given(userService.findByEmail("actor@pki.local")).willReturn(actor);
        given(certificateService.findById(42L)).willReturn(Optional.of(c));

        mockMvc.perform(post("/api/revocations/{id}/revoke", 42L)
                        .param("reason", "KEY_COMPROMISE")
                        .header("Token", "Bearer tok") // не обязателен, т.к. getToken замокан
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Certificate 42 revoked")));

        verify(revocationService).revokeCertificate(eq(c), eq("KEY_COMPROMISE"), eq(actor));
    }

    @Test
    @DisplayName("POST /api/revocations/{id}/revoke — 401 без токена")
    void revoke_unauthorized_when_no_token() throws Exception {
        given(tokenUtils.getToken(any())).willReturn(null);

        mockMvc.perform(post("/api/revocations/{id}/revoke", 1L)
                        .param("reason", "KEY_COMPROMISE"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Missing token")));
    }

    @Test
    @DisplayName("POST /api/revocations/{id}/revoke — 400 когда сертификат не найден")
    void revoke_cert_not_found() throws Exception {
        var actor = user(7, "actor@pki.local");

        given(tokenUtils.getToken(any())).willReturn("tok");
        given(tokenUtils.getUsernameFromToken("tok")).willReturn("actor@pki.local");
        given(userService.findByEmail("actor@pki.local")).willReturn(actor);
        given(certificateService.findById(999L)).willReturn(Optional.empty());

        mockMvc.perform(post("/api/revocations/{id}/revoke", 999L)
                        .param("reason", "KEY_COMPROMISE"))
                // если GlobalExceptionHandler мапит IllegalArgumentException -> 400:
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/revocations — список отзывов")
    void list_revoked_ok() throws Exception {
        var c = cert(10L);
        var actor = user(7, "actor@pki.local");

        RevokedCertificate r = new RevokedCertificate();
        r.setId(1L);
        r.setCertificate(c);
        r.setReason(RevocationReason.KEY_COMPROMISE);
        r.setRevokedBy(actor);
        r.setRevocationDate(LocalDateTime.now());

        given(revocationService.listRevoked()).willReturn(List.of(r));

        mockMvc.perform(get("/api/revocations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].reason").value("KEY_COMPROMISE"));
    }

    @Test
    @DisplayName("GET /api/revocations/crl — отдать CRL по issuerId")
    void download_crl_ok() throws Exception {
        var issuer = cert(5L);
        given(certificateService.findById(5L)).willReturn(Optional.of(issuer));

        byte[] crl = "FAKE-CRL".getBytes();
        given(revocationService.generateCRL(issuer)).willReturn(crl);

        mockMvc.perform(get("/api/revocations/crl").param("issuerId", "5"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("crl_5.crl")))
                .andExpect(content().bytes(crl));
    }
}
