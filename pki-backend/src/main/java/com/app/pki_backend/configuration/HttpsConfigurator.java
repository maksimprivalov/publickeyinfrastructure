package com.app.pki_backend.configuration;

import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.entity.certificates.CertificateStatus;
import com.app.pki_backend.entity.certificates.CertificateType;
import com.app.pki_backend.service.interfaces.CertificateService;
import com.app.pki_backend.service.interfaces.MasterKeyService;
import com.app.pki_backend.service.interfaces.PrivateKeyService;
import com.app.pki_backend.util.PEMConverter;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HttpsConfigurator implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private PrivateKeyService privateKeyService;

    @Autowired
    private MasterKeyService masterKeyService;

    @Autowired
    private PEMConverter pemConverter;

    @Autowired
    private ServletWebServerApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            System.out.println(" --- Setting up HTTPS dynamically after application startup... --- ");

            Certificate serverCert = getOrCreateServerCertificate();

            setupHttpsDynamically(serverCert);

            System.out.println("✅ HTTPS configured successfully");
            System.out.println("🔒 HTTPS available on: https://localhost:8443");

        } catch (Exception e) {
            System.err.println("❌ Failed to setup HTTPS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Certificate getOrCreateServerCertificate() {
        try {
            // First, try to find a Root CA that we can actually decrypt
            Certificate usableRootCA = findUsableRootCA();

            if (usableRootCA == null) {
                System.out.println("🔑 No usable Root CA found, creating new one for HTTPS...");
                usableRootCA = certificateService.issueRootCertificate();
                System.out.println("✅ New Root CA created for HTTPS");
            }

            System.out.println("🔐 Creating new server certificate for HTTPS...");
            Certificate serverCert = certificateService.issueServerCertificate("localhost-https", usableRootCA);
            System.out.println("✅ New server certificate created successfully");

            return serverCert;

        } catch (Exception e) {
            System.err.println("❌ Failed to create server certificate: " + e.getMessage());
            throw new RuntimeException("Failed to create server certificate", e);
        }
    }

    private Certificate findUsableRootCA() {
        // Try to find a Root CA whose private key we can actually decrypt
        List<Certificate> rootCAs = certificateService.findAll().stream()
                .filter(cert -> cert.getType() == CertificateType.ROOT_CA)
                .filter(cert -> cert.getStatus() == CertificateStatus.ACTIVE)
                .collect(Collectors.toList());

        for (Certificate rootCA : rootCAs) {
            try {
                // Test if we can decrypt this Root CA's private key
                SecretKey masterKey = masterKeyService.getCurrentMasterKey();
                privateKeyService.retrievePrivateKey(rootCA, masterKey);

                System.out.println("✅ Found usable Root CA: " + rootCA.getSubject());
                return rootCA; // This one works!

            } catch (Exception e) {
                System.out.println("⚠️ Cannot decrypt Root CA " + rootCA.getId() + ": " + e.getMessage());
                // Continue to next Root CA
            }
        }

        return null; // No usable Root CA found
    }

    private void setupHttpsDynamically(Certificate serverCert) {
        try {

            String keystorePassword = "pkiServer123";
            createKeystore(serverCert, keystorePassword);

            addHttpsConnectorToRunningTomcat(keystorePassword);

        } catch (Exception e) {
            System.err.println("❌ Failed to setup HTTPS: " + e.getMessage());
            throw new RuntimeException("Failed to configure HTTPS", e);
        }
    }

    private void createKeystore(Certificate serverCert, String keystorePassword) throws Exception {

        SecretKey masterKey = masterKeyService.getCurrentMasterKey();
        PrivateKey privateKey = privateKeyService.retrievePrivateKey(serverCert, masterKey);

        X509Certificate x509Cert = pemConverter.parseCertificate(serverCert.getCertificateData());

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);

        keyStore.setKeyEntry("server", privateKey, keystorePassword.toCharArray(),
                new java.security.cert.Certificate[]{x509Cert});

        String keystorePath = "server-keystore.p12";
        try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
            keyStore.store(fos, keystorePassword.toCharArray());
        }

        System.out.println("🔑 Keystore created: " + Paths.get(keystorePath).toAbsolutePath());
    }

    private void addHttpsConnectorToRunningTomcat(String keystorePassword) {
        try {

            TomcatWebServer tomcatWebServer = (TomcatWebServer) applicationContext.getWebServer();
            StandardService standardService = (StandardService) tomcatWebServer.getTomcat().getServer().findService("Tomcat");

            Connector httpsConnector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
            httpsConnector.setPort(8443);
            httpsConnector.setScheme("https");
            httpsConnector.setSecure(true);

            org.apache.tomcat.util.net.SSLHostConfig sslHostConfig = new org.apache.tomcat.util.net.SSLHostConfig();
            sslHostConfig.setHostName("_default_");

            org.apache.tomcat.util.net.SSLHostConfigCertificate certificate =
                    new org.apache.tomcat.util.net.SSLHostConfigCertificate(sslHostConfig, org.apache.tomcat.util.net.SSLHostConfigCertificate.Type.UNDEFINED);

            certificate.setCertificateKeystoreFile(Paths.get("server-keystore.p12").toAbsolutePath().toString());
            certificate.setCertificateKeystorePassword(keystorePassword);
            certificate.setCertificateKeystoreType("PKCS12");
            certificate.setCertificateKeyAlias("server");

            sslHostConfig.addCertificate(certificate);
            sslHostConfig.setProtocols("TLSv1.2,TLSv1.3");
            httpsConnector.addSslHostConfig(sslHostConfig);
            httpsConnector.setProperty("SSLEnabled", "true");

            standardService.addConnector(httpsConnector);
            httpsConnector.start();

            System.out.println("🔌 HTTPS connector added and started on port 8443");

        } catch (Exception e) {
            System.err.println("❌ Failed to add HTTPS connector to running Tomcat: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
