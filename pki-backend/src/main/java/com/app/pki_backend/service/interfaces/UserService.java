package com.app.pki_backend.service.interfaces;

import com.app.pki_backend.entity.user.User;

public interface UserService {

    User findByEmail(String username);
}
