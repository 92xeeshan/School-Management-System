-- =====================================================================
-- V35: Online exams / quizzes
--   quiz_question      - reusable question bank
--   quiz               - timed quiz assigned to a class/section/subject
--   quiz_item          - questions attached to a quiz
--   quiz_attempt       - student timed attempt
--   quiz_answer        - per-question responses and marks
--   quiz_offline_mark  - paper/offline marks for the same quiz
-- =====================================================================

SET app.bypass_rls = 'true';

-- ---- Permissions -----------------------------------------------------
INSERT INTO permission (code, module, name, description)
SELECT * FROM (VALUES
    ('QUIZ_READ',   'examinations', 'Read quizzes',   'View quizzes, take published quizzes and read results'),
    ('QUIZ_MANAGE', 'examinations', 'Manage quizzes', 'Create quizzes, question bank, offline marks and grade answers')
) AS p(code, module, name, description)
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = p.code);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code IN ('QUIZ_READ', 'QUIZ_MANAGE')
  AND r.code IN ('SUPER_ADMIN', 'ADMIN', 'TEACHER')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code = 'QUIZ_READ'
  AND r.code IN ('STUDENT', 'PARENT')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ---- Tables ----------------------------------------------------------
CREATE TABLE quiz_question (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    subject_id          UUID REFERENCES subject(id),
    class_id            UUID REFERENCES school_class(id),
    question_type       VARCHAR(20) NOT NULL,
    prompt              TEXT NOT NULL,
    options_json        TEXT,
    correct_answer      VARCHAR(500),
    marks               NUMERIC(8,2) NOT NULL DEFAULT 1,
    explanation         VARCHAR(1000),
    created_by_user_id  UUID REFERENCES app_user(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_quiz_question_type CHECK (question_type IN ('MCQ', 'TRUE_FALSE', 'SHORT_ANSWER')),
    CONSTRAINT chk_quiz_question_marks CHECK (marks > 0)
);

CREATE INDEX idx_quiz_question_school ON quiz_question (school_id, subject_id, class_id, question_type);

CREATE TABLE quiz (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    academic_year_id    UUID NOT NULL REFERENCES academic_year(id),
    class_id            UUID NOT NULL REFERENCES school_class(id),
    section_id          UUID REFERENCES section(id),
    subject_id          UUID NOT NULL REFERENCES subject(id),
    title               VARCHAR(200) NOT NULL,
    instructions        VARCHAR(2000),
    duration_minutes    INTEGER NOT NULL DEFAULT 30,
    max_attempts        INTEGER NOT NULL DEFAULT 1,
    start_at            TIMESTAMPTZ,
    end_at              TIMESTAMPTZ,
    pass_marks          NUMERIC(8,2),
    shuffle_questions   BOOLEAN NOT NULL DEFAULT false,
    show_results        BOOLEAN NOT NULL DEFAULT true,
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by_user_id  UUID REFERENCES app_user(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_quiz_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED', 'ARCHIVED')),
    CONSTRAINT chk_quiz_duration CHECK (duration_minutes > 0),
    CONSTRAINT chk_quiz_attempts CHECK (max_attempts > 0)
);

CREATE INDEX idx_quiz_scope ON quiz (school_id, academic_year_id, class_id, section_id, subject_id, status);

CREATE TABLE quiz_item (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    quiz_id             UUID NOT NULL REFERENCES quiz(id) ON DELETE CASCADE,
    question_id         UUID NOT NULL REFERENCES quiz_question(id),
    sort_order          INTEGER NOT NULL DEFAULT 0,
    marks               NUMERIC(8,2) NOT NULL DEFAULT 1,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_quiz_item UNIQUE (quiz_id, question_id),
    CONSTRAINT chk_quiz_item_marks CHECK (marks > 0)
);

CREATE INDEX idx_quiz_item_quiz ON quiz_item (school_id, quiz_id, sort_order);

CREATE TABLE quiz_attempt (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    quiz_id             UUID NOT NULL REFERENCES quiz(id) ON DELETE CASCADE,
    student_id          UUID NOT NULL REFERENCES student(id),
    attempt_no          INTEGER NOT NULL DEFAULT 1,
    status              VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    submitted_at        TIMESTAMPTZ,
    expires_at          TIMESTAMPTZ NOT NULL,
    auto_score          NUMERIC(8,2),
    manual_score        NUMERIC(8,2),
    total_score         NUMERIC(8,2),
    max_score           NUMERIC(8,2),
    percentage          NUMERIC(8,2),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_quiz_attempt_status CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'TIMED_OUT', 'GRADED')),
    CONSTRAINT uq_quiz_attempt UNIQUE (quiz_id, student_id, attempt_no)
);

CREATE INDEX idx_quiz_attempt_student ON quiz_attempt (school_id, student_id, quiz_id, status);

CREATE TABLE quiz_answer (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    attempt_id          UUID NOT NULL REFERENCES quiz_attempt(id) ON DELETE CASCADE,
    question_id         UUID NOT NULL REFERENCES quiz_question(id),
    response            TEXT,
    auto_correct        BOOLEAN,
    auto_marks          NUMERIC(8,2),
    awarded_marks       NUMERIC(8,2),
    grader_user_id      UUID REFERENCES app_user(id),
    grader_comment      VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_quiz_answer UNIQUE (attempt_id, question_id)
);

