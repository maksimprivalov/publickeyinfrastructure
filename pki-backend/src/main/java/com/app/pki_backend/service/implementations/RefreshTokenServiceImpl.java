package com.app.pki_backend.service.implementations;
import com.app.pki_backend.entity.RefreshToken;
import com.app.pki_backend.entity.user.User;
import com.app.pki_backend.repository.RefreshTokenRepository;
import com.app.pki_backend.service.interfaces.RefreshTokenService;
import com.app.pki_backend.util.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TokenUtils tokenUtils;

    @Override
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(tokenUtils.generateRefreshToken(user));
        refreshToken.setExpiryDate(Instant.now().plusMillis(tokenUtils.getRefreshExpiresIn()));

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}