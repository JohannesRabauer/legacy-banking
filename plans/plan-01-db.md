# Plan 01 — Database Layer

## Output file
`db/init.sql`

## Purpose
Single SQL script that PostgreSQL 9.3 executes on first container start.
It creates all tables and inserts the two demo accounts plus six seed transactions.

---

## Schema

### Table `accounts`

```sql
CREATE TABLE accounts (
    account_number VARCHAR(34)   PRIMARY KEY,
    owner_name     VARCHAR(100)  NOT NULL,
    balance        NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Table `transactions`

```sql
CREATE TABLE transactions (
    id               SERIAL         PRIMARY KEY,
    from_account     VARCHAR(34),               -- NULL means "external deposit"
    to_account       VARCHAR(34),               -- NULL means "external withdrawal"
    amount           NUMERIC(15,2)  NOT NULL,
    description      VARCHAR(255),
    transaction_date TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

No foreign key constraints on purpose — old code never relied on referential integrity here.

---

## Seed data

### Accounts

| account_number               | owner_name      | balance   | created_at          |
|------------------------------|-----------------|-----------|---------------------|
| DE89370400440532013000       | Hans Mueller    | 1250.00   | 2003-06-15 09:12:00 |
| DE91100000000123456789       | Maria Schmidt   | 3420.50   | 2004-11-02 14:33:00 |

### Transactions (6 rows)

| id | from_account           | to_account             | amount  | description                    | transaction_date        |
|----|------------------------|------------------------|---------|--------------------------------|-------------------------|
| 1  | NULL                   | DE89370400440532013000 | 2000.00 | Initial deposit                | 2003-06-15 09:15:00     |
| 2  | DE89370400440532013000 | DE91100000000123456789 | 500.00  | Rent payment July 2003         | 2003-07-03 11:22:00     |
| 3  | DE89370400440532013000 | NULL                   | 250.00  | ATM withdrawal                 | 2003-08-20 17:44:00     |
| 4  | NULL                   | DE91100000000123456789 | 4000.00 | Salary August 2004             | 2004-08-31 23:59:00     |
| 5  | DE91100000000123456789 | DE89370400440532013000 | 79.50   | Borrowed money repayment       | 2004-09-14 10:03:00     |
| 6  | DE91100000000123456789 | NULL                   | 160.00  | Utility bills September 2004   | 2004-09-30 18:05:00     |

Note: balances already reflect these transactions (no triggers needed — balances are maintained in application code).

---

## Implementation notes

- Use `\connect bankdb;` at the top because `docker-entrypoint-initdb.d` runs as `postgres` superuser but the DB is named `bankdb` via `POSTGRES_DB` env-var.
- Wrap everything in a single `BEGIN`/`COMMIT` transaction so the init either fully succeeds or fails cleanly.
- Use `INSERT INTO ... VALUES (...)` with explicit literals — no `COPY FROM stdin` (easier to read, maximum portability across PG versions).
- Force serial sequence to start after the last seeded id with `SELECT setval('transactions_id_seq', 6);` after the inserts.
- Add a comment header block describing the purpose, DB name, and encoding (UTF8).

---

## Full file skeleton

```
-- =============================================================
-- Legacy Banking System — Database Initialisation Script
-- PostgreSQL 9.3   |   Database: bankdb   |   Encoding: UTF8
-- =============================================================

BEGIN;

-- ---- Schema -----------------------------------------------

CREATE TABLE accounts ( ... );

CREATE TABLE transactions ( ... );

-- ---- Accounts ---------------------------------------------

INSERT INTO accounts (account_number, owner_name, balance, created_at)
VALUES
    ('DE89370400440532013000', 'Hans Mueller',  1250.00, '2003-06-15 09:12:00'),
    ('DE91100000000123456789', 'Maria Schmidt', 3420.50, '2004-11-02 14:33:00');

-- ---- Transactions -----------------------------------------

INSERT INTO transactions (id, from_account, to_account, amount, description, transaction_date)
VALUES
    (1, NULL,                     'DE89370400440532013000',  2000.00, 'Initial deposit',                '2003-06-15 09:15:00'),
    (2, 'DE89370400440532013000', 'DE91100000000123456789',   500.00, 'Rent payment July 2003',         '2003-07-03 11:22:00'),
    (3, 'DE89370400440532013000', NULL,                       250.00, 'ATM withdrawal',                 '2003-08-20 17:44:00'),
    (4, NULL,                     'DE91100000000123456789',  4000.00, 'Salary August 2004',             '2004-08-31 23:59:00'),
    (5, 'DE91100000000123456789', 'DE89370400440532013000',    79.50, 'Borrowed money repayment',       '2004-09-14 10:03:00'),
    (6, 'DE91100000000123456789', NULL,                       160.00, 'Utility bills September 2004',   '2004-09-30 18:05:00');

SELECT setval('transactions_id_seq', 6);

COMMIT;
```
