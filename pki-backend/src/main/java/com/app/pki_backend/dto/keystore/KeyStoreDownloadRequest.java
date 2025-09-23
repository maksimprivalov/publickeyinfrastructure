package com.app.pki_backend.dto.keystore;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for KeyStore download requests with password protection.
 */
public class KeyStoreDownloadRequest {

    @NotBlank(message = "Keystore password is required")
    @Size(min = 6, message = "Keystore password must be at least 6 characters")
    private String keystorePassword;

    @NotBlank(message = "Key password is required")
    @Size(min = 6, message = "Key password must be at least 6 characters")
    private String keyPassword;

    private String format = "PKCS12"; // Default format

    public KeyStoreDownloadRequest() {}

    public KeyStoreDownloadRequest(String keystorePassword, String keyPassword) {
        this.keystorePassword = keystorePassword;
        this.keyPassword = keyPassword;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
