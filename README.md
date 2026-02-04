AppSec Java Core

Stack
Java, Spring Boot, Spring Security, PostgreSQL, Docker, GitHub Actions

Goal
Build an AppSec portfolio with a real backend project and production-style setup.

Run Postgres
docker compose up -d postgres

Run app
mvn spring-boot:run

Run tests
mvn test

Endpoints
GET /api/v1/ping

Health
GET /actuator/health

Rules
Finish features before adding new ones.

Audit logging
The application records incoming HTTP requests and stores them in PostgreSQL.

Demo:
1) Call the public ping endpoint
curl http://localhost:8080/api/v1/ping

2) Retrieve recent audit events (basic auth required)
User: dev
Password: devpass

curl -u dev:devpass http://localhost:8080/api/v1/audit-events/recent

Audit logging
Demo
curl http://localhost:8080/api/v1/ping

curl -u dev:devpass http://localhost:8080/api/v1/audit-events/recent

curl -H "X-Forwarded-For: 203.0.113.10" http://localhost:8080/api/v1/ping
