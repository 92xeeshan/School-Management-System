SET app.bypass_rls = 'true';

ALTER TABLE certificate_template DROP CONSTRAINT IF EXISTS chk_certificate_template_type;
ALTER TABLE certificate_template
    ADD CONSTRAINT chk_certificate_template_type
    CHECK (type IN ('TC', 'BONAFIDE', 'CHARACTER', 'COURSE_COMPLETION'));

ALTER TABLE certificate_issued DROP CONSTRAINT IF EXISTS chk_certificate_issued_type;
ALTER TABLE certificate_issued
    ADD CONSTRAINT chk_certificate_issued_type
    CHECK (certificate_type IN ('TC', 'BONAFIDE', 'CHARACTER', 'COURSE_COMPLETION'));

ALTER TABLE certificate_issued DROP CONSTRAINT IF EXISTS chk_certificate_issued_status;
ALTER TABLE certificate_issued
    ADD CONSTRAINT chk_certificate_issued_status
    CHECK (status IN ('DRAFT', 'APPROVED', 'ISSUED'));

ALTER TABLE certificate_issued
    ADD COLUMN IF NOT EXISTS request_id UUID,
    ADD COLUMN IF NOT EXISTS academic_progress VARCHAR(500),
    ADD COLUMN IF NOT EXISTS last_exam_attended VARCHAR(200),
    ADD COLUMN IF NOT EXISTS dues_library BOOLEAN,
    ADD COLUMN IF NOT EXISTS dues_accounts BOOLEAN,
    ADD COLUMN IF NOT EXISTS dues_sports BOOLEAN,
    ADD COLUMN IF NOT EXISTS tc_issued_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deactivation_scheduled_at DATE,
    ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMPTZ;

CREATE TABLE certificate_request (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id              UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    student_id             UUID NOT NULL REFERENCES student(id) ON DELETE CASCADE,
    certificate_type       VARCHAR(20) NOT NULL,
    status                 VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    reason                 VARCHAR(500),
    conduct_remarks        VARCHAR(500),
    academic_progress      VARCHAR(500),
    last_exam_attended     VARCHAR(200),
    dues_library           BOOLEAN,
    dues_accounts          BOOLEAN,
    dues_sports            BOOLEAN,
    teacher_notes          VARCHAR(500),
    rejection_reason       VARCHAR(500),
    supporting_doc_key     VARCHAR(500),
    supporting_doc_name    VARCHAR(255),
    requested_by_user_id   UUID NOT NULL REFERENCES app_user(id),
    reviewed_by_user_id    UUID REFERENCES app_user(id),
    reviewed_at            TIMESTAMPTZ,
    approved_by_user_id    UUID REFERENCES app_user(id),
    approved_at            TIMESTAMPTZ,
    issued_id              UUID REFERENCES certificate_issued(id),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_certificate_request_type CHECK (certificate_type IN ('TC', 'BONAFIDE', 'CHARACTER', 'COURSE_COMPLETION')),
    CONSTRAINT chk_certificate_request_status CHECK (status IN ('SUBMITTED', 'TEACHER_REVIEWED', 'REJECTED', 'CANCELLED', 'ISSUED'))
);

CREATE INDEX idx_certificate_request_student ON certificate_request (school_id, student_id, created_at DESC);
CREATE INDEX idx_certificate_request_status ON certificate_request (school_id, status, created_at DESC);
CREATE UNIQUE INDEX uq_certificate_request_pending
    ON certificate_request (school_id, student_id, certificate_type)
    WHERE status IN ('SUBMITTED', 'TEACHER_REVIEWED');

ALTER TABLE certificate_issued
    ADD CONSTRAINT fk_certificate_issued_request
    FOREIGN KEY (request_id) REFERENCES certificate_request(id);

ALTER TABLE certificate_request ENABLE ROW LEVEL SECURITY;
ALTER TABLE certificate_request FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON certificate_request
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

GRANT SELECT, INSERT, UPDATE, DELETE ON certificate_request TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON certificate_request TO app_admin;

INSERT INTO certificate_template (id, school_id, type, header_html, footer_html, is_active, requires_approval)
VALUES
    ('70000000-0000-0000-0000-000000000003',
     '20000000-0000-0000-0000-000000000001',
     'CHARACTER',
     'Demo Public School',
     'This is a computer generated certificate. Principal signature and school seal to be affixed after printing.',
     true, true),
    ('70000000-0000-0000-0000-000000000004',
     '20000000-0000-0000-0000-000000000001',
     'COURSE_COMPLETION',
     'Demo Public School',
     'This is a computer generated certificate. Principal signature and school seal to be affixed after printing.',
     true, true)
ON CONFLICT DO NOTHING;

ALTER TABLE notifications DROP CONSTRAINT IF EXISTS chk_notifications_category;
ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_category
    CHECK (category IN ('EXAM', 'ATTENDANCE', 'FEE', 'ASSIGNMENT', 'CERTIFICATE'));
