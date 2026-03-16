# Legacy Banking Suite

**Originally released: 2006 (Enterprise Edition 1.0)**

Legacy Banking Suite delivers secure account operations for branch and back-office workflows using the proven enterprise standards of its time: **SOAP Web Services**, **Java EE web deployment**, and a robust **PostgreSQL relational core**.

Designed for reliability in regulated environments, the platform separates concerns cleanly:
- **banking-service**: central transaction processing and account ledger logic
- **banking-client**: rich desktop front-end for teller and internal operator usage

For a 2006-era architecture, this system represents a high-quality service-oriented design: typed contracts, strict database operations, and predictable operational behavior.

---

## Key Capabilities

- Account login by account number
- Balance lookup
- Funds transfer between accounts
- Transaction history review
- Seeded demo environment with 2 example accounts and transaction history

---

## Architecture

- **Desktop Client**: Java Swing (Java 8 runtime compatible)
- **Service Layer**: Spring 3.x + JAX-WS SOAP endpoint on Tomcat 7
- **Database**: PostgreSQL 9.3 with schema/data bootstrap script
- **Operations**: Docker Compose orchestration for backend stack

Service endpoint:
- SOAP endpoint: `http://localhost:8080/banking-service/banking`
- WSDL: `http://localhost:8080/banking-service/banking?wsdl`

---

## Project Layout

- `banking-service/` - SOAP backend (WAR)
- `banking-client/` - Swing desktop client (JAR)
- `db/init.sql` - schema + seed data
- `docker-compose.yml` - backend runtime stack

---

## Quick Start

### 1. Start backend (database + service)

From repository root:

```bash
docker compose up -d --build
```

Check status:

```bash
docker compose ps
```

Check backend logs:

```bash
docker compose logs banking-service --tail=200
```

Verify WSDL is available:

```bash
curl http://localhost:8080/banking-service/banking?wsdl
```

### 2. Build and run desktop client

```bash
cd banking-client
mvn clean package -DskipTests
```

Run client (Windows, no terminal window):

```bash
javaw -jar target/banking-client-1.0.jar
```

Alternative (with console):

```bash
java -jar target/banking-client-1.0.jar
```

---

## Demo Accounts

- `DE89370400440532013000` (Hans Mueller)
- `DE91100000000123456789` (Maria Schmidt)

Both accounts are preloaded from `db/init.sql` with limited historical transactions.

---

## Build Commands

Backend build:

```bash
cd banking-service
mvn clean package
```

Client build:

```bash
cd banking-client
mvn clean package -DskipTests
```

---

## Operational Notes

- Keep the client WSDL contract aligned with backend service contract when SOAP operations change.
- The backend intentionally uses direct SQL (`PreparedStatement`) for deterministic query behavior.
- This repository preserves historical enterprise patterns and compatibility decisions by design.

---

## Historical Context

In 2006, enterprise teams prioritized:
- standards-based service contracts (WSDL/SOAP)
- strong deployment isolation (application server + relational DB)
- durable transaction records and explicit SQL control

Legacy Banking Suite reflects that era's best practices and engineering mindset while remaining runnable in a modern developer environment.
