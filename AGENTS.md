# AGENTS.md — QuickSeat Backend

## Interaction Rules

- The user may ask questions in English, but always respond in Myanmar language.

- Never add, update, remove, rename, move, fix, or modify any code, file, folder, configuration, dependency, or project structure without explicit permission.

- Do not make any project changes unless the user explicitly says the exact phrase:

  "Build Now"

- Before the phrase "Build Now" is given, only:
  - discuss
  - explain
  - review
  - plan
  - suggest
  - provide commands or code snippets without applying them

- When the user says "Build Now", you may make only the changes that were discussed or explicitly requested.

- Do not interpret similar phrases such as "go ahead", "continue", "start", "do it", or "proceed" as permission to modify the project.

- If "Build Now" has not been explicitly provided, do not modify the project.


## 1. Project Overview

**QuickSeat** is a multi-cinema ticket-booking platform.

Customers can:
- register and verify their email
- browse movies
- choose cinema, date, showtime, and seats
- temporarily hold seats
- complete a mock payment
- receive a QR ticket
- view and manage their bookings

Cinema staff can:
- access only their assigned cinema
- view showtimes and bookings
- view seat status
- scan or manually enter QR ticket tokens
- validate tickets
- mark valid tickets as used

Admins can:
- manage movies
- manage cinemas
- manage screens
- manage seat layouts
- manage showtimes
- manage customers and staff
- manage bookings
- view useful dashboard metrics

The backend should be completed and stabilized before frontend development begins.

---

## 2. Backend Tech Stack

Use:

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- PostgreSQL
- Flyway
- Bean Validation
- Lombok
- Springdoc OpenAPI / Swagger
- Gmail SMTP
- Google OAuth login
- Cloudinary
- QR code generation library

Do **not** introduce unnecessary infrastructure.

Avoid unless explicitly required:
- microservices
- Kafka
- Redis
- CQRS
- Event Sourcing
- distributed locks
- Kubernetes
- complex domain frameworks
- excessive abstraction
- generic repository/service layers
- premature optimization

The target style is **mid-junior level**:
- not beginner/demo-code style
- not senior/enterprise overengineering
- clean, practical, understandable, interview-ready

---

## 3. Development Style

Follow this development style strictly:

1. Backend first.
2. Complete one phase before starting the next.
3. Prefer the most valuable implementation over the fanciest implementation.
4. Keep architecture simple and consistent.
5. Business rules must be enforced in the backend.
6. Database constraints should protect important invariants.
7. Use transactions where consistency matters.
8. Add tests especially for valuable business logic.
9. Do not build features outside the finalized scope.
10. Do not overengineer.

When Codex proposes code or architecture:
- prefer simple Spring Boot conventions
- explain tradeoffs briefly
- avoid adding libraries unless they clearly help
- do not create unnecessary interfaces/classes
- do not split a simple feature across too many files

---

## 4. Package Structure

Use a simple layer-based structure:

```text
com.quickseat

├── config
├── controller
├── dto
├── entity
├── exception
├── mapper
├── repository
├── security
├── service
├── util
└── QuickSeatApplication
```

Do not switch to a complex domain-driven or multi-module architecture unless explicitly requested.

---

## 5. API Conventions

Base API prefix:

```text
/api/v1
```

Suggested endpoint groups:

```text
/api/v1/auth
/api/v1/movies
/api/v1/cinemas
/api/v1/screens
/api/v1/showtimes
/api/v1/bookings
/api/v1/customer
/api/v1/staff
/api/v1/admin
```

Use DTOs for controller input/output.

Do not expose JPA entities directly from controllers.

Use Bean Validation:
- `@NotBlank`
- `@NotNull`
- `@Email`
- `@Size`
- `@Positive`
- other simple validation annotations as needed

Use `@Valid` in controllers.

---

## 6. API Response and Exception Handling

Use a lightweight success response wrapper if useful:

```java
public record ApiResponse<T>(
        boolean success,
        String message,
        T data
) {}
```

Use centralized exception handling with `@RestControllerAdvice`.

Main exception types can include:

- `ResourceNotFoundException`
- `BadRequestException`
- `ConflictException`
- `UnauthorizedException`
- `ForbiddenException`

Keep error responses consistent.

Do not create a complicated error framework.

