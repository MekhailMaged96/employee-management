package com.example.employee_management_system.security.jwt;

import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Component
public class JwtUtil {
    private final String SECRET = "mySecretKey123";

    private byte[] signingKey() {
        return SECRET.getBytes(StandardCharsets.UTF_8);
    }

    // ─────────────────────────────────────────────────────────────────
    // ✅ Token Generation
    // We embed three things into every token:
    //   "sub"    → username  (standard JWT claim)
    //   "userId" → user's DB id  (custom claim — avoids extra DB call)
    //   "roles"  → list of role names  (custom claim — for authorization checks)
    //
    // Why roles in the token?
    //   So any service can check permissions WITHOUT hitting the DB.
    //   e.g.  token: { sub:"john", userId:1, roles:["ROLE_ADMIN","ROLE_EMPLOYEE"] }
    // ─────────────────────────────────────────────────────────────────
    public String generateToken(String username, Long userId, Set<String> roles) {
        return Jwts.builder()
                .setSubject(username)                          // standard claim → "sub"
                .claim("userId", userId)                       // custom claim   → "userId"
                .claim("roles", roles)                         // custom claim   → "roles"
                .setIssuedAt(new java.util.Date())
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 1000L * 60 * 60))
                .signWith(SignatureAlgorithm.HS256, signingKey())
                .compact();
    }

    // ─────────────────────────────────────────────────────────────────
    // ✅ Claim Extraction helpers (reusable generic approach)
    // ─────────────────────────────────────────────────────────────────

    /** Extract ALL claims from a token — the single source of truth. */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(signingKey())
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Generic extractor — pass any lambda to pull the claim you need.
     *
     * Examples:
     *   extractClaim(token, Claims::getSubject)
     *   extractClaim(token, c -> c.get("userId", Long.class))
     *   extractClaim(token, c -> c.get("roles", List.class))
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {

        return claimsResolver.apply(extractAllClaims(token));
    }

    /** Convenience: username from the "sub" claim. */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Convenience: userId from the custom "userId" claim. */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    /**
     * Convenience: roles from the custom "roles" claim.
     *
     * JWT stores arrays as List<String> internally — that is why the
     * return type is List, not Set (we can convert at the call site).
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get("roles"));
    }
}
