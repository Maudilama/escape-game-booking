CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE bookings (
                          id              BIGSERIAL     PRIMARY KEY,
                          room_id         BIGINT        NOT NULL REFERENCES rooms (id),
                          customer_name   VARCHAR(100)  NOT NULL,
                          customer_email  VARCHAR(255)  NOT NULL,
                          start_at        TIMESTAMP     NOT NULL,
                          end_at          TIMESTAMP     NOT NULL,
                          player_count    SMALLINT      NOT NULL CHECK (player_count > 0),
                          total_price     NUMERIC(8, 2) NOT NULL CHECK (total_price >= 0),
                          status          VARCHAR(20)   NOT NULL,
                          created_at      TIMESTAMP     NOT NULL,
                          version         BIGINT        NOT NULL DEFAULT 0,
                          CHECK (end_at > start_at),
                          CONSTRAINT bookings_no_overlap EXCLUDE USING gist (
        room_id WITH =,
        tsrange(start_at, end_at, '[)') WITH &&
    ) WHERE (status = 'CONFIRMED')
);

CREATE INDEX idx_bookings_room_start ON bookings (room_id, start_at);