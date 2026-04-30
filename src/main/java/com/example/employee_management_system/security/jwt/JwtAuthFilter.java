package com.example.employee_management_system.security.jwt;

import com.example.employee_management_system.security.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter
 *
 * Runs ONCE per request (OncePerRequestFilter) before Spring Security processes it.
 *
 * Flow:
 *   1. Read the "Authorization: Bearer <token>" header
 *   2. Extract the username from the token via JwtUtil
 *   3. Load UserDetails (with roles) from the DB
 *   4. Build an Authentication object and store it in SecurityContextHolder
 *   5. Continue the filter chain → Spring Security sees the user as authenticated
 *
 * If no token is present, the request continues unauthenticated —
 * SecurityConfig will then reject it if the endpoint requires authentication.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // ── Step 1: Read the Authorization header ──────────────────────
        final String authHeader = request.getHeader("Authorization");

        // No token → pass the request along (unauthenticated)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── Step 2: Extract username from token ─────────────────────────
        final String token    = authHeader.substring(7); // strip "Bearer "
        final String username = jwtUtil.extractUsername(token);

        // ── Step 3: Only set context if not already authenticated ───────
        // (avoids overwriting an existing session-based auth if ever used)
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load full UserDetails (including roles) from DB
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // ── Step 4: Build Authentication object ─────────────────────
            // credentials = null  (we don't store passwords in the context after login)
            // authorities = roles from CustomUserDetails.getAuthorities()
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            // Attach request metadata (IP, session id) for auditing
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // ── Step 5: Store in SecurityContextHolder ───────────────────
            // From this point forward, SecurityContextHolder.getContext().getAuthentication()
            // returns this object — which is what TokenUserExtractor reads.
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // Continue to the next filter / controller
        filterChain.doFilter(request, response);
    }
}

