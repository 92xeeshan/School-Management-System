SET app.bypass_rls = 'true';

INSERT INTO permission (code, module, name, description)
SELECT * FROM (VALUES
    ('REPORT_CARD_READ', 'academics', 'View report cards', 'View, print and download student report cards')
) AS p(code, module, name, description)
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = p.code);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code = 'REPORT_CARD_READ'
  AND r.code IN ('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT', 'PARENT')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

CREATE TABLE report_card (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id             UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    academic_year_id      UUID NOT NULL REFERENCES academic_year(id) ON DELETE CASCADE,
    class_id              UUID NOT NULL REFERENCES school_class(id) ON DELETE CASCADE,
    section_id            UUID NOT NULL REFERENCES section(id) ON DELETE CASCADE,
    student_id            UUID NOT NULL REFERENCES student(id) ON DELETE CASCADE,
    exam_term             VARCHAR(40) NOT NULL,
    teacher_comment       VARCHAR(300),
    principal_comment     VARCHAR(300),
    behaviour_conduct     VARCHAR(40) NOT NULL DEFAULT 'GOOD',
    behaviour_discipline  VARCHAR(40) NOT NULL DEFAULT 'GOOD',
    behaviour_punctuality VARCHAR(40) NOT NULL DEFAULT 'GOOD',
    co_curricular         VARCHAR(300),
    published             BOOLEAN NOT NULL DEFAULT false,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_report_card UNIQUE (school_id, academic_year_id, student_id, exam_term),
    CONSTRAINT chk_report_card_term CHECK (exam_term IN ('QUIZ', 'UNIT', 'MIDTERM', 'TERM', 'FINAL', 'CONTINUOUS')),
    CONSTRAINT chk_report_card_conduct CHECK (behaviour_conduct IN ('EXCELLENT', 'GOOD', 'SATISFACTORY', 'NEEDS_IMPROVEMENT')),
    CONSTRAINT chk_report_card_discipline CHECK (behaviour_discipline IN ('EXCELLENT', 'GOOD', 'SATISFACTORY', 'NEEDS_IMPROVEMENT')),
    CONSTRAINT chk_report_card_punctuality CHECK (behaviour_punctuality IN ('EXCELLENT', 'GOOD', 'SATISFACTORY', 'NEEDS_IMPROVEMENT'))
);
CREATE INDEX idx_report_card_section ON report_card(school_id, section_id, exam_term);

ALTER TABLE report_card ENABLE ROW LEVEL SECURITY;
ALTER TABLE report_card FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON report_card
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

GRANT SELECT, INSERT, UPDATE, DELETE ON report_card TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON report_card TO app_admin;

WITH target AS (
    SELECT e.school_id, e.academic_year_id, sec.class_id, e.section_id, e.student_id
    FROM student s
    JOIN student_enrollment e ON e.student_id = s.id AND e.status = 'ACTIVE'
    JOIN academic_year y ON y.id = e.academic_year_id AND y.is_current
    JOIN section sec ON sec.id = e.section_id
    WHERE s.admission_no = 'ADM0001'
    LIMIT 1
)
INSERT INTO exam_entry (
    school_id, academic_year_id, class_id, section_id, subject_id, exam_term,
    max_theory, max_practical, max_assignment, locked)
SELECT
    t.school_id,
    t.academic_year_id,
    t.class_id,
    t.section_id,
    subj.id,
    'TERM',
    80,
    0,
    20,
    true
FROM target t
JOIN subject subj ON subj.school_id = t.school_id
JOIN class_subject cs ON cs.class_id = t.class_id AND cs.subject_id = subj.id
ON CONFLICT (school_id, academic_year_id, section_id, subject_id, exam_term) DO NOTHING;

WITH target AS (
    SELECT e.school_id, e.academic_year_id, e.section_id
    FROM student s
    JOIN student_enrollment e ON e.student_id = s.id AND e.status = 'ACTIVE'
    JOIN academic_year y ON y.id = e.academic_year_id AND y.is_current
    WHERE s.admission_no = 'ADM0001'
    LIMIT 1
),
roster AS (
    SELECT se.student_id,
           ROW_NUMBER() OVER (ORDER BY se.roll_number NULLS LAST, se.student_id) AS idx
    FROM student_enrollment se
    JOIN target t ON t.section_id = se.section_id AND t.academic_year_id = se.academic_year_id
    WHERE se.status = 'ACTIVE'
    LIMIT 16
),
subjects AS (
    SELECT e.id AS exam_entry_id,
           e.school_id,
           e.subject_id,
           ROW_NUMBER() OVER (ORDER BY e.subject_id) AS sidx
    FROM exam_entry e
    JOIN target t ON t.school_id = e.school_id
                 AND t.academic_year_id = e.academic_year_id
                 AND t.section_id = e.section_id
    WHERE e.exam_term = 'TERM'
)
INSERT INTO exam_mark (
    school_id, exam_entry_id, student_id, theory, practical, assignment,
    attendance_status, remarks, total, percentage, grade_label, status)
