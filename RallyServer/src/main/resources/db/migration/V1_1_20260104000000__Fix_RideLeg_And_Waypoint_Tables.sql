-- Fix ride_leg table - remove unnecessary seq column
ALTER TABLE ride_leg DROP COLUMN IF EXISTS seq;
ALTER TABLE ride_leg DROP COLUMN IF EXISTS is_optional;

-- Fix waypoint table - correct the bonus_point_id foreign key constraint
-- First drop the incorrect constraint
ALTER TABLE waypoint DROP CONSTRAINT IF EXISTS waypoint_bonus_point_id_fkey;

-- Add the correct constraint pointing to bonus_point table
ALTER TABLE waypoint
    ADD CONSTRAINT waypoint_bonus_point_id_fkey
    FOREIGN KEY (bonus_point_id)
    REFERENCES bonus_point (id)
    ON DELETE CASCADE;
