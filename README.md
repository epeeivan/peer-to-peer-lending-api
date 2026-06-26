# P2P Lending Platform API

REST API for a peer-to-peer lending platform: loan funding by multiple investors,
repayment distribution, and a transactional wallet ledger. Built with Java 21 & Spring Boot.

## Tech stack
- Java 21, Spring Boot 3.5.15
- PostgreSQL 16
- Flyway (migrations), Testcontainers (integration tests)

## Prerequisites
- **Java 21** must be your active JDK — check with `java -version` (should print `21.x`)
- A container runtime for PostgreSQL: **Docker Desktop** or **Colima**

## Run the project
```bash
# 1. Clone and enter the project
git clone https://github.com/epeeivan/peer-to-peer-lending-api.git
cd peer-to-peer-lending-api

# 2. Start PostgreSQL (Colima users: run `colima start` first)
docker compose up -d

# 3. Make the Maven wrapper executable (first time only)
chmod +x mvnw

# 4. Start the API — Flyway creates the schema on boot
./mvnw spring-boot:run
```
The API runs on **http://localhost:8080**.

> **Windows:** use `mvnw.cmd` instead of `./mvnw`.
> **macOS tip:** if `java -version` isn't 21, activate it with
> `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`.

## Run the tests
```bash
./mvnw verify
```
Tests use Testcontainers — a throwaway PostgreSQL starts automatically, no manual setup.

> **Colima users only** — Testcontainers needs `DOCKER_HOST` to find the daemon
> (not needed on Docker Desktop):
> ```bash
> export DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock"
> export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
> ```
