CREATE TABLE IF NOT EXISTS member
(
    id SERIAL PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    real_name TEXT,
    password TEXT NOT NULL,
    spotwalla_username TEXT,
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    refresh_token TEXT
);

CREATE TYPE ownership_status AS ENUM
    ('OWNED', 'BORROWED', 'RENTED', 'SOLD', 'WRECKED');

CREATE TYPE rally_participant_type AS ENUM
    ('ORGANIZER', 'AIDE', 'RIDER');

CREATE TABLE IF NOT EXISTS motorcycle
(
    id        SERIAL PRIMARY KEY,
    member_id INTEGER REFERENCES member (id) ON DELETE CASCADE,
    make      TEXT,
    model     TEXT,
    year      INTEGER,
    color     TEXT,
    status    ownership_status NOT NULL DEFAULT 'OWNED' NOT NULL,
    active    boolean          not null default true
);

CREATE TABLE IF NOT EXISTS rally
(
    id SERIAL PRIMARY KEY,
    name TEXT,
    description TEXT,
    start_date DATE,
    end_date DATE,
    latitude FLOAT,
    longitude FLOAT,
    location_city TEXT,
    location_state TEXT,
    location_country TEXT,
	is_public BOOLEAN,
	points_public BOOLEAN,
	riders_public BOOLEAN,
	organizers_public BOOLEAN
);


CREATE TABLE IF NOT EXISTS bonus_point
(
    id          SERIAL PRIMARY KEY,
    rally_id    INTEGER REFERENCES rally (id) ON DELETE CASCADE,
    code        TEXT,
    name        TEXT,
    description TEXT,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    address     TEXT,
    points      INTEGER,
    required    BOOLEAN NOT NULL,
    repeatable  BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS combination
(
    id           SERIAL PRIMARY KEY,
    rally_id     INTEGER REFERENCES rally (id) ON DELETE CASCADE,
    code         TEXT,
    name         TEXT,
    description  TEXT,
    points       INTEGER,
    requires_all BOOLEAN NOT NULL,
    num_required INTEGER -- if requires_all is false
);

-- one of the points for a combo
CREATE TABLE IF NOT EXISTS combination_point
(
    id             SERIAL PRIMARY KEY,
    combination_id INTEGER REFERENCES combination (id) ON DELETE CASCADE,
    bonus_point_id INTEGER REFERENCES bonus_point (id) ON DELETE CASCADE,
    required       BOOLEAN NOT NULL
);



CREATE TABLE IF NOT EXISTS rally_participant
(
    id               SERIAL PRIMARY KEY,
    rally_id         INTEGER REFERENCES rally (id) ON DELETE CASCADE,
    member_id        INTEGER REFERENCES member (id) ON DELETE CASCADE,
    participant_type rally_participant_type NOT NULL,
    odometer_in      INTEGER,
    odometer_out     INTEGER,
    finisher         BOOLEAN,
    final_score      INTEGER
);

CREATE TABLE IF NOT EXISTS earned_bonus_point
(
    id                   SERIAL PRIMARY KEY,
    rally_participant_id INTEGER REFERENCES rally_participant (id) ON DELETE CASCADE,
    bonus_point_id       INTEGER REFERENCES bonus_point (id) ON DELETE CASCADE,
    odometer             INTEGER,
    earned_at            TIMESTAMPTZ,
    confirmed            BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS earned_combination
(
    id                   SERIAL PRIMARY KEY,
    rally_participant_id INTEGER REFERENCES rally_participant (id) ON DELETE CASCADE,
    combination_id       INTEGER REFERENCES combination (id) ON DELETE CASCADE,
    confirmed            BOOLEAN NOT NULL
);


CREATE TABLE IF NOT EXISTS ride
(
    id                   SERIAL PRIMARY KEY,
    rally_id             INTEGER REFERENCES rally (id) ON DELETE SET NULL,
    member_id            INTEGER REFERENCES member (id) ON DELETE CASCADE,
    name                 TEXT,
    description          TEXT,
    expected_start       timestamptz,
    expected_end         timestamptz,
    stop_duration        INTEGER,
    spotwalla_link       TEXT,
    actual_start         timestamptz,
    actual_end           timestamptz,
    odometer_start       INTEGER,
    odometer_end         INTEGER
);

CREATE TABLE IF NOT EXISTS route
(
    id                   SERIAL PRIMARY KEY,
    ride_id              INTEGER REFERENCES ride (id) ON DELETE CASCADE,
    name                 TEXT,
    description          TEXT,
    is_primary           BOOLEAN
);

CREATE TABLE IF NOT EXISTS ride_leg
(
    id                   SERIAL PRIMARY KEY,
    route_id             INTEGER REFERENCES route (id) ON DELETE CASCADE,
    name                 TEXT,
    description          TEXT,
    is_optional          BOOLEAN,
    sequence_order       INTEGER,
    seq                  INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS waypoint
(
    id                   SERIAL PRIMARY KEY,
    ride_leg_id          INTEGER REFERENCES ride_leg (id) ON DELETE CASCADE,
    bonus_point_id       INTEGER REFERENCES ride_leg (id) ON DELETE CASCADE,
    name                 TEXT,
    description          TEXT,
    sequence_order       INTEGER,
    latitude             DOUBLE PRECISION,
    longitude            DOUBLE PRECISION,
    address              TEXT,
    expected_arrival     timestamptz,
    expected_departure   timestamptz,
    stop_duration        INTEGER,
    actual_arrival       timestamptz,
    actual_departure     timestamptz

);

--
-- Create admin and test users
--
INSERT INTO member (id, email, real_name, password, spotwalla_username, is_admin, refresh_token)
VALUES (1, 'admin', 'Joe Larson', '$2a$10$716npnoapsrVPkgBQ1vOj.6WqIUd9pGSEc/5wJFQYeoXxCUXB9obK', null, true, null);

INSERT INTO member (id, email, real_name, password, spotwalla_username, is_admin, refresh_token)
VALUES (2, 'jpl@showpage.org', 'Joe', '$2a$10$fN6lMQuS1RYEK/zG0zRayOrbNPh2GQ7jIpbhfwIJkxItMqwhxluDq', null, false, null);

INSERT INTO member (id, email, real_name, password, spotwalla_username, is_admin, refresh_token)
VALUES (3, 'joe@showpage.org', 'Joseph', '$2a$10$bq2pUib7Av88zYc1cZ2deOUfHYrACfhUHMIo87ibjgqjv9Oqr1MtC', null, false, null);

