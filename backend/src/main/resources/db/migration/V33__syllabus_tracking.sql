-- =====================================================================
-- V33: Syllabus & lesson tracking
--   syllabus_course   - one syllabus per academic year/class/section/subject
--   syllabus_unit     - units (or terms) within a course
--   syllabus_chapter  - lessons within a unit, with completion status
--   syllabus_material - course outline / learning resources
-- =====================================================================

SET app.bypass_rls = 'true';

-- ---- Permissions -----------------------------------------------------
INSERT INTO permission (code, module, name, description)
SELECT * FROM (VALUES
    ('SYLLABUS_READ',   'academics', 'Read syllabus',   'View syllabus coverage and lesson progress'),
    ('SYLLABUS_MANAGE', 'academics', 'Manage syllabus', 'Create/update syllabus units, chapters and materials')
) AS p(code, module, name, description)
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = p.code);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code IN ('SYLLABUS_READ', 'SYLLABUS_MANAGE')
  AND r.code IN ('SUPER_ADMIN', 'ADMIN', 'TEACHER')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code = 'SYLLABUS_READ'
  AND r.code IN ('STUDENT', 'PARENT')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ---- Tables ----------------------------------------------------------
CREATE TABLE syllabus_course (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id               UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    academic_year_id        UUID NOT NULL REFERENCES academic_year(id),
    class_id                UUID NOT NULL REFERENCES school_class(id),
    section_id              UUID REFERENCES section(id),
    subject_id              UUID NOT NULL REFERENCES subject(id),
    teacher_id              UUID REFERENCES teacher_profile(id),
    title                   VARCHAR(200) NOT NULL,
    description             VARCHAR(1000),
    target_completion_date  DATE,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_syllabus_course_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT uq_syllabus_course UNIQUE NULLS NOT DISTINCT
        (school_id, academic_year_id, class_id, section_id, subject_id)
);

CREATE INDEX idx_syllabus_course_scope
    ON syllabus_course (school_id, academic_year_id, class_id, section_id, subject_id);

