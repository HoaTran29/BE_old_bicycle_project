ALTER TYPE payment_status ADD VALUE IF NOT EXISTS 'expired';

ALTER TABLE payments
ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(40),
ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_orders_payment_deadline_pending
ON orders(payment_deadline)
WHERE status = 'pending' AND funding_status = 'awaiting_payment';
