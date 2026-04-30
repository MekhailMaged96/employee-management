package com.example.employee_management_system.security;

import com.example.employee_management_system.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class TokenUserExtractor {

    // Used only for PATTERN 2 (direct token parsing)
    private final JwtUtil jwtUtil;

    // ================================================================
    //  PATTERN 1 ─ SecurityContextHolder
    //  ► Use this in 95% of cases inside your services / controllers.
    // ================================================================

    /**
     * Returns the Authentication object stored in the security context
     * for the current request thread.
     * All methods below are built on top of this.
     */
    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Returns the username of the currently authenticated user.
     *
     * How it works:
     *   Filter parses JWT → sets Authentication(principal=CustomUserDetails)
     *   → SecurityContextHolder stores it → we read it here.
     */
    public String getUsernameFromContext() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        // The principal is the object set in our JWT filter.
        // If it is our CustomUserDetails, use its getUsername().
        if (auth.getPrincipal() instanceof CustomUserDetails customUser) {
            return customUser.getUsername();           // ← clean cast with pattern matching
        }

        // Fallback: Spring sometimes sets the username string directly
        return auth.getName();
    }

    /**
     * Returns the userId of the currently authenticated user.
     *
     * Why is this useful?
     *   Most DB queries need an id, not a username string.
     *   Getting it from context means zero extra DB calls.
     */
    public Long getUserIdFromContext() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        if (auth.getPrincipal() instanceof CustomUserDetails customUser) {
            return customUser.getUserId();
        }

        return null; // userId not available (anonymous or non-JWT auth)
    }

    /**
     * Returns the roles of the currently authenticated user.
     *
     * How it works:
     *   Spring Security stores roles as GrantedAuthority objects inside
     *   the Authentication object — we just stream them to strings.
     *
     * Example result: ["ROLE_ADMIN", "ROLE_EMPLOYEE"]
     */
    public List<String> getRolesFromContext() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return List.of();
        }

        // getAuthorities() returns the roles set in CustomUserDetails.getAuthorities()
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)   // e.g. "ROLE_ADMIN"
                .collect(Collectors.toList());
    }

    // ================================================================
    //  PATTERN 2 ─ Direct JWT Token Parsing
    //  ► Use this only when you have the raw token string
    //    (e.g. inside a filter, a webhook handler, or a utility class).
    // ================================================================

    /**
     * Extracts the username directly from a raw JWT string.
     *
     * @param token  raw Bearer token value (without "Bearer " prefix)
     */
    public String getUsernameFromToken(String token) {
        return jwtUtil.extractUsername(token);          // reads "sub" claim
    }

    /**
     * Extracts the userId directly from a raw JWT string.
     *
     * @param token  raw Bearer token value (without "Bearer " prefix)
     */
    public Long getUserIdFromToken(String token) {
        return jwtUtil.extractUserId(token);            // reads "userId" claim
    }

    /**
     * Extracts the roles directly from a raw JWT string.
     *
     * @param token  raw Bearer token value (without "Bearer " prefix)
     * @return list of role name strings, e.g. ["ROLE_ADMIN", "ROLE_EMPLOYEE"]
     */
    public List<String> getRolesFromToken(String token) {
        return jwtUtil.extractRoles(token);             // reads "roles" claim
    }

    // ================================================================
    //  BONUS ─ Helper: strip the "Bearer " prefix before parsing
    //  Tokens arrive in the Authorization header as:
    //      Authorization: Bearer eyJhbGci...
    //  Use this before calling the PATTERN 2 methods.
    // ================================================================

    /**
     * Strips the "Bearer " prefix from the Authorization header value.
     *
     * Example:
     *   stripBearer("Bearer eyJhbGci...") → "eyJhbGci..."
     */
    public String stripBearer(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);   // "Bearer " is 7 characters
        }
        return authorizationHeader;
    }
}
