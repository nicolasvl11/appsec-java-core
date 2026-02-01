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
