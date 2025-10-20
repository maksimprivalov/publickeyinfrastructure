package com.app.pki_backend.e2e;

import com.app.pki_backend.dto.certificate.CSRRequestDTO;
import com.app.pki_backend.dto.user.LoginRequestDTO;
import com.app.pki_backend.dto.user.TokenDTO;
import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.entity.certificates.CertificateStatus;
import com.app.pki_backend.entity.certificates.CertificateType;
import com.app.pki_backend.repository.CertificateRepository;
import com.app.pki_backend.repository.RefreshTokenRepository;
import com.app.pki_backend.util.CSRTestHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * End-to-End tests for certificate issuance workflow
 * Tests the complete flow: Root CA -> Intermediate CA -> End Entity
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CertificateIssuanceE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;
    private String adminToken;
    private Long rootCAId;
    private Long intermediateCAId;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = "http://localhost:" + port + "/api";

        // Очистка refresh tokens перед каждым тестом
        refreshTokenRepository.deleteAll();

        // Login as admin to get token
        if (adminToken == null) {
            adminToken = loginAsAdmin();
        }

        // Find Root CA
        if (rootCAId == null) {
            rootCAId = findRootCA();
        }
    }

    @AfterEach
    void tearDown() {
        // Очистка refresh tokens после каждого теста
        refreshTokenRepository.deleteAll();
    }

    /**
     * Test 1: Issue Intermediate CA from Root CA
     */
    @Test
    @Order(1)
    @DisplayName("Should issue Intermediate CA certificate from Root CA")
    void testIssueIntermediateFromRoot() throws Exception {
        // Generate CSR for Intermediate CA
        String csrContent = CSRTestHelper.generateIntermediateCACSR(
                "Test Intermediate CA",
                "Test Organization"
        );

        // Create request DTO
        CSRRequestDTO requestDto = new CSRRequestDTO(csrContent, rootCAId);

        // Make request to issue intermediate certificate
        HttpHeaders headers = createAuthHeaders(adminToken);
        HttpEntity<CSRRequestDTO> request = new HttpEntity<>(requestDto, headers);

        String url = baseUrl + "/certificates/issue/intermediate/" + rootCAId;

        // First try to get response
        ResponseEntity<String> rawResponse = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
        );

        // Check for errors and provide detailed message
        if (rawResponse.getStatusCode().is5xxServerError()) {
            System.err.println("❌ Server returned 5xx error");
            System.err.println("Status: " + rawResponse.getStatusCode());
            System.err.println("Body: " + rawResponse.getBody());
            fail("Server returned 5xx error. Check server logs for details. Response: " + rawResponse.getBody());
        }

        if (rawResponse.getStatusCode().is4xxClientError()) {
            System.err.println("❌ Server returned 4xx error");
            System.err.println("Status: " + rawResponse.getStatusCode());
            System.err.println("Body: " + rawResponse.getBody());
            fail("Server returned 4xx error. Check authorization and request format. Response: " + rawResponse.getBody());
        }

        assertEquals(HttpStatus.CREATED, rawResponse.getStatusCode(),
                "Should return 201 CREATED");

        // Now parse as Certificate
        Certificate intermediate = objectMapper.readValue(rawResponse.getBody(), Certificate.class);
        assertNotNull(intermediate, "Certificate should not be null");

        intermediateCAId = intermediate.getId();

        // Verify certificate properties
        assertEquals(CertificateType.INTERMEDIATE_CA, intermediate.getType(),
                "Certificate type should be INTERMEDIATE_CA");

        assertEquals(CertificateStatus.ACTIVE, intermediate.getStatus(),
                "Certificate should be ACTIVE");

        assertTrue(intermediate.getSubject().contains("Test Intermediate CA"),
                "Subject should contain the CN from CSR");

        assertNotNull(intermediate.getSerialNumber(),
                "Should have serial number");

        assertNotNull(intermediate.getPublicKey(),
                "Should have public key");

        assertNotNull(intermediate.getCertificateData(),
                "Should have certificate data in PEM format");

        System.out.println("✅ Intermediate CA issued successfully!");
        System.out.println("   ID: " + intermediate.getId());
        System.out.println("   Subject: " + intermediate.getSubject());
        System.out.println("   Serial: " + intermediate.getSerialNumber().toString(16));
    }

    /**
     * Test 2: Issue End-Entity certificate from Intermediate CA
     */
    @Test
    @Order(2)
    @DisplayName("Should issue End-Entity certificate from Intermediate CA")
    void testIssueEndEntityFromIntermediate() throws Exception {
        // Ensure we have intermediate CA
        if (intermediateCAId == null) {
            testIssueIntermediateFromRoot();
        }

        // Generate CSR for End-Entity
        String csrContent = CSRTestHelper.generateEndEntityCSR(
                "test-user",
                "Test Organization",
                "test@example.com"
        );

        // Create request DTO
        CSRRequestDTO requestDto = new CSRRequestDTO(csrContent, intermediateCAId);

        // Make request to issue end-entity certificate
        HttpHeaders headers = createAuthHeaders(adminToken);
        HttpEntity<CSRRequestDTO> request = new HttpEntity<>(requestDto, headers);

        String url = baseUrl + "/certificates/issue/ee/" + intermediateCAId;

        ResponseEntity<Certificate> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                Certificate.class
        );

        // Assertions
        assertEquals(HttpStatus.CREATED, response.getStatusCode(),
                "Should return 201 CREATED");

        assertNotNull(response.getBody(), "Response body should not be null");

        Certificate jsonCert = response.getBody();
        Certificate endEntity = certificateRepository.findById(jsonCert.getId()).get();

        // Verify certificate properties
        assertEquals(CertificateType.END_ENTITY, endEntity.getType(),
                "Certificate type should be END_ENTITY");

        assertEquals(CertificateStatus.ACTIVE, endEntity.getStatus(),
                "Certificate should be ACTIVE");

        assertTrue(endEntity.getSubject().contains("test-user"),
                "Subject should contain the CN from CSR");

        assertNotNull(endEntity.getIssuerCertificate(),
                "Should have issuer certificate reference");

        assertEquals(intermediateCAId, endEntity.getIssuerCertificate().getId(),
                "Issuer should be Intermediate CA");

        assertNotNull(endEntity.getSerialNumber(),
                "Should have serial number");

        assertNotNull(endEntity.getPublicKey(),
                "Should have public key");

        System.out.println("✅ End-Entity certificate issued successfully!");
        System.out.println("   ID: " + endEntity.getId());
        System.out.println("   Subject: " + endEntity.getSubject());
        System.out.println("   Serial: " + endEntity.getSerialNumber().toString(16));
    }

    /**
     * Test 3: Verify certificate chain
     */
    @Test
    @Order(3)
    @DisplayName("Should have valid certificate chain: Root -> Intermediate -> End-Entity")
    void testCertificateChainValidity() throws Exception {

        testIssueIntermediateFromRoot();
        testIssueEndEntityFromIntermediate();

        // Get all certificates
        List<Certificate> allCerts = certificateRepository.findAllWithIssuer();

        assertTrue(allCerts.size() >= 3,
                "Should have at least Root, Intermediate, and End-Entity certificates");

        Certificate root = certificateRepository.findById(rootCAId).get();
        Certificate intermediate = certificateRepository.findById(intermediateCAId).get();

        // Find End-Entity issued by Intermediate
        Certificate endEntity = allCerts.stream()
                .filter(c -> c.getType() == CertificateType.END_ENTITY)
                .filter(c -> c.getIssuerCertificate() != null)
                .filter(c -> c.getIssuerCertificate().getId().equals(intermediateCAId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("End-Entity certificate not found"));

        // Verify chain
        assertNull(root.getIssuerCertificate(),
                "Root CA should be self-signed (no issuer)");

        assertEquals(root.getId(), intermediate.getIssuerCertificate().getId(),
                "Intermediate should be signed by Root");

        assertEquals(intermediate.getId(), endEntity.getIssuerCertificate().getId(),
                "End-Entity should be signed by Intermediate");

        System.out.println("✅ Certificate chain is valid!");
        System.out.println("   Root CA: " + root.getId() + " -> " + root.getSubject());
        System.out.println("   Intermediate: " + intermediate.getId() + " -> " + intermediate.getSubject());
        System.out.println("   End-Entity: " + endEntity.getId() + " -> " + endEntity.getSubject());
    }

    /**
     * Test 4: Unauthorized access should fail
     */
    @Test
    @Order(4)
    @DisplayName("Should reject certificate issuance without authentication")
    void testUnauthorizedAccess() throws Exception {
        String csrContent = CSRTestHelper.generateEndEntityCSR(
                "unauthorized-user",
                "Test Org",
                "unauth@example.com"
        );

        CSRRequestDTO requestDto = new CSRRequestDTO(csrContent, rootCAId);
        HttpEntity<CSRRequestDTO> request = new HttpEntity<>(requestDto);

        String url = baseUrl + "/certificates/issue/ee/" + rootCAId;

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(),
                "Should return 401 UNAUTHORIZED without token");
    }

    // ==================== Helper Methods ====================

    /**
     * Login as admin and get JWT token
     */
    private String loginAsAdmin() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("admin@pki.local");
        loginRequest.setPassword("admin123");

        ResponseEntity<TokenDTO> response = restTemplate.postForEntity(
                baseUrl + "/users/login",
                loginRequest,
                TokenDTO.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Admin login should succeed");

        assertNotNull(response.getBody(), "Token response should not be null");
        assertNotNull(response.getBody().getAccessToken(), "Access token should not be null");

        return response.getBody().getAccessToken();
    }

    /**
     * Find Root CA certificate from database
     */
    private Long findRootCA() {
        List<Certificate> rootCAs = certificateRepository.findByType(CertificateType.ROOT_CA);

        assertFalse(rootCAs.isEmpty(), "Root CA should exist (created by DataInitializer)");

        Certificate rootCA = rootCAs.stream()
                .filter(c -> c.getStatus() == CertificateStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No active Root CA found"));

        return rootCA.getId();
    }

    /**
     * Create HTTP headers with authorization token
     */
    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", "Bearer " + token);
        return headers;
    }
}