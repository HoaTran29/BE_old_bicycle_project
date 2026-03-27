DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_type
        WHERE typname = 'platform_fee_status'
    ) THEN
        CREATE TYPE platform_fee_status AS ENUM (
            'not_applicable',
            'pending',
            'recognized',
            'reversed'
        );
    END IF;
END $$;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS fee_base_amount NUMERIC NOT NULL DEFAULT 0;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS platform_fee_rate NUMERIC(5,4) NOT NULL DEFAULT 0;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS platform_fee_total NUMERIC NOT NULL DEFAULT 0;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS buyer_fee_amount NUMERIC NOT NULL DEFAULT 0;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS seller_fee_amount NUMERIC NOT NULL DEFAULT 0;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS buyer_charge_amount NUMERIC NOT NULL DEFAULT 0;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS seller_gross_payout_amount NUMERIC NOT NULL DEFAULT 0;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS seller_net_payout_amount NUMERIC NOT NULL DEFAULT 0;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS platform_fee_status platform_fee_status NOT NULL DEFAULT 'not_applicable';

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS platform_fee_recognized_at TIMESTAMP;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS platform_fee_reversed_at TIMESTAMP;

UPDATE orders
SET fee_base_amount = total_amount
WHERE fee_base_amount = 0
  AND total_amount IS NOT NULL;

UPDATE orders
SET buyer_charge_amount = COALESCE(required_upfront_amount, deposit_amount, total_amount, 0)
WHERE buyer_charge_amount = 0;

UPDATE orders
SET seller_gross_payout_amount = COALESCE(required_upfront_amount, deposit_amount, paid_amount, 0),
    seller_net_payout_amount = COALESCE(required_upfront_amount, deposit_amount, paid_amount, 0)
WHERE seller_gross_payout_amount = 0
   OR seller_net_payout_amount = 0;

CREATE INDEX IF NOT EXISTS idx_orders_platform_fee_status
ON orders(platform_fee_status);

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS protected_amount NUMERIC NOT NULL DEFAULT 0;

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS buyer_fee_amount NUMERIC NOT NULL DEFAULT 0;

UPDATE payments
SET protected_amount = amount
WHERE protected_amount = 0;

ALTER TABLE payouts
    ADD COLUMN IF NOT EXISTS gross_amount NUMERIC NOT NULL DEFAULT 0;

ALTER TABLE payouts
    ADD COLUMN IF NOT EXISTS fee_deduction_amount NUMERIC NOT NULL DEFAULT 0;

ALTER TABLE payouts
    ADD COLUMN IF NOT EXISTS net_amount NUMERIC NOT NULL DEFAULT 0;

UPDATE payouts
SET gross_amount = amount,
    net_amount = amount
WHERE gross_amount = 0
   OR net_amount = 0;

CREATE TABLE IF NOT EXISTS financial_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID REFERENCES orders(id) ON DELETE SET NULL,
    payment_id UUID REFERENCES payments(id) ON DELETE SET NULL,
    payout_id UUID REFERENCES payouts(id) ON DELETE SET NULL,
    refund_request_id UUID REFERENCES refund_requests(id) ON DELETE SET NULL,
    entry_type VARCHAR(50) NOT NULL,
    amount NUMERIC NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    note TEXT,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_financial_transactions_order_id
ON financial_transactions(order_id);

CREATE INDEX IF NOT EXISTS idx_financial_transactions_payment_id
ON financial_transactions(payment_id);

CREATE INDEX IF NOT EXISTS idx_financial_transactions_payout_id
ON financial_transactions(payout_id);

CREATE INDEX IF NOT EXISTS idx_financial_transactions_refund_request_id
ON financial_transactions(refund_request_id);

CREATE INDEX IF NOT EXISTS idx_financial_transactions_created_at
ON financial_transactions(created_at);
