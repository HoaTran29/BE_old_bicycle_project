ALTER TABLE products
ADD COLUMN IF NOT EXISTS deleted_at timestamp;

CREATE INDEX IF NOT EXISTS idx_products_deleted_at ON products(deleted_at);
CREATE INDEX IF NOT EXISTS idx_products_status_deleted_at ON products(status, deleted_at);
