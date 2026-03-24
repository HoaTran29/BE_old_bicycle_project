ALTER TABLE inspections
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE inspections
SET updated_at = COALESCE(updated_at, created_at, NOW());

ALTER TABLE inspections
ALTER COLUMN updated_at SET DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_inspections_inspector_id ON inspections(inspector_id);
CREATE INDEX IF NOT EXISTS idx_inspections_updated_at ON inspections(updated_at DESC);
