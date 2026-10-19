# Finance Management API

An independently buildable Spring Boot 3 / Java 21 REST API backed by PostgreSQL. The database schema is provisioned explicitly from a SQL script; Hibernate runs in `validate` mode, and Flyway is disabled for application startup.

## Prerequisites

- JDK 21
- Maven 3.9+
- PostgreSQL 16+ (pgAdmin or `psql` to run the schema script)
- Docker Compose is optional

## Manual PostgreSQL schema setup (no automatic migrations)

The production application does not create or migrate database tables automatically. Create an empty PostgreSQL database named `finance_mgmt`, then run [`database/create_finance_mgmt_schema.sql`](./database/create_finance_mgmt_schema.sql) against that database using pgAdmin's Query Tool or `psql`:

```powershell
createdb -U postgres finance_mgmt
psql -U postgres -d finance_mgmt -f .\database\create_finance_mgmt_schema.sql
```

The script creates the `finance_mgmt` schema, tables, indexes, constraints, and initial cash-flow row. It is intended to initialize a new database and can be safely rerun for this schema. For an existing database, run [`database/upgrade_monthly_interest_loans.sql`](./database/upgrade_monthly_interest_loans.sql) once to allow a zero installment count for monthly-interest loans.

`spring.jpa.hibernate.ddl-auto=validate` remains enabled. It checks that the expected tables and columns exist when the application starts; it does not create or modify them. Flyway is disabled for normal application startup. Tests use their separate test configuration.

## Windows client bundle

Place `run-finance.cmd`, `run-finance.ps1`, `stop-finance.cmd`, `stop-finance.ps1`, `finance-mgmt-0.0.1-SNAPSHOT.jar`, and the `database` folder containing `create_finance_mgmt_schema.sql` together in one folder. The client starts PostgreSQL and double-clicks `run-finance.cmd`. The launcher prompts for the PostgreSQL username and securely prompts for its password, creates the `finance_mgmt` database if needed, and applies the schema script on first run. It checks for the application's tables on subsequent runs and skips the setup script when they already exist, then starts the application. To stop it, double-click `stop-finance.cmd`; it stops only a Java process running this bundle's exact JAR. Stop it when no one is in the middle of a transaction; the database itself remains running and the data is preserved. The PostgreSQL user must be able to create databases and schemas on first run. Java and PostgreSQL command-line tools (`psql` and `createdb`) must be available; PostgreSQL's `bin` folder can be added to PATH if the launcher cannot find them. Keep the `run-finance.cmd` window open and visit `http://localhost:8080`.

## Run locally

By default, the application connects to the local `finance_mgmt` database at `localhost:5432` as PostgreSQL user `postgres`. Set `SUPABASE_DB_PASSWORD` to that local user's password before starting the application. You may override `SUPABASE_DB_URL` and `SUPABASE_DB_USERNAME` if your local PostgreSQL uses a different address, database, or user. The database user needs permission to read/write the schema. Optional setting: `SERVER_PORT` (default `8080`).

PowerShell example (replace the placeholders with local values):

```powershell
$env:SUPABASE_DB_USERNAME = "<local database user>"
$env:SUPABASE_DB_PASSWORD = "<local database password>"
$env:SUPABASE_DB_URL = "jdbc:postgresql://localhost:5432/finance_mgmt"
mvn spring-boot:run
```

On bash, export the same environment variables before running `mvn spring-boot:run`.

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
| GET | `/api/cash-flow/transfers` | List cash transfer history |
| POST | `/api/cash-flow/transfers` | Transfer capital between tracked balances |
| GET | `/api/collections/history` | List financial transaction history, including linked customer and loan names |
| POST | `/api/collections` | Record a collected amount or profit transaction |
| GET | `/api/payments/pending` | List each unpaid due date from a loan's next installment through today |
| GET | `/api/backup` | Download a dated SQL snapshot |
| POST | `/api/backup/import` | Replace app data from an application-generated SQL backup |

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

Customer `name` is required and must be unique ignoring case and surrounding whitespace; `mobileNumber` and `address` are optional and may be omitted, null, or blank. `loanType` is `INSTALLMENT_BASED` or `MONTHLY_INTEREST`; `repaymentFrequency` is `DAILY`, `WEEKLY`, or `MONTHLY`; collection `transactionType` is `COLLECTED` or `PROFIT`; transfer `fromAccount` is `INVESTMENT`, `FINANCIAL_BALANCE`, `PROFIT`, or `COLLECTION`. Loan principal and installment amounts must be positive; interest rate must be nonnegative (zero-interest loans are supported). Transfer and collection amounts must be positive. The legacy transfer request key `source` is accepted as an alias for `fromAccount`; responses continue to report the account as `source`. Weekly loans require a valid uppercase or lowercase weekday name; other frequencies reject `weeklyDay`.

`MONTHLY_INTEREST` loans require `MONTHLY` repayment frequency. The finance UI restricts that loan type to monthly; the API enforces the same rule for direct requests.

Collections for `INSTALLMENT_BASED` loans must use `COLLECTED`. `MONTHLY_INTEREST` loans accept either `COLLECTED` or `PROFIT`: collected payments advance the next due date, while profit entries do not. The UI defaults monthly-interest collections to `PROFIT`.

The `GET /api/backup` endpoint downloads an SQL snapshot. `POST /api/backup/import` accepts a multipart SQL backup created by this application and transactionally replaces application data. Imported identity sequences are reset after the restored IDs, so new customer, transaction, and transfer records continue without ID collisions. Import reads the structured backup payload and does not execute arbitrary uploaded SQL.

Monthly-interest loans store the monthly interest (`principalAmount * interestRate / 100`, rounded to two decimal places) in `installment_amount` and use `installment_count = 0`.

Customer names are stored and returned as UTF-8. Loan IDs use the customer name with Unicode letters/digits preserved and other characters separated by hyphens, followed by `-DDMMYYYY` from `startDate` (for example, `Asha-Rao-17102026`). If that ID already exists, a numeric suffix (`-2`, `-3`, ...) is added. Loan creation locks the singleton cash-flow row before checking/allocating IDs and debiting the principal, so concurrent creations cannot allocate the same ID or overspend the balance.

Installment count is `ceil(principalAmount / installmentAmount)`. Profit on loan creation is `principalAmount * interestRate / 100`, rounded to two decimal places using half-up rounding. First due date is one day, the next chosen weekday strictly after start date, or the next calendar month respectively. Monthly dates retain the start date's day-of-month, clamped to the last day when necessary (e.g. January 31 → February 28 → March 31). Collected transactions move the stored next due date forward one frequency; a `PROFIT` collection entry does not advance it. Pending payments expand each loan's stored next due date into every scheduled unpaid due date through today (inclusive), so overdue daily, weekly, and monthly installments each appear as separate rows.

Transfers from `INVESTMENT` increase both investment and financial balance. Transfers from `PROFIT` or `COLLECTION` debit that source total and credit financial balance. `FINANCIAL_BALANCE` verifies funds are available but is a same-account transfer, so its net balance is unchanged. Each transfer/collection and all corresponding balance/schedule updates run in one database transaction. Insufficient funds return HTTP 409.

Errors use a consistent JSON object with `timestamp`, `status`, `error`, `message`, and `path`. Validation and malformed enum/body requests return 400; missing resources return 404; balance/uniqueness conflicts return 409.
