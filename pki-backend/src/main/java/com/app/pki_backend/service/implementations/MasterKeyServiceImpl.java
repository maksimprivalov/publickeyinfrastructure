package com.app.pki_backend.service.implementations;

import com.app.pki_backend.service.interfaces.CryptographyService;
import com.app.pki_backend.service.interfaces.MasterKeyService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Base64;

/**
 * ИСПРАВЛЕННАЯ версия MasterKeyService с персистентным хранилищем
 */
@Service
public class MasterKeyServiceImpl implements MasterKeyService {

    @Autowired
    private CryptographyService cryptographyService;

    @Value("${pki.master-key.storage-path:./master-keys}")
    private String masterKeyStoragePath;

    @Value("${pki.master-key.current-id:default}")
    private String currentMasterKeyId;

    private SecretKey currentMasterKey;

    // КРИТИЧНО: Инициализация при старте приложения
    @PostConstruct
    public void init() {
        try {
            // Создать директорию для хранения, если не существует
            Files.createDirectories(Paths.get(masterKeyStoragePath));

            // Попытка загрузить существующий master key
            Path keyFile = Paths.get(masterKeyStoragePath, currentMasterKeyId + ".key");

            if (Files.exists(keyFile)) {
                System.out.println("🔑 Loading existing master key from: " + keyFile);
                currentMasterKey = loadMasterKeyFromFile(keyFile);
                System.out.println("✅ Master key loaded successfully");
            } else {
                System.out.println("🔑 No master key found, generating new one...");
                currentMasterKey = generateMasterKey();
                saveMasterKeyToFile(currentMasterKey, keyFile);
                System.out.println("✅ New master key generated and saved to: " + keyFile);
            }

        } catch (Exception e) {
            throw new RuntimeException("CRITICAL: Failed to initialize master key", e);
        }
    }

    @Override
    public SecretKey generateMasterKey() {
        return cryptographyService.generateAESKey();
    }

    @Override
    public SecretKey getCurrentMasterKey() {
        if (currentMasterKey == null) {
            throw new IllegalStateException("Master key not initialized!");
        }
        return currentMasterKey;
    }

    @Override
    public void storeMasterKey(SecretKey masterKey, String keyId) {
        try {
            Path keyFile = Paths.get(masterKeyStoragePath, keyId + ".key");
            saveMasterKeyToFile(masterKey, keyFile);

            if (keyId.equals(currentMasterKeyId)) {
                currentMasterKey = masterKey;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to store master key", e);
        }
    }

    @Override
    public SecretKey retrieveMasterKey(String keyId) {
        try {
            Path keyFile = Paths.get(masterKeyStoragePath, keyId + ".key");

            if (!Files.exists(keyFile)) {
                throw new IllegalArgumentException("Master key not found for ID: " + keyId);
            }

            return loadMasterKeyFromFile(keyFile);

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve master key: " + keyId, e);
        }
    }

    @Override
    public SecretKey rotateMasterKey() {
        // TODO: Implement proper key rotation with re-encryption
        throw new UnsupportedOperationException("Key rotation not yet implemented");
    }

    @Override
    public boolean isMasterKeyAvailable() {
        return currentMasterKey != null;
    }

    // ============= ПРИВАТНЫЕ МЕТОДЫ ДЛЯ РАБОТЫ С ФАЙЛАМИ =============

    /**
     * Сохранить master key в файл (Base64 encoded)
     */
    private void saveMasterKeyToFile(SecretKey masterKey, Path filePath) throws IOException {
        String base64Key = Base64.getEncoder().encodeToString(masterKey.getEncoded());

        // Установить строгие права доступа (только владелец может читать)
        Files.write(filePath, base64Key.getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

        // На Unix системах установить права 600
        try {
            Files.setPosixFilePermissions(filePath,
                    PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException e) {
            // Windows не поддерживает POSIX permissions
            System.out.println("⚠️ Cannot set POSIX permissions on this OS");
        }
    }

    /**
     * Загрузить master key из файла
     */
    private SecretKey loadMasterKeyFromFile(Path filePath) throws IOException {
        String base64Key = new String(Files.readAllBytes(filePath));
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(keyBytes, "AES");
    }
}