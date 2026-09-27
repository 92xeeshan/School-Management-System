SET app.bypass_rls = 'true';

CREATE TABLE notifications (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id     UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    recipient_id  UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    title         VARCHAR(255) NOT NULL,
    message       TEXT NOT NULL,
    category      VARCHAR(50) NOT NULL,
    action_url    VARCHAR(500),
    source_key    VARCHAR(200),
    is_read       BOOLEAN NOT NULL DEFAULT false,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at       TIMESTAMPTZ,
    CONSTRAINT chk_notifications_category CHECK (category IN ('EXAM', 'ATTENDANCE', 'FEE', 'ASSIGNMENT'))
);
CREATE INDEX idx_notifications_recipient ON notifications (school_id, recipient_id, is_read, created_at DESC);
CREATE UNIQUE INDEX uq_notifications_source
    ON notifications (school_id, recipient_id, source_key)
    WHERE source_key IS NOT NULL;

ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON notifications
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

GRANT SELECT, INSERT, UPDATE, DELETE ON notifications TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON notifications TO app_admin;

INSERT INTO notifications (
    id, school_id, recipient_id, title, message, category, action_url, source_key, is_read, created_at, read_at)
VALUES
    ('a1000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000002',
     'Marks submitted for review',
     'Class 7-A marks submitted by Asha Sharma for review.',
     'EXAM', '/downloads/marksheet', NULL, false, now() - interval '12 minutes', NULL),
    ('a1000000-0000-0000-0000-000000000002',
     '20000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000003',
     'Marks entry deadline',
     'Reminder: Marks submission for Term Examinations closes in 24 hours.',
     'EXAM', '/examinations/marks', 'SEED:MARKS_DEADLINE:TEACHER', false, now() - interval '2 hours', NULL),
    ('a1000000-0000-0000-0000-000000000003',
     '20000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000003',
     'Marks require revision',
     'Class 7-A marks require revision.',
     'EXAM', '/examinations/marks', NULL, false, now() - interval '1 day', NULL),
    ('a1000000-0000-0000-0000-000000000004',
     '20000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000005',
     'Attendance warning',
     'Attendance warning: Current attendance is 72%.',
     'ATTENDANCE', '/attendance', 'SEED:ATTENDANCE:STUDENT', false, now() - interval '3 hours', NULL),
    ('a1000000-0000-0000-0000-000000000005',
     '20000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000004',
     'Attendance warning',
     'Attendance warning: Current attendance is 72%.',
     'ATTENDANCE', '/attendance', 'SEED:ATTENDANCE:PARENT', false, now() - interval '3 hours', NULL),
    ('a1000000-0000-0000-0000-000000000006',
     '20000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000005',
     'Fee due',
     'Fee Due: Installment #2 of 400 is due on Oct 1.',
     'FEE', '/fees', 'SEED:FEE:STUDENT', false, now() - interval '6 hours', NULL),
    ('a1000000-0000-0000-0000-000000000007',
     '20000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000004',
     'Fee due',
     'Fee Due: Installment #2 of 400 is due on Oct 1.',
     'FEE', '/fees', 'SEED:FEE:PARENT', false, now() - interval '6 hours', NULL),
    ('a1000000-0000-0000-0000-000000000008',
     '20000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000005',
     'Mark sheet published',
     'Mark sheet published for Term Examinations.',
     'ASSIGNMENT', '/downloads', 'SEED:PUBLISHED:STUDENT', false, now() - interval '20 minutes', NULL),
    ('a1000000-0000-0000-0000-000000000009',
     '20000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000004',
     'Mark sheet published',
     'Mark sheet published for Term Examinations.',
     'ASSIGNMENT', '/downloads', 'SEED:PUBLISHED:PARENT', false, now() - interval '20 minutes', NULL),
    ('a1000000-0000-0000-0000-000000000010',
     '20000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000002',
     'Marks submitted for review',
     'Class 6-A marks submitted by Asha Sharma for review.',
     'EXAM', '/downloads/marksheet', 'SEED:SUBMIT:ADMIN:READ', true, now() - interval '3 days', now() - interval '2 days');