---

## 7. Security Model

Roles:

```text
CUSTOMER
STAFF
ADMIN
```

Providers:

```text
LOCAL
GOOGLE
```

Security requirements:

- use Spring Security
- use password hashing for local accounts
- use JWT authentication
- use a practical refresh-token strategy
- protect endpoints by role
- disabled users must not authenticate
- customers may access only their own booking/ticket data
- staff may access only data for their assigned cinema
- admins may access system-wide management data

Important staff rule:

```text
staff.cinema_id
==
ticket -> booking -> showtime -> screen -> cinema.id
```

This must be enforced in the backend, not only hidden in the frontend.

---

## 8. Final Backend Feature Scope

### Authentication
- customer registration
- login
- logout
- email verification
- Google login
- forgot password
- password reset with 6-digit OTP
- role-based access
- account activation/deactivation
- customer profile management

### Movies
- browse now-showing movies
- browse upcoming movies
- search movies
- filter movies
- movie details
- admin create/update movies
- movie lifecycle status
- activate/deactivate movies
- Cloudinary media handling

### Cinema / Screen / Seat
- manage cinemas
- manage screens
- generate and edit seat layouts
- normal seats
- couple seats
- activate/deactivate operational records

### Showtimes
- create/update/cancel showtimes
- assign movie and screen
- normal/couple prices
- movie-duration-aware end time
- cleaning buffer
- prevent overlapping showtimes
- query by movie/cinema/date

### Showtime Seat Inventory
- generate showtime-specific seat inventory
- `AVAILABLE`
- `HELD`
- `BOOKED`
- `UNAVAILABLE`

### Seat Hold
- temporarily hold selected seats
- backend-controlled expiration
- countdown source
- auto-release expired holds
- prevent concurrent double booking
- transaction-safe seat reservation

### Booking
- create `PENDING` booking
- unique booking reference
- store selected seats
- snapshot seat price at booking time
- calculate total
- confirm booking
- cancel eligible booking
- expire booking
- mark used
- customer booking history

### Mock Payment
- booking summary
- simulated success/failure
- prevent payment after expiration
- confirm booking only after successful payment
- prevent duplicate success processing

### QR Ticket
- generate ticket after booking confirmation
- secure unique ticket token
- QR generation
- display/download support
- single-use validation
- prevent repeated use

### Customer Dashboard APIs
- pending bookings
- upcoming bookings
- past bookings
- cancelled/expired bookings
- booking details
- continue pending payment
- QR ticket retrieval
- eligible cancellation

### Staff APIs
- assigned cinema information
- assigned cinema showtimes
- assigned cinema bookings
- booking search
- booking details
- showtime seat status
- QR scanning validation
- manual ticket-token validation
- cross-cinema rejection
- mark valid ticket as used

### Admin APIs
- manage movies
- manage cinemas
- manage screens
- manage seats
- manage showtimes
- manage customers
- manage staff and cinema assignment
- manage bookings
- system-wide search/filtering
- dashboard and analytics

### Email Notifications
- email verification
- password reset OTP
- booking confirmation
- QR ticket email
- booking cancellation email

### Time / Localization
- store timestamps with PostgreSQL `TIMESTAMPTZ`
- treat UTC as storage time
- display as Myanmar time in client-facing responses where appropriate
- default currency: MMK

---

## 9. Main Business Rules

1. Only verified customers may create bookings.
2. Access must be enforced by role: `CUSTOMER`, `STAFF`, `ADMIN`.
3. A staff account belongs to one cinema and may access only that cinema's operational data.
4. Inactive cinema, screen, seat, movie, or user records must not be used for new operational actions where applicable.
5. Active showtimes on the same screen must not overlap.
6. Showtime occupied time must include movie duration plus cleaning buffer.
7. Seat availability is managed per showtime.
8. Selected seats are held temporarily and automatically released when the hold expires.
9. The same seat for the same showtime must never be actively held/booked by two users at once.
10. One booking belongs to one showtime.
11. Seat prices must be snapshotted at booking time.
12. Expired bookings cannot be paid.
13. Booking becomes `CONFIRMED` only after successful payment.
14. One confirmed booking generates at most one QR ticket.
15. QR tickets are single-use.
16. Staff may validate tickets only for their assigned cinema.
17. Successful ticket validation changes ticket and booking to `USED`.
18. `USED` bookings cannot be cancelled.
19. Operational records should use status/active fields instead of hard deletion.
20. Revenue metrics should use successful/confirmed booking revenue only.
21. Dates/times are stored as `TIMESTAMPTZ` in UTC.
22. Default currency is MMK.

