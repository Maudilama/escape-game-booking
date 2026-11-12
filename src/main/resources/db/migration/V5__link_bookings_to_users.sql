ALTER TABLE bookings ADD COLUMN user_id BIGINT NOT NULL REFERENCES users (id);
ALTER TABLE bookings DROP COLUMN customer_name;
ALTER TABLE bookings DROP COLUMN customer_email;

CREATE INDEX idx_bookings_user ON bookings (user_id);