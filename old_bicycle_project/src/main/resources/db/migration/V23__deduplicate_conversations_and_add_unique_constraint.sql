CREATE TEMP TABLE tmp_ranked_conversations ON COMMIT DROP AS
SELECT
    c.id,
    c.product_id,
    c.buyer_id,
    c.seller_id,
    c.created_at,
    c.updated_at,
    FIRST_VALUE(c.id) OVER (
        PARTITION BY c.product_id, c.buyer_id, c.seller_id
        ORDER BY c.created_at ASC, c.id ASC
    ) AS canonical_id,
    ROW_NUMBER() OVER (
        PARTITION BY c.product_id, c.buyer_id, c.seller_id
        ORDER BY c.created_at ASC, c.id ASC
    ) AS row_num
FROM conversations c;

CREATE TEMP TABLE tmp_duplicate_conversations ON COMMIT DROP AS
SELECT id AS duplicate_id, canonical_id
FROM tmp_ranked_conversations
WHERE row_num > 1;

UPDATE messages m
SET conversation_id = d.canonical_id
FROM tmp_duplicate_conversations d
WHERE m.conversation_id = d.duplicate_id;

UPDATE notifications n
SET metadata = jsonb_set(
    n.metadata,
    '{conversationId}',
    to_jsonb(d.canonical_id::text),
    false
)
FROM tmp_duplicate_conversations d
WHERE n.metadata IS NOT NULL
  AND jsonb_typeof(n.metadata) = 'object'
  AND n.metadata ? 'conversationId'
  AND n.metadata ->> 'conversationId' = d.duplicate_id::text;

WITH canonical_updates AS (
    SELECT canonical_id, MAX(updated_at) AS latest_updated_at
    FROM tmp_ranked_conversations
    GROUP BY canonical_id
)
UPDATE conversations c
SET updated_at = cu.latest_updated_at
FROM canonical_updates cu
WHERE c.id = cu.canonical_id
  AND cu.latest_updated_at IS NOT NULL
  AND (c.updated_at IS NULL OR c.updated_at < cu.latest_updated_at);

DELETE FROM conversations c
USING tmp_duplicate_conversations d
WHERE c.id = d.duplicate_id;

ALTER TABLE conversations
    ALTER COLUMN product_id SET NOT NULL;

ALTER TABLE conversations
    ALTER COLUMN buyer_id SET NOT NULL;

ALTER TABLE conversations
    ALTER COLUMN seller_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_conversations_product_buyer_seller'
    ) THEN
        ALTER TABLE conversations
            ADD CONSTRAINT uq_conversations_product_buyer_seller
                UNIQUE (product_id, buyer_id, seller_id);
    END IF;
END $$;
