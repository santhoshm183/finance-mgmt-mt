# Finance Management API

An independently buildable Spring Boot 3 / Java 21 REST API backed by PostgreSQL. Database schema changes are versioned in Flyway migrations; Hibernate runs in `validate` mode and never creates or mutates production schema implicitly.

## Prerequisites

- JDK 21
- Maven 3.9+
- Docker Compose (for local PostgreSQL)

## Run locally

Set `SUPABASE_DB_PASSWORD` in your shell or secret manager; credentials are deliberately not stored in this repository. The connection uses the same `SUPABASE_DB_URL`, `SUPABASE_DB_USERNAME`, and `SUPABASE_DB_PASSWORD` variables as `expense-manager-mt`. The URL and username default to that project's Supabase connection. Finance tables and Flyway history are isolated in the `finance_mgmt` schema, leaving the shared `public` schema untouched. The database user must be allowed to create that schema. Optional settings are `SERVER_PORT` (default `8080`), `DB_NAME`, and `DB_PORT` (Compose defaults to `finance_mgmt` and `5432`).

PowerShell example (replace the placeholders with local values):

```powershell
$env:SUPABASE_DB_USERNAME = "<local database user>"
$env:SUPABASE_DB_PASSWORD = "<local database password>"
$env:SUPABASE_DB_URL = "jdbc:postgresql://localhost:5432/finance_mgmt"
docker compose up -d postgres
mvn spring-boot:run
```

On bash, export the same environment variables before running `docker compose up -d postgres` and `mvn spring-boot:run`. For the shared Supabase database, use the same `SUPABASE_DB_*` values as `expense-manager-mt` instead of the local URL above. The Compose service only runs PostgreSQL and persists its data in the named `finance-postgres-data` volume. Flyway applies `src/main/resources/db/migration` on application startup, creating the initial zero-valued cash-flow account and restoring it if it is missing from an already migrated database.

Run tests and produce the executable jar:

```shell
mvn test
mvn package
```

The project also includes a Dockerfile to build the packaged backend image. It expects `SUPABASE_DB_PASSWORD` at runtime and optionally accepts `SUPABASE_DB_URL` and `SUPABASE_DB_USERNAME`.

## REST API

All endpoints are rooted at `/api`; JSON dates use ISO-8601 local dates (`yyyy-MM-dd`). CORS permits the Vite origins `http://localhost:5173` and `http://127.0.0.1:5173`.

| Method | Path | Behavior |
|---|---|---|
| GET | `/api/customers?search=` | List customers; optional case-insensitive search covers name, mobile number, and address |
| POST | `/api/customers` | Create customer |
| GET | `/api/customers/{id}` | Get customer |
| PUT | `/api/customers/{id}` | Update name, mobile number, address, and optionally status |
| GET | `/api/loans?customerId=` | List loans, optionally for one customer |
| GET | `/api/loans/{id}` | Get loan |
| POST | `/api/loans` | Create loan |
| GET | `/api/cash-flow` | Read investment, financial balance, profit, and collection totals |
| POST | `/api/cash-flow/transfers` | Transfer capital between tracked balances |
| POST | `/api/collections` | Record a collected amount or profit transaction |
| GET | `/api/payments/pending` | List loans whose next installment date is today or later |

### Request shapes

```json
// Customer create/update
{"name":"Asha Rao","mobileNumber":"5550101","address":"12 Main Street","status":"ACTIVE"}

// Loan create
{
  "customerId": 1,
  "loanType": "INSTALLMENT_BASED",
  "principalAmount": 1000.00,
  "interestRate": 10,
  "repaymentFrequency": "WEEKLY",
  "weeklyDay": "MONDAY",
  "startDate": "2026-10-17",
  "installmentAmount": 100
}

// Collection
{"loanId":"Asha-Rao-17102026","collectionDate":"2026-10-24","amount":100,"transactionType":"COLLECTED"}

// Cash-flow transfer
{"fromAccount":"INVESTMENT","amount":500,"date":"2026-10-17"}
```

Customer `name` is required; `mobileNumber` and `address` are optional and may be omitted, null, or blank. `loanType` is `INSTALLMENT_BASED` or `MONTHLY_INTEREST`; `repaymentFrequency` is `DAILY`, `WEEKLY`, or `MONTHLY`; collection `transactionType` is `COLLECTED` or `PROFIT`; transfer `fromAccount` is `INVESTMENT`, `FINANCIAL_BALANCE`, `PROFIT`, or `COLLECTION`. Loan principal and installment amounts must be positive; interest rate must be nonnegative (zero-interest loans are supported). Transfer and collection amounts must be positive. The legacy transfer request key `source` is accepted as an alias for `fromAccount`; responses continue to report the account as `source`. Weekly loans require a valid uppercase or lowercase weekday name; other frequencies reject `weeklyDay`.

Customer names are stored and returned as UTF-8. Loan IDs use the customer name with Unicode letters/digits preserved and other characters separated by hyphens, followed by `-DDMMYYYY` from `startDate` (for example, `Asha-Rao-17102026`). If that ID already exists, a numeric suffix (`-2`, `-3`, ...) is added. Loan creation locks the singleton cash-flow row before checking/allocating IDs and debiting the principal, so concurrent creations cannot allocate the same ID or overspend the balance.

Installment count is `ceil(principalAmount / installmentAmount)`. Profit on loan creation is `principalAmount * interestRate / 100`, rounded to two decimal places using half-up rounding. First due date is one day, the next chosen weekday strictly after start date, or the next calendar month respectively. Monthly dates retain the start date's day-of-month, clamped to the last day when necessary (e.g. January 31 → February 28 → March 31). Collected transactions move a due date forward one frequency; a `PROFIT` collection entry does not advance it. Pending payments intentionally follow the supplied inclusive condition `today <= nextInstallmentDate`.

Transfers from `INVESTMENT` increase both investment and financial balance. Transfers from `PROFIT` or `COLLECTION` debit that source total and credit financial balance. `FINANCIAL_BALANCE` verifies funds are available but is a same-account transfer, so its net balance is unchanged. Each transfer/collection and all corresponding balance/schedule updates run in one database transaction. Insufficient funds return HTTP 409.

Errors use a consistent JSON object with `timestamp`, `status`, `error`, `message`, and `path`. Validation and malformed enum/body requests return 400; missing resources return 404; balance/uniqueness conflicts return 409.
