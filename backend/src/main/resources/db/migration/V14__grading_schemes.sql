SET app.bypass_rls = 'true';

INSERT INTO permission (code, module, name, description)
SELECT * FROM (VALUES
    ('EXAM_READ',   'academics', 'Read grading schemes',   'View examination and grading schemes'),
    ('EXAM_MANAGE', 'academics', 'Manage grading schemes', 'Create and edit examination and grading schemes')
) AS p(code, module, name, description)
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = p.code);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code IN ('EXAM_READ', 'EXAM_MANAGE')
  AND r.code IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code = 'EXAM_READ'
  AND r.code = 'TEACHER'
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

CREATE TABLE grading_scheme (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id             UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    academic_year_id      UUID NOT NULL REFERENCES academic_year(id) ON DELETE CASCADE,
    class_id              UUID NOT NULL REFERENCES school_class(id) ON DELETE CASCADE,
    name                  VARCHAR(80) NOT NULL,
    academic_level        VARCHAR(40) NOT NULL DEFAULT 'PRIMARY',
    exam_type             VARCHAR(40) NOT NULL DEFAULT 'TERM',
    scale_type            VARCHAR(20) NOT NULL DEFAULT 'LETTER',
    pass_marks            NUMERIC(8,2) NOT NULL,
    pass_percent          NUMERIC(5,2) NOT NULL,
    max_marks             NUMERIC(8,2) NOT NULL,
    evaluation_criteria   VARCHAR(500),
    status                VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_grading_scheme_name UNIQUE (school_id, academic_year_id, class_id, name),
    CONSTRAINT chk_grading_scheme_scale CHECK (scale_type IN ('PERCENTAGE', 'LETTER', 'GPA')),
    CONSTRAINT chk_grading_scheme_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_grading_scheme_level CHECK (academic_level IN ('PRIMARY', 'MIDDLE', 'SECONDARY', 'SENIOR')),
    CONSTRAINT chk_grading_scheme_exam CHECK (exam_type IN ('QUIZ', 'UNIT', 'MIDTERM', 'TERM', 'FINAL', 'CONTINUOUS')),
    CONSTRAINT chk_grading_scheme_marks CHECK (pass_marks >= 0 AND max_marks > 0 AND pass_marks <= max_marks),
    CONSTRAINT chk_grading_scheme_percent CHECK (pass_percent >= 0 AND pass_percent <= 100)
);

CREATE UNIQUE INDEX uq_grading_scheme_active
    ON grading_scheme (school_id, academic_year_id, class_id)
    WHERE status = 'ACTIVE';

CREATE TABLE grade_boundary (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id    UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    scheme_id    UUID NOT NULL REFERENCES grading_scheme(id) ON DELETE CASCADE,
    label        VARCHAR(20) NOT NULL,
    min_percent  NUMERIC(5,2) NOT NULL,
    max_percent  NUMERIC(5,2) NOT NULL,
    gpa_value    NUMERIC(4,2),
    sort_order   INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_grade_boundary_range CHECK (min_percent >= 0 AND max_percent <= 100 AND min_percent <= max_percent)
);

CREATE TABLE assessment_weightage (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id        UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    scheme_id        UUID NOT NULL REFERENCES grading_scheme(id) ON DELETE CASCADE,
    assessment_type  VARCHAR(40) NOT NULL,
    weight_percent   NUMERIC(5,2) NOT NULL,
    CONSTRAINT uq_assessment_weightage UNIQUE (scheme_id, assessment_type),
    CONSTRAINT chk_assessment_type CHECK (assessment_type IN ('QUIZ', 'ASSIGNMENT', 'MIDTERM', 'PRACTICAL', 'FINAL')),
    CONSTRAINT chk_weight_percent CHECK (weight_percent > 0 AND weight_percent <= 100)
);

ALTER TABLE grading_scheme ENABLE ROW LEVEL SECURITY;
ALTER TABLE grading_scheme FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON grading_scheme
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE grade_boundary ENABLE ROW LEVEL SECURITY;
ALTER TABLE grade_boundary FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON grade_boundary
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE assessment_weightage ENABLE ROW LEVEL SECURITY;
ALTER TABLE assessment_weightage FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON assessment_weightage
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

GRANT SELECT, INSERT, UPDATE, DELETE ON grading_scheme TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON grading_scheme TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON grade_boundary TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON grade_boundary TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON assessment_weightage TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON assessment_weightage TO app_admin;

INSERT INTO grading_scheme (
    school_id, academic_year_id, class_id, name, academic_level, exam_type,
    scale_type, pass_marks, pass_percent, max_marks, evaluation_criteria, status)
SELECT
    c.school_id,
    y.id,
    c.id,
    'Class letter scheme',
    'PRIMARY',
    'TERM',
    'LETTER',
    33,
    33,
    100,
    'Term evaluation with letter grades A to F.',
    'ACTIVE'
FROM school_class c
JOIN academic_year y ON y.school_id = c.school_id AND y.is_current = true
ORDER BY c.sort_order, c.name
LIMIT 1
ON CONFLICT DO NOTHING;

INSERT INTO grade_boundary (school_id, scheme_id, label, min_percent, max_percent, gpa_value, sort_order)
SELECT s.school_id, s.id, b.label, b.min_percent, b.max_percent, b.gpa_value, b.sort_order
FROM grading_scheme s
CROSS JOIN (
    VALUES
        ('A', 80::numeric, 100::numeric, 4.0::numeric, 1),
        ('B', 60::numeric, 79.99::numeric, 3.0::numeric, 2),
        ('C', 45::numeric, 59.99::numeric, 2.0::numeric, 3),
        ('D', 33::numeric, 44.99::numeric, 1.0::numeric, 4),
        ('F', 0::numeric, 32.99::numeric, 0.0::numeric, 5)
) AS b(label, min_percent, max_percent, gpa_value, sort_order)
WHERE s.name = 'Class letter scheme'
ON CONFLICT DO NOTHING;

INSERT INTO assessment_weightage (school_id, scheme_id, assessment_type, weight_percent)
SELECT s.school_id, s.id, w.assessment_type, w.weight_percent
FROM grading_scheme s
CROSS JOIN (
    VALUES
        ('QUIZ', 20::numeric),
        ('MIDTERM', 30::numeric),
        ('FINAL', 50::numeric)
) AS w(assessment_type, weight_percent)
WHERE s.name = 'Class letter scheme'
ON CONFLICT DO NOTHING;
