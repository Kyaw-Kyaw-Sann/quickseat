# QuickSeat Database ER Diagram

The database has 13 Flyway-managed tables. Timestamps are stored in PostgreSQL `TIMESTAMPTZ` in UTC.

```mermaid
erDiagram
    CINEMAS ||--o{ SCREENS : contains
    CINEMAS o|--o{ USERS : "assigned staff"
    USERS ||--o{ EMAIL_VERIFICATION_TOKENS : receives
    USERS ||--o{ PASSWORD_RESET_OTPS : receives
    USERS ||--o{ BOOKINGS : creates
    USERS o|--o{ TICKETS : verifies
    SCREENS ||--o{ SEATS : defines
    SCREENS ||--o{ SHOWTIMES : hosts
    MOVIES ||--o{ SHOWTIMES : schedules
    SHOWTIMES ||--o{ SHOWTIME_SEATS : inventories
    SEATS ||--o{ SHOWTIME_SEATS : maps
    SHOWTIMES ||--o{ BOOKINGS : receives
    BOOKINGS ||--|{ BOOKING_SEATS : snapshots
    SEATS ||--o{ BOOKING_SEATS : selected
    BOOKINGS ||--o| PAYMENTS : has
    BOOKINGS ||--o| TICKETS : generates
    BOOKINGS o|--o{ SHOWTIME_SEATS : holds

    CINEMAS {
      bigint id PK
      varchar name
      varchar city
      boolean active
    }
    USERS {
      bigint id PK
      varchar email UK
      varchar role
      bigint cinema_id FK
      boolean email_verified
      boolean active
    }
    EMAIL_VERIFICATION_TOKENS {
      bigint id PK
      bigint user_id FK
      varchar token UK
      timestamptz expires_at
      boolean used
    }
    PASSWORD_RESET_OTPS {
      bigint id PK
      bigint user_id FK
      varchar otp
      timestamptz expires_at
      boolean verified
    }
    SCREENS {
      bigint id PK
      bigint cinema_id FK
      varchar name
      boolean active
    }
    SEATS {
      bigint id PK
      bigint screen_id FK
      varchar row_name
      int seat_number
      varchar seat_type
      boolean active
    }
    MOVIES {
      bigint id PK
      varchar title
      int duration_minutes
      varchar status
      boolean active
    }
    SHOWTIMES {
      bigint id PK
      bigint movie_id FK
      bigint screen_id FK
      timestamptz start_time
      timestamptz end_time
      varchar status
    }
    SHOWTIME_SEATS {
      bigint id PK
      bigint showtime_id FK
      bigint seat_id FK
      bigint held_by_booking_id FK
      varchar status
      timestamptz hold_expires_at
    }
    BOOKINGS {
      bigint id PK
      varchar booking_reference UK
      bigint user_id FK
      bigint showtime_id FK
      varchar status
      numeric total_amount
    }
    BOOKING_SEATS {
      bigint id PK
      bigint booking_id FK
      bigint seat_id FK
      varchar seat_type
      numeric unit_price
    }
    PAYMENTS {
      bigint id PK
      bigint booking_id FK
      varchar payment_reference UK
      varchar status
      numeric amount
    }
    TICKETS {
      bigint id PK
      bigint booking_id FK
      varchar ticket_token UK
      bigint verified_by FK
      varchar status
      timestamptz used_at
    }
```

## Important constraints

- `users.email` is unique.
- `screens(cinema_id, name)` and `seats(screen_id, row_name, seat_number)` are unique.
- `showtime_seats(showtime_id, seat_id)` is unique.
- `bookings.booking_reference`, `payments.payment_reference`, and `tickets.ticket_token` are unique.
- A booking has at most one payment and one ticket; `booking_seats(booking_id, seat_id)` is unique.
- STAFF users require `cinema_id`; CUSTOMER and ADMIN users must not have one.
