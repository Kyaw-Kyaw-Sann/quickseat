CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    booking_reference VARCHAR(30) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    showtime_id BIGINT NOT NULL REFERENCES showtimes(id),
    status VARCHAR(20) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    expires_at TIMESTAMPTZ,
    confirmed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE showtime_seats
    ADD CONSTRAINT fk_showtime_seats_held_booking
    FOREIGN KEY (held_by_booking_id) REFERENCES bookings(id);

CREATE TABLE booking_seats (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    seat_id BIGINT NOT NULL REFERENCES seats(id),
    seat_type VARCHAR(20) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    CONSTRAINT uk_booking_seats_booking_seat UNIQUE (booking_id, seat_id)
);
