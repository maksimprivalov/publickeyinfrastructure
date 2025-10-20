package com.app.pki_backend.dto.certificate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for Certificate Signing Request API calls
 * Separated from JPA Entity to follow best practices
 */
public class CSRRequestDTO {

    @NotBlank(message = "CSR content is required")
    private String csrContent; // PEM format CSR

    @NotNull(message = "CA ID is required")
    private Long selectedCAId; // ID of CA that will sign this certificate

    public CSRRequestDTO() {}

    public CSRRequestDTO(String csrContent, Long selectedCAId) {
        this.csrContent = csrContent;
        this.selectedCAId = selectedCAId;
    }

    // Getters and setters
    public String getCsrContent() {
        return csrContent;
    }

    public void setCsrContent(String csrContent) {
        this.csrContent = csrContent;
    }

    public Long getSelectedCAId() {
        return selectedCAId;
    }

    public void setSelectedCAId(Long selectedCAId) {
        this.selectedCAId = selectedCAId;
    }
}