---

## 10. Final Database Design

Use **13 tables**:

```text
1. cinemas
2. users
3. email_verification_tokens
4. password_reset_otps
5. screens
6. seats
7. movies
8. showtimes
9. showtime_seats
10. bookings
11. booking_seats
12. payments
13. tickets
```

---

## 11. Table Design

### `cinemas`

```text
id                  BIGINT PK
name                VARCHAR(150) NOT NULL
address             TEXT NOT NULL
city                VARCHAR(100) NOT NULL
phone               VARCHAR(30)
image_url           TEXT
active              BOOLEAN NOT NULL DEFAULT TRUE
created_at          TIMESTAMPTZ NOT NULL
updated_at          TIMESTAMPTZ NOT NULL
```

---

### `users`

```text
id                  BIGINT PK
name                VARCHAR(120) NOT NULL
email               VARCHAR(255) NOT NULL UNIQUE
password            VARCHAR(255) NULL
phone               VARCHAR(30)
role                VARCHAR(20) NOT NULL
cinema_id           BIGINT NULL FK -> cinemas.id
provider            VARCHAR(20) NOT NULL
email_verified      BOOLEAN NOT NULL DEFAULT FALSE
active              BOOLEAN NOT NULL DEFAULT TRUE
created_at          TIMESTAMPTZ NOT NULL
updated_at          TIMESTAMPTZ NOT NULL
```

Rules:

```text
STAFF     -> cinema_id required
CUSTOMER  -> cinema_id null
ADMIN     -> cinema_id null
```

---

### `email_verification_tokens`

```text
id                  BIGINT PK
user_id             BIGINT NOT NULL FK -> users.id
token               VARCHAR(255) NOT NULL UNIQUE
expires_at          TIMESTAMPTZ NOT NULL
used                BOOLEAN NOT NULL DEFAULT FALSE
created_at          TIMESTAMPTZ NOT NULL
```

---

### `password_reset_otps`

```text
id                  BIGINT PK
user_id             BIGINT NOT NULL FK -> users.id
otp                 VARCHAR(6) NOT NULL
expires_at          TIMESTAMPTZ NOT NULL
verified            BOOLEAN NOT NULL DEFAULT FALSE
attempt_count       INTEGER NOT NULL DEFAULT 0
created_at          TIMESTAMPTZ NOT NULL
```

---

### `screens`

```text
id                  BIGINT PK
cinema_id           BIGINT NOT NULL FK -> cinemas.id
name                VARCHAR(100) NOT NULL
active              BOOLEAN NOT NULL DEFAULT TRUE
created_at          TIMESTAMPTZ NOT NULL
updated_at          TIMESTAMPTZ NOT NULL
```

Constraint:

```text
UNIQUE(cinema_id, name)
```

---

### `seats`

```text
id                  BIGINT PK
screen_id           BIGINT NOT NULL FK -> screens.id
row_name            VARCHAR(10) NOT NULL
seat_number         INTEGER NOT NULL
seat_type           VARCHAR(20) NOT NULL
active              BOOLEAN NOT NULL DEFAULT TRUE
created_at          TIMESTAMPTZ NOT NULL
updated_at          TIMESTAMPTZ NOT NULL
```

Seat types:

```text
NORMAL
COUPLE
```

Constraint:

```text
UNIQUE(screen_id, row_name, seat_number)
```

A couple seat is stored as one bookable seat unit with capacity 2.

---

### `movies`

```text
id                  BIGINT PK
title               VARCHAR(200) NOT NULL
description         TEXT
duration_minutes    INTEGER NOT NULL
release_date        DATE
language            VARCHAR(50)
genres              TEXT[]
age_rating          VARCHAR(20)
director            VARCHAR(150)
cast_text           TEXT
poster_url          TEXT
trailer_url         TEXT
status              VARCHAR(20) NOT NULL
active              BOOLEAN NOT NULL DEFAULT TRUE
created_at          TIMESTAMPTZ NOT NULL
updated_at          TIMESTAMPTZ NOT NULL
```

