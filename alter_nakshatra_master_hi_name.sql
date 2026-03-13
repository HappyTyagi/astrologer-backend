-- Add Hindi name column in nakshatra master.
ALTER TABLE nakshatra_master
    ADD COLUMN IF NOT EXISTS hi_name VARCHAR(255) NULL AFTER name;
