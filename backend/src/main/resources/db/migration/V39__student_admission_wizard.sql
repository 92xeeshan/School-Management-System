-- =====================================================================
-- V39: Multi-step student admission (issue #98).
-- school_id tenancy only. No branches. ADMIN / SUPER_ADMIN permissions.
-- Extends student / guardian / class_subject; does not flatten enrollment.
-- =====================================================================
SET app.bypass_rls = 'true';

ALTER TABLE school_class
    ADD COLUMN IF NOT EXISTS stream_enabled BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE student
    ADD COLUMN IF NOT EXISTS middle_name VARCHAR(60),
    ADD COLUMN IF NOT EXISTS aadhaar_no VARCHAR(12),
    ADD COLUMN IF NOT EXISTS apaar_id VARCHAR(12),
    ADD COLUMN IF NOT EXISTS category_id UUID,
    ADD COLUMN IF NOT EXISTS religion_id UUID,
    ADD COLUMN IF NOT EXISTS mother_tongue_id UUID,
    ADD COLUMN IF NOT EXISTS house_id UUID,
    ADD COLUMN IF NOT EXISTS medium_id UUID,
    ADD COLUMN IF NOT EXISTS stream_id UUID,
    ADD COLUMN IF NOT EXISTS email VARCHAR(120),
    ADD COLUMN IF NOT EXISTS medical_notes VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS transport_required BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS hostel_required BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS remarks VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS emergency_name VARCHAR(120),
    ADD COLUMN IF NOT EXISTS emergency_relation VARCHAR(40),
    ADD COLUMN IF NOT EXISTS profile_pending BOOLEAN NOT NULL DEFAULT true;

ALTER TABLE guardian
    ADD COLUMN IF NOT EXISTS qualification VARCHAR(120),
    ADD COLUMN IF NOT EXISTS address VARCHAR(500);

ALTER TABLE class_subject
    ADD COLUMN IF NOT EXISTS stream_id UUID,
    ADD COLUMN IF NOT EXISTS compulsory BOOLEAN NOT NULL DEFAULT true;

CREATE TABLE IF NOT EXISTS stream (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    name        VARCHAR(80) NOT NULL,
    code        VARCHAR(20),
    sort_order  INTEGER NOT NULL DEFAULT 0,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_stream_school_name UNIQUE (school_id, name)
);

CREATE TABLE IF NOT EXISTS school_lookup (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    kind        VARCHAR(30) NOT NULL
                CHECK (kind IN ('RELIGION', 'CATEGORY', 'HOUSE', 'MEDIUM', 'MOTHER_TONGUE')),
    name        VARCHAR(80) NOT NULL,
    code        VARCHAR(20),
    sort_order  INTEGER NOT NULL DEFAULT 0,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_school_lookup UNIQUE (school_id, kind, name)
);
CREATE INDEX IF NOT EXISTS idx_school_lookup_kind ON school_lookup (school_id, kind, sort_order);

CREATE TABLE IF NOT EXISTS student_step_status (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    student_id  UUID NOT NULL REFERENCES student(id) ON DELETE CASCADE,
    step_no     SMALLINT NOT NULL CHECK (step_no BETWEEN 1 AND 6),
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                CHECK (status IN ('PENDING', 'COMPLETED')),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_student_step UNIQUE (student_id, step_no)
);
CREATE INDEX IF NOT EXISTS idx_student_step_school ON student_step_status (school_id, status);

CREATE TABLE IF NOT EXISTS student_address (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id    UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    student_id   UUID NOT NULL REFERENCES student(id) ON DELETE CASCADE,
    address_type VARCHAR(20) NOT NULL CHECK (address_type IN ('CURRENT', 'PERMANENT')),
    line1        VARCHAR(200),
    line2        VARCHAR(200),
    city         VARCHAR(80),
    district     VARCHAR(80),
    state        VARCHAR(80),
    pin          VARCHAR(6),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_student_address_type UNIQUE (student_id, address_type)
);

CREATE TABLE IF NOT EXISTS student_previous_school (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id          UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    student_id         UUID NOT NULL REFERENCES student(id) ON DELETE CASCADE,
    no_previous_school BOOLEAN NOT NULL DEFAULT false,
    school_name        VARCHAR(200),
    board              VARCHAR(40),
    medium             VARCHAR(80),
    last_class         VARCHAR(50),
    year_of_passing    VARCHAR(10),
    percentage         VARCHAR(20),
    tc_number          VARCHAR(60),
    tc_date            DATE,
    reason             VARCHAR(300),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_student_previous_school UNIQUE (student_id)
);

CREATE TABLE IF NOT EXISTS student_document (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id     UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    student_id    UUID NOT NULL REFERENCES student(id) ON DELETE CASCADE,
    doc_type      VARCHAR(40) NOT NULL
                  CHECK (doc_type IN (
                      'BIRTH_CERTIFICATE', 'TRANSFER_CERTIFICATE', 'PREVIOUS_MARKSHEET',
                      'AADHAAR', 'CASTE_CERTIFICATE', 'OTHER')),
    file_key      VARCHAR(500) NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    content_type  VARCHAR(120),
    verified      BOOLEAN NOT NULL DEFAULT false,
    uploaded_by   UUID REFERENCES app_user(id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_student_document_student ON student_document (school_id, student_id);

CREATE TABLE IF NOT EXISTS student_subject (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id        UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    student_id       UUID NOT NULL REFERENCES student(id) ON DELETE CASCADE,
    subject_id       UUID NOT NULL REFERENCES subject(id) ON DELETE CASCADE,
    academic_year_id UUID NOT NULL REFERENCES academic_year(id) ON DELETE CASCADE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_student_subject UNIQUE (student_id, subject_id, academic_year_id)
);

ALTER TABLE student
    DROP CONSTRAINT IF EXISTS fk_student_stream;
ALTER TABLE student
    ADD CONSTRAINT fk_student_stream FOREIGN KEY (stream_id) REFERENCES stream(id) ON DELETE SET NULL;
ALTER TABLE student
    DROP CONSTRAINT IF EXISTS fk_student_category;
ALTER TABLE student
    ADD CONSTRAINT fk_student_category FOREIGN KEY (category_id) REFERENCES school_lookup(id) ON DELETE SET NULL;
ALTER TABLE student
    DROP CONSTRAINT IF EXISTS fk_student_religion;
ALTER TABLE student
    ADD CONSTRAINT fk_student_religion FOREIGN KEY (religion_id) REFERENCES school_lookup(id) ON DELETE SET NULL;
ALTER TABLE student
    DROP CONSTRAINT IF EXISTS fk_student_mother_tongue;
ALTER TABLE student
    ADD CONSTRAINT fk_student_mother_tongue FOREIGN KEY (mother_tongue_id) REFERENCES school_lookup(id) ON DELETE SET NULL;
ALTER TABLE student
    DROP CONSTRAINT IF EXISTS fk_student_house;
ALTER TABLE student
    ADD CONSTRAINT fk_student_house FOREIGN KEY (house_id) REFERENCES school_lookup(id) ON DELETE SET NULL;
ALTER TABLE student
    DROP CONSTRAINT IF EXISTS fk_student_medium;
ALTER TABLE student
    ADD CONSTRAINT fk_student_medium FOREIGN KEY (medium_id) REFERENCES school_lookup(id) ON DELETE SET NULL;

ALTER TABLE class_subject
    DROP CONSTRAINT IF EXISTS fk_class_subject_stream;
ALTER TABLE class_subject
    ADD CONSTRAINT fk_class_subject_stream FOREIGN KEY (stream_id) REFERENCES stream(id) ON DELETE SET NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_student_school_aadhaar
    ON student (school_id, aadhaar_no) WHERE aadhaar_no IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_student_school_apaar
    ON student (school_id, apaar_id) WHERE apaar_id IS NOT NULL;

DO $$
DECLARE
    tbl TEXT;
BEGIN
    FOREACH tbl IN ARRAY ARRAY[
        'stream', 'school_lookup', 'student_step_status', 'student_address',
        'student_previous_school', 'student_document', 'student_subject']
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', tbl);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', tbl);
        EXECUTE format(
            'CREATE POLICY tenant_isolation ON %I FOR ALL
             USING (school_id = app_current_school_id() OR app_bypass_rls())
             WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls())', tbl);
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I TO app_rls', tbl);
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I TO app_admin', tbl);
    END LOOP;
END $$;

UPDATE school_class
SET stream_enabled = true
WHERE name IN ('Class 6', 'Class 9', 'Class 11');

INSERT INTO stream (id, school_id, name, code, sort_order, status)
SELECT v.id, s.id, v.name, v.code, v.sort_order, 'ACTIVE'
FROM school s
CROSS JOIN (VALUES
    ('a1000000-0000-0000-0000-000000000001'::uuid, 'Islamic Studies', 'IS', 1),
    ('a1000000-0000-0000-0000-000000000002'::uuid, 'Science', 'SCI', 2),
    ('a1000000-0000-0000-0000-000000000003'::uuid, 'Arts', 'ART', 3)
) AS v(id, name, code, sort_order)
WHERE s.id = '20000000-0000-0000-0000-000000000001'
ON CONFLICT DO NOTHING;

INSERT INTO school_lookup (id, school_id, kind, name, code, sort_order)
SELECT v.id, '20000000-0000-0000-0000-000000000001', v.kind, v.name, v.code, v.sort_order
FROM (VALUES
    ('a2000000-0000-0000-0000-000000000001'::uuid, 'RELIGION', 'Islam', 'ISL', 1),
    ('a2000000-0000-0000-0000-000000000002'::uuid, 'RELIGION', 'Hinduism', 'HIN', 2),
    ('a2000000-0000-0000-0000-000000000003'::uuid, 'RELIGION', 'Christianity', 'CHR', 3),
    ('a2000000-0000-0000-0000-000000000004'::uuid, 'RELIGION', 'Sikhism', 'SIK', 4),
    ('a2000000-0000-0000-0000-000000000005'::uuid, 'RELIGION', 'Other', 'OTH', 5),
    ('a2000000-0000-0000-0000-000000000011'::uuid, 'CATEGORY', 'General', 'GEN', 1),
    ('a2000000-0000-0000-0000-000000000012'::uuid, 'CATEGORY', 'OBC', 'OBC', 2),
    ('a2000000-0000-0000-0000-000000000013'::uuid, 'CATEGORY', 'SC', 'SC', 3),
    ('a2000000-0000-0000-0000-000000000014'::uuid, 'CATEGORY', 'ST', 'ST', 4),
    ('a2000000-0000-0000-0000-000000000021'::uuid, 'HOUSE', 'Red', 'RED', 1),
    ('a2000000-0000-0000-0000-000000000022'::uuid, 'HOUSE', 'Blue', 'BLU', 2),
    ('a2000000-0000-0000-0000-000000000023'::uuid, 'HOUSE', 'Green', 'GRN', 3),
    ('a2000000-0000-0000-0000-000000000024'::uuid, 'HOUSE', 'Yellow', 'YEL', 4),
    ('a2000000-0000-0000-0000-000000000031'::uuid, 'MEDIUM', 'English', 'ENG', 1),
    ('a2000000-0000-0000-0000-000000000032'::uuid, 'MEDIUM', 'Hindi', 'HIN', 2),
    ('a2000000-0000-0000-0000-000000000033'::uuid, 'MEDIUM', 'Urdu', 'URD', 3),
    ('a2000000-0000-0000-0000-000000000041'::uuid, 'MOTHER_TONGUE', 'Urdu', 'URD', 1),
    ('a2000000-0000-0000-0000-000000000042'::uuid, 'MOTHER_TONGUE', 'Hindi', 'HIN', 2),
    ('a2000000-0000-0000-0000-000000000043'::uuid, 'MOTHER_TONGUE', 'English', 'ENG', 3),
    ('a2000000-0000-0000-0000-000000000044'::uuid, 'MOTHER_TONGUE', 'Arabic', 'ARA', 4)
) AS v(id, kind, name, code, sort_order)
ON CONFLICT DO NOTHING;

INSERT INTO school_class (id, school_id, name, code, sort_order, stream_enabled)
SELECT '30000000-0000-0000-0000-000000000016', '20000000-0000-0000-0000-000000000001',
       'Class 9', 'C9', 9, true
WHERE NOT EXISTS (SELECT 1 FROM school_class WHERE school_id = '20000000-0000-0000-0000-000000000001' AND name = 'Class 9');

INSERT INTO school_class (id, school_id, name, code, sort_order, stream_enabled)
SELECT '30000000-0000-0000-0000-000000000017', '20000000-0000-0000-0000-000000000001',
       'Class 11', 'C11', 11, true
WHERE NOT EXISTS (SELECT 1 FROM school_class WHERE school_id = '20000000-0000-0000-0000-000000000001' AND name = 'Class 11');

INSERT INTO section (id, school_id, class_id, name, capacity)
SELECT v.id, '20000000-0000-0000-0000-000000000001', c.id, 'A', 40
FROM (VALUES
    ('30000000-0000-0000-0000-000000000027'::uuid, 'Class 9'),
    ('30000000-0000-0000-0000-000000000028'::uuid, 'Class 11')
) AS v(id, class_name)
JOIN school_class c ON c.school_id = '20000000-0000-0000-0000-000000000001' AND c.name = v.class_name
WHERE NOT EXISTS (SELECT 1 FROM section s WHERE s.id = v.id);

INSERT INTO subject (id, school_id, name, code, type, status)
SELECT '30000000-0000-0000-0000-000000000036', '20000000-0000-0000-0000-000000000001',
       'Islamic Studies', 'ISL', 'ELECTIVE', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM subject WHERE id = '30000000-0000-0000-0000-000000000036');

INSERT INTO student_step_status (school_id, student_id, step_no, status)
SELECT s.school_id, s.id, step.n, CASE WHEN step.n = 1 THEN 'COMPLETED' ELSE 'PENDING' END
FROM student s
CROSS JOIN (VALUES (1), (2), (3), (4), (5), (6)) AS step(n)
ON CONFLICT DO NOTHING;
