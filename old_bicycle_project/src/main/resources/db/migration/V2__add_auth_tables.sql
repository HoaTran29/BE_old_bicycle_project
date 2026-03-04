-- Bảng lưu Refresh Token (stateful, có thể revoke)
CREATE TABLE refresh_tokens (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token varchar UNIQUE NOT NULL,
    expires_at timestamp NOT NULL,
    created_at timestamp DEFAULT now()
);

-- Bảng lưu Email Verification Token
CREATE TABLE email_verifications (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token varchar UNIQUE NOT NULL,
    expires_at timestamp NOT NULL,
    created_at timestamp DEFAULT now()
);

-- Index để tăng performance tìm kiếm theo token
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_email_verifications_token ON email_verifications(token);
CREATE INDEX idx_email_verifications_user_id ON email_verifications(user_id);
