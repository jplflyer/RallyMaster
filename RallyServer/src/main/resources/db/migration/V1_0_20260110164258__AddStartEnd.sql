--
-- Add start and end locations to Ride.
--
BEGIN;

ALTER TABLE ride
    ADD COLUMN IF NOT EXISTS starting_bonus_point_id INTEGER references bonus_point,
    ADD COLUMN IF NOT EXISTS ending_bonus_point_id INTEGER references bonus_point;

COMMIT;
