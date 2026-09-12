# QuickSeat End-to-End Verification Checklist

Use a dedicated development database. Start the API and confirm `GET /api/v1/health` returns `200`. Use [API_Documentation.md](../API_Documentation.md) or the Bruno collection for exact request bodies.

## Customer journey

- [ ] Register a new customer with `POST /api/v1/auth/register`.
- [ ] Open the email verification link, or call `GET /api/v1/auth/verify-email?token=...`; confirm the account becomes verified.
- [ ] Log in and keep the CUSTOMER access token.
- [ ] Browse active movies with `GET /api/v1/movies`.
- [ ] Browse active cinemas with `GET /api/v1/cinemas` and select one.
- [ ] Select a future active showtime using `GET /api/v1/showtimes?movieId=&cinemaId=&date=`.
- [ ] View the seat map using `GET /api/v1/showtimes/{showtimeId}/seats`.
- [ ] Hold available seats with `POST /api/v1/customer/seat-holds`; record the booking reference and expiry.
- [ ] Confirm a second customer cannot hold the same active seat; expect `409 Conflict`.
- [ ] Read payment details with `GET /api/v1/customer/bookings/{bookingReference}/payment-summary`.
- [ ] Submit successful mock payment with `POST /api/v1/customer/bookings/{bookingReference}/payments`.
- [ ] Confirm the booking is `CONFIRMED` and the selected inventory is `BOOKED`.
- [ ] Generate or retrieve the ticket with `POST`/`GET /api/v1/customer/bookings/{bookingReference}/ticket`.
- [ ] View QR PNG and download the PDF ticket. Confirm the PDF filename uses the booking reference and the QR is readable.

## Staff journey

- [ ] Log in as a STAFF user assigned to the same cinema.
- [ ] Verify `GET /api/v1/staff/cinema` returns only the assigned cinema.
- [ ] Validate the ticket with `POST /api/v1/staff/tickets/validate` using the ticket token.
- [ ] Confirm ticket status and booking status become `USED`, and `used_at`/`verified_by` are recorded.
- [ ] Repeat validation and confirm it is rejected.
- [ ] Attempt the same ticket as staff from another cinema and confirm it is rejected.

## Expiration and authorization checks

- [ ] Create a pending hold, wait past `SEAT_HOLD_DURATION_MINUTES`, then verify it cannot be paid and the seats are available again.
- [ ] Confirm a CUSTOMER cannot use `/admin/**` or `/staff/**` endpoints.
- [ ] Confirm unauthenticated requests to `/customer/**`, `/admin/**`, and `/staff/**` return `401`.
- [ ] Confirm inactive movies/cinemas/screens/seats and cancelled/past showtimes are excluded from new customer actions.

## Handoff checks

- [ ] Test browser access from `http://localhost:3000`; CORS headers allow that origin.
- [ ] Set an explicit `CORS_ALLOWED_ORIGINS` value before deployment.
- [ ] Use the `prod` profile with `DEMO_SEED_ENABLED=false`.
- [ ] Keep Swagger disabled in production unless it is intentionally enabled in a protected environment.
- [ ] Confirm `.env` and all real secrets are untracked before publishing the repository.
