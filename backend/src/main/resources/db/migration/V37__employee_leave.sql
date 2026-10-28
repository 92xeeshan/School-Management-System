-- =====================================================================
-- V37: Employee leave management (teaching and non-teaching staff only).
-- =====================================================================
SET app.bypass_rls = 'true';

INSERT INTO permission (code, module, name, description)
SELECT * FROM (VALUES
    ('LEAVE_READ',    'leave', 'Read leave',    'View own leave balances, requests and history'),
    ('LEAVE_APPLY',   'leave', 'Apply leave',   'Submit, withdraw and cancel own leave requests'),
    ('LEAVE_MANAGE',  'leave', 'Manage leave',  'Configure leave types, policies, balances and reports'),
    ('LEAVE_APPROVE', 'leave', 'Approve leave', 'Approve or reject staff leave requests')
) AS p(code, module, name, description)
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = p.code);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code IN ('LEAVE_READ', 'LEAVE_APPLY', 'LEAVE_MANAGE', 'LEAVE_APPROVE')
  AND r.code IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code IN ('LEAVE_READ', 'LEAVE_APPLY')
  AND r.code = 'TEACHER'
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

ALTER TABLE notifications DROP CONSTRAINT IF EXISTS chk_notifications_category;
ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_category
    CHECK (category IN ('EXAM', 'ATTENDANCE', 'FEE', 'ASSIGNMENT', 'CERTIFICATE', 'LEAVE'));

CREATE TABLE leave_type (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    code              VARCHAR(20) NOT NULL,
    name              VARCHAR(80) NOT NULL,
    yearly_quota      NUMERIC(6,1) NOT NULL DEFAULT 0,
    paid              BOOLEAN NOT NULL DEFAULT true,
    reason_mandatory  BOOLEAN NOT NULL DEFAULT true,
    active            BOOLEAN NOT NULL DEFAULT true,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_leave_type_school_code UNIQUE (school_id, code),
    CONSTRAINT chk_leave_type_quota CHECK (yearly_quota >= 0)
);
CREATE INDEX idx_leave_type_school ON leave_type (school_id);

CREATE TABLE leave_policy (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    name            VARCHAR(80) NOT NULL,
    staff_category  VARCHAR(20) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_leave_policy_school_name UNIQUE (school_id, name),
    CONSTRAINT chk_leave_policy_category CHECK (staff_category IN ('TEACHING', 'NON_TEACHING', 'ALL'))
);
CREATE INDEX idx_leave_policy_school ON leave_policy (school_id);

CREATE TABLE leave_policy_item (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id      UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    policy_id      UUID NOT NULL REFERENCES leave_policy(id) ON DELETE CASCADE,
    leave_type_id  UUID NOT NULL REFERENCES leave_type(id) ON DELETE RESTRICT,
    yearly_quota   NUMERIC(6,1) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_leave_policy_item UNIQUE (policy_id, leave_type_id),
    CONSTRAINT chk_leave_policy_item_quota CHECK (yearly_quota >= 0)
);
CREATE INDEX idx_leave_policy_item_policy ON leave_policy_item (policy_id);

CREATE TABLE leave_year_setting (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    mode        VARCHAR(20) NOT NULL DEFAULT 'ACADEMIC',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_leave_year_setting_school UNIQUE (school_id),
    CONSTRAINT chk_leave_year_mode CHECK (mode IN ('ACADEMIC', 'CALENDAR'))
);