Statuses:

```text
UPCOMING
NOW_SHOWING
ENDED
```

`status` is lifecycle state.
`active` is operational availability.

---

### `showtimes`

```text
id                       BIGINT PK
movie_id                 BIGINT NOT NULL FK -> movies.id
screen_id                BIGINT NOT NULL FK -> screens.id
start_time               TIMESTAMPTZ NOT NULL
end_time                 TIMESTAMPTZ NOT NULL
cleaning_buffer_minutes  INTEGER NOT NULL DEFAULT 15
normal_price             NUMERIC(12,2) NOT NULL
couple_price             NUMERIC(12,2) NOT NULL
status                   VARCHAR(20) NOT NULL
created_at               TIMESTAMPTZ NOT NULL
updated_at               TIMESTAMPTZ NOT NULL
```

Statuses:

```text
ACTIVE
CANCELLED
COMPLETED
```

Overlap rule:

```text
newStart < existingEnd
AND
newEnd > existingStart
```

Cancelled showtimes do not block the screen schedule.

---

### `showtime_seats`

This is a core table.

```text
id                  BIGINT PK
showtime_id         BIGINT NOT NULL FK -> showtimes.id
seat_id             BIGINT NOT NULL FK -> seats.id
status              VARCHAR(20) NOT NULL
held_by_booking_id  BIGINT NULL FK -> bookings.id
hold_expires_at     TIMESTAMPTZ NULL
created_at          TIMESTAMPTZ NOT NULL
updated_at          TIMESTAMPTZ NOT NULL
```

Statuses:

```text
AVAILABLE
HELD
BOOKED
UNAVAILABLE
```

Critical constraint:

```text
UNIQUE(showtime_id, seat_id)
```

Responsibility:
- `seats` = physical layout
- `showtime_seats` = current showtime-specific seat inventory/state
- `booking_seats` = immutable booking seat/price snapshot

---

### `bookings`

```text
id                  BIGINT PK
booking_reference   VARCHAR(30) NOT NULL UNIQUE
user_id             BIGINT NOT NULL FK -> users.id
showtime_id         BIGINT NOT NULL FK -> showtimes.id
status              VARCHAR(20) NOT NULL
total_amount        NUMERIC(12,2) NOT NULL
expires_at          TIMESTAMPTZ NULL
confirmed_at        TIMESTAMPTZ NULL
cancelled_at        TIMESTAMPTZ NULL
created_at          TIMESTAMPTZ NOT NULL
updated_at          TIMESTAMPTZ NOT NULL
```

Statuses:

```text
PENDING
CONFIRMED
CANCELLED
EXPIRED
USED
```

Typical lifecycle:

```text
PENDING -> CONFIRMED
PENDING -> EXPIRED
PENDING -> CANCELLED
CONFIRMED -> USED
CONFIRMED -> CANCELLED
```

Do not allow invalid state transitions.

---

### `booking_seats`

```text
id                  BIGINT PK
booking_id          BIGINT NOT NULL FK -> bookings.id
seat_id             BIGINT NOT NULL FK -> seats.id
seat_type           VARCHAR(20) NOT NULL
unit_price          NUMERIC(12,2) NOT NULL
```

Constraint:

```text
UNIQUE(booking_id, seat_id)
```

This table stores the booking-time snapshot of seat type and price.

---

### `payments`

```text
id                  BIGINT PK
booking_id          BIGINT NOT NULL UNIQUE FK -> bookings.id
payment_reference   VARCHAR(50) NOT NULL UNIQUE
status              VARCHAR(20) NOT NULL
amount              NUMERIC(12,2) NOT NULL
paid_at             TIMESTAMPTZ NULL
created_at          TIMESTAMPTZ NOT NULL
updated_at          TIMESTAMPTZ NOT NULL
```

Statuses:

```text
PENDING
SUCCESS
FAILED
```

MVP rule:
- one booking has at most one payment record

---

### `tickets`

