-- A subset of notifications was persisted with a +06 wall-clock offset into a
-- timezone-naive timestamp column. Those rows appear in the future, which
-- breaks notification ordering and relative-time labels on the clients.
UPDATE notifications
SET created_at = created_at - interval '6 hours'
WHERE created_at > now() + interval '5 minutes';