SELECT
    sub.school_id,
    sub.exam_entry_id,
    r.student_id,
    GREATEST(48, 78 - ((r.idx + sub.sidx) % 7) * 3)::numeric,
    0,
    GREATEST(12, 19 - ((r.idx + sub.sidx) % 5))::numeric,
    'PRESENT',
    CASE (sub.sidx % 4)
        WHEN 1 THEN 'Consistent and accurate work.'
        WHEN 2 THEN 'Reads fluently with good expression.'
        WHEN 3 THEN 'Shows curiosity in class activities.'
        ELSE 'Neat work and regular effort this term.'
    END,
    GREATEST(48, 78 - ((r.idx + sub.sidx) % 7) * 3)
        + GREATEST(12, 19 - ((r.idx + sub.sidx) % 5)),
    ROUND(((GREATEST(48, 78 - ((r.idx + sub.sidx) % 7) * 3)
        + GREATEST(12, 19 - ((r.idx + sub.sidx) % 5))) / 100.0) * 100, 2),
    CASE
        WHEN (GREATEST(48, 78 - ((r.idx + sub.sidx) % 7) * 3)
            + GREATEST(12, 19 - ((r.idx + sub.sidx) % 5))) >= 80 THEN 'A'
        WHEN (GREATEST(48, 78 - ((r.idx + sub.sidx) % 7) * 3)
            + GREATEST(12, 19 - ((r.idx + sub.sidx) % 5))) >= 60 THEN 'B'
        ELSE 'C'
    END,
    'SUBMITTED'
FROM roster r
CROSS JOIN subjects sub
ON CONFLICT (exam_entry_id, student_id) DO NOTHING;

WITH target AS (
    SELECT e.school_id, e.academic_year_id, sec.class_id, e.section_id
    FROM student s
    JOIN student_enrollment e ON e.student_id = s.id AND e.status = 'ACTIVE'
    JOIN academic_year y ON y.id = e.academic_year_id AND y.is_current
    JOIN section sec ON sec.id = e.section_id
    WHERE s.admission_no = 'ADM0001'
    LIMIT 1
),
roster AS (
    SELECT se.student_id, st.first_name,
           ROW_NUMBER() OVER (ORDER BY se.roll_number NULLS LAST, se.student_id) AS idx
    FROM student_enrollment se
    JOIN student st ON st.id = se.student_id
    JOIN target t ON t.section_id = se.section_id AND t.academic_year_id = se.academic_year_id
    WHERE se.status = 'ACTIVE'
    LIMIT 16
)
INSERT INTO report_card (
    school_id, academic_year_id, class_id, section_id, student_id, exam_term,
    teacher_comment, principal_comment, behaviour_conduct, behaviour_discipline,
    behaviour_punctuality, co_curricular, published)
SELECT
    t.school_id,
    t.academic_year_id,
    t.class_id,
    t.section_id,
    r.student_id,
    'TERM',
    LEFT(r.first_name || ' is a sincere learner who participates well in class discussions and maintains neat work. Continue reading daily to strengthen vocabulary.', 300),
    'Progress is noted. Keep a balanced effort in academics and co-curricular activities this year.',
    CASE WHEN r.idx % 3 = 1 THEN 'EXCELLENT' ELSE 'GOOD' END,
    CASE WHEN r.idx % 4 = 0 THEN 'SATISFACTORY' ELSE 'GOOD' END,
    CASE WHEN r.idx % 5 = 0 THEN 'GOOD' ELSE 'EXCELLENT' END,
    CASE WHEN r.idx = 1 THEN 'Mathematics Olympiad Champion; active in the school science club.'
         WHEN r.idx = 2 THEN 'Best Science Project; member of the school choir.'
         ELSE 'Participated in inter-house sports and art week.' END,
    true
FROM roster r
CROSS JOIN target t
ON CONFLICT (school_id, academic_year_id, student_id, exam_term) DO NOTHING;
