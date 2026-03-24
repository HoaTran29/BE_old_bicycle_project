alter table reviews
    add column if not exists seller_reply text,
    add column if not exists seller_replied_at timestamp;
