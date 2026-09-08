ALTER TABLE users
    ADD CONSTRAINT chk_users_role_cinema
    CHECK (
        (role = 'STAFF' AND cinema_id IS NOT NULL)
        OR (role IN ('CUSTOMER', 'ADMIN') AND cinema_id IS NULL)
    );

ALTER TABLE seats
    ADD CONSTRAINT chk_seats_seat_type CHECK (seat_type IN ('NORMAL', 'COUPLE'));

ALTER TABLE movies
    ADD CONSTRAINT chk_movies_duration CHECK (duration_minutes > 0),
    ADD CONSTRAINT chk_movies_status CHECK (status IN ('UPCOMING', 'NOW_SHOWING', 'ENDED'));

ALTER TABLE showtimes
    ADD CONSTRAINT chk_showtimes_end_after_start CHECK (end_time > start_time),
    ADD CONSTRAINT chk_showtimes_buffer CHECK (cleaning_buffer_minutes >= 0),
    ADD CONSTRAINT chk_showtimes_prices CHECK (normal_price >= 0 AND couple_price >= 0),
    ADD CONSTRAINT chk_showtimes_status CHECK (status IN ('ACTIVE', 'CANCELLED', 'COMPLETED'));

ALTER TABLE showtime_seats
    ADD CONSTRAINT chk_showtime_seats_status CHECK (status IN ('AVAILABLE', 'HELD', 'BOOKED', 'UNAVAILABLE'));

ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_amount CHECK (total_amount >= 0),
    ADD CONSTRAINT chk_bookings_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'USED'));

ALTER TABLE booking_seats
    ADD CONSTRAINT chk_booking_seats_type CHECK (seat_type IN ('NORMAL', 'COUPLE')),
    ADD CONSTRAINT chk_booking_seats_price CHECK (unit_price >= 0);

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_amount CHECK (amount >= 0),
    ADD CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'));

ALTER TABLE tickets
    ADD CONSTRAINT chk_tickets_status CHECK (status IN ('ACTIVE', 'USED', 'CANCELLED'));

CREATE INDEX idx_screens_cinema_id ON screens(cinema_id);
CREATE INDEX idx_seats_screen_id ON seats(screen_id);
CREATE INDEX idx_showtimes_movie_id ON showtimes(movie_id);
CREATE INDEX idx_showtimes_screen_start_time ON showtimes(screen_id, start_time);
CREATE INDEX idx_showtimes_start_time_status ON showtimes(start_time, status);
CREATE INDEX idx_showtime_seats_showtime_status ON showtime_seats(showtime_id, status);
CREATE INDEX idx_showtime_seats_hold_expires_at ON showtime_seats(hold_expires_at);
CREATE INDEX idx_bookings_user_created_at ON bookings(user_id, created_at);
CREATE INDEX idx_bookings_showtime_status ON bookings(showtime_id, status);
CREATE INDEX idx_bookings_status_expires_at ON bookings(status, expires_at);
CREATE INDEX idx_booking_seats_booking_id ON booking_seats(booking_id);
