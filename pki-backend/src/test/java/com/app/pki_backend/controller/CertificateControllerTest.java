package com.app.pki_backend.controller;

import com.app.pki_backend.dto.certificate.CertificateSigningRequest;
import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.entity.certificates.CertificateStatus;
import com.app.pki_backend.entity.certificates.CertificateTemplate;
import com.app.pki_backend.entity.certificates.CertificateType;
import com.app.pki_backend.entity.user.Admin;
import com.app.pki_backend.entity.user.CAUser;
import com.app.pki_backend.entity.user.User;
import com.app.pki_backend.service.implementations.CertificateServiceImpl;
import com.app.pki_backend.service.implementations.CertificateTemplateServiceImpl;
import com.app.pki_backend.service.implementations.RevocationServiceImpl;
import com.app.pki_backend.service.interfaces.UserService;
import com.app.pki_backend.util.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
/**
 * Controller-layer tests for CertificateController WITHOUT @MockBean.
 * Mocks are provided via @TestConfiguration beans.
 */
@WebMvcTest(controllers = CertificateController.class)
@AutoConfigureMockMvc(addFilters = false)
//@Import(CertificateControllerTest.MockConfig.class)
class CertificateControllerTest {
    @MockBean CertificateServiceImpl certificateService;
    @MockBean RevocationServiceImpl revocationService;
    @MockBean
    CertificateTemplateServiceImpl templateService;
    @MockBean TokenUtils tokenUtils;
    @MockBean UserService userService;
    @TestConfiguration
    static class MockConfig {
        @Bean CertificateServiceImpl certificateService() {
            return Mockito.mock(CertificateServiceImpl.class);
        }
        @Bean RevocationServiceImpl revocationService() {
            return Mockito.mock(RevocationServiceImpl.class);
        }
        @Bean CertificateTemplateServiceImpl templateService() {
            return Mockito.mock(CertificateTemplateServiceImpl.class);
        }
        @Bean TokenUtils tokenUtils() {
            return Mockito.mock(TokenUtils.class);
        }
        @Bean UserService userService() {
            return Mockito.mock(UserService.class);
        }
    }

    @Autowired MockMvc mockMvc;
//    @Autowired CertificateServiceImpl certificateService;
//    @Autowired RevocationServiceImpl revocationService;
//    @Autowired CertificateTemplateServiceImpl templateService;
//    @Autowired TokenUtils tokenUtils;
//    @Autowired UserService userService;

    private User admin, caUser, regularUser;

    private Certificate sampleCert(Long id, CertificateType type, String org, Integer ownerId) {
        Certificate c = new Certificate();
        c.setId(id);
        c.setSerialNumber(new BigInteger("123456789"));
        c.setSubject("CN=example.com,O=" + org);
        c.setIssuer("CN=issuer");
        c.setPublicKey("PEM-PUB");
        c.setCertificateData("PEM-CERT");
        c.setValidFrom(LocalDateTime.now().minusDays(1));
        c.setValidTo(LocalDateTime.now().plusDays(365));
        c.setType(type);
        c.setStatus(CertificateStatus.ACTIVE);
        c.setOrganization(org);
        if (ownerId != null) {
            User u = new User();
            try {
                var f = User.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(u, ownerId);
            } catch (Exception ignored) {}
            u.setRole("User");
            u.setEmail("owner" + ownerId + "@pki.local");
            u.setName("Owner");
            u.setSurname("Test");
            u.setOrganizationName(org);
            u.setActive(true);
            u.setPassword("pass");
            c.setOwner(u);
        }
        return c;
    }

