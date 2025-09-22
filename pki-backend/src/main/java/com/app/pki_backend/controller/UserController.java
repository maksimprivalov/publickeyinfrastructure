package com.app.pki_backend.controller;

import com.app.pki_backend.dto.user.LoginRequestDTO;
import com.app.pki_backend.dto.user.TokenDTO;
import com.app.pki_backend.entity.RefreshToken;
import com.app.pki_backend.service.interfaces.RefreshTokenService;
import com.app.pki_backend.service.interfaces.UserService;
import com.app.pki_backend.util.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.app.pki_backend.dto.user.RegistrationRequestDTO;
import com.app.pki_backend.entity.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/users")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private TokenUtils tokenUtils;
    @Autowired
    private RefreshTokenService refreshTokenService;
    public static BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody RegistrationRequestDTO registrationRequestDTO) {
        if (registrationRequestDTO.getEmail() == null || registrationRequestDTO.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email and password are required");
        }

       registrationRequestDTO.setPassword(passwordEncoder.encode(registrationRequestDTO.getPassword()));
        User newUser = userService.saveInactive(registrationRequestDTO);

        String activationToken = UUID.randomUUID().toString();
        userService.saveActivationToken(newUser, activationToken);

        userService.sendActivationEmail(newUser.getEmail(), activationToken);

        return ResponseEntity.status(HttpStatus.CREATED).body("User registered. Please check your email to activate account.");
    }

    @GetMapping("/activate")
    public ResponseEntity<String> activateUser(@RequestParam("token") String token) {
        boolean activated = userService.activateUser(token);
        if (activated) {
            return ResponseEntity.ok("Account activated successfully!");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired activation token.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequestDTO loginRequest) {
        try {
            User user = userService.findByEmail(loginRequest.getEmail());

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }

            if (!user.getActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Account not activated. Check your email.");
            }

            this.authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            UserDetails userDetails = this.userDetailsService.loadUserByUsername(loginRequest.getEmail());

            String accessToken = this.tokenUtils.generateToken((User) userDetails);

            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            return ResponseEntity.ok(new TokenDTO(accessToken, refreshToken.getToken()));


        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Wrong password!");
        }
    }
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestParam("refreshToken") String requestToken) {
        return refreshTokenService.findByToken(requestToken)
                .map(refreshToken -> {
                    if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token expired");
                    }
                    String newAccessToken = tokenUtils.generateToken(refreshToken.getUser());
                    return ResponseEntity.ok(new TokenDTO(newAccessToken, requestToken));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token"));
    }

//    @PostMapping("/logout")
//    public ResponseEntity<?> logout(@RequestParam("userId") Integer userId) {
//        User user = userService.findById(userId);
//        refreshTokenService.deleteByUser(user);
//        return ResponseEntity.ok("Logged out successfully, refresh token revoked");
//    }
@PostMapping("/logout")
public ResponseEntity<String> logout(HttpServletRequest request) {
    String token = tokenUtils.getToken(request);
    if (token == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No token provided");
    }

    String email = tokenUtils.getUsernameFromToken(token);
    User user = userService.findByEmail(email);

    if (user == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
    }

    refreshTokenService.deleteByUser(user);

    return ResponseEntity.ok("Logged out successfully");
}


}
