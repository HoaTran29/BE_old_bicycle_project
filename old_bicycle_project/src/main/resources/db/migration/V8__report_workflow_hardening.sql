ALTER TABLE reports
    ADD COLUMN IF NOT EXISTS admin_note TEXT,
    ADD COLUMN IF NOT EXISTS processed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS processed_by UUID REFERENCES users(id);

CREATE INDEX IF NOT EXISTS idx_reports_reporter_created_at
    ON reports(reporter_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_reports_status_target_type
    ON reports(status, target_type);
