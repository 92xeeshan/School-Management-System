-- =====================================================================
-- V30: Fee heads, class/session structures, sibling discounts, ad-hoc
-- levies, and fee-structure audit. Extends the V1 fee domain.
-- =====================================================================
SET app.bypass_rls = 'true';

ALTER TABLE fee_category
    ADD COLUMN IF NOT EXISTS is_optional BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS is_refundable BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    ADD COLUMN IF NOT EXISTS status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE fee_category DROP CONSTRAINT IF EXISTS fee_category_frequency_check;
ALTER TABLE fee_category ADD CONSTRAINT fee_category_frequency_check
    CHECK (frequency IN ('ONE_TIME', 'MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'ANNUAL'));

ALTER TABLE fee_category DROP CONSTRAINT IF EXISTS fee_category_status_check;
ALTER TABLE fee_category ADD CONSTRAINT fee_category_status_check
    CHECK (status IN ('ACTIVE', 'INACTIVE'));

ALTER TABLE fee_structure ALTER COLUMN frequency TYPE VARCHAR(20);
ALTER TABLE fee_structure DROP CONSTRAINT IF EXISTS fee_structure_frequency_check;
ALTER TABLE fee_structure ADD CONSTRAINT fee_structure_frequency_check
    CHECK (frequency IN ('ONE_TIME', 'MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'ANNUAL'));

ALTER TABLE fee_structure ADD COLUMN IF NOT EXISTS due_date DATE;

UPDATE fee_category SET frequency = 'MONTHLY', is_optional = false, status = 'ACTIVE'
 WHERE code = 'TUITION';
UPDATE fee_category SET frequency = 'MONTHLY', is_optional = true, status = 'ACTIVE'
 WHERE code = 'TRANSPORT';

CREATE TABLE sibling_discount_rule (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id          UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    academic_year_id   UUID NOT NULL REFERENCES academic_year(id) ON DELETE CASCADE,
    sibling_order      SMALLINT NOT NULL CHECK (sibling_order >= 2),
    discount_type      VARCHAR(10) NOT NULL CHECK (discount_type IN ('FIXED', 'PERCENT')),
    discount_value     NUMERIC(12,2) NOT NULL CHECK (discount_value >= 0),
    fee_category_id    UUID NOT NULL REFERENCES fee_category(id) ON DELETE CASCADE,
    status             VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_sibling_discount UNIQUE (school_id, academic_year_id, sibling_order, fee_category_id)
);
CREATE INDEX idx_sibling_discount_year ON sibling_discount_rule(school_id, academic_year_id);

CREATE TABLE adhoc_fee_levy (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id          UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    academic_year_id   UUID NOT NULL REFERENCES academic_year(id) ON DELETE CASCADE,
    fee_category_id    UUID NOT NULL REFERENCES fee_category(id) ON DELETE RESTRICT,
    amount             NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    due_date           DATE NOT NULL,
    scope              VARCHAR(20) NOT NULL
                       CHECK (scope IN ('GLOBAL', 'CLASS', 'SECTION', 'STUDENT')),
    class_id           UUID REFERENCES school_class(id) ON DELETE SET NULL,
    section_id         UUID REFERENCES section(id) ON DELETE SET NULL,
    student_id         UUID REFERENCES student(id) ON DELETE SET NULL,
    remarks            VARCHAR(300),
    assigned_count     INTEGER NOT NULL DEFAULT 0,
    created_by         UUID REFERENCES app_user(id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_adhoc_levy_school_year ON adhoc_fee_levy(school_id, academic_year_id);

CREATE TABLE fee_structure_audit (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id            UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    fee_structure_id     UUID REFERENCES fee_structure(id) ON DELETE SET NULL,
    academic_year_id     UUID REFERENCES academic_year(id) ON DELETE SET NULL,
    class_id             UUID,
    category_id          UUID,
    action               VARCHAR(20) NOT NULL CHECK (action IN ('CREATE', 'UPDATE', 'CLONE')),
    previous_amount      NUMERIC(12,2),
    new_amount           NUMERIC(12,2),
    previous_frequency   VARCHAR(20),
    new_frequency        VARCHAR(20),
    changed_by           UUID REFERENCES app_user(id),
    changed_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_fee_structure_audit_school ON fee_structure_audit(school_id, changed_at DESC);

ALTER TABLE sibling_discount_rule ENABLE ROW LEVEL SECURITY;
ALTER TABLE sibling_discount_rule FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON sibling_discount_rule
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE adhoc_fee_levy ENABLE ROW LEVEL SECURITY;
ALTER TABLE adhoc_fee_levy FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON adhoc_fee_levy
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE fee_structure_audit ENABLE ROW LEVEL SECURITY;
ALTER TABLE fee_structure_audit FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON fee_structure_audit
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

GRANT SELECT, INSERT, UPDATE, DELETE ON sibling_discount_rule TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON sibling_discount_rule TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON adhoc_fee_levy TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON adhoc_fee_levy TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON fee_structure_audit TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON fee_structure_audit TO app_admin;

INSERT INTO fee_category (id, school_id, name, code, description, is_optional, is_refundable, frequency, status)
SELECT v.id, '20000000-0000-0000-0000-000000000001', v.name, v.code, v.descr, v.opt, v.ref, v.freq, 'ACTIVE'
FROM (VALUES
    ('50000000-0000-0000-0000-000000000003'::uuid, 'Examination Fee', 'EXAM', 'Term examination fee', false, false, 'ONE_TIME'),
    ('50000000-0000-0000-0000-000000000004'::uuid, 'Annual Activity Fee', 'ACTIVITY', 'Sports and cultural activities', false, false, 'ANNUAL'),
    ('50000000-0000-0000-0000-000000000005'::uuid, 'Lost ID Card Fee', 'LOST_ID', 'Replacement of student ID card', true, false, 'ONE_TIME')
) AS v(id, name, code, descr, opt, ref, freq)
WHERE NOT EXISTS (SELECT 1 FROM fee_category c WHERE c.id = v.id);

INSERT INTO academic_year (id, school_id, name, start_date, end_date, is_current)
SELECT '30000000-0000-0000-0000-0000000000a1', '20000000-0000-0000-0000-000000000001',
       '2024-25', DATE '2024-04-01', DATE '2025-03-31', false
WHERE NOT EXISTS (SELECT 1 FROM academic_year y WHERE y.id = '30000000-0000-0000-0000-0000000000a1');

INSERT INTO academic_year (id, school_id, name, start_date, end_date, is_current)
SELECT '30000000-0000-0000-0000-0000000000a2', '20000000-0000-0000-0000-000000000001',
       '2026-27', DATE '2026-04-01', DATE '2027-03-31', false
WHERE NOT EXISTS (SELECT 1 FROM academic_year y WHERE y.id = '30000000-0000-0000-0000-0000000000a2');

INSERT INTO fee_structure (id, school_id, class_id, academic_year_id, category_id, amount, frequency, due_day, due_date)
SELECT v.id, '20000000-0000-0000-0000-000000000001', v.class_id, v.year_id, v.cat_id, v.amount, v.freq, v.due_day, v.due_date
FROM (VALUES
    ('50000000-0000-0000-0000-0000000000b1'::uuid, '30000000-0000-0000-0000-000000000011'::uuid,
     '30000000-0000-0000-0000-0000000000a1'::uuid, '50000000-0000-0000-0000-000000000001'::uuid,
     1400.00, 'MONTHLY', 10::smallint, NULL::date),
    ('50000000-0000-0000-0000-0000000000b2'::uuid, '30000000-0000-0000-0000-000000000011'::uuid,
     '30000000-0000-0000-0000-000000000001'::uuid, '50000000-0000-0000-0000-000000000003'::uuid,
     800.00, 'ONE_TIME', NULL::smallint, DATE '2025-09-10'),
    ('50000000-0000-0000-0000-0000000000b3'::uuid, '30000000-0000-0000-0000-000000000012'::uuid,
     '30000000-0000-0000-0000-000000000001'::uuid, '50000000-0000-0000-0000-000000000003'::uuid,
     1200.00, 'ONE_TIME', NULL::smallint, DATE '2025-09-10')
) AS v(id, class_id, year_id, cat_id, amount, freq, due_day, due_date)
WHERE NOT EXISTS (SELECT 1 FROM fee_structure f WHERE f.id = v.id);

INSERT INTO sibling_discount_rule
    (id, school_id, academic_year_id, sibling_order, discount_type, discount_value, fee_category_id, status)
SELECT v.id, '20000000-0000-0000-0000-000000000001',
       '30000000-0000-0000-0000-000000000001', v.ord, 'PERCENT', v.val,
       '50000000-0000-0000-0000-000000000001', 'ACTIVE'
FROM (VALUES
    ('51000000-0000-0000-0000-000000000001'::uuid, 2::smallint, 10.00),
    ('51000000-0000-0000-0000-000000000002'::uuid, 3::smallint, 20.00)
) AS v(id, ord, val)
WHERE NOT EXISTS (SELECT 1 FROM sibling_discount_rule r WHERE r.id = v.id);

INSERT INTO student (id, school_id, admission_no, first_name, last_name, date_of_birth, gender, admission_date, status)
SELECT '40000000-0000-0000-0000-000000000030', '20000000-0000-0000-0000-000000000001',
       'ADM0013', 'Anya', 'Kumar', DATE '2016-09-01', 'FEMALE', DATE '2025-04-01', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM student s WHERE s.id = '40000000-0000-0000-0000-000000000030');

INSERT INTO student_guardian (school_id, student_id, guardian_id, relationship, is_primary)
SELECT '20000000-0000-0000-0000-000000000001',
       '40000000-0000-0000-0000-000000000030',
       '40000000-0000-0000-0000-000000000011',
       'FATHER', true
WHERE NOT EXISTS (
    SELECT 1 FROM student_guardian sg
    WHERE sg.student_id = '40000000-0000-0000-0000-000000000030'
      AND sg.guardian_id = '40000000-0000-0000-0000-000000000011');

INSERT INTO student_enrollment (id, school_id, student_id, section_id, academic_year_id, roll_number, status)
SELECT '40000000-0000-0000-0000-000000000130', '20000000-0000-0000-0000-000000000001',
       '40000000-0000-0000-0000-000000000030', '30000000-0000-0000-0000-000000000021',
       '30000000-0000-0000-0000-000000000001', 3, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM student_enrollment e WHERE e.id = '40000000-0000-0000-0000-000000000130');
