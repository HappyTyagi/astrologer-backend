-- Add Hindi name columns in additional master tables.
ALTER TABLE gender_master
    ADD COLUMN IF NOT EXISTS hi_name VARCHAR(255) NULL AFTER name;

ALTER TABLE state_master
    ADD COLUMN IF NOT EXISTS hi_name VARCHAR(255) NULL AFTER name;

ALTER TABLE district_master
    ADD COLUMN IF NOT EXISTS hi_name VARCHAR(255) NULL AFTER name;

ALTER TABLE gemstone_master
    ADD COLUMN IF NOT EXISTS hi_name VARCHAR(255) NULL AFTER name;