```text
id                  BIGINT PK
booking_id          BIGINT NOT NULL UNIQUE FK -> bookings.id
ticket_token        VARCHAR(255) NOT NULL UNIQUE
qr_image_url        TEXT
status              VARCHAR(20) NOT NULL
used_at             TIMESTAMPTZ NULL
verified_by         BIGINT NULL FK -> users.id
created_at          TIMESTAMPTZ NOT NULL
updated_at          TIMESTAMPTZ NOT NULL
```

Statuses:

```text
ACTIVE
USED
CANCELLED
```

Rules:
- one booking has at most one ticket
- `ticket_token` must be unique
- `verified_by` stores the staff user ID

---

## 12. Important Database Constraints

Must preserve these:

```text
users.email
-> UNIQUE

screens(cinema_id, name)
-> UNIQUE

seats(screen_id, row_name, seat_number)
-> UNIQUE

showtime_seats(showtime_id, seat_id)
-> UNIQUE

bookings.booking_reference
-> UNIQUE

booking_seats(booking_id, seat_id)
-> UNIQUE

payments.booking_id
-> UNIQUE

payments.payment_reference
-> UNIQUE

tickets.booking_id
-> UNIQUE

tickets.ticket_token
-> UNIQUE
```

Use database constraints for important invariants, not only service validation.

---

## 13. Recommended Indexes

Keep indexing practical.

Suggested:

```text
screens(cinema_id)

seats(screen_id)

showtimes(movie_id)
showtimes(screen_id, start_time)
showtimes(start_time, status)

showtime_seats(showtime_id, status)
showtime_seats(hold_expires_at)

bookings(user_id, created_at)
bookings(showtime_id, status)
bookings(status, expires_at)

booking_seats(booking_id)
```

Do not add duplicate indexes for columns already covered by unique constraints.

---

## 14. Flyway Rules

Use Flyway.

Hibernate configuration:

