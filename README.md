# appsec-java-core

A production-grade Spring Boot REST API demonstrating security engineering fundamentals. Every design decision maps to a concrete threat or operational requirement — nothing here is security theatre.

## Quick start

```bash
docker run -e POSTGRES_DB=appsec -e POSTGRES_USER=appsec -e POSTGRES_PASSWORD=appsec \
       -p 5432:5432 postgres:16
./mvnw spring-boot:run
```

Register a user, then use the token on protected endpoints:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password1"}' | jq -r .token)

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/users/me
```

---

## Architecture

```
Request
  │
  ├─ RequestIdFilter         — assigns/echoes X-Request-Id, sets MDC
  ├─ JwtAuthenticationFilter — validates RS256 Bearer token, sets SecurityContext
  ├─ [Spring Security]       — CORS, CSRF disabled, header writers, authz
  ├─ AuditLoggingFilter      — records every request to audit_event table + metrics
  ├─ RateLimitFilter         — fixed-window per (path × client IP)
  └─ DispatcherServlet       — controller routing, @Valid, GlobalExceptionHandler
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

---

## Design decisions

### JWT: RS256 over HS256
RS256 (asymmetric RSA-2048) was chosen over HS256 (symmetric HMAC). In a microservices topology, downstream services can verify tokens using the public key without ever seeing the signing secret. With HS256, any verifier is also a potential signer.

**Tradeoff**: RS256 tokens are ~400 bytes larger and signing is ~5× slower. For a high-traffic API, consider EC keys (ES256) for smaller size and equivalent security.

### Rate limiting: fixed window in-memory
The `InMemoryRateLimiter` uses a fixed-window counter in a `ConcurrentHashMap`. Zero-dependency and sufficient for a single-instance deployment.

**Known limitation**: Allows a burst of `2 × limit` requests across a window boundary. The `RateLimiter` interface is designed to swap in a Redis-backed sliding-window implementation without changing callers. See `RateLimiter.java` for the Redis design spec (Lua EVALSHA, atomic increment + TTL).

### Audit log: relational DB, not a log file
Audit events are written to PostgreSQL via JPA. Queryable, retainable for compliance, and accessible to a DBA without shell access.

**Tradeoff**: Each write adds ~1 ms. The filter writes in the `finally` block after the response is committed, so it does not affect client-visible response time.

### Error responses: RFC 9457 Problem Detail
All errors use `Content-Type: application/problem+json` with `status`, `title`, `detail`, `path`. Clients distinguish error types by status code, not English text. `GlobalExceptionHandler` is the single mapping point from exceptions to HTTP status codes.

### CORS: allowlist per origin
`CorsConfigurationSource` rejects cross-origin requests from unlisted origins with 403. Origins are configurable via `app.cors.allowed-origins` so staging and prod can differ without a code change.

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
| GET | `/api/v1/users/me` | Return own profile |
| PATCH | `/api/v1/users/me/password` | Change password |

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
| GET | `/actuator/health` | Liveness check |
| GET | `/actuator/metrics` | Micrometer counters |

Custom metrics exposed: `http.requests.total`, `ratelimit.blocked.total`, `auth.failures.total`.

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
./mvnw test                    # unit + slice tests (~64 tests, <30 s)
RUN_TC=true ./mvnw test        # includes Testcontainers integration tests
# Coverage report (≥70% line required by Jacoco gate):
start target/site/jacoco/index.html   # Windows
open target/site/jacoco/index.html    # macOS/Linux
```

---

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `app.jwt.private-key` | dev key | PKCS8 RSA-2048 private key (Base64 DER). **Replace in production.** |
| `app.jwt.public-key` | dev key | X509 RSA-2048 public key (Base64 DER). **Replace in production.** |
| `app.jwt.expiration-ms` | `86400000` | Token TTL in ms (24 h) |
| `app.cors.allowed-origins` | `http://localhost:3000` | Comma-separated allowed CORS origins |
| `app.trusted-proxies` | `127.0.0.1,::1` | IPs allowed to set `X-Forwarded-For` |
| `spring.profiles.active` | `default` | Set to `prod` for structured JSON logging |

## Stack

- Java 21, Spring Boot 3.5, Spring Security 6
- PostgreSQL + Flyway (schema migrations)
- jjwt 0.12 (JWT RS256)
- Micrometer + Spring Actuator (metrics)
- Logstash Logback Encoder (structured JSON logs in `prod` profile)
- Testcontainers (full integration tests with `RUN_TC=true`)
- Jacoco (line coverage gate ≥70%)
