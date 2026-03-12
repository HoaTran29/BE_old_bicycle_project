ALTER TABLE orders
ADD COLUMN IF NOT EXISTS required_upfront_amount NUMERIC,
ADD COLUMN IF NOT EXISTS paid_amount NUMERIC DEFAULT 0,
ADD COLUMN IF NOT EXISTS remaining_amount NUMERIC,
ADD COLUMN IF NOT EXISTS payment_option VARCHAR(20) DEFAULT 'partial',
ADD COLUMN IF NOT EXISTS funding_status VARCHAR(30) DEFAULT 'unpaid',
ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS payment_deadline TIMESTAMP;

UPDATE orders
SET required_upfront_amount = COALESCE(required_upfront_amount, deposit_amount, 0),
    paid_amount = COALESCE(
        paid_amount,
        CASE
            WHEN status IN ('deposited', 'completed') THEN COALESCE(deposit_amount, 0)
            ELSE 0
        END
    ),
    remaining_amount = COALESCE(
        remaining_amount,
        GREATEST(
            COALESCE(total_amount, 0) - CASE
                WHEN status IN ('deposited', 'completed') THEN COALESCE(deposit_amount, 0)
                ELSE 0
            END,
            0
        )
    ),
    payment_option = COALESCE(payment_option, 'partial'),
    funding_status = COALESCE(
        funding_status,
        CASE
            WHEN status = 'deposited' THEN 'held'
            WHEN status = 'completed' THEN 'released'
            ELSE 'unpaid'
        END
    );

ALTER TABLE payments
ADD COLUMN IF NOT EXISTS gateway VARCHAR(20) DEFAULT 'manual',
ADD COLUMN IF NOT EXISTS phase VARCHAR(20) DEFAULT 'upfront',
ADD COLUMN IF NOT EXISTS gateway_order_code VARCHAR(120),
ADD COLUMN IF NOT EXISTS checkout_url TEXT,
ADD COLUMN IF NOT EXISTS qr_code_url TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS idx_payments_gateway_order_code
ON payments(gateway_order_code)
WHERE gateway_order_code IS NOT NULL;

CREATE TABLE IF NOT EXISTS refund_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    requester_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount NUMERIC NOT NULL,
    reason TEXT NOT NULL,
    evidence_note TEXT,
    status VARCHAR(20) DEFAULT 'pending',
    admin_note TEXT,
    refund_reference VARCHAR(255),
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMP,
    processed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_refund_requests_order_id ON refund_requests(order_id);
CREATE INDEX IF NOT EXISTS idx_refund_requests_status ON refund_requests(status);