```text
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

Flyway owns schema evolution.
Hibernate validates mappings.

Keep migrations simple:
- create table
- alter table
- add constraints
- add indexes
- simple seed/demo data if needed

Do not introduce advanced Flyway features unless necessary.

Suggested migration progression:

```text
V1__create_auth_and_users.sql
V2__create_cinema_screen_seat.sql
V3__create_movies_and_showtimes.sql
V4__create_showtime_seats.sql
V5__create_bookings_and_booking_seats.sql
V6__create_payments_and_tickets.sql
V7__add_indexes_and_final_constraints.sql
```

When a migration has already been applied:
- do not casually edit it
- create a new migration for schema changes

---

## 15. Transaction Rules

Use `@Transactional` only where it provides real value.

Important transactional operations:

### Seat Hold
Inside one transaction:
1. load/check target showtime seats
2. lock/re-check availability
3. create pending booking
4. create booking seat snapshots
5. mark showtime seats as `HELD`
6. set hold expiration

### Payment Success
Inside one transaction:
1. validate booking is still `PENDING`
2. validate not expired
3. mark payment `SUCCESS`
4. mark booking `CONFIRMED`
5. mark held seats `BOOKED`

### QR Verification
Inside one transaction:
1. validate ticket
2. validate booking
3. validate staff cinema
4. ensure not already used
5. mark ticket `USED`
6. mark booking `USED`
7. store `used_at`
8. store `verified_by`

Do not create transactions around simple read-only code unnecessarily.

---

## 16. Seat Hold and Concurrency Strategy

This is one of the most valuable backend features.

Requirements:

- backend time is the source of truth
- frontend countdown is display only
- expired holds must not block seats
- same showtime + same seat cannot be held/booked by two users
- re-check availability inside the transaction

Use:
- `@Transactional`
- database uniqueness
- JPA/PostgreSQL row locking where appropriate
- simple scheduled cleanup if needed

Do not use:
- Redis
- distributed locks
- WebSockets
- complex queueing

A valid result for concurrent attempts:

```text
Customer A -> Seat A5 -> success
Customer B -> Seat A5 -> conflict/rejected
```

Exactly one may succeed.

---

## 17. Showtime Overlap Rules

When creating/updating an active showtime:

```text
occupiedEnd = startTime + movieDuration + cleaningBuffer
```

Reject if another active showtime on the same screen satisfies:

```text
newStart < existingEnd
AND
newEnd > existingStart
```

Do this in the backend.

Do not rely only on frontend validation.

Cancelled showtimes should be excluded from overlap blocking.

---

## 18. QR Ticket Validation Rules

Validation flow:

```text
ticket exists?
-> ticket ACTIVE?
-> booking CONFIRMED?
-> staff cinema matches booking cinema?
-> not already used?
-> valid
```

Possible API outcomes:

```text
VALID
INVALID_TICKET
ALREADY_USED
CANCELLED
INVALID_STATUS
WRONG_CINEMA
```

On success:

```text
ticket.status = USED
ticket.used_at = now
ticket.verified_by = staff.id
booking.status = USED
```

Repeated scans must not consume the ticket twice.

---

## 19. Dashboard Metrics

Keep analytics useful, not decorative.

Include:

- total bookings
- confirmed bookings
- today's bookings
- total revenue
- occupancy rate
- cancellation rate
- most-booked movies
- movie performance
- cinema-wise bookings
- cinema-wise revenue
- cinema-wise occupancy
- revenue trend

Use SQL/JPA aggregation concepts such as:
- `COUNT`
- `SUM`
- `GROUP BY`
- joins
- date aggregation

Do not build an analytics warehouse.

Revenue should use successful/confirmed revenue only.

---

## 20. Backend Development Roadmap

Complete phases in order.

### Phase 1 — Project Foundation
- Spring Boot setup
- PostgreSQL connection
- package structure
- environment/profile configuration
- global API response
- global exception handling
- validation
- logging
- Swagger/OpenAPI
- Git setup
- health endpoint

### Phase 2 — Database & JPA Foundation
- Flyway migrations
- entities
- enums
- relationships
- constraints
- indexes
- repositories

### Phase 3 — Authentication & Security
- registration
- login
- JWT
- refresh strategy
- logout
- password hashing
- role authorization
- active-user checks
- current authenticated user

### Phase 4 — Verification & Recovery
- Gmail SMTP
- email verification
- forgot password
- OTP
- reset password
- Google login

### Phase 5 — Cinema / Screen / Seat
- cinema CRUD-style management
- screen management
- seat layout generation/editing
- activation/deactivation

### Phase 6 — Movies
- movie management
- search/filter
- lifecycle status
- Cloudinary
- public movie APIs

### Phase 7 — Showtimes
- create/update/cancel
- pricing
- duration/end-time calculation
- cleaning buffer
- overlap prevention
- filters

### Phase 8 — Showtime Seat Inventory
- generate inventory
- availability states
- showtime seat map

### Phase 9 — Seat Hold & Concurrency
- pending booking creation
- temporary holds
- expiration
- auto-release
- locking/concurrency protection

### Phase 10 — Booking Management
- booking lifecycle
- booking reference
- seat snapshot
- pricing
- cancellation
- history

### Phase 11 — Mock Payment
- success/failure simulation
- expiration checks
- confirmation transaction
- duplicate success prevention

### Phase 12 — QR Ticket & Email
- ticket token
- QR generation
- booking confirmation email
- QR email
- ticket retrieval

### Phase 13 — Staff Backend
- assigned cinema data
- booking/showtime access
- seat status
- QR validation
- cinema-aware authorization

### Phase 14 — Admin Management
- customer management
- staff management
- staff cinema assignment
- booking management

### Phase 15 — Dashboard & Analytics
- booking/revenue/occupancy/cancellation metrics
- movie performance
- cinema performance
- trends

### Phase 16 — Testing & Hardening
Prioritize:
- showtime overlap tests
- seat concurrency tests
- expired booking payment test
- QR double-scan test
- cross-cinema staff authorization test
- authentication integration tests
- booking/payment integration tests

Also review:
- validation
- exceptions
- transactions
- security
- important query performance
- N+1 issues on important endpoints only

### Phase 17 — Backend Finalization
- API naming consistency
- DTO cleanup
- Swagger
- demo/seed data
- CORS
- migration review
- README
- ER diagram
- Postman collection
- deployment preparation
- end-to-end flow validation

Backend is considered complete only after this flow works:

```text
Register
-> Verify Email
-> Login
-> Browse Movie
-> Choose Showtime
-> Hold Seat
-> Create Booking
-> Mock Payment
-> Generate QR
-> Staff Scan
-> Ticket USED
```

Only then begin the Next.js frontend.

---

## 21. Phase 1 Foundation Rules

Current phase is Phase 1.

Use:
- Java 21
- Spring Boot
- PostgreSQL
- `application.yml`
- a simple `dev` profile
- environment variables for secrets
- `/api/v1` base path
- Swagger/OpenAPI
- global exception handling
- validation
- basic logging
- health endpoint
- Git repository



Do not begin:
- business entities
- JWT logic
- cinema CRUD
- booking logic

until Phase 1 is complete.

---

## 22. Logging Rules

Use Spring logging / Lombok `@Slf4j`.

Log useful events only.

Examples:
- successful entity creation
- important state changes
- failed authorization checks
- booking expiration
- QR validation failures

Never log:
- passwords
- JWTs
- OTPs
- ticket tokens
- secrets

Do not add a custom logging framework.

---

## 23. Testing Style

Testing should be practical.

Use:
- unit tests for isolated business logic
- integration tests for valuable end-to-end backend behavior

High-value tests:
- two users try same seat concurrently
- overlapping showtime is rejected
- expired booking cannot be paid
- staff cannot validate another cinema's ticket
- already-used QR cannot be used again
- inactive seat cannot be selected
- price snapshot does not change when showtime price changes

Do not chase unrealistic 100% coverage.

---

## 24. Soft Delete / Status Strategy

Avoid hard deletion of operational records.

Use:
- `active` for user/cinema/screen/seat/movie operational availability
- lifecycle `status` for movie/showtime/booking/payment/ticket state

Examples:

```text
Movie:
UPCOMING
NOW_SHOWING
ENDED

