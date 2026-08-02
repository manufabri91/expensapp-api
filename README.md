# expensapp-api

[![CI](https://github.com/manufabri91/expensapp-api/actions/workflows/ci-build-test.yml/badge.svg)](https://github.com/manufabri91/expensapp-api/actions/workflows/ci-build-test.yml)
[![codecov](https://codecov.io/gh/manufabri91/expensapp-api/branch/develop/graph/badge.svg)](https://codecov.io/gh/manufabri91/expensapp-api)

REST API for **Expenses App** — a personal expense/income tracker with multi-account, multi-currency support,
category/subcategory budgeting, transfers between accounts, and recurring (scheduled) transactions.

Built with Spring Boot 3 / Java 17, backed by PostgreSQL, authenticated via Firebase.

## Table of Contents

- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Database Migrations](#database-migrations)
- [API Overview](#api-overview)
- [Authentication](#authentication)
- [Testing & Coverage](#testing--coverage)
- [Contributing](#contributing)
- [CI/CD](#cicd)

## Tech Stack

| Concern              | Choice                                                                     |
| -------------------- | --------------------------------------------------------------------------- |
| Language / runtime   | Java 17                                                                    |
| Framework            | Spring Boot 3.1.12 (Web, Data JPA, Security, Validation, Actuator)         |
| Database             | PostgreSQL, versioned with Liquibase                                       |
| Auth                 | Firebase Authentication (ID token verification via Firebase Admin SDK)    |
| DTO ↔ Entity mapping | ModelMapper (not MapStruct — see [AGENTS.md](AGENTS.md))                   |
| External config      | Spring Cloud Config Client (`bootstrap.yml` points at a remote config server) |
| Build                | Maven (wrapper included — `./mvnw`)                                        |
| Containerization     | Docker / Docker Compose                                                   |
| Coverage             | JaCoCo (report generation) + Codecov (patch/project coverage checks)      |
| CI                   | GitHub Actions                                                             |
| Deployment           | Railway, via Docker Hub images                                            |

## Project Structure

```
src/main/java/com/manuelfabri/expenses/
├── config/            # Spring beans: Firebase Admin SDK init, Security filter chain + CORS
├── constants/         # Shared constants (Urls.java — REST path prefixes)
├── controller/        # REST controllers, one per resource
├── dto/               # Request/response DTOs (Lombok @Data — DTOs only, never entities)
├── exception/         # Custom exceptions + GlobalExceptionHandler (@RestControllerAdvice)
├── filter/            # FirebaseAuthorizationFilter — validates the Authorization header
├── model/             # JPA entities (BaseEntity supplies owner/audit/soft-delete for all of them)
├── repository/        # Spring Data JPA repositories (BaseEntityRepository<T> for the shared
│                       #   active/soft-delete query family, pre-scoped to the current user)
└── service/           # Business logic (interface + implementation/ subpackage)
```

Each resource (accounts, transactions, categories/subcategories, recurring transactions, summaries) follows the
same controller → service → repository → entity layering. See [AGENTS.md](AGENTS.md) for the conventions behind
each layer (naming, validation, transactions, testing, etc.) — that file is the source of truth for *how* to write
code here; this README is about getting the project running.

## Getting Started

### Prerequisites

- **Docker & Docker Compose** — the fastest way to run the full stack (API + Postgres) locally.
- **[OPTIONAL]** Java 17 and a local PostgreSQL instance, if you'd rather run the app directly instead of via Docker.
- **[OPTIONAL]** Node.js — only needed to install the pre-push coverage git hook (see [Testing & Coverage](#testing--coverage)); the API itself has no Node dependency.

### 1. Clone the repository

```bash
git clone https://github.com/manufabri91/expensapp-api.git
cd expensapp-api
```

`develop` is the default/integration branch — branch your feature work from there. `main` only receives merges
from `develop` and represents production; pushing to it triggers a real deploy (see [CI/CD](#cicd)).

### 2. Configure environment variables

```bash
cp .env.example .env
```

Fill in the values described in [Environment Variables](#environment-variables) below. For local development, the
`DECRYPT_KEY` value can be any non-empty string — it's only meaningful against the real config server used in
dev/prod.

### 3. Run with Docker Compose

The `local` profile starts both Postgres and the API against it:

```bash
docker compose --profile local build
docker compose --profile local up -d
```

The API is exposed on `http://localhost:8080`, Postgres on `localhost:5433`. Stop everything with:

```bash
docker compose --profile local down
```

`dev` and `prod` profiles also exist in `docker-compose.yml`, but those expect a real config server / database and
are meant for the deployed environments, not local development.

### 4. (Alternative) Run directly with Maven

If you already have Postgres running locally (matching the connection details in
`src/main/resources/application.yml` — `localhost:5433/expensapp`, user `postgres`, password `admin`, or override
via your own `application.yml`/env):

```bash
./mvnw spring-boot:run
```

### 5. Install the pre-push git hook (one-time)

```bash
npm install
```

This wires up a Husky `pre-push` hook that runs the test suite and checks that any lines you're about to push are
adequately covered — see [Testing & Coverage](#testing--coverage). It's the only reason this Java project has a
`package.json`.

## Environment Variables

| Variable                        | Description                                                                                          |
| -------------------------------- | ------------------------------------------------------------------------------------------------------ |
| `FIREBASE_API_KEY`               | Firebase Web API key, used for the password-based login/refresh REST calls to Firebase's identity toolkit. |
| `GOOGLE_APPLICATION_CREDENTIALS` | Firebase service account JSON (as a raw JSON string, not a file path) — used to initialize the Firebase Admin SDK for verifying ID tokens. |
| `DECRYPT_KEY`                    | Decryption key for values encrypted in the remote Spring Cloud Config server. Any non-empty string works locally; a dummy value is fine when running with the `local` profile. |

## Database Migrations

Schema changes are managed with **Liquibase**, applied automatically on startup
(`spring.liquibase.change-log: classpath:db/changelog/db.changelog-master.yaml`).

- Changelogs live in `src/main/resources/db/changelog/`, numbered sequentially (`0-init-tables.yaml`,
  `1-insert-basic-role.yaml`, ..., `8-add-recurrent-transactions.yaml`) and registered, in order, in
  `db.changelog-master.yaml`.
- To add a schema change: create a new `db/changelog/<n>-description.yaml` file (next number in sequence), add it
  to `db.changelog-master.yaml`, and match the `createTable`/`addForeignKeyConstraint`/`createIndex` style already
  used in `0-init-tables.yaml`.
- **Any enum stored on an entity must use `@Enumerated(EnumType.STRING)`** with a `VARCHAR` column from its very
  first migration — see `7-fix-enum-storage-to-string.yaml` for the retrofit this codebase had to do the one time
  that wasn't followed.

## API Overview

All endpoints except `/auth/**` require a Firebase-issued bearer token (see [Authentication](#authentication)).

| Prefix                 | Resource                                                          |
| ------------------------ | -------------------------------------------------------------------- |
| `/auth`                 | Login, registration, token refresh                                |
| `/account`              | Bank/cash accounts (balance, currency)                            |
| `/category`             | Income/expense categories                                          |
| `/subcategory`          | Subcategories, nested under a category                            |
| `/transaction`          | One-off income/expense/transfer transactions                      |
| `/recurrent-transaction`| Recurring transaction definitions (interval- or monthly-day-based schedule) that generate real transactions automatically |
| `/summary`              | Aggregated balance/category/monthly-history summaries for the dashboard |

## Authentication

The frontend authenticates against Firebase directly and forwards the resulting ID token as a bearer token on
every request. `FirebaseAuthorizationFilter` (a servlet filter registered before Spring Security's own
authentication filter):

1. Rejects requests with no `Authorization` header (401), except under `/auth/**`.
2. Verifies the token via the Firebase Admin SDK (`FirebaseService.parseToken`); an invalid/expired token also
   yields 401.
3. Looks up the local `User` record for the token's UID and sets it as the Spring Security principal — controllers
   and services access it via `SecurityContextHolder`, never by re-parsing the token themselves.

`SecurityConfig` applies `anyRequest().authenticated()` to everything except `/auth/**`, and configures CORS via
the `app.cors.allowed-origins` property (defaults to `http://localhost:3000` if unset).

## Testing & Coverage

- Unit tests use JUnit 5, AssertJ, and Mockito — no Spring context unless the thing under test genuinely needs
  one (`@DataJpaTest` for repository queries, `@WebMvcTest` for controller slices). See
  [AGENTS.md](AGENTS.md#testing) for the full testing conventions and current gaps (there's no
  integration/end-to-end test suite yet — a known, tracked gap, not a blocker for individual PRs).
- Run the full suite:

  ```bash
  ./mvnw test
  ```

- **Coverage is enforced via [Codecov](https://codecov.io), not a hand-maintained per-class list.** Two checks run
  on every PR (configured in `codecov.yml`):
  - **`patch`** — the lines you added/changed must be ≥80% covered.
  - **`project`** — total coverage must not regress from the base branch.

  This ratchets coverage upward over time as new code lands, without anyone maintaining a per-file allowlist.
  `pom.xml`'s `jacoco-maven-plugin` only produces the report (`target/site/jacoco/jacoco.xml`) that Codecov reads —
  it no longer fails the build itself.
- **A pre-push git hook mirrors the `patch` check locally**, so a coverage regression is caught before you even
  open a PR: `.husky/pre-push` runs `scripts/check-diff-coverage.mjs`, which runs `mvn test`, diffs
  `src/main/java` against `origin/develop`, and fails the push if the lines you added fall under 80% covered.
  Requires a one-time `npm install` after cloning (see [Getting Started](#getting-started)).

## Contributing

1. Branch off `develop` (the default branch) — `feature/…`, `fix/…`, or similar.
2. Follow the conventions in [AGENTS.md](AGENTS.md) (naming, Lombok usage, DTO mapping, transactions, testing,
   coverage). It's kept up to date as the actual source of truth for this codebase's style, not a generic
   template.
3. Open a PR against `develop`. CI runs the full test suite and Codecov reports patch/project coverage on the PR.
4. `develop` merges to `main` for production releases (see [CI/CD](#cicd) below).

## CI/CD

Three GitHub Actions workflows:

| Workflow                    | Trigger                        | What it does                                                                 |
| ---------------------------- | --------------------------------- | --------------------------------------------------------------------------------- |
| `ci-build-test.yml`         | PR → `main` or `develop`         | `mvn verify` (build + test), uploads the JaCoCo report as an artifact and to Codecov |
| `cd-merges-develop.yml`     | Push → `develop`                 | Builds a `beta`-tagged Docker image, pushes to Docker Hub, redeploys the `dev` Railway environment |
| `cd-merges-main.yml`        | Push → `main`                    | Builds a versioned Docker image, pushes to Docker Hub, tags the release in git, redeploys the production Railway environment, bumps `pom.xml` to the next `-SNAPSHOT` version |

Both `cd-*` workflows deploy the same Railway service (`expensapp-api`) but to different Railway *environments*
(`dev` vs. production) — each needs its own environment-scoped `RAILWAY_TOKEN_DEV`/`RAILWAY_TOKEN` secret, since
Railway project tokens are scoped to a single environment.
