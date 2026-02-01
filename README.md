# appsec-java-core
Java backend project focused on application security using Spring Boot

AppSec Java Lab

Stack
Java, Spring Boot, Spring Security, PostgreSQL, Docker, GitHub Actions

What this repo includes
- Spring Boot API
- Public ping endpoint
- Basic security defaults
- Flyway migrations
- Unit and integration tests
- CI with GitHub Actions
- Docker setup for local Postgres

Quick start

1) Start PostgreSQL (Docker)
docker compose up -d postgres

2) Run the app
mvn spring-boot:run

3) Run tests
mvn test

Endpoints

Ping (public)
GET http://localhost:8080/api/v1/ping

Health (public)
GET http://localhost:8080/actuator/health

Auth note
All other endpoints require HTTP Basic auth.

Configuration

Postgres env vars (optional)
DB_URL=jdbc:postgresql://localhost:5432/appsec
DB_USER=appsec
DB_PASS=appsec

Docker

Build image
docker build -t appsec-java .

Run image (needs Postgres running)
docker run --rm -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/appsec \
  -e DB_USER=appsec \
  -e DB_PASS=appsec \
  appsec-java

Development rules
- Finish features before adding new ones
- Keep README updated when behavior changes