CREATE INDEX idx_quiz_answer_attempt ON quiz_answer (school_id, attempt_id);

CREATE TABLE quiz_offline_mark (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    quiz_id             UUID NOT NULL REFERENCES quiz(id) ON DELETE CASCADE,
    student_id          UUID NOT NULL REFERENCES student(id),
    marks               NUMERIC(8,2),
    remarks             VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_quiz_offline_mark UNIQUE (quiz_id, student_id)
);

CREATE INDEX idx_quiz_offline_quiz ON quiz_offline_mark (school_id, quiz_id);

-- ---- RLS + grants ----------------------------------------------------
ALTER TABLE quiz_question ENABLE ROW LEVEL SECURITY;
ALTER TABLE quiz_question FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON quiz_question
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE quiz ENABLE ROW LEVEL SECURITY;
ALTER TABLE quiz FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON quiz
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE quiz_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE quiz_item FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON quiz_item
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE quiz_attempt ENABLE ROW LEVEL SECURITY;
ALTER TABLE quiz_attempt FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON quiz_attempt
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE quiz_answer ENABLE ROW LEVEL SECURITY;
ALTER TABLE quiz_answer FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON quiz_answer
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE quiz_offline_mark ENABLE ROW LEVEL SECURITY;
ALTER TABLE quiz_offline_mark FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON quiz_offline_mark
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

GRANT SELECT, INSERT, UPDATE, DELETE ON quiz_question TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON quiz TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON quiz_item TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON quiz_attempt TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON quiz_answer TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON quiz_offline_mark TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON quiz_question TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON quiz TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON quiz_item TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON quiz_attempt TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON quiz_answer TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON quiz_offline_mark TO app_admin;

-- ---- Demo question bank + published quiz -----------------------------
INSERT INTO quiz_question (id, school_id, subject_id, class_id, question_type, prompt, options_json, correct_answer, marks, explanation, created_by_user_id)
VALUES
    ('32000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
     '30000000-0000-0000-0000-000000000031', '30000000-0000-0000-0000-000000000011',
     'MCQ', 'What is 12 × 8?',
     '[{"key":"A","text":"84"},{"key":"B","text":"96"},{"key":"C","text":"108"},{"key":"D","text":"88"}]',
     'B', 2, '12 × 8 = 96', '20000000-0000-0000-0000-000000000003'),
    ('32000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001',
     '30000000-0000-0000-0000-000000000031', '30000000-0000-0000-0000-000000000011',
     'TRUE_FALSE', 'A square has four equal sides.',
     '[{"key":"TRUE","text":"True"},{"key":"FALSE","text":"False"}]',
     'TRUE', 1, 'By definition a square has four equal sides and four right angles.',
     '20000000-0000-0000-0000-000000000003'),
    ('32000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001',
     '30000000-0000-0000-0000-000000000031', '30000000-0000-0000-0000-000000000011',
     'SHORT_ANSWER', 'Write the next odd number after 17.',
     NULL, '19', 2, 'Odd numbers increase by 2: 17 + 2 = 19.',
     '20000000-0000-0000-0000-000000000003');

INSERT INTO quiz (id, school_id, academic_year_id, class_id, section_id, subject_id, title, instructions,
                  duration_minutes, max_attempts, start_at, end_at, pass_marks, shuffle_questions, show_results,
                  status, created_by_user_id)
SELECT '32000000-0000-0000-0000-000000000011', y.school_id, y.id,
       '30000000-0000-0000-0000-000000000011', '30000000-0000-0000-0000-000000000021',
       '30000000-0000-0000-0000-000000000031',
       'Class 5 Maths Warm-up',
       'Answer all questions. Multiple choice and true/false are auto-graded. Short answers are reviewed by your teacher.',
       20, 2, now() - interval '1 day', now() + interval '30 days', 3, false, true, 'PUBLISHED',
       '20000000-0000-0000-0000-000000000003'
FROM academic_year y
WHERE y.id = '30000000-0000-0000-0000-000000000001'
  AND NOT EXISTS (SELECT 1 FROM quiz q WHERE q.id = '32000000-0000-0000-0000-000000000011');

INSERT INTO quiz_item (id, school_id, quiz_id, question_id, sort_order, marks)
SELECT v.id, '20000000-0000-0000-0000-000000000001', '32000000-0000-0000-0000-000000000011',
       v.question_id, v.sort_order, v.marks
FROM (VALUES
    ('32000000-0000-0000-0000-000000000021'::uuid, '32000000-0000-0000-0000-000000000001'::uuid, 1, 2::numeric),
    ('32000000-0000-0000-0000-000000000022'::uuid, '32000000-0000-0000-0000-000000000002'::uuid, 2, 1::numeric),
    ('32000000-0000-0000-0000-000000000023'::uuid, '32000000-0000-0000-0000-000000000003'::uuid, 3, 2::numeric)
) AS v(id, question_id, sort_order, marks)
WHERE EXISTS (SELECT 1 FROM quiz q WHERE q.id = '32000000-0000-0000-0000-000000000011')
  AND NOT EXISTS (SELECT 1 FROM quiz_item i WHERE i.id = v.id);
