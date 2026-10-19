-- =====================================================================
-- V34: Academic terms & sessions, batch promotion history
-- =====================================================================

SET app.bypass_rls = 'true';

-- ---- Permissions -----------------------------------------------------
INSERT INTO permission (code, module, name, description)
SELECT * FROM (VALUES
    ('TERM_READ',   'academics', 'Read academic terms',   'View academic years, terms and promotion history'),
    ('TERM_MANAGE', 'academics', 'Manage academic terms', 'Configure academic years, terms and run batch promotion')
) AS p(code, module, name, description)
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = p.code);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code IN ('TERM_READ', 'TERM_MANAGE')
  AND r.code IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code = 'TERM_READ'
  AND r.code = 'TEACHER'
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- Keep at most one current academic year per school, then enforce it
UPDATE academic_year SET is_current = false
WHERE is_current
  AND id NOT IN (
      SELECT keep_id FROM (
          SELECT DISTINCT ON (school_id) id AS keep_id
          FROM academic_year
          WHERE is_current
          ORDER BY school_id, start_date DESC
      ) kept
  );

CREATE UNIQUE INDEX IF NOT EXISTS uq_academic_year_one_current
    ON academic_year (school_id)
    WHERE is_current;

-- ---- Tables ----------------------------------------------------------
CREATE TABLE academic_term (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    academic_year_id    UUID NOT NULL REFERENCES academic_year(id) ON DELETE CASCADE,
    name                VARCHAR(80) NOT NULL,
    start_date          DATE NOT NULL,
    end_date            DATE NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    sort_order          INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_academic_term_status CHECK (status IN ('PLANNED', 'ACTIVE', 'COMPLETED')),
    CONSTRAINT chk_academic_term_dates CHECK (end_date >= start_date),
    CONSTRAINT uq_academic_term_name UNIQUE (school_id, academic_year_id, name)
);

CREATE INDEX idx_academic_term_year ON academic_term (school_id, academic_year_id, start_date);

CREATE TABLE promotion_batch (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id               UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    source_year_id          UUID NOT NULL REFERENCES academic_year(id),
    destination_year_id     UUID NOT NULL REFERENCES academic_year(id),
    source_class_id         UUID NOT NULL REFERENCES school_class(id),
    source_section_id       UUID REFERENCES section(id),
    destination_class_id    UUID NOT NULL REFERENCES school_class(id),
    destination_section_id  UUID NOT NULL REFERENCES section(id),
    promoted_count          INTEGER NOT NULL DEFAULT 0,
    skipped_count           INTEGER NOT NULL DEFAULT 0,
    notes                   VARCHAR(500),
    created_by_user_id      UUID REFERENCES app_user(id),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_promotion_batch_school ON promotion_batch (school_id, created_at DESC);

CREATE TABLE promotion_batch_item (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id                   UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    batch_id                    UUID NOT NULL REFERENCES promotion_batch(id) ON DELETE CASCADE,
    student_id                  UUID NOT NULL REFERENCES student(id),
    source_enrollment_id        UUID REFERENCES student_enrollment(id),
    destination_enrollment_id   UUID REFERENCES student_enrollment(id),
    outcome                     VARCHAR(20) NOT NULL,
    reason                      VARCHAR(200),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_promotion_item_outcome CHECK (outcome IN ('PROMOTED', 'SKIPPED'))
);

CREATE INDEX idx_promotion_item_batch ON promotion_batch_item (school_id, batch_id);

-- ---- RLS + grants ----------------------------------------------------
ALTER TABLE academic_term ENABLE ROW LEVEL SECURITY;
ALTER TABLE academic_term FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON academic_term
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE promotion_batch ENABLE ROW LEVEL SECURITY;
ALTER TABLE promotion_batch FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON promotion_batch
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE promotion_batch_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE promotion_batch_item FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON promotion_batch_item
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

GRANT SELECT, INSERT, UPDATE, DELETE ON academic_term TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON promotion_batch TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON promotion_batch_item TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON academic_term TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON promotion_batch TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON promotion_batch_item TO app_admin;

-- ---- Demo terms ------------------------------------------------------
INSERT INTO academic_term (id, school_id, academic_year_id, name, start_date, end_date, status, sort_order)
SELECT v.id, y.school_id, y.id, v.name, v.start_date, v.end_date, v.status, v.sort_order
FROM academic_year y
JOIN (VALUES
    ('31000000-0000-0000-0000-000000000001'::uuid, true,  'Term 1', DATE '2025-04-01', DATE '2025-09-30', 'COMPLETED', 1),
    ('31000000-0000-0000-0000-000000000002'::uuid, true,  'Term 2', DATE '2025-10-01', DATE '2026-03-31', 'ACTIVE',    2)
) AS v(id, current_year, name, start_date, end_date, status, sort_order) ON true
WHERE y.school_id = '20000000-0000-0000-0000-000000000001'
  AND y.is_current = v.current_year
  AND NOT EXISTS (SELECT 1 FROM academic_term t WHERE t.id = v.id);

INSERT INTO academic_term (id, school_id, academic_year_id, name, start_date, end_date, status, sort_order)
SELECT v.id, y.school_id, y.id, v.name, v.start_date, v.end_date, v.status, v.sort_order
FROM academic_year y
JOIN (VALUES
    ('31000000-0000-0000-0000-000000000003'::uuid, '2026-27', 'Term 1', DATE '2026-04-01', DATE '2026-09-30', 'PLANNED', 1),
    ('31000000-0000-0000-0000-000000000004'::uuid, '2026-27', 'Term 2', DATE '2026-10-01', DATE '2027-03-31', 'PLANNED', 2),
    ('31000000-0000-0000-0000-000000000005'::uuid, '2024-25', 'Term 1', DATE '2024-04-01', DATE '2024-09-30', 'COMPLETED', 1),
    ('31000000-0000-0000-0000-000000000006'::uuid, '2024-25', 'Term 2', DATE '2024-10-01', DATE '2025-03-31', 'COMPLETED', 2)
) AS v(id, year_name, name, start_date, end_date, status, sort_order) ON true
WHERE y.school_id = '20000000-0000-0000-0000-000000000001'
  AND y.name = v.year_name
  AND NOT EXISTS (SELECT 1 FROM academic_term t WHERE t.id = v.id);
