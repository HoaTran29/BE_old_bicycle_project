CREATE TABLE IF NOT EXISTS order_evidence_submissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    submitted_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    submitted_by_role VARCHAR(40) NOT NULL,
    evidence_type VARCHAR(40) NOT NULL,
    note TEXT,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    CONSTRAINT uk_order_evidence_submission_order_type UNIQUE (order_id, evidence_type)
);

CREATE TABLE IF NOT EXISTS order_evidence_files (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    submission_id UUID NOT NULL REFERENCES order_evidence_submissions(id) ON DELETE CASCADE,
    file_url TEXT NOT NULL,
    file_name VARCHAR(255),
    content_type VARCHAR(120),
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_order_evidence_submissions_order_id
    ON order_evidence_submissions(order_id);

CREATE INDEX IF NOT EXISTS idx_order_evidence_submissions_type
    ON order_evidence_submissions(evidence_type);

CREATE INDEX IF NOT EXISTS idx_order_evidence_files_submission_id
    ON order_evidence_files(submission_id);
