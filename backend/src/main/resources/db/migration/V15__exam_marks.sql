SET app.bypass_rls = 'true';

INSERT INTO permission (code, module, name, description)
SELECT * FROM (VALUES
    ('EXAM_MARK', 'academics', 'Enter exam marks', 'Enter and submit student examination marks')
) AS p(code, module, name, description)
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = p.code);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code = 'EXAM_MARK'
  AND r.code IN ('SUPER_ADMIN', 'ADMIN', 'TEACHER')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

CREATE TABLE exam_entry (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    academic_year_id  UUID NOT NULL REFERENCES academic_year(id) ON DELETE CASCADE,
    class_id          UUID NOT NULL REFERENCES school_class(id) ON DELETE CASCADE,
    section_id        UUID NOT NULL REFERENCES section(id) ON DELETE CASCADE,
    subject_id        UUID NOT NULL REFERENCES subject(id) ON DELETE CASCADE,
    exam_term         VARCHAR(40) NOT NULL,
    max_theory        NUMERIC(8,2) NOT NULL DEFAULT 80,
    max_practical     NUMERIC(8,2) NOT NULL DEFAULT 0,
    max_assignment    NUMERIC(8,2) NOT NULL DEFAULT 20,
    entry_deadline    DATE,
    locked            BOOLEAN NOT NULL DEFAULT false,
    manual_unlock     BOOLEAN NOT NULL DEFAULT false,
    locked_at         TIMESTAMPTZ,
    locked_by         UUID,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_exam_entry UNIQUE (school_id, academic_year_id, section_id, subject_id, exam_term),
    CONSTRAINT chk_exam_entry_term CHECK (exam_term IN ('QUIZ', 'UNIT', 'MIDTERM', 'TERM', 'FINAL', 'CONTINUOUS')),
    CONSTRAINT chk_exam_entry_max CHECK (max_theory >= 0 AND max_practical >= 0 AND max_assignment >= 0)
);

CREATE TABLE exam_mark (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    exam_entry_id       UUID NOT NULL REFERENCES exam_entry(id) ON DELETE CASCADE,
    student_id          UUID NOT NULL REFERENCES student(id) ON DELETE CASCADE,
    theory              NUMERIC(8,2),
    practical           NUMERIC(8,2),
    assignment          NUMERIC(8,2),
    attendance_status   VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    remarks             VARCHAR(500),
    total               NUMERIC(8,2),
    percentage          NUMERIC(5,2),
    grade_label         VARCHAR(20),
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_exam_mark UNIQUE (exam_entry_id, student_id),
    CONSTRAINT chk_exam_mark_attendance CHECK (attendance_status IN ('PRESENT', 'ABSENT', 'EXCUSED')),
    CONSTRAINT chk_exam_mark_status CHECK (status IN ('DRAFT', 'SUBMITTED'))
);

ALTER TABLE exam_entry ENABLE ROW LEVEL SECURITY;
ALTER TABLE exam_entry FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON exam_entry
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE exam_mark ENABLE ROW LEVEL SECURITY;
ALTER TABLE exam_mark FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON exam_mark
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

GRANT SELECT, INSERT, UPDATE, DELETE ON exam_entry TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON exam_entry TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON exam_mark TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON exam_mark TO app_admin;
