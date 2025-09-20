package com.app.pki_backend.service.implementations;

import com.app.pki_backend.entity.ActivationToken;
import com.app.pki_backend.entity.user.User;
import com.app.pki_backend.repository.ActivationTokenRepository;
import com.app.pki_backend.service.interfaces.ActivationTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ActivationTokenServiceImpl implements ActivationTokenService {

    private final ActivationTokenRepository activationTokenRepository;

    @Autowired
    public ActivationTokenServiceImpl(ActivationTokenRepository activationTokenRepository) {
        this.activationTokenRepository = activationTokenRepository;
    }

    @Override
    public ActivationToken createToken(User user, String token) {
        ActivationToken activationToken = new ActivationToken();
        activationToken.setToken(token);
        activationToken.setUser(user);
        activationToken.setExpiryDate(LocalDateTime.now().plusHours(24));
        return activationTokenRepository.save(activationToken);
    }

    @Override
    public ActivationToken findByToken(String token) {
        return activationTokenRepository.findByToken(token).orElse(null);
    }

    @Override
    public void deleteToken(ActivationToken token) {
        activationTokenRepository.delete(token);
    }
}