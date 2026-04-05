# AppSec Java Core

Backend project for an Application Security portfolio with production-style fundamentals.

## Stack

- Java 21
- Spring Boot
- Spring Security with Basic Auth
- PostgreSQL
- Flyway
- Docker Compose
- Maven Wrapper
- GitHub Actions

## What this project shows

- Clean backend structure
- Basic authentication for protected endpoints
- Audit logging of HTTP requests
- Request ID generation and propagation for traceability
- In-memory rate limiting by IP and endpoint
- Trusted proxy handling for `X-Forwarded-For`
- Reproducible local setup with Docker
- Automated tests for core behavior

## Endpoints

Public:
- `GET /api/v1/ping`

Protected with Basic Auth:
- `GET /api/v1/audit-events/recent`

Protected with role `ADMIN`:
- `GET /api/v1/admin`

Ops:
- `GET /actuator/health`

## Security error responses

Protected endpoints return `application/problem+json` for security-related errors.

### 401 Unauthorized

Returned when authentication is missing.

Example:

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Authentication is required to access this resource.",
  "path": "/api/v1/audit-events/recent"
}
403 Forbidden

Returned when the user is authenticated but does not have permission.

Example:

{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "You do not have permission to access this resource.",
  "path": "/api/v1/admin"
}
429 Too Many Requests

Returned when the client exceeds the configured rate limit.

Example:

{
  "type": "about:blank",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "Rate limit exceeded for this client and endpoint.",
  "path": "/api/v1/ping",
  "retryAfterSeconds": 12
}
Local setup
1. Start PostgreSQL
docker compose up -d
docker ps
2. Run the application
./mvnw spring-boot:run
3. Run tests
./mvnw test
Configuration
Database defaults

The app uses these defaults unless you override them with environment variables:

DB_URL=jdbc:postgresql://localhost:5432/appsec
DB_USER=appsec
DB_PASS=appsec
Dev Basic Auth

Defined in src/main/resources/application.properties:

User: dev
Password: devpass
Role: ADMIN
Trusted proxies

Only requests coming from trusted proxy IPs are allowed to supply X-Forwarded-For.

app.trusted-proxies=127.0.0.1,::1

If the request does not come from a trusted proxy, the app uses request.getRemoteAddr().

Audit logging

Each HTTP request is recorded in PostgreSQL as an audit event.

Stored fields include:

actor
action
target
eventTime
meta as JSONB with:
ip
method
status
userAgent
durationMs
requestId
Request ID tracing

Each request receives an X-Request-Id header.

If the client sends X-Request-Id, the application echoes it back.
If the header is missing, the application generates one.
The request ID is also stored in the audit event metadata.

Example:

curl -i http://localhost:8080/api/v1/ping
Rate limiting

The application applies in-memory rate limiting per IP and endpoint using a fixed 60-second window.

Current limits:

/api/v1/ping: 30 requests per 60 seconds per IP
/api/v1/audit-events/recent: 10 requests per 60 seconds per IP

Responses include:

RateLimit-Limit
RateLimit-Remaining
RateLimit-Reset

Blocked responses also include:

HTTP 429
Retry-After
Content type application/problem+json

Example rate limit error body:

{
  "type": "about:blank",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "Rate limit exceeded for this client and endpoint.",
  "path": "/api/v1/ping",
  "retryAfterSeconds": 12
}

Rate-limited requests are still written to the audit log.

Demo commands
Ping
curl -i http://localhost:8080/api/v1/ping
Recent audit events
curl -i -u dev:devpass http://localhost:8080/api/v1/audit-events/recent
Admin endpoint
curl -i -u dev:devpass http://localhost:8080/api/v1/admin
Ping with custom request ID
curl -i -H "X-Request-Id: req-123" http://localhost:8080/api/v1/ping
Rate limit headers
curl -s -i -H "X-Forwarded-For: 203.0.113.10" http://localhost:8080/api/v1/ping | head -n 20
Trigger 429
for i in {1..40}; do
  curl -s -o /dev/null -w "%{http_code} " \
    -H "X-Forwarded-For: 203.0.113.10" \
    http://localhost:8080/api/v1/ping
done
echo
Confirm 429 in audit log
curl -s -u dev:devpass http://localhost:8080/api/v1/audit-events/recent | head -c 900
Test strategy

Default test run:

./mvnw test

This runs the fast test suite.

The PostgreSQL integration test for audit logging uses Testcontainers and only runs when explicitly enabled:

RUN_TC=true ./mvnw test

On Windows PowerShell:

$env:RUN_TC="true"
./mvnw test
Rules
Finish features before starting new ones
Keep changes small and testable
Update the README when behavior changes
Do not commit stray files or logs