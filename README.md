# QuickSeat Backend

QuickSeat is a multi-cinema ticket-booking REST API built as a job-ready backend portfolio project. Customers can browse movies, select a cinema/showtime/seats, temporarily hold seats, complete a mock payment, retrieve QR/PDF tickets, and enter the cinema with a single-use QR ticket.

The backend is complete and ready for Next.js frontend integration.

## Why this project is interesting

QuickSeat is not only CRUD. Its core backend highlights are:

- **Seat concurrency protection** — two customers cannot hold the same showtime seat at once.
- **Booking lifecycle** — `PENDING → CONFIRMED → USED`, with expiry and eligible cancellation rules.
- **Showtime overlap prevention** — a screen cannot have overlapping active showtimes.
- **Cinema-aware staff authorization** — staff can access and validate tickets only for their assigned cinema.
- **Single-use QR tickets** — a successful scan changes both ticket and booking to `USED`.

## Features

| User | Main capabilities |
| --- | --- |
| Public | Browse movies, cinemas, showtimes, and seat maps; register/login; verify email; reset password |
| Customer | Hold seats, mock payment, booking history, cancellation, QR/PDF ticket retrieval |
| Staff | View assigned cinema operations and validate QR/manual ticket tokens |
| Admin | Manage cinemas, screens, seats, movies, showtimes, users, bookings, and analytics |

## Tech stack

- Java 21, Spring Boot, Spring Web MVC
- Spring Data JPA, Spring Security, Bean Validation
- PostgreSQL / Neon PostgreSQL, Flyway, Hibernate schema validation
- JWT access and refresh tokens, Gmail SMTP, Google OAuth foundation
- Cloudinary uploads, ZXing QR codes, Apache PDFBox ticket PDFs
- Springdoc OpenAPI / Swagger, JUnit, Mockito, Spring Security Test

## Prerequisites

- JDK 21 or newer compatible JDK
- Git
- PostgreSQL database; Neon PostgreSQL is supported
- Internet access when using Neon, Gmail, Cloudinary, or Google OAuth
- No separate Maven installation is needed because Maven Wrapper is included

Check Java:

```powershell
java -version
```

## Quick start for Git clone users

### 1. Clone the repository

```powershell
git clone <repository-url>
cd quickseat
```

### 2. Create `.env`

`.env` contains local secrets and is ignored by Git.

```powershell
Copy-Item .env.example .env
```

Open `.env` and replace the blank required values. The minimum configuration is:

```text
DB_URL=jdbc:postgresql://<host>/<database>?sslmode=require
DB_USERNAME=<database-user>
DB_PASSWORD=<database-password>
JWT_SECRET=<random-secret-with-at-least-32-characters>
```

For Neon, copy its PostgreSQL connection values, but ensure the URL is in JDBC form:

```text
DB_URL=jdbc:postgresql://<neon-host>/<database>?sslmode=require
```

For local PostgreSQL:

```text
DB_URL=jdbc:postgresql://localhost:5432/quickseat
DB_USERNAME=postgres
DB_PASSWORD=your_password
JWT_SECRET=replace-with-a-long-random-development-secret
```

### 3. Run without an IDE (PowerShell)

Use the included helper. It loads `.env`, checks the required values without printing secrets, then starts Spring Boot:

```powershell
.\scripts\run-dev.ps1
```

Stop the application with `Ctrl + C`.

If port `8080` is already in use, choose another port:

```powershell
.\scripts\run-dev.ps1 -Port 8081
```

### 4. Run from an IDE

1. Import/open the Maven project.
2. Configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` as environment variables in the run configuration.
3. Run `com.quickseat.QuickSeatApplication`.

## Verify the application

Default port URLs:

```text
Health:  http://localhost:8080/api/v1/health
Swagger: http://localhost:8080/api/v1/swagger-ui.html
OpenAPI: http://localhost:8080/api/v1/api-docs
```

Expected health response:

```json
{
  "success": true,
  "message": "QuickSeat API is running",
  "data": {
    "status": "UP"
  }
}
```

## Environment variables

| Variable | Required | Description |
| --- | --- | --- |
| `DB_URL` | Yes | JDBC PostgreSQL/Neon URL |
| `DB_USERNAME` | Yes | Database username |
| `DB_PASSWORD` | Yes | Database password |
| `JWT_SECRET` | Yes | JWT signing secret; use 32+ random characters |
| `CORS_ALLOWED_ORIGINS` | No | Allowed frontend origins; defaults to `http://localhost:3000` |
| `MAIL_ENABLED` | No | Enables email delivery; defaults to `false` |
| `GMAIL_USERNAME`, `GMAIL_APP_PASSWORD` | When mail enabled | Gmail SMTP credentials |
| `FRONTEND_BASE_URL`, `VERIFICATION_BASE_URL` | No | Frontend and email verification links |
| `CLOUDINARY_ENABLED` and Cloudinary variables | No | Cinema/movie image upload |
| `GOOGLE_OAUTH_ENABLED` and Google variables | No | Google OAuth login foundation |
| `SEAT_HOLD_DURATION_MINUTES` | No | Hold duration; defaults to 5 minutes |
| `DEMO_SEED_ENABLED` | No | Enables development-only sample data |
| `DEMO_SEED_PASSWORD` | No | Password for demo accounts |
| `SWAGGER_ENABLED` | No | Enables Swagger in the production profile; default `false` |

