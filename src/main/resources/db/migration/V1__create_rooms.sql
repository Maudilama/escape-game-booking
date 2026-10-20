CREATE TABLE rooms (
                       id                 BIGSERIAL PRIMARY KEY,
                       name               VARCHAR(100)  NOT NULL UNIQUE,
                       theme              VARCHAR(100)  NOT NULL,
                       description        TEXT          NOT NULL,
                       difficulty         SMALLINT      NOT NULL CHECK (difficulty BETWEEN 1 AND 5),
                       min_players        SMALLINT      NOT NULL CHECK (min_players >= 1),
                       max_players        SMALLINT      NOT NULL,
                       session_minutes    INTEGER       NOT NULL CHECK (session_minutes > 0),
                       reset_minutes      INTEGER       NOT NULL CHECK (reset_minutes >= 0),
                       price_per_person   NUMERIC(6, 2) NOT NULL CHECK (price_per_person > 0),
                       active             BOOLEAN       NOT NULL DEFAULT TRUE,
                       CHECK (max_players >= min_players)
);