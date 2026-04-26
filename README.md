# appsec-java-core

A production-grade Spring Boot REST API demonstrating security engineering fundamentals. Every design decision maps to a concrete threat or operational requirement — nothing here is security theatre.

## Quick start

### With Docker Compose (recommended)

```bash
# Starts PostgreSQL, Redis, and the app (builds image from Dockerfile)
docker compose up --build
```

The app is ready when `/actuator/health` returns `{"status":"UP"}`.

### Local dev (PostgreSQL only)

```bash
docker compose up postgres redis -d
./mvnw spring-boot:run
```

Register a user, then use the token on protected endpoints:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password1"}' | jq -r .token)

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/users/me
```

Interactive API docs: **http://localhost:8080/swagger-ui.html**

---

## Architecture

```
Request
  │
  ├─ RequestIdFilter         — assigns/echoes X-Request-Id, sets MDC
  ├─ JwtAuthenticationFilter — validates RS256 Bearer token, sets SecurityContext
  ├─ [Spring Security]       — CORS, CSRF disabled, header writers, authz, oauth2Login
  ├─ AuditLoggingFilter      — records every request to audit_event table + metrics
  ├─ RateLimitFilter         — fixed-window per (path × client IP)
  └─ DispatcherServlet       — controller routing, @Valid, GlobalExceptionHandler
```

### OAuth2 login flow

```
Browser → /oauth2/authorization/google  (or /github)
        ← redirect to provider
Provider → /login/oauth2/code/google
        → OAuth2LoginSuccessHandler
              findOrCreateOAuth2User(provider, sub, email)
              generateToken(username, role)
        ← redirect to {app.oauth2.redirect-uri}?token=<JWT>