Never commit `.env`, access tokens, passwords, database credentials, Gmail app passwords, OAuth secrets, Cloudinary secrets, or `JWT_SECRET`.

If a real `.env` was committed in the past, rotate all exposed credentials. Ignoring a file today does not remove it from Git history.

## Database and Flyway

Flyway owns the database schema through migrations `V1`–`V7`. Hibernate runs with `ddl-auto=validate`, so it validates entity mappings but does not create or alter tables.

```text
Application start
→ Connect to PostgreSQL
→ Flyway validates/applies migrations
→ Hibernate validates entity mappings
→ API starts
```

Do not edit an already-applied Flyway migration. Future schema changes require a new migration.

## Development demo data

Demo data is disabled by default. Enable it only for a dedicated local/development database:

```text
DEMO_SEED_ENABLED=true
DEMO_SEED_PASSWORD=DemoPassword123!
```

It creates an idempotent sample set:

- Demo cinema and screen
- NORMAL and COUPLE seat layout
- Active NOW_SHOWING movie
- Future active showtime and seat inventory
- Verified, active demo users

| Role | Email |
| --- | --- |
| ADMIN | `demo.admin@quickseat.local` |
| STAFF | `demo.staff@quickseat.local` |
| CUSTOMER | `demo.customer@quickseat.local` |

The demo staff user belongs to the demo cinema. Production forcibly disables demo seed data. Never enable it on shared or production data.

## Run tests and build

Run the full test suite:

```powershell
.\mvnw.cmd test
```

Build the JAR:

```powershell
.\mvnw.cmd package
```

Run the built JAR after loading environment variables:

```powershell
java -jar target\quickseat-0.0.1-SNAPSHOT.jar
```

## Troubleshooting

| Problem | Likely cause and solution |
| --- | --- |
| `Port 8080 was already in use` | Stop the process using it, or run `./scripts/run-dev.ps1 -Port 8081`. |
| `Missing required .env values` | Fill `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` in `.env`. |
| `DB_URL must start with jdbc:postgresql://` | Convert the PostgreSQL/Neon connection string to JDBC format. |
| Database/Flyway startup failure | Verify Neon/local PostgreSQL is reachable, credentials are correct, and use a database compatible with the existing Flyway history. |
| No verification/reset email | Set `MAIL_ENABLED=true` and add a valid Gmail address/app password. |
| Image upload is unavailable | Set `CLOUDINARY_ENABLED=true` and add valid Cloudinary credentials. |
| Swagger is unavailable in production | It is disabled by default; enable `SWAGGER_ENABLED=true` only for intentional protected access. |

## Test APIs with Bruno

Open [bruno/QuickSeat API](bruno/QuickSeat%20API) in Bruno and select the `Local` environment.

Set only your local test variables:

```text
baseUrl=http://localhost:8080/api/v1
customerToken=
adminToken=
staffToken=
bookingReference=
ticketToken=
```

The collection contains no real secrets or JWT values. Use [API_Documentation.md](API_Documentation.md) for all request/response examples.

## Frontend integration

The backend base path is `/api/v1`. It allows the Next.js local origin `http://localhost:3000` by default.

Use [FRONTEND_CONTRACT.md](FRONTEND_CONTRACT.md) as the frontend integration source of truth for:

- authentication/token refresh behavior
- API response and error handling
- customer page flow and seat-hold UX
- role/route mapping
- time, MMK, QR, PDF, and image-upload handling

## Deployment notes

Use the production profile:

```text
SPRING_PROFILES_ACTIVE=prod
DEMO_SEED_ENABLED=false
CORS_ALLOWED_ORIGINS=https://your-frontend-domain.example
```

Production keeps Flyway and Hibernate validation enabled, disables demo data, and disables Swagger by default. Enable `SWAGGER_ENABLED=true` only when API documentation is intentionally exposed in a protected environment.

## Documentation

- [Complete API documentation](API_Documentation.md)
- [Frontend contract](FRONTEND_CONTRACT.md)
- [Database ER diagram](docs/ER_Diagram.md)
- [End-to-end verification checklist](docs/End_to_End_Checklist.md)
- [Bruno collection guide](bruno/QuickSeat%20API/README.md)

## Final end-to-end flow

```text
Register
→ Verify email
→ Login
→ Browse movie
→ Select cinema, date, and showtime
→ View seat map
→ Hold seats
→ Mock payment
→ Confirmed booking
→ Generate/view QR ticket
→ Download PDF ticket
→ Staff validates ticket
→ Ticket and booking become USED
```
