package com.app.pki_backend.service.implementations;

import com.app.pki_backend.entity.User;
import com.app.pki_backend.service.interfaces.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.app.pki_backend.repository.UserRepository;
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository1) {

        this.userRepository = userRepository1;
    }


    @Override
    public User findByEmail(String username) {
        return null;
    }
}