CREATE TABLE leave_assignment (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id    UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    staff_type   VARCHAR(20) NOT NULL,
    staff_id     UUID NOT NULL,
    policy_id    UUID NOT NULL REFERENCES leave_policy(id) ON DELETE RESTRICT,
    year_start   DATE NOT NULL,
    assigned_by  UUID REFERENCES app_user(id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_leave_assignment_staff UNIQUE (school_id, staff_type, staff_id),
    CONSTRAINT chk_leave_assignment_staff_type CHECK (staff_type IN ('TEACHING', 'NON_TEACHING'))
);
CREATE INDEX idx_leave_assignment_policy ON leave_assignment (school_id, policy_id);

CREATE TABLE leave_balance (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id      UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    staff_type     VARCHAR(20) NOT NULL,
    staff_id       UUID NOT NULL,
    leave_type_id  UUID NOT NULL REFERENCES leave_type(id) ON DELETE RESTRICT,
    year_start     DATE NOT NULL,
    entitled       NUMERIC(6,1) NOT NULL DEFAULT 0,
    used           NUMERIC(6,1) NOT NULL DEFAULT 0,
    pending        NUMERIC(6,1) NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_leave_balance UNIQUE (school_id, staff_type, staff_id, leave_type_id, year_start),
    CONSTRAINT chk_leave_balance_staff_type CHECK (staff_type IN ('TEACHING', 'NON_TEACHING')),
    CONSTRAINT chk_leave_balance_non_negative CHECK (entitled >= 0 AND used >= 0 AND pending >= 0)
);
CREATE INDEX idx_leave_balance_staff ON leave_balance (school_id, staff_type, staff_id, year_start);

CREATE TABLE leave_balance_ledger (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id      UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    staff_type     VARCHAR(20) NOT NULL,
    staff_id       UUID NOT NULL,
    leave_type_id  UUID NOT NULL REFERENCES leave_type(id) ON DELETE RESTRICT,
    year_start     DATE NOT NULL,
    entry_type     VARCHAR(20) NOT NULL,
    days           NUMERIC(6,1) NOT NULL,
    request_id     UUID,
    reason         VARCHAR(500),
    created_by     UUID REFERENCES app_user(id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_leave_ledger_type CHECK (entry_type IN ('CREDIT', 'PENDING', 'USED', 'RELEASE', 'ADJUST')),
    CONSTRAINT chk_leave_ledger_staff_type CHECK (staff_type IN ('TEACHING', 'NON_TEACHING'))
);
CREATE INDEX idx_leave_ledger_staff ON leave_balance_ledger (school_id, staff_type, staff_id, year_start);

CREATE TABLE leave_request (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id            UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    staff_type           VARCHAR(20) NOT NULL,
    staff_id             UUID NOT NULL,
    user_id              UUID REFERENCES app_user(id),
    leave_type_id        UUID NOT NULL REFERENCES leave_type(id) ON DELETE RESTRICT,
    from_date            DATE NOT NULL,
    to_date              DATE NOT NULL,
    days                 NUMERIC(6,1) NOT NULL,
    reason               VARCHAR(500) NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    supporting_doc_key   VARCHAR(500),
    supporting_doc_name  VARCHAR(255),
    rejection_reason     VARCHAR(500),
    cancel_reason        VARCHAR(500),
    decided_by           UUID REFERENCES app_user(id),
    decided_at           TIMESTAMPTZ,
    cancelled_by         UUID REFERENCES app_user(id),
    cancelled_at         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_leave_request_staff_type CHECK (staff_type IN ('TEACHING', 'NON_TEACHING')),
    CONSTRAINT chk_leave_request_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_leave_request_dates CHECK (to_date >= from_date),
    CONSTRAINT chk_leave_request_days CHECK (days > 0)
);
CREATE INDEX idx_leave_request_staff ON leave_request (school_id, staff_type, staff_id, created_at DESC);
CREATE INDEX idx_leave_request_status ON leave_request (school_id, status, created_at DESC);
CREATE UNIQUE INDEX uq_leave_request_pending
    ON leave_request (school_id, staff_type, staff_id, leave_type_id, from_date, to_date)
    WHERE status = 'PENDING';

CREATE TABLE leave_audit (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    entity_type VARCHAR(40) NOT NULL,
    entity_id   UUID,
    action      VARCHAR(40) NOT NULL,
    details     VARCHAR(1000),
    changed_by  UUID REFERENCES app_user(id),
    changed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_leave_audit_school ON leave_audit (school_id, changed_at DESC);

DO $$
DECLARE
    tbl TEXT;
BEGIN
    FOREACH tbl IN ARRAY ARRAY[
        'leave_type', 'leave_policy', 'leave_policy_item', 'leave_year_setting',
        'leave_assignment', 'leave_balance', 'leave_balance_ledger', 'leave_request', 'leave_audit'
    ]
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

INSERT INTO leave_year_setting (id, school_id, mode)
VALUES ('a2000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'ACADEMIC')
ON CONFLICT DO NOTHING;

INSERT INTO leave_type (id, school_id, code, name, yearly_quota, paid, reason_mandatory, active)
VALUES
    ('a1000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
     'CL', 'Casual Leave', 12, true, true, true),
    ('a1000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001',
     'ML', 'Medical Leave', 10, true, true, true),
    ('a1000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001',
     'EL', 'Earned Leave', 15, true, true, true),
    ('a1000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000001',
     'MAT', 'Maternity Leave', 180, true, true, true),
    ('a1000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000001',
     'PAT', 'Paternity Leave', 15, true, true, true),
    ('a1000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000001',
     'LWP', 'Leave Without Pay', 0, false, true, true)
ON CONFLICT DO NOTHING;

INSERT INTO leave_policy (id, school_id, name, staff_category, active)
VALUES
    ('b1000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
     'Teaching', 'TEACHING', true),
    ('b1000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001',
     'Non-teaching', 'NON_TEACHING', true)
ON CONFLICT DO NOTHING;

INSERT INTO leave_policy_item (id, school_id, policy_id, leave_type_id, yearly_quota)
SELECT gen_random_uuid(), '20000000-0000-0000-0000-000000000001', p.policy_id, t.id, t.yearly_quota
FROM (VALUES
    ('b1000000-0000-0000-0000-000000000001'::uuid),
    ('b1000000-0000-0000-0000-000000000002'::uuid)
) AS p(policy_id)
CROSS JOIN leave_type t
WHERE t.school_id = '20000000-0000-0000-0000-000000000001'
  AND NOT EXISTS (
      SELECT 1 FROM leave_policy_item i
      WHERE i.policy_id = p.policy_id AND i.leave_type_id = t.id);

INSERT INTO leave_assignment (id, school_id, staff_type, staff_id, policy_id, year_start)
SELECT gen_random_uuid(), t.school_id, 'TEACHING', t.id,
       'b1000000-0000-0000-0000-000000000001', DATE '2025-04-01'
FROM teacher_profile t
WHERE t.school_id = '20000000-0000-0000-0000-000000000001'
  AND NOT EXISTS (
      SELECT 1 FROM leave_assignment a
      WHERE a.school_id = t.school_id AND a.staff_type = 'TEACHING' AND a.staff_id = t.id);

INSERT INTO leave_assignment (id, school_id, staff_type, staff_id, policy_id, year_start)
SELECT gen_random_uuid(), s.school_id, 'NON_TEACHING', s.id,
       'b1000000-0000-0000-0000-000000000002', DATE '2025-04-01'
FROM non_teaching_staff s
WHERE s.school_id = '20000000-0000-0000-0000-000000000001'
  AND NOT EXISTS (
      SELECT 1 FROM leave_assignment a
      WHERE a.school_id = s.school_id AND a.staff_type = 'NON_TEACHING' AND a.staff_id = s.id);

INSERT INTO leave_balance (id, school_id, staff_type, staff_id, leave_type_id, year_start, entitled, used, pending)
SELECT gen_random_uuid(), a.school_id, a.staff_type, a.staff_id, i.leave_type_id, a.year_start, i.yearly_quota, 0, 0
FROM leave_assignment a
JOIN leave_policy_item i ON i.policy_id = a.policy_id
WHERE a.school_id = '20000000-0000-0000-0000-000000000001'
  AND NOT EXISTS (
      SELECT 1 FROM leave_balance b
      WHERE b.school_id = a.school_id
        AND b.staff_type = a.staff_type
        AND b.staff_id = a.staff_id
        AND b.leave_type_id = i.leave_type_id
        AND b.year_start = a.year_start);

INSERT INTO leave_balance_ledger (id, school_id, staff_type, staff_id, leave_type_id, year_start, entry_type, days, reason)
SELECT gen_random_uuid(), b.school_id, b.staff_type, b.staff_id, b.leave_type_id, b.year_start,
       'CREDIT', b.entitled, 'Initial policy credit'
FROM leave_balance b
WHERE b.school_id = '20000000-0000-0000-0000-000000000001'
  AND NOT EXISTS (
      SELECT 1 FROM leave_balance_ledger l
      WHERE l.school_id = b.school_id
        AND l.staff_type = b.staff_type
        AND l.staff_id = b.staff_id
        AND l.leave_type_id = b.leave_type_id
        AND l.year_start = b.year_start
        AND l.entry_type = 'CREDIT');
