# AppSec Java Core

Backend project to build an AppSec portfolio with a production-style setup.

## Stack
- Java 21
- Spring Boot
- Spring Security (Basic Auth)
- PostgreSQL + Flyway
- Docker Compose
- Maven Wrapper
- GitHub Actions

## Local setup

### 1) Start Postgres
```bash
docker compose up -d
docker ps

2) Run the app
./mvnw spring-boot:run

3) Run tests
./mvnw test

Configuration
Database (defaults)
The app uses these defaults unless you override them with env vars:
DB_URL: jdbc:postgresql://localhost:5432/appsec
DB_USER: appsec
DB_PASS: appsec
Dev basic auth (for protected endpoints)
Configured in src/main/resources/application.properties:
User: dev
Password: devpass
Role: ADMIN
Trusted proxies (X-Forwarded-For)
Only requests coming from these proxy IPs are allowed to supply X-Forwarded-For:
app.trusted-proxies=127.0.0.1,::1
If the request is not from a trusted proxy, the app uses request.getRemoteAddr().

Endpoints
Public:
GET /api/v1/ping
Protected (Basic Auth):
GET /api/v1/audit-events/recent
Ops:
GET /actuator/health

Sprint 3: Audit logging (HTTP requests)
The application records incoming HTTP requests and stores them in PostgreSQL.
Recorded fields:
actor (authenticated username or anonymous)
method, path, status
ip (supports X-Forwarded-For when trusted)
userAgent
durationMs
Demo:
1. Call the public ping endpoint
curl -i http://localhost:8080/api/v1/ping

2. Retrieve recent audit events (Basic Auth required)
curl -i -u dev:devpass http://localhost:8080/api/v1/audit-events/recent

3. Send a forwarded client IP (trusted proxy only)
curl -i -H "X-Forwarded-For: 203.0.113.10" http://localhost:8080/api/v1/ping

Sprint 4: Rate limiting (in-memory)

Rate limits per IP + endpoint using an in-memory counter and a fixed 60s window.
Current limits:
/api/v1/ping: 30 requests per 60s per IP
/api/v1/audit-events/recent: 10 requests per 60s per IP
Responses include:
RateLimit-Limit
RateLimit-Remaining
RateLimit-Reset
Retry-After (only on 429)
Rate-limited requests are still audited (you will see status 429 in audit events).

Demo: show headers
curl -s -i -H "X-Forwarded-For: 203.0.113.10" http://localhost:8080/api/v1/ping | head -n 20

Demo: trigger 429
for i in {1..40}; do \
  curl -s -o /dev/null -w "%{http_code} " \
  -H "X-Forwarded-For: 203.0.113.10" \
  http://localhost:8080/api/v1/ping; \
done; echo

Demo: confirm 429 in audit log
curl -s -u dev:devpass http://localhost:8080/api/v1/audit-events/recent | head -c 600

Rules
Finish features before starting new ones.
Keep changes small and testable.
Update README when behavior changes.