Showtime:
ACTIVE
CANCELLED
COMPLETED

Booking:
PENDING
CONFIRMED
CANCELLED
EXPIRED
USED

Payment:
PENDING
SUCCESS
FAILED

Ticket:
ACTIVE
USED
CANCELLED
```

Historical booking/payment/ticket data must remain intact.

---

## 25. Time Rules

- use `TIMESTAMPTZ`
- store timestamps in UTC
- backend time is authoritative
- show Myanmar time to users where appropriate
- never trust frontend countdown for expiration logic

---

## 26. Scope Explicitly Excluded from MVP

Do not add these unless the project owner explicitly changes scope:

- real payment gateway
- PWA
- movie favourites
- AI chat
- dynamic pricing
- WebSockets
- recommendation system
- multiple cinema-company tenants
- loyalty points
- refund/settlement system
- React Native app
- microservices

---

## 27. Coding Guidance for Codex

When generating code for this project:

1. Prefer readable code over clever code.
2. Use constructor injection.
3. Use records for simple immutable DTOs where suitable.
4. Use enums for controlled statuses/roles.
5. Keep controllers thin.
6. Put business rules in services.
7. Put database query logic in repositories.
8. Use mappers only when they provide clear value.
9. Avoid giant utility classes.
10. Avoid unnecessary inheritance.
11. Avoid one-interface-per-service unless there is a real reason.
12. Avoid premature generic abstractions.
13. Do not expose entities directly in APIs.
14. Validate input at API boundaries.
15. Enforce critical rules again in the service/database layer.
16. Use explicit, descriptive method names.
17. Prefer small, meaningful transactions.
18. Keep queries understandable.
19. Optimize only important endpoints when evidence or obvious need exists.
20. Never change finalized business rules silently.

If a requirement is ambiguous:
- choose the simplest approach consistent with this file
- mention the assumption
- do not invent extra features

---

## 28. Project Goal

QuickSeat is a **skill-proof portfolio project** aimed at demonstrating job-ready junior backend capability with:

- Spring Boot
- PostgreSQL
- REST APIs
- authentication/authorization
- relational database design
- Flyway migrations
- transactions
- concurrency handling
- business-rule validation
- email integration
- media integration
- QR validation
- SQL aggregation
- testing

The strongest technical highlights should remain:

```text
Seat Concurrency
Booking Lifecycle
Showtime Overlap Prevention
Cinema-Aware Staff Authorization
Single-Use QR Validation
```

These are more important than adding more features.

---

## 29. Final Instruction

Always optimize for:

```text
Correctness
Clarity
Business-rule enforcement
Portfolio value
Mid-junior maintainability
```

Never optimize for:

```text
Architecture complexity
Enterprise-style abstraction
Technology count
Premature scalability
Overengineering
```
