# Plan 08 — Docker Compose Orchestration

## Output file
```
docker-compose.yml
```

---

## Services overview

| Service | Image / Build | Port | Depends on |
|---|---|---|---|
| `postgres` | `postgres:9.3` | `5432:5432` | — |
| `banking-service` | build `./banking-service` | `8080:8080` | `postgres` (healthy) |

---

## Full `docker-compose.yml`

```yaml
version: "2.1"

services:

  # ------------------------------------------------------------------
  # PostgreSQL 9.3 - legacy database
  # ------------------------------------------------------------------
  postgres:
    image: postgres:9.3
    container_name: legacy-banking-db
    environment:
      POSTGRES_DB:       bankdb
      POSTGRES_USER:     bankuser
      POSTGRES_PASSWORD: bankpassword
    ports:
      - "5432:5432"
    volumes:
      - ./db/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U bankuser -d bankdb"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ------------------------------------------------------------------
  # Banking Service - Spring 3.2 WAR on Tomcat 7
  # ------------------------------------------------------------------
  banking-service:
    build:
      context: ./banking-service
      dockerfile: Dockerfile
    container_name: legacy-banking-service
    environment:
      DB_HOST:     postgres
      DB_USER:     bankuser
      DB_PASSWORD: bankpassword
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/banking-service/ws/banking?wsdl || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 90s

volumes:
  postgres_data:
```

---

## Version choice: `version: "2.1"`

Compose file format `2.1` is used because:
- `depends_on` with `condition: service_healthy` requires **at least v2.1** (the feature was removed from v3.x and only re-added in v3.9+ — `2.1` is safest for cross-platform compat).
- This is also authentically "old" — `2.1` was the standard format in circa 2016–2018.

---

## PostgreSQL 9.3 Docker Hub note

`postgres:9.3` is available as a legacy tag on Docker Hub.
It is no longer maintained by the official Postgres team but the image still exists and is suitable for legacy/demo use.

The `docker-entrypoint-initdb.d` mechanism works identically across all Postgres versions:
any `.sql` file mounted there is executed on first container start (when the data directory is empty).

---

## Environment variable flow

```
docker-compose.yml
  └── banking-service.environment:
        DB_HOST=postgres  ─────────────────────────────────┐
        DB_USER=bankuser                                    │
        DB_PASSWORD=bankpassword                            │
                                                            ▼
applicationContext.xml                            Tomcat JVM environment
  <property name="jdbcUrl"                        Spring 3.2 StandardEnvironment
    value="jdbc:postgresql://${DB_HOST:localhost}:5432/bankdb"/>
                                                            │
DatabaseManager.java                                        ▼
  DriverManager.getConnection(jdbcUrl, username, password) ─→ postgres:5432/bankdb
```

---

## Build notes

### First-time build
```bash
docker-compose up --build
```
This will:
1. Pull `postgres:9.3` from Docker Hub
2. Pull `maven:3.6.3-jdk-8` and build the WAR (downloads all Maven deps — may take 3–5 minutes)
3. Pull `tomcat:7.0.109-jdk8` as the runtime layer
4. Start Postgres, wait for health check
5. Start `banking-service` once Postgres is healthy

### Subsequent starts (no source changes)
```bash
docker-compose up
```
Docker cache means the WAR is not rebuilt.

### Full rebuild after source change
```bash
docker-compose up --build banking-service
```

---

## Port exposure

| Service | Host port | Container port | Usage |
|---|---|---|---|
| postgres | 5432 | 5432 | Direct DB access for debugging (e.g., `psql -h localhost -U bankuser -d bankdb`) |
| banking-service | 8080 | 8080 | WSDL at `http://localhost:8080/banking-service/ws/banking?wsdl` |

---

## Troubleshooting reference

### Check Postgres is running and seeded
```bash
docker exec -it legacy-banking-db psql -U bankuser -d bankdb -c "SELECT * FROM accounts;"
```
Expected: 2 rows — Hans Mueller and Maria Schmidt.

### Check WAR deployed successfully
```bash
docker logs legacy-banking-service | grep "BankingService"
```
Should see JAX-WS endpoint registration log line.

### Verify WSDL endpoint
```bash
curl http://localhost:8080/banking-service/ws/banking?wsdl
```
Should return XML beginning with `<definitions ...>`.

### Common failure: service starts before DB is ready
`depends_on: condition: service_healthy` in Compose 2.1 handles this.
If using a Compose v3 file (e.g., for Swarm), you must handle DB waiting inside the application or a startup script.

---

## Data persistence

`postgres_data` named volume persists the database between `docker-compose down` runs.
To reset to the seed data:
```bash
docker-compose down -v   # removes the named volume
docker-compose up        # re-runs init.sql on fresh volume
```
