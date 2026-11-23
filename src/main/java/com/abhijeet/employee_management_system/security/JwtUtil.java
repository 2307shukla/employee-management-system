package com.abhijeet.employee_management_system.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final int MIN_SECRET_BYTES = 32; // 256 bits, the HMAC-SHA minimum

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @PostConstruct
    void validateSecretLength() {
        int actualBytes = secret.getBytes(StandardCharsets.UTF_8).length;
        if (actualBytes < MIN_SECRET_BYTES) {
            // Without this check, the app starts "successfully" and only fails
            // later, confusingly, the first time a token is actually signed -
            // this happened on a real deploy where JWT_SECRET was set but too
            // short, and only /api/auth/register and /api/auth/login (the only
            // endpoints that call generateToken) returned a bare 500.
            throw new IllegalStateException(
                "JWT_SECRET is only " + actualBytes + " bytes (" + (actualBytes * 8) + " bits); "
                + "HMAC-SHA requires at least " + MIN_SECRET_BYTES + " bytes (256 bits). "
                + "Generate one with: openssl rand -base64 32");
        }
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username) {
        return Jwts.builder()
            .subject(username)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey())
            .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser().verifyWith((javax.crypto.SecretKey) getSigningKey()).build().parseSignedClaims(token).getPayload().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith((javax.crypto.SecretKey) getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}