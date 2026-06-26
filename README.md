# P2P Lending Platform API

REST API for a peer-to-peer lending platform: investors fund loans requested by borrowers,
with a transactional wallet ledger, escrow-held funds, and pro-rata repayment distribution.
Built with Java 21 & Spring Boot.

## Tech stack
- Java 21, Spring Boot 3.5.15
- PostgreSQL 16
- Flyway (migrations), Testcontainers (integration tests)

## Prerequisites
- **Java 21** must be your active JDK (check with `java -version`, it should print `21.x`)
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

# 4. Start the API (Flyway creates the schema on boot)
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
Tests use Testcontainers: a throwaway PostgreSQL starts automatically, no manual setup.
They cover the full loan lifecycle (create, fund by multiple investors, repay, fully repaid),
funding concurrency (no over-funding under parallel requests), repayment atomicity (a failure
rolls back every wallet update), the financial math, and the HTTP status codes.

> **Colima users only**: Testcontainers needs `DOCKER_HOST` to find the daemon
> (not needed on Docker Desktop):
> ```bash
> export DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock"
> export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
> ```

## API endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/users` | Create a user (also creates their wallet) |
| GET  | `/api/users/{id}/wallet` | Get the wallet balance |
| POST | `/api/users/{id}/wallet/deposit` | Deposit simulated funds |
| POST | `/api/users/{id}/wallet/withdraw` | Withdraw simulated funds |
| POST | `/api/loans` | Request a loan (starts `PENDING`, 14-day funding deadline) |
| GET  | `/api/loans/{id}` | Get a loan's details |
| POST | `/api/loans/{id}/fund` | Fund a `PENDING` loan; auto-disburses when fully funded |
| POST | `/api/loans/{id}/repay` | Make a monthly repayment (distributes to investors) |
| POST | `/api/loans/{id}/cancel` | Cancel a `PENDING` loan (refunds investors from escrow) |

Example request to create a loan:
```bash
curl -X POST http://localhost:8080/api/loans \
  -H 'Content-Type: application/json' \
  -d '{"borrowerId":1,"principal":1000.00,"annualInterestRate":0.12,"termMonths":12}'
```

## Design decisions

- **Money & rounding**: all amounts use `BigDecimal`, stored as `NUMERIC(19,2)`, rounded with
  `HALF_EVEN` (banker's rounding, IEEE 754). Never `double`.
- **Interest rate**: the provided rate is a nominal annual rate; the monthly rate is `annual / 12`.
- **Ledger & escrow**: every money movement is an append-only `ledger_entries` row. Internal
  movements are balanced two-leg transfers, so money is never created or destroyed and balances
  are reconcilable from the ledger. Committed funds are held in a **segregated escrow account**
  (separate from the platform account) until the loan is disbursed or refunded, aligned with
  client-money segregation principles (UK FCA **CASS**).
- **Multiple fundings**: an investor can fund the same loan several times; each funding is a
  separate record (fractional-notes model, as in core-banking engines like Mambu), aggregated per
  investor at distribution so each receives a single payout.
- **Conflict of interest**: a borrower cannot fund their own loan (rejected with `409`), in line
  with P2P/crowdfunding conflict-of-interest rules (EU ECSPR, UK FCA).
- **Concurrency**: a pessimistic row lock on the loan during funding, plus a DB
  `CHECK (funded_amount <= principal)`, prevents over-funding under parallel requests. Within a
  transaction, all wallet locks are acquired in ascending id order to avoid deadlocks. Optimistic
  locking was considered but adds retry complexity for no real gain at this scale.
- **Repayment**: a single `@Transactional` method, all-or-nothing. Payment = principal + interest;
  the platform takes a **1% fee on the interest portion**; the remainder is distributed pro-rata to
  investors with a **largest-remainder** rule so the parts always sum exactly to the distributable
  amount.
- **Funding window**: loans have a **14-day** funding deadline (configurable via
  `p2p.funding.window-days`); funding after the deadline is rejected. An hourly scheduled job
  cancels expired `PENDING` loans and refunds their investors from escrow; a borrower can also
  cancel a `PENDING` loan via `POST /api/loans/{id}/cancel` (never after disbursement).

## Database schema

Normalized, ledger-style schema (DDL: `src/main/resources/db/migration/V1__init.sql`):

- **users**: `system_role` discriminates the platform and escrow system accounts.
- **wallets**: one per user; `balance` with `CHECK (balance >= 0)`.
- **loans**: borrower, principal, annual rate, term, status, funded amount, funding deadline,
  monthly payment, remaining principal.
- **loan_investments**: many-to-many between loans and investors; one row per funding event.
- **ledger_entries**: append-only journal: wallet, type, signed amount, `balance_after`,
  `correlation_id` (groups the rows of one operation).

## Error responses

Errors return a consistent JSON body: `{ "timestamp", "status", "error", "message" }`.

| Situation | HTTP |
|---|---|
| Unknown user / loan / wallet | 404 |
| Insufficient wallet funds | 409 |
| Funding would exceed the principal | 409 |
| Action on a loan in the wrong status | 409 |
| Borrower funding their own loan | 409 |
| Duplicate email | 409 |
| Invalid request body | 400 |

## References

Design choices are grounded in: UK FCA **CASS** (client-money segregation, escrow); **IEEE 754**
banker's rounding and **double-entry bookkeeping** (precision and ledger integrity); and market
practice: **Prosper** (14-day funding window), **LendingClub** (cancel before disbursement only),
**Mambu** (per-funding records).
