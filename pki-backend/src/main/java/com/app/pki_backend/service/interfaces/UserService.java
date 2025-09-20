package com.app.pki_backend.service.interfaces;

import com.app.pki_backend.entity.user.User;
import com.app.pki_backend.dto.user.RegistrationRequestDTO;

public interface UserService {
    User saveInactive(RegistrationRequestDTO dto);
    void saveActivationToken(User user, String token);
    boolean activateUser(String token);
    void sendActivationEmail(String email, String token);
    User save(User user);
    User findByEmail(String username);
}
