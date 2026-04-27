package com.example.employee_management_system.security.jwt;

import org.springframework.stereotype.Component;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {
    private final String SECRET = "mySecretKey123";

    // Use byte[] signing key to avoid reliance on javax.xml.bind.DatatypeConverter which
    // is not present on Java 9+ without adding JAXB dependencies.
    private byte[] signingKey() {
        return SECRET.getBytes(StandardCharsets.UTF_8);
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new java.util.Date())
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(SignatureAlgorithm.HS256, signingKey())
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .setSigningKey(signingKey())
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