    private UsernamePasswordAuthenticationToken auth(User u) {
        return new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities());
    }

    @BeforeEach
    void setUp() {
        admin = new Admin();
        fillUser(admin, 1, "admin@pki.local", "PKI-Org");
        admin.setRole("ADMIN");   // 👈 лучше upper

        caUser = new CAUser();
        fillUser(caUser, 2, "ca@pki.local", "OrgA");
        caUser.setRole("CAUSER"); // 👈 upper

        regularUser = new User();
        fillUser(regularUser, 3, "user@pki.local", "Client-Org");
        regularUser.setRole("USER"); // 👈 upper
    }

    private void fillUser(User user, int id, String email, String org) {
        user.setId(id);
        user.setEmail(email);
        user.setName("Test");
        user.setSurname("User");
        user.setOrganizationName(org);
        user.setActive(true);
        user.setPassword("pass");
    }
    private void setUser(User u, int id, String email, String role, String org) throws Exception {
        var idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(u, id);
        u.setEmail(email);
        u.setRole(role);
        u.setOrganizationName(org);
        u.setActive(true);
    }

    // --- RBAC listing ---
    @Test
    @DisplayName("Admin sees all certs")
    void getAll_asAdmin() throws Exception {
        var list = List.of(
                sampleCert(10L, CertificateType.INTERMEDIATE_CA, "OrgA", null),
                sampleCert(11L, CertificateType.END_ENTITY, "Client-Org", 3)
        );
        given(certificateService.findAll()).willReturn(list);

        given(tokenUtils.getToken(any())).willReturn("admintoken");
        given(tokenUtils.getUsernameFromToken("admintoken")).willReturn("admin@pki.local");
        given(userService.findByEmail("admin@pki.local")).willReturn(admin);

        mockMvc.perform(get("/api/certificates")
                        .header("Authorization", "Bearer admintoken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("CA user sees only org chain")
    void getAll_asCAUser() throws Exception {
        var list = List.of(
                sampleCert(20L, CertificateType.INTERMEDIATE_CA, "OrgA", null),
                sampleCert(21L, CertificateType.END_ENTITY, "OrgA", 5)
        );
        given(certificateService.findAllByOrganization("OrgA")).willReturn(list);

        given(tokenUtils.getToken(any())).willReturn("faketoken");
        given(tokenUtils.getUsernameFromToken("faketoken")).willReturn("ca@pki.local");
        given(userService.findByEmail("ca@pki.local")).willReturn(caUser);

        mockMvc.perform(get("/api/certificates")
                        .header("Authorization", "Bearer faketoken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].organization", everyItem(is("OrgA"))));
    }


    @Test
    @DisplayName("Regular user sees only own EE certs")
    void getAll_asUser() throws Exception {
        var list = List.of(sampleCert(30L, CertificateType.END_ENTITY, "Client-Org", 3));
        given(certificateService.findAllByOwnerId(3)).willReturn(list);

        given(tokenUtils.getToken(any())).willReturn("faketoken");
        given(tokenUtils.getUsernameFromToken("faketoken")).willReturn("user@pki.local");
        given(userService.findByEmail("user@pki.local")).willReturn(regularUser);

        mockMvc.perform(get("/api/certificates")
                        .header("Authorization", "Bearer faketoken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type", is("END_ENTITY")));
    }

    // --- Basic CRUD ---
    @Test
    void getById_found() throws Exception {
        var cert = sampleCert(42L, CertificateType.END_ENTITY, "X", 3);
        given(certificateService.findById(42L)).willReturn(Optional.of(cert));

        mockMvc.perform(get("/api/certificates/42").with(authentication(auth(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(42)));
    }

    @Test
    void getById_notFound() throws Exception {
        given(certificateService.findById(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/certificates/99").with(authentication(auth(admin))))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_ok() throws Exception {
        given(certificateService.findById(50L)).willReturn(Optional.of(sampleCert(50L, CertificateType.END_ENTITY, "X", 1)));

        mockMvc.perform(delete("/api/certificates/50").with(authentication(auth(admin))))
                .andExpect(status().isNoContent());
        verify(certificateService).delete(50L);
    }

    @Test
    void delete_notFound() throws Exception {
        given(certificateService.findById(77L)).willReturn(Optional.empty());

        mockMvc.perform(delete("/api/certificates/77").with(authentication(auth(admin))))
                .andExpect(status().isNotFound());
    }

    // --- Issue endpoints ---
//    @Test
//    void issueRoot_created() throws Exception {
//        var created = sampleCert(100L, CertificateType.ROOT_CA, "PKI Root CA", null);
//        given(certificateService.issueRootCertificate()).willReturn(created);
//
//        mockMvc.perform(post("/api/certificates/issue/root").with(authentication(auth(admin))))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.type", is("ROOT_CA")));
//    }

    @Test
    void issueIntermediate_created() throws Exception {
        var issuer = sampleCert(1L, CertificateType.ROOT_CA, "Root", null);
        var created = sampleCert(101L, CertificateType.INTERMEDIATE_CA, "OrgA", null);

        given(certificateService.findById(1L)).willReturn(Optional.of(issuer));
        given(certificateService.issueIntermediateCertificate(any(CertificateSigningRequest.class), eq(issuer)))
                .willReturn(created);

        String body = "{\"csrContent\":\"-----BEGIN CSR-----\\n...\\n-----END CSR-----\"}";
        mockMvc.perform(post("/api/certificates/issue/intermediate/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(authentication(auth(admin))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("INTERMEDIATE_CA")));
    }

    @Test
    void issueEE_created() throws Exception {
        var issuer = sampleCert(2L, CertificateType.INTERMEDIATE_CA, "OrgA", null);
        var created = sampleCert(102L, CertificateType.END_ENTITY, "OrgA", 3);

        given(certificateService.findById(2L)).willReturn(Optional.of(issuer));
        given(certificateService.issueEndEntityCertificate(any(CertificateSigningRequest.class), eq(issuer)))
                .willReturn(created);

        String body = "{\"csrContent\":\"-----BEGIN CSR-----\\n...\\n-----END CSR-----\"}";
        mockMvc.perform(post("/api/certificates/issue/ee/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(authentication(auth(caUser))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("END_ENTITY")));
    }

    // --- Issue with template ---
//    @Test
//    void issueRoot_withTemplate_created() throws Exception {
//        var created = sampleCert(200L, CertificateType.ROOT_CA, "Root", null);
//        given(certificateService.issueRootWithTemplate(5L)).willReturn(created);
//
//        mockMvc.perform(post("/api/certificates/issue/root/template/5")
//                        .with(authentication(auth(admin))))
//                .andExpect(status().isCreated());
//    }

    @Test
    void issueIntermediate_withTemplate_created() throws Exception {
        var created = sampleCert(201L, CertificateType.INTERMEDIATE_CA, "OrgA", null);
        given(certificateService.issueIntermediateWithTemplate(eq(7L), any(CertificateSigningRequest.class)))
                .willReturn(created);

        String body = "{\"csrContent\":\"CSR\"}";
        mockMvc.perform(post("/api/certificates/issue/intermediate/template/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(authentication(auth(caUser))))
                .andExpect(status().isCreated());
    }

    @Test
    void issueEE_withTemplate_created() throws Exception {
        var created = sampleCert(202L, CertificateType.END_ENTITY, "OrgA", 3);
        given(certificateService.issueEndEntityWithTemplate(eq(8L), any(CertificateSigningRequest.class)))
                .willReturn(created);

        String body = "{\"csrContent\":\"CSR\"}";
        mockMvc.perform(post("/api/certificates/issue/ee/template/8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(authentication(auth(regularUser))))
                .andExpect(status().isCreated());
    }

    // --- Templates CRUD ---
    @Test
    void createTemplate_created() throws Exception {
        CertificateTemplate t = new CertificateTemplate();
        t.setId(1L); t.setName("Default");
        given(templateService.createTemplate(any(CertificateTemplate.class))).willReturn(t);

        String body = """
          {"name":"Default","cnRegex":".*","sanRegex":".*","maxTtlDays":365}
        """;
        mockMvc.perform(post("/api/certificates/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(authentication(auth(caUser))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    void listTemplates_ok() throws Exception {
        CertificateTemplate t = new CertificateTemplate();
        t.setId(2L); t.setName("Web");
        given(templateService.getAllTemplates()).willReturn(List.of(t));

        mockMvc.perform(get("/api/certificates/templates")
                        .with(authentication(auth(caUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Web")));
    }

    @Test
    void deleteTemplate_ok() throws Exception {
        mockMvc.perform(delete("/api/certificates/templates/9")
                        .with(authentication(auth(admin))))
                .andExpect(status().isOk());
        verify(templateService).deleteTemplate(9);
    }

    // --- Download PKCS#12 ---
    @Test
    void download_pkcs12_ok() throws Exception {
        byte[] bytes = "PKCS12".getBytes(StandardCharsets.UTF_8);
        given(certificateService.exportAsPkcs12(123L, "pass")).willReturn(bytes);

        mockMvc.perform(get("/api/certificates/123/download")
                        .param("password", "pass")
                        .with(authentication(auth(regularUser))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("certificate_123.p12")))
                .andExpect(content().bytes(bytes));
    }

    // --- CSR Upload ---
    @Test
    void uploadCsr_created() throws Exception {
        given(tokenUtils.getToken(any())).willReturn("Bearer token");
        given(tokenUtils.getUsernameFromToken("Bearer token")).willReturn("user@pki.local");
        given(userService.findByEmail("user@pki.local")).willReturn(regularUser);

        var issuer = sampleCert(2L, CertificateType.INTERMEDIATE_CA, "OrgA", null);
        given(certificateService.findById(2L)).willReturn(Optional.of(issuer));

        var issued = sampleCert(300L, CertificateType.END_ENTITY, "OrgA", 3);
        given(certificateService.issueCertificateFromCSR(any(byte[].class), eq(issuer))).willReturn(issued);

        MockMultipartFile file = new MockMultipartFile(
                "file", "req.csr", "application/pkcs10",
                "-----BEGIN CSR-----\n...\n-----END CSR-----".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/certificates/csr/upload/2")
                        .file(file)
                        .with(authentication(auth(regularUser))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(300)));
    }

    // --- Search with paging ---
    @Test
    void search_ok() throws Exception {
        Page<Certificate> page = new PageImpl<>(
                List.of(sampleCert(400L, CertificateType.END_ENTITY, "OrgA", 3)),
                PageRequest.of(0, 10), 1
        );
        given(certificateService.search(eq(CertificateStatus.ACTIVE), eq(CertificateType.END_ENTITY), eq("OrgA"), any()))
                .willReturn(page);

        mockMvc.perform(get("/api/certificates/search")
                        .param("status","ACTIVE")
                        .param("type","END_ENTITY")
                        .param("organization","OrgA")
                        .param("page","0").param("size","10")
                        .with(authentication(auth(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type", is("END_ENTITY")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }
}