package com.app.pki_backend.service.interfaces;

import com.app.pki_backend.entity.ActivationToken;
import com.app.pki_backend.entity.user.User;

public interface ActivationTokenService {
    ActivationToken createToken(User user, String token);
    ActivationToken findByToken(String token);
    void deleteToken(ActivationToken token);
}
