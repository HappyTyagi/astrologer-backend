-- Add popup validity and CMS fields to puja table
ALTER TABLE puja
    ADD COLUMN popup_enabled TINYINT(1) NOT NULL DEFAULT 1,
    ADD COLUMN popup_start_date DATE NULL,
    ADD COLUMN popup_end_date DATE NULL,
    ADD COLUMN popup_priority INT NOT NULL DEFAULT 0,
    ADD COLUMN popup_label VARCHAR(255) NULL;

-- Optional: speed up popup lookup
CREATE INDEX idx_puja_popup_active_dates
    ON puja (is_active, popup_enabled, popup_start_date, popup_end_date, popup_priority);
