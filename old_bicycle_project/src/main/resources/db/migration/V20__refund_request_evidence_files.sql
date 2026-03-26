CREATE TABLE IF NOT EXISTS refund_request_files (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    refund_request_id UUID NOT NULL REFERENCES refund_requests(id) ON DELETE CASCADE,
    file_url TEXT NOT NULL,
    file_name VARCHAR(255),
    content_type VARCHAR(120),
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_refund_request_files_refund_request_id
    ON refund_request_files(refund_request_id);
