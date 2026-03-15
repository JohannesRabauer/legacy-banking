# Plan 05 — Backend: Dockerfile

## Output file
```
banking-service/Dockerfile
```

---

## Strategy
Multi-stage Docker build:
- **Stage 1 (`builder`)**: `maven:3.6.3-jdk-8` — compiles the project and produces `banking-service.war`
- **Stage 2 (`runtime`)**: `tomcat:7.0.109-jdk8` — copies the WAR into `webapps/`, starts Tomcat

This keeps the final image lean (no Maven installation, no source code).

---

## Full Dockerfile

```dockerfile
# ---------------------------------------------------------------
# Stage 1: Build the WAR with Maven
# ---------------------------------------------------------------
FROM maven:3.6.3-jdk-8 AS builder

WORKDIR /build

# Copy POM first so Maven dependency layer is cached
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn package -B -DskipTests

# ---------------------------------------------------------------
# Stage 2: Deploy WAR to Tomcat 7
# ---------------------------------------------------------------
FROM tomcat:7.0.109-jdk8

# Remove default Tomcat webapps (security + cleanliness)
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy the built WAR
COPY --from=builder /build/target/banking-service.war \
                    /usr/local/tomcat/webapps/banking-service.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
```

---

## Image choices — rationale

| Image | Version | Reason |
|---|---|---|
| `maven:3.6.3-jdk-8` | 3.6.3 (2019) | Last Maven 3.6.x; works with Spring 3.2 POM conventions; Java 8 |
| `tomcat:7.0.109-jdk8` | 7.0.109 (last 7.x) | Servlet 3.0; compatible with Spring 3.2 WAR + JAX-WS RI; Java 8 JDK |

`tomcat:7.0.109-jdk8` is available on Docker Hub under the official `tomcat` repository.

---

## Environment variables consumed at runtime

These are injected by Docker Compose (see Plan 08) and read by Spring's `applicationContext.xml`:

| Variable | Example value | Purpose |
|---|---|---|
| `DB_HOST` | `postgres` | PostgreSQL hostname (Docker Compose service name) |
| `DB_USER` | `bankuser` | Database username |
| `DB_PASSWORD` | `bankpassword` | Database password |

Spring 3.2 resolves `${DB_HOST:localhost}` syntax via `StandardEnvironment` (system env → system props → defaults).

---

## Build optimisation notes

- `mvn dependency:go-offline -B` downloads all dependencies before copying source. This means if only source code changes, Docker cache still hits on the dependency layer.
- `-DskipTests` — no unit tests in the backend (old legacy code style; tests are manual).
- `-B` (batch mode) suppresses interactive prompts and ANSI colour: needed inside Docker.

---

## Tomcat configuration notes

- No custom `context.xml` or `server.xml` changes needed.
- Context path is derived from the WAR filename: `banking-service.war` → `/banking-service`.
- JAX-WS RI servlet is configured inside the WAR's own `web.xml` (self-contained deployment).
- Tomcat 7 auto-deploys WARs placed in `webapps/` on startup.
- No HTTPS or TLS — old style, plain HTTP on port 8080.

---

## Health check in Docker Compose (reference)

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/banking-service/ws/banking?wsdl"]
  interval: 30s
  timeout: 10s
  retries: 5
  start_period: 60s
```

The WAR takes ~30 seconds to deploy after Tomcat starts; `start_period: 60s` prevents Compose from marking the container unhealthy during startup.
