# QuickSeat Backend

QuickSeat is a multi-cinema ticket-booking REST API. It is a portfolio backend that demonstrates secure authentication, cinema operations, concurrency-safe seat holds, booking/payment lifecycles, QR tickets, staff validation, and dashboard analytics.

## Tech stack

- Java 21, Spring Boot, Spring Web MVC, Spring Data JPA, Spring Security
- PostgreSQL (Neon supported), Flyway, Hibernate validation
- JWT with refresh tokens, Gmail SMTP, Google OAuth foundation
- Cloudinary image upload, ZXing QR generation, Apache PDFBox ticket PDFs
- Springdoc OpenAPI/Swagger, JUnit and Spring Security test support

## Prerequisites

- JDK 21
- Maven Wrapper included in this repository (`mvnw.cmd` on Windows)
- A PostgreSQL database. Neon PostgreSQL is supported.
- Optional accounts/configuration: Gmail app password, Google OAuth, and Cloudinary.

## Setup and run

1. Clone the repository and enter it.

   ```powershell
   git clone <repository-url>
   cd quickseat
   ```

2. Create a local environment file from the template. `.env` is ignored by Git.

   ```powershell
   Copy-Item .env.example .env
   ```

3. Fill in the required variables in `.env`. The minimum runtime configuration is:

   ```text
   DB_URL=jdbc:postgresql://<neon-host>/<database>?sslmode=require
   DB_USERNAME=<database-user>
   DB_PASSWORD=<database-password>
   JWT_SECRET=<random-secret-with-at-least-32-characters>
   ```

   `DB_URL` must be JDBC format. For Neon, use its PostgreSQL connection details with `sslmode=require`.

4. Make the variables available to your IDE/run configuration, then run `QuickSeatApplication`, or load `.env` in PowerShell and run:

   ```powershell
   Get-Content .env | ForEach-Object {
     if ($_ -match '^([^#=]+)=(.*)$') {
       [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
     }
   }
   .\mvnw.cmd spring-boot:run
   ```

The default profile is `dev`. The health endpoint is `http://localhost:8080/api/v1/health`. Swagger UI is available at `http://localhost:8080/api/v1/swagger-ui.html` in development.

## Environment variables

| Variable | Required | Purpose |
| --- | --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Yes | PostgreSQL/Neon connection |
| `JWT_SECRET` | Yes | JWT signing secret; use at least 32 random characters |
| `CORS_ALLOWED_ORIGINS` | No | Comma-separated frontend origins; defaults to `http://localhost:3000` |
| `MAIL_ENABLED`, `GMAIL_USERNAME`, `GMAIL_APP_PASSWORD` | Optional | Gmail verification/reset/booking emails |
| `FRONTEND_BASE_URL`, `VERIFICATION_BASE_URL` | Optional | Email and frontend links |
| `CLOUDINARY_ENABLED`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Optional | Movie/cinema image upload |
| `GOOGLE_OAUTH_ENABLED`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Optional | Google OAuth login foundation |
| `SEAT_HOLD_DURATION_MINUTES` | Optional | Seat hold duration; defaults to 5 minutes |
| `DEMO_SEED_ENABLED` | Optional | Set `true` only in the `dev` profile to create sample data |
| `DEMO_SEED_PASSWORD` | Optional | Password for demo accounts; default is documented below |
| `SWAGGER_ENABLED` | Optional | Enables Swagger/API docs in the `prod` profile; defaults to `false` |

Never commit `.env`, database passwords, JWT secrets, OAuth secrets, real tokens, or Gmail app passwords.
If a real `.env` was ever committed previously, rotate every exposed credential; ignoring or untracking the file does not remove secrets from Git history.

## Database and Flyway

Flyway owns the schema through migrations `V1` to `V7`. Hibernate uses `ddl-auto=validate`, so it validates mappings and does not create or modify tables. Use a dedicated development Neon database when testing seed data or destructive test scenarios.

Do not edit an already-applied migration. Add a new migration for future schema changes.

## Testing

Run all tests:

```powershell
.\mvnw.cmd test
```

Build the deployable application after tests:

```powershell
.\mvnw.cmd package
```

The full customer-to-staff verification checklist is in [docs/End_to_End_Checklist.md](docs/End_to_End_Checklist.md). Request examples and access requirements are in [API_Documentation.md](API_Documentation.md). A Git-friendly Bruno collection is in [bruno/QuickSeat API](bruno/QuickSeat%20API).

## Development demo data

Demo data is disabled by default and is only eligible to run under the `dev` profile. To enable it for a dedicated local/development database:

```text
DEMO_SEED_ENABLED=true
DEMO_SEED_PASSWORD=DemoPassword123!
```

It creates one demo cinema, screen, NORMAL/COUPLE seats, a future showtime with inventory, and these verified active LOCAL accounts:

| Role | Email |
| --- | --- |
| ADMIN | `demo.admin@quickseat.local` |
| STAFF | `demo.staff@quickseat.local` |
| CUSTOMER | `demo.customer@quickseat.local` |

The demo staff account is assigned to the demo cinema. The initializer is idempotent and production forcibly sets `app.demo-seed.enabled=false`. Never enable it against shared or production data.

## Frontend and deployment handoff

- CORS permits `http://localhost:3000` by default for a local Next.js application. Set `CORS_ALLOWED_ORIGINS` to the deployed frontend origins in production.
- Use `SPRING_PROFILES_ACTIVE=prod` for deployment. The production profile keeps Flyway and Hibernate validation enabled, disables demo seed data, and disables Swagger by default.
- If operational documentation is needed in a protected deployment environment, set `SWAGGER_ENABLED=true` deliberately; do not expose it publicly by default.
- The API base path is `/api/v1`. Customer/public discovery can happen before login; holding seats and all booking actions require the correct authenticated role.

## Important backend features

- Email verification, password recovery, local login, JWT refresh/logout, and role authorization
- Public movie/cinema/showtime discovery and seat maps
- Admin cinema, screen, seat, movie, showtime, customer, staff, booking, and analytics operations
- Pessimistic-lock, transactional seat hold handling to prevent double reservations
- Mock payment confirmation, QR ticket generation, PNG/PDF ticket retrieval, and single-use staff validation
- Cinema-aware staff access and revenue/occupancy/cancellation analytics

## Repository documentation

- [API documentation](API_Documentation.md)
- [Database ER diagram](docs/ER_Diagram.md)
- [End-to-end verification checklist](docs/End_to_End_Checklist.md)
- [Bruno collection guide](bruno/QuickSeat%20API/README.md)
