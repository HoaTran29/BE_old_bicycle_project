CREATE TABLE IF NOT EXISTS groupsets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_groupsets_name_ci
    ON groupsets ((lower(name)));

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS groupset_id UUID REFERENCES groupsets(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_products_groupset_id ON products(groupset_id);

INSERT INTO groupsets (name)
SELECT DISTINCT trimmed.group_name
FROM (
    SELECT btrim(groupset) AS group_name
    FROM products
    WHERE groupset IS NOT NULL
      AND btrim(groupset) <> ''
) trimmed
LEFT JOIN groupsets existing
    ON lower(existing.name) = lower(trimmed.group_name)
WHERE existing.id IS NULL;

UPDATE products product_row
SET groupset_id = groupset_ref.id
FROM groupsets groupset_ref
WHERE product_row.groupset_id IS NULL
  AND product_row.groupset IS NOT NULL
  AND btrim(product_row.groupset) <> ''
  AND lower(groupset_ref.name) = lower(btrim(product_row.groupset));
