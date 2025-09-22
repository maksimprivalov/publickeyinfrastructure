package com.app.pki_backend.util;

import com.app.pki_backend.entity.user.User;
import com.app.pki_backend.service.interfaces.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Component
public class TokenUtils {

    @Value("PKI")
    private String APP_NAME;

    @Value("${jwt.secret}")
    public String SECRET;

    @Value("18000000")
    private int ACCESS_EXPIRES_IN;

    @Value("604800000")
    private int REFRESH_EXPIRES_IN;

    @Value("Token")
    private String AUTH_HEADER;

    @Autowired
    private UserService userService;

    private static final String AUDIENCE_WEB = "web";

    private SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS512;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .setIssuer(APP_NAME)
                .setSubject(user.getEmail())
                .setAudience(generateAudience())
                .setIssuedAt(new Date())
                .claim("role", user.getRole())
                .claim("userId", user.getId())
                .claim("type", "access")
                .setExpiration(generateExpirationDate())
                .signWith(getSigningKey(), SIGNATURE_ALGORITHM)
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .setIssuer(APP_NAME)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .claim("type", "refresh")
                .setExpiration(generateRefreshExpirationDate())
                .signWith(getSigningKey(), SIGNATURE_ALGORITHM)
                .compact();
    }

    private Date generateRefreshExpirationDate() {
        return new Date(new Date().getTime() + REFRESH_EXPIRES_IN);
    }
    public boolean isAccessToken(String token) {
        try {
            return "access".equals(getAllClaimsFromToken(token).get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(getAllClaimsFromToken(token).get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    private String generateAudience() {
        return AUDIENCE_WEB;
    }

    private Date generateExpirationDate() {
        return new Date(new Date().getTime() + ACCESS_EXPIRES_IN);
    }

    public String getToken(HttpServletRequest request) {
        String authHeader = getAuthHeaderFromHeader(request);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    public String getUsernameFromToken(String token) {
        String email;

        try {
            final Claims claims = this.getAllClaimsFromToken(token);
            email = claims.getSubject();
        } catch (ExpiredJwtException ex) {
            throw ex;
        } catch (Exception e) {
            email = null;
        }

        return email;
    }

    public Date getIssuedAtDateFromToken(String token) {
        Date issueAt;
        try {
            final Claims claims = this.getAllClaimsFromToken(token);
            issueAt = claims.getIssuedAt();
        } catch (ExpiredJwtException ex) {
            throw ex;
        } catch (Exception e) {
            issueAt = null;
        }
        return issueAt;
    }


    public Date getExpirationDateFromToken(String token) {
        Date expiration;
        try {
            final Claims claims = this.getAllClaimsFromToken(token);
            expiration = claims.getExpiration();
        } catch (ExpiredJwtException ex) {
            throw ex;
        } catch (Exception e) {
            expiration = null;
        }

        return expiration;
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        User user = (User) userDetails;
        final String email = getUsernameFromToken(token);
        final Date created = getIssuedAtDateFromToken(token);

        return (email != null
                && email.equals(userDetails.getUsername())
                && getExpirationDateFromToken(token).after(Date.from(Instant.now())));
    }

    private Boolean isCreatedBeforeLastPasswordReset(Date created, Date lastPasswordReset) {
        return (lastPasswordReset != null && created.before(lastPasswordReset));
    }


    public int getExpiredIn() {
        return ACCESS_EXPIRES_IN;
    }
    public int getRefreshExpiresIn() {
        return REFRESH_EXPIRES_IN;
    }

    public String getAuthHeaderFromHeader(HttpServletRequest request) {
        return request.getHeader(AUTH_HEADER);
    }

}


