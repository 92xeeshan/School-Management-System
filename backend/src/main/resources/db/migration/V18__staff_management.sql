-- =====================================================================
-- V18: Staff management.
-- Adds STAFF_READ/STAFF_CREATE/STAFF_UPDATE/STAFF_DELETE permissions,
-- enriches teacher_profile with staff profile columns, creates the
-- non_teaching_staff table with tenant RLS, and seeds demo staff.
-- =====================================================================
SET app.bypass_rls = 'true';

-- Permissions ---------------------------------------------------------
INSERT INTO permission (code, module, name, description)
SELECT * FROM (VALUES
    ('STAFF_READ',   'staff', 'Read staff',   'View teaching and non-teaching staff records'),
    ('STAFF_CREATE', 'staff', 'Create staff', 'Add teaching and non-teaching staff records'),
    ('STAFF_UPDATE', 'staff', 'Update staff', 'Edit staff records and class teacher assignments'),
    ('STAFF_DELETE', 'staff', 'Delete staff', 'Deactivate teaching and non-teaching staff records')
) AS p(code, module, name, description)
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = p.code);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code = 'STAFF_READ'
  AND r.code IN ('SUPER_ADMIN', 'ADMIN', 'TEACHER')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code IN ('STAFF_CREATE', 'STAFF_UPDATE', 'STAFF_DELETE')
  AND r.code IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- Teaching staff profile columns --------------------------------------
ALTER TABLE teacher_profile
    ADD COLUMN IF NOT EXISTS gender          VARCHAR(10)
        CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    ADD COLUMN IF NOT EXISTS date_of_birth   DATE,
    ADD COLUMN IF NOT EXISTS department      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS employment_type VARCHAR(20)
        CHECK (employment_type IN ('FULL_TIME', 'PART_TIME', 'CONTRACT', 'VOLUNTEER')),
    ADD COLUMN IF NOT EXISTS address         VARCHAR(255);

-- Non-teaching staff --------------------------------------------------
CREATE TABLE non_teaching_staff (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    user_id         UUID REFERENCES app_user(id),
    employee_no     VARCHAR(30) NOT NULL,
    first_name      VARCHAR(60) NOT NULL,
    last_name       VARCHAR(60),
    email           VARCHAR(120),
    phone           VARCHAR(30),
    gender          VARCHAR(10) CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    date_of_birth   DATE,
    designation     VARCHAR(100),
    department      VARCHAR(100),
    qualification   VARCHAR(150),
    employment_type VARCHAR(20)
        CHECK (employment_type IN ('FULL_TIME', 'PART_TIME', 'CONTRACT', 'VOLUNTEER')),
    join_date       DATE,
    address         VARCHAR(255),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_non_teaching_staff_school_emp UNIQUE (school_id, employee_no)
);
CREATE INDEX idx_non_teaching_staff_school ON non_teaching_staff(school_id, status);

ALTER TABLE non_teaching_staff ENABLE ROW LEVEL SECURITY;
ALTER TABLE non_teaching_staff FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON non_teaching_staff
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

GRANT SELECT, INSERT, UPDATE, DELETE ON non_teaching_staff TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON non_teaching_staff TO app_admin;

-- Demo data -----------------------------------------------------------
UPDATE teacher_profile
SET gender          = COALESCE(gender, 'FEMALE'),
    department      = COALESCE(department, 'Primary'),
    employment_type = COALESCE(employment_type, 'FULL_TIME'),
    qualification   = COALESCE(qualification, 'B.Ed'),
    join_date       = COALESCE(join_date, DATE '2022-06-01')
WHERE school_id = '20000000-0000-0000-0000-000000000001';

INSERT INTO non_teaching_staff
    (id, school_id, employee_no, first_name, last_name, email, phone, gender,
     designation, department, qualification, employment_type, join_date, status)
VALUES
    ('90000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
     'NTS001', 'Ravi', 'Verma', 'ravi.verma@demo.school', '9876510001', 'MALE',
     'Accountant', 'Administration', 'B.Com', 'FULL_TIME', DATE '2021-04-12', 'ACTIVE'),
    ('90000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001',
     'NTS002', 'Meera', 'Iyer', 'meera.iyer@demo.school', '9876510002', 'FEMALE',
     'Librarian', 'Library', 'M.Lib.Sc', 'FULL_TIME', DATE '2022-07-01', 'ACTIVE'),
    ('90000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001',
     'NTS003', 'Sana', 'Khan', 'sana.khan@demo.school', '9876510003', 'FEMALE',
     'Front Office Executive', 'Administration', 'B.A', 'FULL_TIME', DATE '2023-01-09', 'ACTIVE'),
    ('90000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000001',
     'NTS004', 'Imran', 'Sheikh', 'imran.sheikh@demo.school', '9876510004', 'MALE',
     'Lab Assistant', 'Science Lab', 'B.Sc', 'CONTRACT', DATE '2024-08-19', 'ACTIVE'),
    ('90000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000001',
     'NTS005', 'Dinesh', 'Patil', NULL, '9876510005', 'MALE',
     'Security Guard', 'Facilities', NULL, 'PART_TIME', DATE '2023-11-02', 'ACTIVE')
ON CONFLICT (school_id, employee_no) DO NOTHING;
