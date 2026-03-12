ALTER TYPE product_status ADD VALUE IF NOT EXISTS 'pending_inspection';
ALTER TYPE product_status ADD VALUE IF NOT EXISTS 'inspected_passed';
ALTER TYPE product_status ADD VALUE IF NOT EXISTS 'inspected_failed';

ALTER TABLE users
ADD COLUMN IF NOT EXISTS average_rating DOUBLE PRECISION DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS total_reviews INTEGER DEFAULT 0;
