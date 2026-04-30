# 🔐 Spring Security & JWT — Complete Guide
### Employee Management System

---

## 📚 Table of Contents

1. [The Big Picture](#1-the-big-picture)
2. [What is Spring Security?](#2-what-is-spring-security)
3. [The Filter Chain — The Heart of Spring Security](#3-the-filter-chain--the-heart-of-spring-security)
4. [JWT (JSON Web Token) — What & Why](#4-jwt-json-web-token--what--why)
5. [Our Custom JWT Filter (JwtAuthFilter)](#5-our-custom-jwt-filter-jwtauthfilter)
6. [SecurityConfig — The Rules Engine](#6-securityconfig--the-rules-engine)
7. [UserDetails & CustomUserDetails](#7-userdetails--customuserdetails)
8. [CustomUserDetailsService](#8-customuserdetailsservice)
9. [JwtUtil — Token Factory](#9-jwtutil--token-factory)
10. [TokenUserExtractor — The Helper](#10-tokenuserextractor--the-helper)
11. [Authorization — @PreAuthorize](#11-authorization--preauthorize)
12. [Swagger / OpenAPI — SwaggerConfig](#12-swagger--openapi--swaggerconfig)
13. [Full Request Lifecycle](#13-full-request-lifecycle)
14. [Role-Based Access Summary](#14-role-based-access-summary)
15. [Common Errors & What They Mean](#15-common-errors--what-they-mean)

---

## 1. The Big Picture

Before diving into details, here is the **complete flow** of a protected API request:

```
CLIENT
  │
  │  HTTP Request
  │  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
  │
  ▼
┌─────────────────────────────────────────────────────────┐
│                   SERVLET CONTAINER (Tomcat)            │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │            Spring Security Filter Chain          │  │
│  │                                                  │  │
│  │  Filter 1: SecurityContextPersistenceFilter      │  │
│  │  Filter 2: UsernamePasswordAuthenticationFilter  │  │
│  │  ...                                             │  │
│  │  Filter N: ★ JwtAuthFilter (OUR CUSTOM FILTER)  │  │
│  │  Filter N+1: ExceptionTranslationFilter          │  │
│  │  Filter N+2: AuthorizationFilter                 │  │
│  └──────────────────────────────────────────────────┘  │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │              DispatcherServlet                   │  │
│  │         (Spring MVC request routing)             │  │
│  └──────────────────────────────────────────────────┘  │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │              @RestController                     │  │
│  │         (Your actual business logic)             │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

**Key insight:** The request passes through a chain of filters BEFORE it ever reaches your controller.
If any filter rejects the request → 401/403 is returned immediately, the controller is never called.

---

## 2. What is Spring Security?

Spring Security is a framework that handles two things:

| Concept | Question it answers | Example |
|---|---|---|
| **Authentication** | *Who are you?* | Are you logged in? Is your token valid? |
| **Authorization** | *What can you do?* | Are you an Admin? Can you delete employees? |

### How it works at a high level

```
Request arrives
      │
      ▼
Authentication (prove who you are)
      │
      ├── ✅ Valid JWT token → SecurityContext populated → continue
      │
      └── ❌ No token / bad token → 401 Unauthorized
              │
              ▼
          Authorization (check what you can do)
              │
              ├── ✅ Has required role → Controller runs
              │
              └── ❌ Wrong role → 403 Forbidden
```

### SecurityContextHolder — The Central Store

The most important concept in Spring Security is the `SecurityContextHolder`.
Think of it as a **thread-local box** that holds "who is currently making this request".

```java
// How Spring Security stores authentication after our filter runs:
SecurityContextHolder
    └── SecurityContext
            └── Authentication
                    ├── principal  → CustomUserDetails (your user object)
                    ├── credentials → null (cleared after login for security)
                    └── authorities → [GrantedAuthority("Admin"), ...]
```

**Thread-local** means: each HTTP request gets its own separate box.
Request from User A never sees the authentication of User B.

```
Thread-1 (User A's request): SecurityContextHolder → Authentication(user=Alice, roles=[Admin])
Thread-2 (User B's request): SecurityContextHolder → Authentication(user=Bob,   roles=[Employee])
```

---

## 3. The Filter Chain — The Heart of Spring Security

### What is a Filter?

A Servlet Filter is a component that intercepts HTTP requests/responses.
Every request goes through ALL filters in order, like a pipeline.

```
Request → Filter1 → Filter2 → Filter3 → ... → Controller
Response ← Filter1 ← Filter2 ← Filter3 ← ... ← Controller
```

Each filter decides:
- ✅ Continue: call `filterChain.doFilter(request, response)` → passes to next filter
- ❌ Stop: write directly to `response` → request never reaches controller

### Spring Security's Built-in Filters (simplified)

Spring Security registers ~15 filters automatically. The key ones:

```
1. SecurityContextHolderFilter
   └── Creates an empty SecurityContext for this thread

2. UsernamePasswordAuthenticationFilter
   └── Handles form-login (username/password in POST body)
   └── We DON'T use this — we use JWT instead

3. ★ JwtAuthFilter  ← OUR FILTER (inserted here)
   └── Reads Bearer token, validates it, sets SecurityContext

4. ExceptionTranslationFilter
   └── Catches AccessDeniedException → returns 403
   └── Catches AuthenticationException → returns 401

5. AuthorizationFilter  (last filter)
   └── Checks if SecurityContext has the required authorities
   └── If not → throws AccessDeniedException (caught by filter 4)
```

### OncePerRequestFilter — Why We Extend It

```java
public class JwtAuthFilter extends OncePerRequestFilter {
```

`OncePerRequestFilter` guarantees our filter runs **exactly once per request**,
even in cases where Spring internally forwards a request (which could trigger
the filter chain again). Without it, the filter could run twice.

### How Our Filter Fits In

```java
// In SecurityConfig:
.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
```

This tells Spring: "Run our JwtAuthFilter BEFORE the UsernamePasswordAuthenticationFilter".

Why? Because by the time the authorization check happens (AuthorizationFilter),
the SecurityContext must already be populated.

```
Request
  │
  ▼
JwtAuthFilter ← runs first, populates SecurityContext
  │
  ▼
UsernamePasswordAuthenticationFilter ← skipped (no form login)
  │
  ▼
AuthorizationFilter ← checks SecurityContext, allows or rejects
  │
  ▼
Controller ← runs only if authorized
```

---

## 4. JWT (JSON Web Token) — What & Why

### What is a JWT?

A JWT is a **self-contained token** — all the information needed to identify a user
is packed inside the token itself. No need to query the database on every request.

A JWT has 3 parts separated by dots:

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huIiwidXNlcklkIjoxLCJyb2xlcyI6WyJFbXBsb3llZSJdfQ.abc123
     │                              │                                              │
  HEADER                         PAYLOAD                                       SIGNATURE
(algorithm)              (your data — the claims)                    (proves nobody tampered)
```

**Header** (Base64 decoded):
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload** (Base64 decoded) — this is what WE put in it:
```json
{
  "sub": "john",
  "userId": 1,
  "roles": ["Employee"],
  "iat": 1746000000,
  "exp": 1746003600
}
```

**Signature** — created by:
```
HMACSHA256(
  base64(header) + "." + base64(payload),
  SECRET_KEY
)
```

The signature ensures: if someone modifies the payload, the signature breaks → server rejects it.

### Why JWT instead of Sessions?

| Sessions (Old way) | JWT (Our way) |
|---|---|
| Server stores session in memory/DB | Server stores nothing |
| Client sends session ID cookie | Client sends token in header |
| Every request → DB lookup to find session | Every request → just verify signature (math) |
| Hard to scale (sticky sessions) | Scales perfectly (stateless) |
| Works across one server | Works across many servers |

### Claims in Our Token

```java
// JwtUtil.generateToken() puts these 3 things in every token:
Jwts.builder()
    .setSubject(username)         // "sub"    → "john"
    .claim("userId", userId)      // custom   → 1
    .claim("roles", roles)        // custom   → ["Admin"]
```

| Claim | Type | Why we need it |
|---|---|---|
| `sub` | Standard | Standard JWT username field |
| `userId` | Custom | So we can query DB by id without extra lookup |
| `roles` | Custom | For authorization checks without extra DB call |
| `iat` | Standard | "Issued at" — when the token was created |
| `exp` | Standard | "Expires at" — token is invalid after this time |

---

## 5. Our Custom JWT Filter (JwtAuthFilter)

**File:** `security/jwt/JwtAuthFilter.java`

This is the most important class for understanding how authentication works.

```java
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {

        // STEP 1: Try to read the token from the header
        String authHeader = request.getHeader("Authorization");

        //         No header or not a Bearer token?
        //         → Pass request through WITHOUT setting authentication
        //         → SecurityContext stays empty
        //         → AuthorizationFilter will reject if endpoint is protected
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // ← continue chain
            return;
        }

        // STEP 2: Extract the raw token (remove "Bearer " prefix)
        String token    = authHeader.substring(7);

        // STEP 3: Extract username from the token's "sub" claim
        String username = jwtUtil.extractUsername(token);

        // STEP 4: Only set auth if:
        //   a) Token had a valid username
        //   b) SecurityContext is not already set (avoid overwriting)
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // STEP 5: Load the full user object from DB
            // This gives us the latest roles, even if they changed since token was issued
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // STEP 6: Build the Authentication object
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                    userDetails,    // principal (who are you?)
                    null,           // credentials (password — null after login, not needed)
                    userDetails.getAuthorities()  // authorities (what roles?)
                );

            // Attach IP address, session info for auditing/logging
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // STEP 7: Store in SecurityContextHolder
            // ★ THIS is the moment the user becomes "authenticated" for this request
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // STEP 8: ALWAYS continue the filter chain
        filterChain.doFilter(request, response);
    }
}
```

### Step-by-step visual:

```
Incoming Request: GET /api/employee/all
                  Authorization: Bearer eyJhbGci...

JwtAuthFilter:
  │
  ├─ 1. Read header: "Bearer eyJhbGci..."
  │
  ├─ 2. Strip "Bearer " → "eyJhbGci..."
  │
  ├─ 3. JwtUtil.extractUsername("eyJhbGci...") → "john"
  │
  ├─ 4. SecurityContext empty? YES → continue
  │
  ├─ 5. DB: SELECT * FROM users WHERE username='john' (with roles JOIN)
  │         → CustomUserDetails { id=1, username="john", roles=["Employee"] }
  │
  ├─ 6. Build: UsernamePasswordAuthenticationToken(
  │              principal=CustomUserDetails,
  │              credentials=null,
  │              authorities=[GrantedAuthority("Employee")]
  │            )
  │
  ├─ 7. SecurityContextHolder.setAuthentication(authToken)
  │         → User is now "authenticated" for this thread
  │
  └─ 8. filterChain.doFilter() → pass to next filter → reaches Controller ✅
```

### What happens with an invalid token?

```
Incoming Request: GET /api/employee/all
                  Authorization: Bearer INVALID_TOKEN

JwtAuthFilter:
  │
  ├─ 3. JwtUtil.extractUsername("INVALID_TOKEN") → throws SignatureException
  │
  │     (The exception propagates up — Spring's ExceptionTranslationFilter catches it)
  │
  └─ Response: 401 Unauthorized
```

---

## 6. SecurityConfig — The Rules Engine

**File:** `config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // ← enables @PreAuthorize annotations
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    private static final String[] PUBLIC_URLS = {
        "/api/auth/**",       // login + register
        "/swagger-ui/**",     // Swagger UI
        "/v3/api-docs/**",    // OpenAPI spec
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ① No CSRF — stateless APIs don't need it
            //    CSRF protects against browser-based attacks.
            //    Since we don't use cookies (we use Bearer tokens), CSRF isn't needed.
            .csrf(csrf -> csrf.disable())

            // ② STATELESS — no HttpSession created or used
            //    This means Spring will NEVER create a cookie or session.
            //    Every request must be authenticated independently with its JWT.
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ③ URL rules (coarse-grained)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_URLS).permitAll()  // these need no token
                .anyRequest().authenticated()              // everything else needs a valid JWT
            )

            // ④ Register our custom filter BEFORE Spring's form-login filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

### @EnableMethodSecurity

This annotation **activates method-level security**.

Without it:
```java
@PreAuthorize("hasAuthority('Admin')")  // ← silently IGNORED
public ResponseEntity<...> deleteEmployee(...) { ... }
```

With it:
```java
@PreAuthorize("hasAuthority('Admin')")  // ← actually ENFORCED ✅
public ResponseEntity<...> deleteEmployee(...) { ... }
```

### CSRF — Why disabled?

```
CSRF Attack (Cross-Site Request Forgery):
  1. You are logged into bank.com (cookie saved in browser)
  2. You visit evil.com
  3. evil.com silently sends: POST bank.com/transfer?amount=1000
  4. Your browser AUTOMATICALLY sends the bank.com cookie!
  5. Bank processes the request thinking it's you

Protection: CSRF token — a hidden random value that evil.com can't know.

Why we DON'T need it:
  Our API uses "Authorization: Bearer <token>" header.
  Browsers do NOT automatically add custom headers to cross-site requests.
  Only our app code explicitly sets the Authorization header.
  So CSRF is not a threat here.
```

### SessionCreationPolicy.STATELESS

```
Traditional (STATEFUL):
  Login → server creates Session → stores in memory → sends session cookie
  Next request → browser sends cookie → server looks up session → ✅

Our way (STATELESS):
  Login → server creates JWT → sends JWT to client → stores NOTHING
  Next request → client sends JWT in header → server validates signature → ✅

  No session = No memory usage on server = Scales to millions of users
```

---

## 7. UserDetails & CustomUserDetails

**File:** `security/CustomUserDetails.java`

Spring Security doesn't know about YOUR `User` entity.
It only understands the `UserDetails` interface.

`CustomUserDetails` is the **adapter** between your `User` entity and Spring Security.

```java
public class CustomUserDetails implements UserDetails {

    private final User user;  // YOUR entity

    // Spring Security calls this to get the username
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    // Spring Security calls this to get the password (for form-login)
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    // Spring Security calls this to get roles/permissions
    // SimpleGrantedAuthority wraps a string like "Admin" or "Employee"
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.getName()))
            .collect(Collectors.toList());
    }

    // ✅ Our addition — expose the DB id so TokenUserExtractor can use it
    public Long getUserId() {
        return user.getId();
    }

    // These return true by default → account is valid
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
```

### GrantedAuthority

This is how Spring Security represents a permission or role:

```java
new SimpleGrantedAuthority("Admin")
// represents the authority string "Admin"

// In @PreAuthorize:
hasAuthority('Admin')    // checks if "Admin" is in the authorities list
hasRole('Admin')         // checks if "ROLE_Admin" is in the list (adds ROLE_ prefix!)
```

> **Important in our project:**
> We use `hasAuthority('Admin')` not `hasRole('Admin')` because our role names
> in the DB are `"Admin"` and `"Employee"` (without the `ROLE_` prefix).

---

## 8. CustomUserDetailsService

**File:** `security/CustomUserDetailsService.java`

This service is called by Spring Security (inside our JwtAuthFilter) to load
the user from the database by username.

```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // ✅ findWithRolesByUsername uses @EntityGraph
        // This is critical! It loads the user AND their roles in ONE SQL query.
        // Without @EntityGraph, roles are LAZY — accessing them outside a
        // transaction throws LazyInitializationException.
        var user = userRepository.findWithRolesByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new CustomUserDetails(user);
    }
}
```

### Why @EntityGraph?

```sql
-- Without @EntityGraph (LAZY) — two separate queries:
SELECT * FROM users WHERE username = 'john';
SELECT * FROM user_roles WHERE user_id = 1;  -- only when you access user.getRoles()

-- With @EntityGraph — ONE query with JOIN:
SELECT u.*, r.*
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.username = 'john';
```

Without `@EntityGraph`, accessing `user.getRoles()` in the filter (which runs outside
a `@Transactional` boundary) throws:
```
org.hibernate.LazyInitializationException: failed to lazily initialize a collection
```

---

## 9. JwtUtil — Token Factory

**File:** `security/jwt/JwtUtil.java`

This class has one responsibility: **create and parse JWT tokens**.

```java
@Component
public class JwtUtil {

    private final String SECRET = "mySecretKey123";
    // ⚠️ In production: store this in application.properties or environment variable!
    // spring.jwt.secret=${JWT_SECRET}

    // ── Generating a token ──────────────────────────────────────────

    public String generateToken(String username, Long userId, Set<String> roles) {
        return Jwts.builder()
            .setSubject(username)          // "sub" claim — standard
            .claim("userId", userId)       // custom claim
            .claim("roles", roles)         // custom claim
            .setIssuedAt(new Date())       // "iat" — now
            .setExpiration(new Date(      // "exp" — 1 hour from now
                System.currentTimeMillis() + 1000L * 60 * 60
            ))
            .signWith(SignatureAlgorithm.HS256, SECRET.getBytes())
            .compact();                    // serialize to the "aaa.bbb.ccc" string
    }

    // ── Generic claim extractor ─────────────────────────────────────

    // This is the KEY design pattern:
    // One method that accepts a FUNCTION to extract any claim.
    // All other extract methods call this one.

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
            .setSigningKey(SECRET.getBytes())
            .parseClaimsJws(token)   // ← validates signature + expiry here!
            .getBody();
        return claimsResolver.apply(claims);
    }

    // ── Convenience methods (built on top of extractClaim) ──────────

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);  // "sub" claim
    }

    public Long extractUserId(String token) {
        return extractClaim(token, c -> c.get("userId", Long.class));
    }

    public List<String> extractRoles(String token) {
        return extractClaim(token, c -> (List<String>) c.get("roles"));
    }
}
```

### The Generic Pattern — Why It Matters

```java
// Instead of this repetitive code:
public String extractUsername(String token) {
    return parseToken(token).getBody().getSubject();
}
public Long extractUserId(String token) {
    return parseToken(token).getBody().get("userId", Long.class);
}
public List<String> extractRoles(String token) {
    return parseToken(token).getBody().get("roles", List.class);
}

// We write this ONCE:
public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    return claimsResolver.apply(parseToken(token).getBody());
}

// Then all extractions become ONE-LINERS:
extractClaim(token, Claims::getSubject)             // username
extractClaim(token, c -> c.get("userId", Long.class)) // userId
extractClaim(token, c -> c.get("roles", List.class))  // roles

// You can even add NEW claims without touching extractClaim:
extractClaim(token, c -> c.get("email", String.class))  // works immediately!
```

---

## 10. TokenUserExtractor — The Helper

**File:** `security/TokenUserExtractor.java`

A utility bean that provides clean, readable methods to get the current user's
info anywhere in your application.

### Pattern 1: SecurityContextHolder (Recommended — 95% of cases)

```java
// Use this in @Service and @Controller classes

String username = tokenUserExtractor.getUsernameFromContext();
Long   userId   = tokenUserExtractor.getUserIdFromContext();
List<String> roles = tokenUserExtractor.getRolesFromContext();

// Example in a service:
public EmployeeDto getMyEmployee() {
    Long userId = tokenUserExtractor.getUserIdFromContext();
    return employeeRepository.findByUserId(userId);
}
```

**How it works:**

```
JwtAuthFilter sets:
  SecurityContextHolder
    └── Authentication
            └── principal = CustomUserDetails { id=1, username="john" }

TokenUserExtractor reads:
  SecurityContextHolder.getContext().getAuthentication()
    └── .getPrincipal()  →  instanceof CustomUserDetails
            └── .getUserId()    → 1
            └── .getUsername()  → "john"
            └── .getAuthorities() → ["Admin"]
```

### Pattern 2: Direct Token Parsing (For filters/special cases)

```java
// Use this only when you have the raw JWT string
String token = tokenUserExtractor.stripBearer(request.getHeader("Authorization"));
String username = tokenUserExtractor.getUsernameFromToken(token);
Long   userId   = tokenUserExtractor.getUserIdFromToken(token);
List<String> roles = tokenUserExtractor.getRolesFromToken(token);
```

### Why two patterns?

```
Pattern 1 — SecurityContextHolder:
  ✅ No token re-parsing (faster)
  ✅ Roles are from DB (always up-to-date)
  ✅ Works anywhere — service, controller, component
  ❌ Only works AFTER JwtAuthFilter has run

Pattern 2 — Direct token parsing:
  ✅ Works even before SecurityContext is set (e.g., inside a filter)
  ✅ Useful for logging/auditing raw requests
  ❌ Roles are from token (could be stale if roles changed)
  ❌ Requires re-parsing the token (slightly slower)
```

---

## 11. Authorization — @PreAuthorize

**Annotation:** `org.springframework.security.access.prepost.PreAuthorize`

After authentication (who you are), Spring checks authorization (what you can do).

### How @PreAuthorize works internally

```
Request reaches Controller method
        │
        ▼
Spring AOP intercepts the method call
        │
        ▼
Evaluates the SpEL expression in @PreAuthorize(...)
        │
        ├── ✅ Expression is TRUE  → method runs
        │
        └── ❌ Expression is FALSE → throws AccessDeniedException
                                             │
                                             ▼
                                     ExceptionTranslationFilter catches it
                                             │
                                             ▼
                                     Returns 403 Forbidden
```

### SpEL Expressions Reference

```java
// Is the user authenticated at all?
@PreAuthorize("isAuthenticated()")

// Has a specific authority (exact string match)
@PreAuthorize("hasAuthority('Admin')")

// Has any one of these authorities
@PreAuthorize("hasAnyAuthority('Admin', 'Employee')")

// Has a role (Spring automatically prepends "ROLE_")
// hasRole('Admin') checks for "ROLE_Admin" — use only if your roles have ROLE_ prefix
@PreAuthorize("hasRole('ADMIN')")

// Combine with AND / OR
@PreAuthorize("hasAuthority('Admin') and isAuthenticated()")

// Check against a method parameter
@PreAuthorize("#userId == authentication.principal.userId")
// ↑ Only allow users to access their OWN data

// Access the current user inside the expression
@PreAuthorize("authentication.name == #username")
```

### Where to put @PreAuthorize

```java
// 1. On the CLASS → applies to ALL methods in the controller
@PreAuthorize("hasAuthority('Admin')")
@RestController
public class RoleController { ... }

// 2. On individual METHODS → fine-grained control
@GetMapping("/all")
@PreAuthorize("hasAnyAuthority('Admin', 'Employee')")
public List<EmployeeDto> getAll() { ... }

// 3. Method-level OVERRIDES class-level
@PreAuthorize("hasAuthority('Admin')")  // class default = Admin only
@RestController
public class SomeController {

    @GetMapping("/read")
    @PreAuthorize("hasAnyAuthority('Admin', 'Employee')")  // this method allows Employee too
    public List<...> getAll() { ... }

    @DeleteMapping("/{id}")
    // no method annotation → inherits class-level → Admin only
    public void delete(...) { ... }
}
```

### Our Authorization Rules

```
POST   /api/auth/login          → 🌐 Public (no token needed)
POST   /api/auth/register       → 🌐 Public (no token needed)

GET    /swagger-ui/**           → 🌐 Public
GET    /v3/api-docs/**          → 🌐 Public

GET    /api/users/me            → ✅ Any authenticated user

GET    /api/employee/all        → ✅ Admin or Employee
GET    /api/employee/{id}       → ✅ Admin or Employee
POST   /api/employee/create     → 🔒 Admin only
PUT    /api/employee/update/**  → 🔒 Admin only
DELETE /api/employee/delete/**  → 🔒 Admin only
PUT    /api/employee/assign/**  → 🔒 Admin only

GET    /api/departments         → ✅ Admin or Employee
GET    /api/departments/{id}    → ✅ Admin or Employee
POST   /api/departments         → 🔒 Admin only
PUT    /api/departments/{id}    → 🔒 Admin only
DELETE /api/departments/{id}    → 🔒 Admin only

ALL    /api/roles/**            → 🔒 Admin only
ALL    /api/users/**            → 🔒 Admin only (except /me)
```

---

## 12. Swagger / OpenAPI — SwaggerConfig

**File:** `config/SwaggerConfig.java`

Swagger is an interactive API documentation UI. It lets you test your API
directly in the browser without needing Postman.

```java
@Bean
public OpenAPI openAPI() {
    return new OpenAPI()
        .info(new Info().title("Employee Management System API").version("1.0.0"))

        // Register a security scheme named "bearerAuth"
        .components(new Components()
            .addSecuritySchemes("bearerAuth", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)  // HTTP-based
                .scheme("bearer")                // Bearer scheme
                .bearerFormat("JWT")             // hint: it's a JWT
            ))

        // Apply this scheme to ALL endpoints globally
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
}
```

### Accessing Swagger

```
Start the application → open browser:

http://localhost:8080/swagger-ui/index.html

Steps to test a protected endpoint:
1. Find POST /api/auth/login → click "Try it out"
2. Enter username and password → click "Execute"
3. Copy the token from the response
4. Click the "Authorize 🔒" button at the top of the page
5. Paste the token into the field (just the token, no "Bearer " prefix)
6. Click "Authorize" → close
7. Now all requests will include "Authorization: Bearer <your-token>"
```

---

## 13. Full Request Lifecycle

Let's trace a complete request from a browser to the database and back.

### Scenario: Admin deletes an employee

```
Request: DELETE /api/employee/delete/5
         Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

─────────────────────────────────────────────────────────────────
LAYER 1: Tomcat (Web Server)
  Receives the HTTP request
  Creates HttpServletRequest and HttpServletResponse objects
  Passes to Spring Security Filter Chain

─────────────────────────────────────────────────────────────────
LAYER 2: Spring Security Filter Chain

  ► Filter: SecurityContextHolderFilter
    Creates empty SecurityContext for this thread

  ► Filter: JwtAuthFilter  ← OUR FILTER
    1. Reads header: "Bearer eyJhbGci..."
    2. Strips prefix → raw token
    3. JwtUtil.parseClaimsJws(token) → validates signature ✅
    4. Extracts username: "john_admin"
    5. Calls userDetailsService.loadUserByUsername("john_admin")
       → SQL: SELECT u.*, r.* FROM users u JOIN ... WHERE u.username='john_admin'
       → Returns CustomUserDetails { id=2, username="john_admin", roles=["Admin"] }
    6. Creates UsernamePasswordAuthenticationToken(
         principal=CustomUserDetails, credentials=null, authorities=["Admin"]
       )
    7. SecurityContextHolder.setAuthentication(authToken)
    8. filterChain.doFilter() → continue

  ► Filter: AuthorizationFilter
    Checks: anyRequest().authenticated()
    SecurityContext has authentication? YES ✅ → continue

─────────────────────────────────────────────────────────────────
LAYER 3: Spring MVC DispatcherServlet
  Matches URL: DELETE /api/employee/delete/5
  Routes to: EmployeeController.deleteEmployee(id=5)

─────────────────────────────────────────────────────────────────
LAYER 4: Spring AOP (triggered by @PreAuthorize)
  Intercepts the method call before it runs
  Evaluates: hasAuthority('Admin')
  Checks SecurityContext: authorities = ["Admin"]
  "Admin" in ["Admin"]? YES ✅ → allow method to run

─────────────────────────────────────────────────────────────────
LAYER 5: Controller
  @DeleteMapping("/delete/{id}")
  @PreAuthorize("hasAuthority('Admin')")
  public ResponseEntity<Boolean> deleteEmployee(@PathVariable Long id) {
      employeeService.delete(id);       // calls service
      return ResponseEntity.ok(true);   // returns response
  }

─────────────────────────────────────────────────────────────────
LAYER 6: Service → Repository → Database
  employeeService.delete(5)
    → employeeRepository.deleteById(5)
    → SQL: DELETE FROM employees WHERE id = 5

─────────────────────────────────────────────────────────────────
RESPONSE travels back:
  Controller → Spring MVC → Filter Chain (in reverse) → Tomcat → Client

Final Response:
  HTTP 200 OK
  Body: true
```

### What happens if the token is expired?

```
JwtAuthFilter:
  jwtUtil.extractUsername(expiredToken)
    → Jwts.parser().parseClaimsJws(token)
    → throws ExpiredJwtException: JWT expired at 2026-04-30T10:00:00Z

  The exception propagates to ExceptionTranslationFilter
    → Returns: 401 Unauthorized
    → Body: { "status": 401, "error": "Unauthorized" }
```

### What if user has wrong role (Employee tries to delete)?

```
JwtAuthFilter runs successfully → SecurityContext set with roles=["Employee"]

AuthorizationFilter:
  anyRequest().authenticated() → authenticated? YES ✅ → pass to controller

Spring AOP (@PreAuthorize):
  hasAuthority('Admin')
  Checks: "Admin" in ["Employee"]? NO ❌
  Throws: AccessDeniedException

ExceptionTranslationFilter catches AccessDeniedException:
  Returns: 403 Forbidden
```

---

## 14. Role-Based Access Summary

### Roles in this system

| Role name (in DB) | Who has it | Assigned when |
|---|---|---|
| `Employee` | All users | At registration (automatic) |
| `Admin` | Admin users | Manually via `/api/users/{id}/roles/assign` |

### Why we use `hasAuthority()` not `hasRole()`

```java
// Spring Security's hasRole() internally does this:
hasRole('Admin')  →  checks for authority "ROLE_Admin"

// But our DB stores "Admin" (no prefix)
// So hasRole('Admin') would NEVER match!

// Correct approach for our project:
hasAuthority('Admin')   // ✅ checks for exactly "Admin"
hasAuthority('Employee') // ✅ checks for exactly "Employee"

// If you wanted to use hasRole(), you'd need to change:
// RoleConstants.ADMIN = "ROLE_ADMIN"
// and role names in DB to "ROLE_ADMIN"
```

---

## 15. Common Errors & What They Mean

### 401 Unauthorized

```
Cause 1: No Authorization header
  Fix: Add "Authorization: Bearer <token>" header

Cause 2: Token expired (exp claim is in the past)
  Fix: Login again to get a new token

Cause 3: Token signature invalid (tampered or wrong secret)
  Fix: Use a token from this server

Cause 4: Username in token no longer exists in DB
  Fix: Re-register or check DB
```

### 403 Forbidden

```
Cause: Authenticated but wrong role
  Fix: Assign the required role to the user

Example: Employee tries to DELETE /api/employee/delete/1
  → JwtAuthFilter authenticates them ✅
  → @PreAuthorize("hasAuthority('Admin')") fails ❌
  → 403 Forbidden
```

### LazyInitializationException

```
org.hibernate.LazyInitializationException:
  failed to lazily initialize a collection of role: User.roles

Cause: Accessing user.getRoles() outside a @Transactional context
  (typically happens inside a filter)

Fix: Use @EntityGraph to load roles eagerly:
  @EntityGraph(attributePaths = {"roles"})
  Optional<User> findWithRolesByUsername(String username);
  ↑ Already fixed in our CustomUserDetailsService
```

### NullPointerException on getUserIdFromContext()

```java
Long userId = tokenUserExtractor.getUserIdFromContext(); // returns null!

Cause: Request reached the controller without authentication
  (endpoint is @PreAuthorize("isAuthenticated()") but somehow got through,
   or you're calling it without any @PreAuthorize)

Fix: Always add @PreAuthorize("isAuthenticated()") on endpoints
     that call getUserIdFromContext()
     OR null-check the result.
```

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────┐
│             Spring Security in This Project          │
├─────────────────────────────────────────────────────┤
│                                                     │
│  1. JwtUtil           → Creates & parses tokens     │
│  2. JwtAuthFilter     → Validates token per-request │
│  3. SecurityConfig    → Rules: who can access what  │
│  4. CustomUserDetails → Adapts User → UserDetails   │
│  5. UserDetailsService→ Loads user from DB          │
│  6. TokenUserExtractor→ Gets current user anywhere  │
│  7. @PreAuthorize     → Method-level role check     │
│  8. SwaggerConfig     → API docs with auth support  │
│                                                     │
├─────────────────────────────────────────────────────┤
│  TOKEN PAYLOAD:                                     │
│    sub    = username                                │
│    userId = database id                             │
│    roles  = ["Admin"] or ["Employee"]               │
│    exp    = expiry (1 hour from login)              │
├─────────────────────────────────────────────────────┤
│  GET CURRENT USER (in any service/controller):      │
│    tokenUserExtractor.getUserIdFromContext()        │
│    tokenUserExtractor.getUsernameFromContext()      │
│    tokenUserExtractor.getRolesFromContext()         │
├─────────────────────────────────────────────────────┤
│  SWAGGER UI:                                        │
│    http://localhost:8080/swagger-ui/index.html      │
└─────────────────────────────────────────────────────┘
```

