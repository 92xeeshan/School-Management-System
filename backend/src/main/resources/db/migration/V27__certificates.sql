SET app.bypass_rls = 'true';

INSERT INTO permission (code, module, name, description)
SELECT * FROM (VALUES
    ('CERTIFICATE_READ',     'certificates', 'View certificates',     'View and download issued student certificates'),
    ('CERTIFICATE_GENERATE', 'certificates', 'Generate certificates', 'Generate transfer and bonafide certificates'),
    ('CERTIFICATE_MANAGE',   'certificates', 'Manage certificates',   'Edit certificate templates and school letterhead'),
    ('CERTIFICATE_APPROVE',  'certificates', 'Approve certificates',  'Approve class-teacher certificate requests')
) AS p(code, module, name, description)
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = p.code);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code IN ('CERTIFICATE_READ', 'CERTIFICATE_GENERATE', 'CERTIFICATE_MANAGE', 'CERTIFICATE_APPROVE')
  AND r.code IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code IN ('CERTIFICATE_READ', 'CERTIFICATE_GENERATE')
  AND r.code = 'TEACHER'
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code = 'CERTIFICATE_READ'
  AND r.code IN ('STUDENT', 'PARENT')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

CREATE TABLE certificate_template (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id            UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    type                 VARCHAR(20) NOT NULL,
    header_html          TEXT,
    footer_html          TEXT,
    signature_image_url  VARCHAR(500),
    seal_image_url       VARCHAR(500),
    is_active            BOOLEAN NOT NULL DEFAULT true,
    requires_approval    BOOLEAN NOT NULL DEFAULT true,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_certificate_template UNIQUE (school_id, type),
    CONSTRAINT chk_certificate_template_type CHECK (type IN ('TC', 'BONAFIDE'))
);

CREATE TABLE certificate_issued (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    student_id          UUID NOT NULL REFERENCES student(id) ON DELETE CASCADE,
    template_id         UUID NOT NULL REFERENCES certificate_template(id),
    certificate_type    VARCHAR(20) NOT NULL,
    certificate_no      VARCHAR(80),
    sequence_year       INTEGER,
    issued_date         DATE,
    issued_by_user_id   UUID NOT NULL REFERENCES app_user(id),
    approved_by_user_id UUID REFERENCES app_user(id),
    approved_at         TIMESTAMPTZ,
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    reason              VARCHAR(500),
    conduct_remarks     VARCHAR(500),
    data_json           TEXT,
    pdf_object_key      VARCHAR(500),
    is_duplicate        BOOLEAN NOT NULL DEFAULT false,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_certificate_issued_type CHECK (certificate_type IN ('TC', 'BONAFIDE')),
    CONSTRAINT chk_certificate_issued_status CHECK (status IN ('DRAFT', 'APPROVED', 'ISSUED')),
    CONSTRAINT uq_certificate_no UNIQUE (school_id, certificate_no)
);

CREATE INDEX idx_certificate_issued_student ON certificate_issued (school_id, student_id, created_at DESC);
CREATE INDEX idx_certificate_issued_status ON certificate_issued (school_id, status, issued_date DESC);
CREATE INDEX idx_certificate_issued_type_year ON certificate_issued (school_id, certificate_type, sequence_year);

ALTER TABLE certificate_template ENABLE ROW LEVEL SECURITY;
ALTER TABLE certificate_template FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON certificate_template
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE certificate_issued ENABLE ROW LEVEL SECURITY;
ALTER TABLE certificate_issued FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON certificate_issued
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

GRANT SELECT, INSERT, UPDATE, DELETE ON certificate_template TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON certificate_template TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON certificate_issued TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON certificate_issued TO app_admin;

INSERT INTO certificate_template (id, school_id, type, header_html, footer_html, is_active, requires_approval)
VALUES
    ('70000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000001',
     'TC',
     'Demo Public School',
     'This is a computer generated certificate. Principal signature and school seal to be affixed after printing.',
     true, true),
    ('70000000-0000-0000-0000-000000000002',
     '20000000-0000-0000-0000-000000000001',
     'BONAFIDE',
     'Demo Public School',
     'This is a computer generated certificate. Principal signature and school seal to be affixed after printing.',
     true, true);
