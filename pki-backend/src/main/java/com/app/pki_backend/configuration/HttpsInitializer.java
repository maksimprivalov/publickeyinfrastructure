package com.app.pki_backend.configuration;

import com.app.pki_backend.entity.certificates.Certificate;
import com.app.pki_backend.service.interfaces.CertificateService;
import com.app.pki_backend.service.interfaces.MasterKeyService;
import com.app.pki_backend.service.interfaces.PrivateKeyService;
import com.app.pki_backend.util.PEMConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * УПРОЩЕННАЯ инициализация HTTPS сертификата
 * Создает keystore ДО запуска Tomcat
 */
@Component
@Order(1) // Выполняется перед DataInitializer
public class HttpsInitializer implements CommandLineRunner {

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private PrivateKeyService privateKeyService;

    @Autowired
    private MasterKeyService masterKeyService;

    @Autowired
    private PEMConverter pemConverter;

    @Value("${server.ssl.key-store:classpath:server-keystore.p12}")
    private String keystorePath;

    @Value("${server.ssl.key-store-password:changeit}")
    private String keystorePassword;

    @Override
    public void run(String... args) throws Exception {
        // Извлечь путь без classpath:
        String actualPath = keystorePath.replace("classpath:", "src/main/resources/");

        if (Files.exists(Paths.get(actualPath))) {
            System.out.println("ℹ️ HTTPS keystore already exists: " + actualPath);
            return;
        }

        System.out.println("🔐 Generating HTTPS server certificate...");

        try {
            // Найти Root CA для подписи
            Certificate rootCA = certificateService.findAll().stream()
                    .filter(c -> c.getType().name().equals("ROOT_CA"))
                    .filter(c -> c.getStatus().name().equals("ACTIVE"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No active Root CA found for HTTPS"));

            // Создать server сертификат
            Certificate serverCert = certificateService.issueServerCertificate("localhost", rootCA);

            // Получить приватный ключ
            SecretKey masterKey = masterKeyService.getCurrentMasterKey();
            PrivateKey privateKey = privateKeyService.retrievePrivateKey(serverCert, masterKey);

            // Парсить X509 сертификат
            X509Certificate x509Cert = pemConverter.parseCertificate(serverCert.getCertificateData());

            // Создать keystore
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            keyStore.setKeyEntry(
                    "server",
                    privateKey,
                    keystorePassword.toCharArray(),
                    new java.security.cert.Certificate[]{x509Cert}
            );

            // Сохранить keystore
            try (FileOutputStream fos = new FileOutputStream(actualPath)) {
                keyStore.store(fos, keystorePassword.toCharArray());
            }

            System.out.println("✅ HTTPS keystore created: " + actualPath);
            System.out.println("🔒 Server will be available on: https://localhost:8443");

        } catch (Exception e) {
            System.err.println("❌ Failed to create HTTPS keystore: " + e.getMessage());
            System.err.println("⚠️  Server will start without HTTPS");
            e.printStackTrace();
        }
    }
}
