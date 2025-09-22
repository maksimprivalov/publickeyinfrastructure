package com.app.pki_backend.service.interfaces;

import com.app.pki_backend.entity.RefreshToken;
import com.app.pki_backend.entity.user.User;

import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user);

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);
}