Frontend stores JWT and uses it for all subsequent API calls.
```

---

## Threat model

| Threat | STRIDE | Control | Where |
|--------|--------|---------|-------|
| Unauthenticated access to protected data | Spoofing | JWT RS256 + `authenticated()` authorization | `SecurityConfig`, `JwtAuthenticationFilter` |
| Privilege escalation (USER acting as ADMIN) | Elevation | Role encoded in JWT claim, `hasRole("ADMIN")` enforced by Spring Security | `SecurityConfig`, `AdminController` |
| Brute-force login | Spoofing | Rate limiter: 5 req/min per IP on `/api/v1/auth/*` | `RateLimitFilter` |
| API abuse / DoS via legitimate endpoints | Denial of Service | Rate limiter: 30 req/min on `/ping`, 10 req/min on audit endpoint | `RateLimitFilter` |
| IP spoofing via X-Forwarded-For | Spoofing | XFF only trusted from known proxy IPs (`app.trusted-proxies`) | `TrustedProxyConfig` |
| Weak password storage | Information Disclosure | BCrypt with work factor 10 (`BCryptPasswordEncoder`) | `AuthService`, `SecurityConfig` |
| JWT algorithm confusion (HS256 downgrade) | Tampering | RS256 enforced by fixed `JwtService` parser; no algorithm header trust | `JwtService` |
| Clickjacking | Tampering | `X-Frame-Options: DENY` | `SecurityConfig` headers |
| MIME sniffing | Tampering | `X-Content-Type-Options: nosniff` | `SecurityConfig` headers |
| Protocol downgrade (HTTP) | Tampering | `Strict-Transport-Security: max-age=31536000; includeSubDomains` | `SecurityConfig` headers |
| Inline script injection | Injection | `Content-Security-Policy: default-src 'self'` | `SecurityConfig` headers |
| Cross-Origin data theft | Information Disclosure | CORS allowlist (`app.cors.allowed-origins`), credentials allowed only for listed origins | `SecurityConfig` CORS |
| Mass assignment / malformed input | Tampering | `@Valid` on all request bodies; `GlobalExceptionHandler` → 400 problem+json | DTOs, `GlobalExceptionHandler` |
| Untracked security incidents | Repudiation | Every HTTP request recorded in `audit_event` with actor, IP, requestId, duration | `AuditLoggingFilter`, `AuditEventService` |
| Trace correlation loss across services | Repudiation | `X-Request-Id` propagated in MDC and in every audit record | `RequestIdFilter` |
| Container running as root | Elevation of Privilege | Non-root `appuser` in Dockerfile | `Dockerfile` |
| Known CVEs in container image | Tampering | Trivy scan blocks CRITICAL CVEs on every `main` push | `.github/workflows/docker.yml` |
| Rate limiter state lost on restart | Denial of Service | Redis-backed rate limiter available (`app.ratelimit.type=redis`) for multi-instance deployments | `RedisRateLimiter` |

---

## Design decisions

### JWT: RS256 over HS256
RS256 (asymmetric RSA-2048) was chosen over HS256 (symmetric HMAC). In a microservices topology, downstream services can verify tokens using the public key without ever seeing the signing secret. With HS256, any verifier is also a potential signer.

**Tradeoff**: RS256 tokens are ~400 bytes larger and signing is ~5× slower. For a high-traffic API, consider EC keys (ES256) for smaller size and equivalent security.

### Rate limiting: in-memory (default) or Redis (distributed)
The `InMemoryRateLimiter` uses a fixed-window counter in a `ConcurrentHashMap`. Zero-dependency and sufficient for a single-instance deployment. The `RateLimiter` interface is the Strategy abstraction — swap to `RedisRateLimiter` with one config property.

`RedisRateLimiter` executes an atomic Lua script (`INCR` + `EXPIRE` in one round-trip), making it safe under horizontal scale-out with no TOCTOU gap. **Fail-open**: if Redis is unreachable, requests are allowed through rather than blocking the service.

Enable Redis rate limiting: `app.ratelimit.type=redis` (requires `REDIS_HOST`/`REDIS_PORT`).

### OAuth2 hybrid: provider flow → internal JWT
OAuth2/OIDC login uses Spring Security's standard flow (including session for state exchange), but the `OAuth2LoginSuccessHandler` immediately issues an internal RS256 JWT and redirects the frontend with `?token=<jwt>`. From that point on, the app is fully stateless — no server-side OAuth2 session is retained. Password-based and OAuth2 accounts coexist in the same `users` table.

### Audit log: relational DB, not a log file
Audit events are written to PostgreSQL via JPA. Queryable, retainable for compliance, and accessible to a DBA without shell access. Each write adds ~1 ms and executes in the `finally` block after the response is committed.

### Error responses: RFC 9457 Problem Detail
All errors use `Content-Type: application/problem+json` with `status`, `title`, `detail`, `path`. `GlobalExceptionHandler` is the single mapping point from exceptions to HTTP status codes.

### Container: non-root, distroless-style JRE
Multi-stage Dockerfile: Maven + JDK 21 builds the fat JAR, then copies it into a `eclipse-temurin:21-jre-alpine` image. A dedicated `appuser` (no shell, no sudo) runs the process. JVM is container-aware (`-XX:+UseContainerSupport`, `-XX:MaxRAMPercentage=75`).

---

## API reference

### Auth (public)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/register` | Create account — returns JWT |
| POST | `/api/v1/auth/login` | Authenticate — returns JWT |

### User profile (JWT required)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/users/me` | Return own profile (id, username, role, email, provider) |
| PATCH | `/api/v1/users/me/password` | Change password (local accounts only) |

### OAuth2 (JWT required)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/oauth2/userinfo` | OIDC-like identity: sub, email, provider, oauth2User flag |

### OAuth2 login (browser redirect — no JWT)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/oauth2/authorization/google` | Start Google OIDC flow |
| GET | `/oauth2/authorization/github` | Start GitHub OAuth2 flow |

### Admin (JWT + ADMIN role)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/admin` | Admin status |

### Audit (JWT required)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/audit-events/recent` | Last 20 audit events |

### Observability (public)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/actuator/health` | Overall health (UP / DOWN) |
| GET | `/actuator/health/liveness` | Kubernetes liveness probe |
| GET | `/actuator/health/readiness` | Kubernetes readiness probe |
| GET | `/actuator/metrics` | Micrometer counters |

Custom metrics: `http.requests.total`, `ratelimit.blocked.total`, `auth.failures.total`.

### Documentation (public)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/v3/api-docs` | OpenAPI 3.0 JSON spec |
| GET | `/swagger-ui.html` | Swagger UI (redirects to `/swagger-ui/index.html`) |

---

## CI/CD

Two GitHub Actions workflows run on every push:

| Workflow | Trigger | Steps |
|----------|---------|-------|
| **CI — Test & Coverage** (`ci.yml`) | All branches & PRs | Java 21 setup → `./mvnw test` (`RUN_TC=true`) → JaCoCo HTML + XML artifact → coverage % in job summary |
| **Docker — Build & Scan** (`docker.yml`) | Push to `main` only | Docker Buildx → multi-stage image build → **Trivy CRITICAL scan** (blocks push) → Trivy HIGH scan → SARIF → GitHub Security tab |

Both workflows cache Maven dependencies and Docker layers via GitHub Actions cache.

---

## Security response catalogue

All errors follow RFC 9457 (`application/problem+json`):

| Scenario | Status | title |
|----------|--------|-------|
| No / invalid JWT | 401 | Unauthorized |
| Valid JWT, wrong role | 403 | Forbidden |
| Input validation failure | 400 | Validation Failed |
| Username already taken | 409 | Conflict |
| Rate limit exceeded | 429 | Too Many Requests |
| Route not found | 404 | Not Found |
| Internal error (details hidden) | 500 | Internal Server Error |

---

## Running tests

```bash
./mvnw test                    # unit + slice tests (78 tests, <30 s, Docker not required)
RUN_TC=true ./mvnw test        # also runs AuditLoggingIntegrationTest (needs Docker)
# Testcontainers-based tests (ActuatorHealth, RedisRateLimiter, OpenApiSchema)
# run automatically when Docker Desktop is available (disabledWithoutDocker = true)

# Coverage report (≥70% line required by JaCoCo gate):
start target/site/jacoco/index.html   # Windows
open target/site/jacoco/index.html    # macOS/Linux
```

---

## Configuration

| Property / Env Var | Default | Description |
|--------------------|---------|-------------|
| `app.jwt.private-key` | dev key | PKCS8 RSA-2048 private key (Base64 DER). **Replace in production.** |
| `app.jwt.public-key` | dev key | X509 RSA-2048 public key (Base64 DER). **Replace in production.** |
| `app.jwt.expiration-ms` | `86400000` | Token TTL in ms (24 h) |
| `app.cors.allowed-origins` | `http://localhost:3000` | Comma-separated allowed CORS origins |
| `app.trusted-proxies` | `127.0.0.1,::1` | IPs allowed to set `X-Forwarded-For` |
| `app.ratelimit.type` | `memory` | Rate limiter backend: `memory` or `redis` |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis connection (required when `app.ratelimit.type=redis`) |
| `app.oauth2.redirect-uri` | `http://localhost:3000/oauth2/redirect` | Frontend URI receiving `?token=<jwt>` after OAuth2 login |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | placeholder | Google OAuth2 credentials |
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | placeholder | GitHub OAuth2 credentials |
| `spring.profiles.active` | `default` | Set to `prod` for structured JSON logging |

---

## Stack

- **Java 21**, Spring Boot 3.5, Spring Security 6
- **Spring Security OAuth2 Client** — Google OIDC + GitHub OAuth2
- **PostgreSQL 16** + Flyway (schema migrations, V1-V3)
- **Redis 7** — distributed rate limiting (`RedisRateLimiter` with atomic Lua)
- **jjwt 0.12** — JWT RS256 signing/validation
- **springdoc-openapi 2.8** — OpenAPI 3.0 spec + Swagger UI
- **Micrometer** + Spring Actuator — metrics + Kubernetes health probes
- **Logstash Logback Encoder** — structured JSON logs (`prod` profile)
- **Testcontainers** — PostgreSQL + Redis integration tests
- **JaCoCo** — line coverage gate ≥70%
- **Docker** multi-stage image (JDK build → JRE 21 Alpine runtime, non-root user)
- **Trivy** — container vulnerability scanning in CI (CRITICAL = build failure)
