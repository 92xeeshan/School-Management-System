SET app.bypass_rls = 'true';

INSERT INTO permission (code, module, name, description)
SELECT * FROM (VALUES
    ('MARKSHEET_READ', 'academics', 'View marksheets', 'View, print and download official examination marksheets'),
    ('MARKSHEET_MANAGE', 'academics', 'Manage marksheets', 'Publish, lock and override official examination marksheets')
) AS p(code, module, name, description)
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = p.code);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code = 'MARKSHEET_READ'
  AND r.code IN ('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT', 'PARENT')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code = 'MARKSHEET_MANAGE'
  AND r.code IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

CREATE TABLE marksheet (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id        UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    academic_year_id UUID NOT NULL REFERENCES academic_year(id) ON DELETE CASCADE,
    class_id         UUID NOT NULL REFERENCES school_class(id) ON DELETE CASCADE,
    section_id       UUID NOT NULL REFERENCES section(id) ON DELETE CASCADE,
    student_id       UUID NOT NULL REFERENCES student(id) ON DELETE CASCADE,
    exam_term        VARCHAR(40) NOT NULL,
    serial_no        VARCHAR(80) NOT NULL,
    published        BOOLEAN NOT NULL DEFAULT false,
    locked           BOOLEAN NOT NULL DEFAULT false,
    issued_at        DATE,
    published_at     TIMESTAMPTZ,
    published_by     UUID,
    locked_at        TIMESTAMPTZ,
    locked_by        UUID,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_marksheet UNIQUE (school_id, academic_year_id, student_id, exam_term),
    CONSTRAINT uq_marksheet_serial UNIQUE (school_id, serial_no),
    CONSTRAINT chk_marksheet_term CHECK (exam_term IN ('QUIZ', 'UNIT', 'MIDTERM', 'TERM', 'FINAL', 'CONTINUOUS'))
);
CREATE INDEX idx_marksheet_section ON marksheet(school_id, section_id, exam_term);

ALTER TABLE marksheet ENABLE ROW LEVEL SECURITY;
ALTER TABLE marksheet FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON marksheet
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

GRANT SELECT, INSERT, UPDATE, DELETE ON marksheet TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON marksheet TO app_admin;

WITH target AS (
    SELECT e.school_id, e.academic_year_id, sec.class_id, e.section_id, y.name AS year_name
    FROM student s
    JOIN student_enrollment e ON e.student_id = s.id AND e.status = 'ACTIVE'
    JOIN academic_year y ON y.id = e.academic_year_id AND y.is_current
    JOIN section sec ON sec.id = e.section_id
    WHERE s.admission_no = 'ADM0001'
    LIMIT 1
),
roster AS (
    SELECT se.student_id, st.admission_no, se.roll_number,
           ROW_NUMBER() OVER (ORDER BY se.roll_number NULLS LAST, se.student_id) AS idx
    FROM student_enrollment se
    JOIN student st ON st.id = se.student_id
    JOIN target t ON t.section_id = se.section_id AND t.academic_year_id = se.academic_year_id
    WHERE se.status = 'ACTIVE'
    LIMIT 16
)
INSERT INTO marksheet (
    school_id, academic_year_id, class_id, section_id, student_id, exam_term,
    serial_no, published, locked, issued_at, published_at)
SELECT
    t.school_id,
    t.academic_year_id,
    t.class_id,
    t.section_id,
    r.student_id,
    'TERM',
    'MS-' || replace(t.year_name, ' ', '') || '-' || r.admission_no || '-TERM',
    true,
    false,
    CURRENT_DATE,
    now()
FROM roster r
CROSS JOIN target t
ON CONFLICT (school_id, academic_year_id, student_id, exam_term) DO NOTHING;
