









http://localhost:8080/api/v1/swagger-ui.html

http://localhost:8080/api/v1/api-docs

http://localhost:8080/api/v1/login/oauth2/code/google


┌─────────────────────┐
│       CINEMAS       │
├─────────────────────┤
│ PK id               │
│ name                │
│ address             │
│ city                │
│ phone               │
│ image_url           │
│ active              │
└─────────┬───────────┘
          │
          │ 1
          │
          ├───────────────────────┐
          │                       │
          │ *                     │ *
┌─────────▼───────────┐   ┌───────▼─────────────┐
│       SCREENS       │   │        USERS        │
├─────────────────────┤   ├─────────────────────┤
│ PK id               │   │ PK id               │
│ FK cinema_id        │   │ FK cinema_id NULL   │
│ name                │   │ name                │
│ active              │   │ email UNIQUE        │
└─────────┬───────────┘   │ password            │
          │               │ role                │
          │ 1             │ provider            │
          │               │ email_verified      │
          │ *             │ active              │
┌─────────▼───────────┐   └───┬─────────┬───────┘
│        SEATS        │       │         │
├─────────────────────┤       │         │
│ PK id               │       │         │
│ FK screen_id        │       │         │
│ row_name            │       │         │
│ seat_number         │       │         │
│ seat_type           │       │         │
│ active              │       │         │
└──────┬──────────────┘       │         │
       │                      │         │
       │                      │         │
       │               ┌──────▼──────┐  │
       │               │ EMAIL       │  │
       │               │ VERIFICATION│  │
       │               │ TOKENS      │  │
       │               └─────────────┘  │
       │                                │
       │                         ┌──────▼──────┐
       │                         │ PASSWORD    │
       │                         │ RESET OTPS  │
       │                         └─────────────┘
       │
       │
┌──────▼──────────────────────────────────────┐
│               SHOWTIME_SEATS               │
├────────────────────────────────────────────┤
│ PK id                                      │
│ FK showtime_id                             │
│ FK seat_id                                 │
│ FK held_by_booking_id NULL                 │
│ status                                     │
│ hold_expires_at                            │
│ UNIQUE(showtime_id, seat_id)               │
└──────────────────┬─────────────────────────┘
                   │
                   │ *
                   │
                   │ 1
             ┌─────▼───────┐
             │  SHOWTIMES  │
             ├─────────────┤
             │ PK id       │
             │ FK movie_id │
             │ FK screen_id│
             │ start_time  │
             │ end_time    │
             │ buffer      │
             │ prices      │
             │ status      │
             └──┬───────┬──┘
                │       │
              * │       │ *
                │       │
             1  │       │ 1
       ┌────────▼──┐   ┌▼───────────┐
       │  MOVIES   │   │  SCREENS   │
       └───────────┘   └────────────┘


USERS
  │
  │ 1
  │
  │ *
┌─▼───────────────────┐
│      BOOKINGS       │
├─────────────────────┤
│ PK id               │
│ booking_reference   │
│ FK user_id          │
│ FK showtime_id      │
│ status              │
│ total_amount        │
│ expires_at          │
│ confirmed_at        │
│ cancelled_at        │
└──┬────────┬─────────┘
   │        │
   │        │
   │1       │1
   │        │
   │*       │0..1
┌──▼──────────────┐ ┌─▼───────────────┐
│ BOOKING_SEATS   │ │    PAYMENTS     │
├─────────────────┤ ├─────────────────┤
│ PK id           │ │ PK id           │
│ FK booking_id   │ │ FK booking_id UQ│
│ FK seat_id      │ │ payment_ref UQ  │
│ seat_type       │ │ status          │
│ unit_price      │ │ amount          │
└─────────────────┘ └─────────────────┘

        BOOKINGS
            │
            │ 1
            │
            │ 0..1
     ┌──────▼──────────┐
     │     TICKETS     │
     ├─────────────────┤
     │ PK id           │
     │ FK booking_id UQ│
     │ ticket_token UQ │
     │ qr_image_url    │
     │ status          │
     │ used_at         │
     │ FK verified_by  │──────► USERS (STAFF)
     └─────────────────┘