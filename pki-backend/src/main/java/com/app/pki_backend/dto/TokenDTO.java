package com.app.pki_backend.dto;

public class TokenDTO {
    private String token;
    private boolean muted;


    public TokenDTO() {}

    public TokenDTO(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { this.muted = muted; }

}
