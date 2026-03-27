DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_enum enum_value
        JOIN pg_type enum_type ON enum_type.oid = enum_value.enumtypid
        WHERE enum_type.typname = 'report_status'
          AND enum_value.enumlabel = 'reviewed'
    ) THEN
        ALTER TYPE report_status RENAME VALUE 'reviewed' TO 'investigating';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_enum enum_value
        JOIN pg_type enum_type ON enum_type.oid = enum_value.enumtypid
        WHERE enum_type.typname = 'report_status'
          AND enum_value.enumlabel = 'resolved'
    ) THEN
        ALTER TYPE report_status RENAME VALUE 'resolved' TO 'resolved_upheld';
    END IF;
END $$;

ALTER TYPE report_status ADD VALUE IF NOT EXISTS 'resolved_dismissed';
