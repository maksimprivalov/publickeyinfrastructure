package com.app.pki_backend.service.implementations;

import com.app.pki_backend.entity.user.User;
import com.app.pki_backend.service.interfaces.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.app.pki_backend.dto.user.RegistrationRequestDTO;
import com.app.pki_backend.entity.ActivationToken;

import com.app.pki_backend.repository.UserRepository;
import com.app.pki_backend.service.interfaces.ActivationTokenService;
import jakarta.transaction.Transactional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Objects;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ActivationTokenService activationTokenService;
    private final JavaMailSender mailSender;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           ActivationTokenService activationTokenService,
                           JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.activationTokenService = activationTokenService;
        this.mailSender = mailSender;
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Override
    public User findById(Integer userId) {
        return userRepository.findById(userId).get();
    }

    @Override
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User saveInactive(RegistrationRequestDTO dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setName(dto.getName());
        user.setSurname(dto.getSurname());
        user.setRole("USER");
        user.setOrganizationName(dto.getOrganization());
        return userRepository.save(user);
    }

    @Override
    public void saveActivationToken(User user, String token) {
        activationTokenService.createToken(user, token);
    }

    @Override
    @Transactional
    public boolean activateUser(String token) {
        ActivationToken activationToken = activationTokenService.findByToken(token);
        if (activationToken != null && activationToken.getExpiryDate().isAfter(java.time.LocalDateTime.now())) {
            User user = activationToken.getUser();
            if (Objects.nonNull(user)) {
                user.setActive(true);
                user.setRole("USER");
                userRepository.save(user);
                activationTokenService.deleteToken(activationToken);
                return true;
            }
        }
        return false;
    }

    @Override
    public void sendActivationEmail(String email, String token) {
        String activationLink = "http://localhost:8080/api/users/activate?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Activate your account");
        message.setText("Click here to activate your account: " + activationLink);

        mailSender.send(message);
    }
}