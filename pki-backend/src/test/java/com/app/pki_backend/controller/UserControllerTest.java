package com.app.pki_backend.controller;

import com.app.pki_backend.dto.user.LoginRequestDTO;
import com.app.pki_backend.dto.user.RegistrationRequestDTO;
import com.app.pki_backend.dto.user.TokenDTO;
import com.app.pki_backend.entity.RefreshToken;
import com.app.pki_backend.entity.user.User;
import com.app.pki_backend.service.interfaces.RefreshTokenService;
import com.app.pki_backend.service.interfaces.UserService;
import com.app.pki_backend.util.TokenUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserService userService;
    @MockBean UserDetailsService userDetailsService;
    @MockBean AuthenticationManager authenticationManager;
    @MockBean TokenUtils tokenUtils;
    @MockBean RefreshTokenService refreshTokenService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        setUser(activeUser, 1, "user@pki.local", "User", true, "Client-Org");
    }

    private void setUser(User u, int id, String email, String role, boolean active, String org) {
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, id);
        } catch (Exception ignored) {}
        u.setEmail(email);
        u.setPassword("$2a$10$hash");
        u.setName("Test");
        u.setSurname("User");
        u.setRole(role);
        u.setOrganizationName(org);
        u.setActive(active);
    }

    // ---------------- REGISTER ----------------

    @Test
    @DisplayName("POST /api/users/register — 201 CREATED")
    void register_created() throws Exception {
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail("new@pki.local");
        dto.setPassword("plain");
        dto.setName("Neo");
        dto.setSurname("Anderson");
        dto.setOrganization("OrgX");

        User saved = new User();
        setUser(saved, 10, "new@pki.local", "User", false, "OrgX");

        given(userService.saveInactive(any(RegistrationRequestDTO.class))).willReturn(saved);
        // doNothing().when(userService).saveActivationToken(eq(saved), anyString());

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Please check your email")));
    }

    @Test
    @DisplayName("POST /api/users/register — 400 BAD REQUEST missing fields")
    void register_badRequest_when_missing_fields() throws Exception {
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail(null);
        dto.setPassword(null);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ---------------- ACTIVATE ----------------

    @Test
    @DisplayName("GET /api/users/activate — 200 valid token")
    void activate_ok() throws Exception {
        given(userService.activateUser("tok123")).willReturn(true);

        mockMvc.perform(get("/api/users/activate").param("token", "tok123"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("activated")));
    }

    @Test
    @DisplayName("GET /api/users/activate — 400 invalid token")
    void activate_bad() throws Exception {
        given(userService.activateUser("bad")).willReturn(false);

        mockMvc.perform(get("/api/users/activate").param("token", "bad"))
                .andExpect(status().isBadRequest());
    }

    // ---------------- LOGIN ----------------

    @Test
    @DisplayName("POST /api/users/login — 200 OK access and refresh")
    void login_ok() throws Exception {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail(activeUser.getEmail());
        req.setPassword("pass");

        given(userService.findByEmail(activeUser.getEmail())).willReturn(activeUser);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenAnswer((Answer<UsernamePasswordAuthenticationToken>) inv ->
                        new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

        given(userDetailsService.loadUserByUsername(activeUser.getEmail())).willReturn(activeUser);

        given(tokenUtils.generateToken(activeUser)).willReturn("access-token");
        RefreshToken rt = new RefreshToken();
        rt.setToken("refresh-token");
        rt.setUser(activeUser);
        rt.setExpiryDate(Instant.now().plus(7, ChronoUnit.DAYS));
        given(refreshTokenService.createRefreshToken(activeUser)).willReturn(rt);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("POST /api/users/login — 401 not found user")
    void login_user_not_found() throws Exception {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("unknown@pki.local");
        req.setPassword("x");

        given(userService.findByEmail("unknown@pki.local")).willReturn(null);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/users/login — 403")
    void login_forbidden_when_not_active() throws Exception {
        User inactive = new User();
        setUser(inactive, 2, "inactive@pki.local", "User", false, "OrgX");

        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail(inactive.getEmail());
        req.setPassword("x");

        given(userService.findByEmail(inactive.getEmail())).willReturn(inactive);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ---------------- REFRESH ----------------

    @Test
    @DisplayName("POST /api/users/refresh — 200 OK refresh")
    void refresh_ok() throws Exception {
        RefreshToken rt = new RefreshToken();
        rt.setToken("r1");
        rt.setUser(activeUser);
        rt.setExpiryDate(Instant.now().plus(1, ChronoUnit.DAYS));

        given(refreshTokenService.findByToken("r1")).willReturn(Optional.of(rt));
        given(tokenUtils.generateToken(activeUser)).willReturn("new-access");

        mockMvc.perform(post("/api/users/refresh").param("refreshToken", "r1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("r1"));
    }

    @Test
    @DisplayName("POST /api/users/refresh — 401 refresh")
    void refresh_expired() throws Exception {
        RefreshToken rt = new RefreshToken();
        rt.setToken("r2");
        rt.setUser(activeUser);
        rt.setExpiryDate(Instant.now().minus(1, ChronoUnit.MINUTES));

        given(refreshTokenService.findByToken("r2")).willReturn(Optional.of(rt));

        mockMvc.perform(post("/api/users/refresh").param("refreshToken", "r2"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("expired")));
    }

    @Test
    @DisplayName("POST /api/users/refresh — 401 invalid refresh")
    void refresh_invalid() throws Exception {
        given(refreshTokenService.findByToken("nope")).willReturn(Optional.empty());

        mockMvc.perform(post("/api/users/refresh").param("refreshToken", "nope"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------- LOGOUT ----------------

    @Test
    @DisplayName("POST /api/users/logout — 200 OK")
    void logout_ok() throws Exception {
        given(tokenUtils.getToken(any())).willReturn("access");
        given(tokenUtils.getUsernameFromToken("access")).willReturn(activeUser.getEmail());
        given(userService.findByEmail(activeUser.getEmail())).willReturn(activeUser);

        mockMvc.perform(post("/api/users/logout"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Logged out successfully")));
    }

    @Test
    @DisplayName("POST /api/users/logout — 401")
    void logout_unauthorized() throws Exception {
        given(tokenUtils.getToken(any())).willReturn(null);

        mockMvc.perform(post("/api/users/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No token provided")));
    }

    @Test
    @DisplayName("POST /api/users/logout — 404")
    void logout_user_not_found() throws Exception {
        given(tokenUtils.getToken(any())).willReturn("access");
        given(tokenUtils.getUsernameFromToken("access")).willReturn("ghost@pki.local");
        given(userService.findByEmail("ghost@pki.local")).willReturn(null);

        mockMvc.perform(post("/api/users/logout"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("User not found")));
    }
}
