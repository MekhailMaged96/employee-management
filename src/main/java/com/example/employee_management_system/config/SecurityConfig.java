package com.example.employee_management_system.config;

import com.example.employee_management_system.security.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security Configuration
 *
 * Key decisions:
 *  ┌─────────────────────────────────────────────────────────┐
 *  │ STATELESS session — no cookies, no HttpSession.         │
 *  │ Every request must carry a valid JWT in the header.     │
 *  └─────────────────────────────────────────────────────────┘
 *
 *  @EnableMethodSecurity turns on @PreAuthorize on controllers.
 *  Without it, those annotations are silently ignored.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // ← enables @PreAuthorize / @Secured on controller methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    // ── Public endpoints (no token required) ──────────────────────────
    private static final String[] PUBLIC_URLS = {
            "/api/auth/**",          // login & register
            "/swagger-ui/**",        // Swagger UI assets
            "/swagger-ui.html",
            "/v3/api-docs/**",       // OpenAPI spec JSON
            "/v3/api-docs.yaml"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ① Disable CSRF — not needed for stateless JWT APIs
            .csrf(csrf -> csrf.disable())

            // ② Stateless — Spring will NOT create or use an HttpSession
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ③ URL-level rules (coarse-grained — fine-grained is done via @PreAuthorize)
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PUBLIC_URLS).permitAll()
                    .anyRequest().authenticated()   // everything else → must have valid JWT
            )

            // ④ Plug in our JWT filter BEFORE Spring's username/password filter
            //    This ensures the SecurityContext is populated before any auth check runs
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Expose AuthenticationManager as a bean so it can be injected in
     * AuthServiceImpl if programmatic authentication is ever needed.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