CREATE TABLE syllabus_unit (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id               UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    course_id               UUID NOT NULL REFERENCES syllabus_course(id) ON DELETE CASCADE,
    title                   VARCHAR(200) NOT NULL,
    description             VARCHAR(1000),
    sort_order              INTEGER NOT NULL DEFAULT 0,
    target_completion_date  DATE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_syllabus_unit_course ON syllabus_unit (school_id, course_id, sort_order);

CREATE TABLE syllabus_chapter (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id               UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    unit_id                 UUID NOT NULL REFERENCES syllabus_unit(id) ON DELETE CASCADE,
    title                   VARCHAR(200) NOT NULL,
    description             VARCHAR(1000),
    sort_order              INTEGER NOT NULL DEFAULT 0,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    target_completion_date  DATE,
    completed_at            TIMESTAMPTZ,
    completed_by_user_id    UUID REFERENCES app_user(id),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_syllabus_chapter_status
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED'))
);

CREATE INDEX idx_syllabus_chapter_unit ON syllabus_chapter (school_id, unit_id, sort_order);

CREATE TABLE syllabus_material (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    course_id           UUID NOT NULL REFERENCES syllabus_course(id) ON DELETE CASCADE,
    unit_id             UUID REFERENCES syllabus_unit(id) ON DELETE SET NULL,
    kind                VARCHAR(20) NOT NULL DEFAULT 'DOCUMENT',
    title               VARCHAR(200) NOT NULL,
    file_name           VARCHAR(255),
    content_type        VARCHAR(120),
    file_size           BIGINT,
    content             BYTEA,
    external_url        VARCHAR(1000),
    uploaded_by_user_id UUID REFERENCES app_user(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_syllabus_material_kind CHECK (kind IN ('OUTLINE', 'DOCUMENT', 'LINK')),
    CONSTRAINT chk_syllabus_material_source
        CHECK (content IS NOT NULL OR external_url IS NOT NULL)
);

CREATE INDEX idx_syllabus_material_course ON syllabus_material (school_id, course_id, created_at DESC);

-- ---- Row level security ---------------------------------------------
ALTER TABLE syllabus_course ENABLE ROW LEVEL SECURITY;
ALTER TABLE syllabus_course FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON syllabus_course
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE syllabus_unit ENABLE ROW LEVEL SECURITY;
ALTER TABLE syllabus_unit FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON syllabus_unit
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE syllabus_chapter ENABLE ROW LEVEL SECURITY;
ALTER TABLE syllabus_chapter FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON syllabus_chapter
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE syllabus_material ENABLE ROW LEVEL SECURITY;
ALTER TABLE syllabus_material FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON syllabus_material
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

GRANT SELECT, INSERT, UPDATE, DELETE ON syllabus_course TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON syllabus_unit TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON syllabus_chapter TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON syllabus_material TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON syllabus_course TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON syllabus_unit TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON syllabus_chapter TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON syllabus_material TO app_admin;

-- ---- Demo data -------------------------------------------------------
-- Resolve existing demo classes/sections/subjects so the seed works even
-- when the base demo records were edited or replaced in a local database.
INSERT INTO syllabus_course (id, school_id, academic_year_id, class_id, section_id, subject_id, teacher_id,
                             title, description, target_completion_date, status)
SELECT v.id, '20000000-0000-0000-0000-000000000001', v.academic_year_id, v.class_id, v.section_id,
       v.subject_id, v.teacher_id, v.title, v.description, v.target_date, 'ACTIVE'
FROM (
    SELECT '40000000-0000-0000-0000-000000000001'::uuid AS id, y.id AS academic_year_id,
           c.class_id, c.section_id, s.id AS subject_id, t.id AS teacher_id,
           s.name || ' - ' || cl.name AS title,
           'Coverage plan for ' || s.name || ' in ' || cl.name || '.' AS description,
           DATE '2026-03-15' AS target_date
    FROM (
        SELECT cl.id AS class_id, sec.id AS section_id
        FROM school_class cl
        JOIN section sec ON sec.class_id = cl.id AND sec.school_id = cl.school_id
        WHERE cl.school_id = '20000000-0000-0000-0000-000000000001'
        ORDER BY cl.sort_order, sec.name
        LIMIT 1
    ) c
    JOIN (SELECT id, name FROM subject
          WHERE school_id = '20000000-0000-0000-0000-000000000001'
          ORDER BY name LIMIT 1) s ON true
    JOIN (SELECT id, name FROM school_class
          WHERE school_id = '20000000-0000-0000-0000-000000000001'
          ORDER BY sort_order LIMIT 1) cl ON cl.id = c.class_id
    JOIN (SELECT id FROM teacher_profile
          WHERE school_id = '20000000-0000-0000-0000-000000000001'
          ORDER BY first_name LIMIT 1) t ON true
    JOIN (SELECT id FROM academic_year
          WHERE school_id = '20000000-0000-0000-0000-000000000001'
          ORDER BY is_current DESC, start_date DESC LIMIT 1) y ON true

    UNION ALL

    SELECT '40000000-0000-0000-0000-000000000002'::uuid, y.id, c.class_id, c.section_id, s.id, t.id,
           s.name || ' - ' || cl.name,
           'Coverage plan for ' || s.name || ' in ' || cl.name || '.',
           DATE '2026-02-28'
    FROM (
        SELECT cl.id AS class_id, sec.id AS section_id
        FROM school_class cl
        JOIN section sec ON sec.class_id = cl.id AND sec.school_id = cl.school_id
        WHERE cl.school_id = '20000000-0000-0000-0000-000000000001'
        ORDER BY cl.sort_order, sec.name
        LIMIT 1 OFFSET 1
    ) c
    JOIN (SELECT id, name FROM subject
          WHERE school_id = '20000000-0000-0000-0000-000000000001'
          ORDER BY name LIMIT 1 OFFSET 1) s ON true
    JOIN (SELECT id, name FROM school_class
          WHERE school_id = '20000000-0000-0000-0000-000000000001'
          ORDER BY sort_order LIMIT 1 OFFSET 1) cl ON cl.id = c.class_id
    JOIN (SELECT id FROM teacher_profile
          WHERE school_id = '20000000-0000-0000-0000-000000000001'
          ORDER BY first_name LIMIT 1 OFFSET 1) t ON true
    JOIN (SELECT id FROM academic_year
          WHERE school_id = '20000000-0000-0000-0000-000000000001'
          ORDER BY is_current DESC, start_date DESC LIMIT 1) y ON true
) v
WHERE NOT EXISTS (SELECT 1 FROM syllabus_course c WHERE c.id = v.id);

INSERT INTO syllabus_unit (id, school_id, course_id, title, description, sort_order, target_completion_date)
SELECT v.id, '20000000-0000-0000-0000-000000000001', v.course_id, v.title, v.description, v.sort_order, v.target_date
FROM (VALUES
    ('41000000-0000-0000-0000-000000000001'::uuid, '40000000-0000-0000-0000-000000000001'::uuid,
     'Unit 1 - Foundations', 'Core concepts and terminology.', 1, DATE '2025-09-30'),
    ('41000000-0000-0000-0000-000000000002'::uuid, '40000000-0000-0000-0000-000000000001'::uuid,
     'Unit 2 - Applications', 'Applying the concepts to real problems.', 2, DATE '2025-12-20'),
    ('41000000-0000-0000-0000-000000000003'::uuid, '40000000-0000-0000-0000-000000000001'::uuid,
     'Unit 3 - Revision', 'Consolidation, practice and assessment.', 3, DATE '2026-03-15'),
    ('41000000-0000-0000-0000-000000000004'::uuid, '40000000-0000-0000-0000-000000000002'::uuid,
     'Unit 1 - Introduction', 'Building the base knowledge for the term.', 1, DATE '2025-10-31'),
    ('41000000-0000-0000-0000-000000000005'::uuid, '40000000-0000-0000-0000-000000000002'::uuid,
     'Unit 2 - Practical Work', 'Hands-on activities and observation.', 2, DATE '2026-02-28')
) AS v(id, course_id, title, description, sort_order, target_date)
WHERE EXISTS (SELECT 1 FROM syllabus_course c WHERE c.id = v.course_id)
  AND NOT EXISTS (SELECT 1 FROM syllabus_unit u WHERE u.id = v.id);

INSERT INTO syllabus_chapter (id, school_id, unit_id, title, description, sort_order, status,
                              target_completion_date, completed_at, completed_by_user_id)
SELECT v.id, '20000000-0000-0000-0000-000000000001', v.unit_id, v.title, v.description, v.sort_order,
       v.status, v.target_date,
       CASE WHEN v.status = 'COMPLETED' THEN now() - INTERVAL '10 days' ELSE NULL END,
       CASE WHEN v.status = 'COMPLETED'
            THEN (SELECT id FROM app_user ORDER BY created_at LIMIT 1) ELSE NULL END
FROM (VALUES
    ('42000000-0000-0000-0000-000000000001'::uuid, '41000000-0000-0000-0000-000000000001'::uuid,
     'Introduction and key terms', 'Vocabulary and first examples.', 1, 'COMPLETED', DATE '2025-08-15'),
    ('42000000-0000-0000-0000-000000000002'::uuid, '41000000-0000-0000-0000-000000000001'::uuid,
     'Core principles', 'Worked examples with guided practice.', 2, 'COMPLETED', DATE '2025-09-10'),
    ('42000000-0000-0000-0000-000000000003'::uuid, '41000000-0000-0000-0000-000000000001'::uuid,
     'Practice set', 'Independent exercises and review.', 3, 'COMPLETED', DATE '2025-09-28'),
    ('42000000-0000-0000-0000-000000000004'::uuid, '41000000-0000-0000-0000-000000000002'::uuid,
     'Problem solving', 'Multi-step problem strategies.', 1, 'COMPLETED', DATE '2025-11-05'),
    ('42000000-0000-0000-0000-000000000005'::uuid, '41000000-0000-0000-0000-000000000002'::uuid,
     'Group activity', 'Collaborative task with presentation.', 2, 'IN_PROGRESS', DATE '2025-12-10'),
    ('42000000-0000-0000-0000-000000000006'::uuid, '41000000-0000-0000-0000-000000000002'::uuid,
     'Real-life project', 'Apply learning to a local context.', 3, 'PENDING', DATE '2025-12-20'),
    ('42000000-0000-0000-0000-000000000007'::uuid, '41000000-0000-0000-0000-000000000003'::uuid,
     'Recap quiz', 'Short diagnostic quiz.', 1, 'PENDING', DATE '2026-01-31'),
    ('42000000-0000-0000-0000-000000000008'::uuid, '41000000-0000-0000-0000-000000000003'::uuid,
     'Revision worksheet', 'Mixed practice questions.', 2, 'PENDING', DATE '2026-03-15'),
    ('42000000-0000-0000-0000-000000000009'::uuid, '41000000-0000-0000-0000-000000000004'::uuid,
     'Orientation', 'Scope of the subject and expectations.', 1, 'COMPLETED', DATE '2025-09-20'),
    ('42000000-0000-0000-0000-000000000010'::uuid, '41000000-0000-0000-0000-000000000004'::uuid,
     'Foundational concepts', 'Definitions and demonstrations.', 2, 'COMPLETED', DATE '2025-10-25'),
    ('42000000-0000-0000-0000-000000000011'::uuid, '41000000-0000-0000-0000-000000000005'::uuid,
     'Lab safety and tools', 'Equipment handling and safety rules.', 1, 'IN_PROGRESS', DATE '2026-01-20'),
    ('42000000-0000-0000-0000-000000000012'::uuid, '41000000-0000-0000-0000-000000000005'::uuid,
     'Observation report', 'Record and interpret observations.', 2, 'PENDING', DATE '2026-02-28')
) AS v(id, unit_id, title, description, sort_order, status, target_date)
WHERE EXISTS (SELECT 1 FROM syllabus_unit u WHERE u.id = v.unit_id)
  AND NOT EXISTS (SELECT 1 FROM syllabus_chapter c WHERE c.id = v.id);

INSERT INTO syllabus_material (id, school_id, course_id, unit_id, kind, title, file_name, content_type,
                               file_size, content, external_url, uploaded_by_user_id)
SELECT v.id, '20000000-0000-0000-0000-000000000001', v.course_id, NULL, v.kind, v.title,
       v.file_name, v.content_type, v.file_size, v.content, v.external_url,
       (SELECT id FROM app_user ORDER BY created_at LIMIT 1)
FROM (VALUES
    ('43000000-0000-0000-0000-000000000001'::uuid, '40000000-0000-0000-0000-000000000001'::uuid,
     'OUTLINE', 'Course outline', 'course-outline.txt', 'text/plain',
     CAST(length('Unit 1: foundations. Unit 2: applications. Unit 3: revision and assessment.') AS BIGINT),
     convert_to('Unit 1: foundations. Unit 2: applications. Unit 3: revision and assessment.', 'UTF8'),
     NULL::varchar),
    ('43000000-0000-0000-0000-000000000002'::uuid, '40000000-0000-0000-0000-000000000002'::uuid,
     'LINK', 'Reference portal', NULL, NULL, NULL, NULL,
     'https://www.khanacademy.org/')
) AS v(id, course_id, kind, title, file_name, content_type, file_size, content, external_url)
WHERE EXISTS (SELECT 1 FROM syllabus_course c WHERE c.id = v.course_id)
  AND NOT EXISTS (SELECT 1 FROM syllabus_material m WHERE m.id = v.id);
