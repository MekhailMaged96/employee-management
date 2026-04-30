package com.example.employee_management_system.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI Configuration
 *
 * What this does:
 *  1. Defines the API metadata (title, version, description)
 *  2. Registers a "Bearer Auth" security scheme so the Swagger UI shows
 *     an "Authorize 🔒" button where you can paste your JWT token.
 *  3. Applies that scheme globally so every endpoint shows the padlock icon.
 *
 * Access Swagger UI at:  http://localhost:8080/swagger-ui/index.html
 * Access OpenAPI JSON at: http://localhost:8080/v3/api-docs
 */
@Configuration
public class SwaggerConfig {

    // The name we give to our security scheme — referenced in SecurityRequirement
    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                // ── API Info ───────────────────────────────────────────
                .info(new Info()
                        .title("Employee Management System API")
                        .version("1.0.0")
                        .description("""
                                REST API for managing employees, departments, roles and users.
                                
                                **How to authenticate:**
                                1. Call `POST /api/auth/register` or `POST /api/auth/login`
                                2. Copy the `token` from the response
                                3. Click the **Authorize 🔒** button above and paste: `<your-token>`
                                   (Swagger will automatically add `Bearer ` prefix)
                                """)
                        .contact(new Contact()
                                .name("Employee Management System")))

                // ── Security Scheme: Bearer JWT ────────────────────────
                // This tells Swagger: "there is a security scheme called bearerAuth
                //  that uses HTTP Bearer authentication with JWT tokens"
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)   // HTTP-based scheme
                                .scheme("bearer")                  // "Bearer " prefix
                                .bearerFormat("JWT")               // hint to Swagger UI
                                .description("Paste your JWT token here (without 'Bearer ' prefix)")))

                // ── Apply security globally to all endpoints ───────────
                // Individual public endpoints (@PostMapping /auth/**) won't
                // actually require it — but the padlock will show everywhere.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME));
    }
}

