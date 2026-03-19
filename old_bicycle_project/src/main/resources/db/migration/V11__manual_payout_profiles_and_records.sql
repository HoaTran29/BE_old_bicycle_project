CREATE TABLE IF NOT EXISTS payout_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    bank_code VARCHAR(50) NOT NULL,
    bank_bin VARCHAR(20) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE IF NOT EXISTS payouts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    order_id UUID REFERENCES orders(id) ON DELETE SET NULL,
    refund_request_id UUID REFERENCES refund_requests(id) ON DELETE SET NULL,
    type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'profile_required',
    provider VARCHAR(40) NOT NULL DEFAULT 'vietqr_manual',
    amount NUMERIC NOT NULL,
    bank_code VARCHAR(50),
    bank_bin VARCHAR(20),
    account_number VARCHAR(50),
    account_name VARCHAR(255),
    transfer_content VARCHAR(120),
    qr_code_url TEXT,
    bank_reference VARCHAR(255),
    admin_note TEXT,
    completed_by UUID REFERENCES users(id),
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_payouts_refund_request_unique
ON payouts(refund_request_id)
WHERE refund_request_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_payouts_order_type_unique
ON payouts(order_id, type)
WHERE order_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payouts_recipient_id ON payouts(recipient_id);
CREATE INDEX IF NOT EXISTS idx_payouts_status ON payouts(status);
CREATE INDEX IF NOT EXISTS idx_payouts_type ON payouts(type);
