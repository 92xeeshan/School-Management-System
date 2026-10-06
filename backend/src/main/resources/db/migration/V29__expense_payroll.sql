-- =====================================================================
-- V29: Expense and payroll (greenfield).
-- payroll_run: DRAFT -> PROCESSED -> PUBLISHED -> PAID
-- payslip stores salary snapshots; no pdf_url (PDFs are streamed).
-- =====================================================================
SET app.bypass_rls = 'true';

INSERT INTO permission (code, module, name, description)
SELECT * FROM (VALUES
    ('EXPENSE_READ',     'expense', 'Read expenses',     'View school expenses and charts'),
    ('EXPENSE_MANAGE',   'expense', 'Manage expenses',   'Create and update school expenses'),
    ('PAYROLL_READ',     'payroll', 'Read payroll',      'View payroll runs and payslips'),
    ('PAYROLL_MANAGE',   'payroll', 'Manage payroll',    'Create salary structures and process payroll'),
    ('PAYROLL_PUBLISH',  'payroll', 'Publish payroll',   'Publish processed payroll so staff can view payslips'),
    ('PAYSLIP_READ',     'payroll', 'Read own payslips', 'View and download own published payslips')
) AS p(code, module, name, description)
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = p.code);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code IN ('EXPENSE_READ', 'EXPENSE_MANAGE', 'PAYROLL_READ', 'PAYROLL_MANAGE', 'PAYROLL_PUBLISH')
  AND r.code IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code = 'PAYSLIP_READ'
  AND r.code IN ('SUPER_ADMIN', 'ADMIN', 'TEACHER')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

CREATE TABLE expense_category (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    name        VARCHAR(80) NOT NULL,
    code        VARCHAR(30) NOT NULL,
    description VARCHAR(300),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_expense_category_school UNIQUE (school_id, code)
);
CREATE INDEX idx_expense_category_school ON expense_category(school_id);

CREATE TABLE expense (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id        UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    category_id      UUID NOT NULL REFERENCES expense_category(id) ON DELETE RESTRICT,
    amount           NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    expense_date     DATE NOT NULL,
    vendor           VARCHAR(120),
    description      VARCHAR(500),
    payment_method   VARCHAR(20) NOT NULL DEFAULT 'CASH'
                     CHECK (payment_method IN ('CASH', 'CARD', 'UPI', 'BANK_TRANSFER')),
    recorded_by      UUID REFERENCES app_user(id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_expense_school_date ON expense(school_id, expense_date);
CREATE INDEX idx_expense_school_category ON expense(school_id, category_id);

CREATE TABLE staff_salary_structure (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id        UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    staff_type       VARCHAR(20) NOT NULL CHECK (staff_type IN ('TEACHING', 'NON_TEACHING')),
    staff_id         UUID NOT NULL,
    basic            NUMERIC(12,2) NOT NULL CHECK (basic >= 0),
    hra              NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (hra >= 0),
    allowances_json  TEXT NOT NULL DEFAULT '[]',
    deductions_json  TEXT NOT NULL DEFAULT '[]',
    effective_from   DATE NOT NULL,
    effective_to     DATE,
    status           VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_staff_salary_active UNIQUE (school_id, staff_type, staff_id, effective_from)
);
CREATE INDEX idx_staff_salary_school ON staff_salary_structure(school_id, status);

CREATE TABLE payroll_run (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id      UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    year           SMALLINT NOT NULL CHECK (year BETWEEN 2000 AND 2100),
    month          SMALLINT NOT NULL CHECK (month BETWEEN 1 AND 12),
    status         VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                   CHECK (status IN ('DRAFT', 'PROCESSED', 'PUBLISHED', 'PAID')),
    processed_at   TIMESTAMPTZ,
    published_at   TIMESTAMPTZ,
    paid_at        TIMESTAMPTZ,
    processed_by   UUID REFERENCES app_user(id),
    published_by   UUID REFERENCES app_user(id),
    notes          VARCHAR(500),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_payroll_run_period UNIQUE (school_id, year, month)
);
CREATE INDEX idx_payroll_run_school ON payroll_run(school_id, year DESC, month DESC);

CREATE TABLE payslip (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    payroll_run_id    UUID NOT NULL REFERENCES payroll_run(id) ON DELETE CASCADE,
    staff_type        VARCHAR(20) NOT NULL CHECK (staff_type IN ('TEACHING', 'NON_TEACHING')),
    staff_id          UUID NOT NULL,
    user_id           UUID REFERENCES app_user(id),
    employee_no       VARCHAR(30) NOT NULL,
    staff_name        VARCHAR(120) NOT NULL,
    designation       VARCHAR(100),
    department        VARCHAR(100),
    basic             NUMERIC(12,2) NOT NULL CHECK (basic >= 0),
    hra               NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (hra >= 0),
    allowances_json   TEXT NOT NULL DEFAULT '[]',
    deductions_json   TEXT NOT NULL DEFAULT '[]',
    gross             NUMERIC(12,2) NOT NULL CHECK (gross >= 0),
    total_deductions  NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total_deductions >= 0),
    net               NUMERIC(12,2) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_payslip_run_staff UNIQUE (payroll_run_id, staff_type, staff_id)
);
CREATE INDEX idx_payslip_school_user ON payslip(school_id, user_id);
CREATE INDEX idx_payslip_run ON payslip(payroll_run_id);

ALTER TABLE expense_category ENABLE ROW LEVEL SECURITY;
ALTER TABLE expense_category FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON expense_category
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE expense ENABLE ROW LEVEL SECURITY;
ALTER TABLE expense FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON expense
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE staff_salary_structure ENABLE ROW LEVEL SECURITY;
ALTER TABLE staff_salary_structure FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON staff_salary_structure
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE payroll_run ENABLE ROW LEVEL SECURITY;
ALTER TABLE payroll_run FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON payroll_run
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE payslip ENABLE ROW LEVEL SECURITY;
ALTER TABLE payslip FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON payslip
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

GRANT SELECT, INSERT, UPDATE, DELETE ON expense_category TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON expense_category TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON expense TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON expense TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON staff_salary_structure TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON staff_salary_structure TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON payroll_run TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON payroll_run TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON payslip TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON payslip TO app_admin;

INSERT INTO expense_category (id, school_id, name, code, description) VALUES
    ('a1000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
     'Utilities', 'UTILITIES', 'Electricity, water, internet'),
    ('a1000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001',
     'Supplies', 'SUPPLIES', 'Classroom and office supplies'),
    ('a1000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001',
     'Maintenance', 'MAINTENANCE', 'Building and equipment maintenance'),
    ('a1000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000001',
     'Transport', 'TRANSPORT', 'Bus and vehicle costs'),
    ('a1000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000001',
     'Salary overhead', 'SALARY', 'Contract and overtime payouts outside payroll')
ON CONFLICT (school_id, code) DO NOTHING;

INSERT INTO expense
    (id, school_id, category_id, amount, expense_date, vendor, description, payment_method, recorded_by)
VALUES
    ('a2000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
     'a1000000-0000-0000-0000-000000000001', 18500.00, DATE '2026-04-08', 'BESCOM', 'April electricity', 'BANK_TRANSFER',
     '20000000-0000-0000-0000-000000000002'),
    ('a2000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001',
     'a1000000-0000-0000-0000-000000000002', 6400.00, DATE '2026-04-18', 'Office Mart', 'Stationery restock', 'CASH',
     '20000000-0000-0000-0000-000000000002'),
    ('a2000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001',
     'a1000000-0000-0000-0000-000000000001', 19200.00, DATE '2026-05-07', 'BESCOM', 'May electricity', 'BANK_TRANSFER',
     '20000000-0000-0000-0000-000000000002'),
    ('a2000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000001',
     'a1000000-0000-0000-0000-000000000003', 12500.00, DATE '2026-05-22', 'QuickFix', 'Plumbing repair', 'UPI',
     '20000000-0000-0000-0000-000000000002'),
    ('a2000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000001',
     'a1000000-0000-0000-0000-000000000004', 28000.00, DATE '2026-06-05', 'City Fuels', 'Bus diesel', 'CARD',
     '20000000-0000-0000-0000-000000000002'),
    ('a2000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000001',
     'a1000000-0000-0000-0000-000000000001', 17600.00, DATE '2026-06-09', 'BESCOM', 'June electricity', 'BANK_TRANSFER',
     '20000000-0000-0000-0000-000000000002'),
    ('a2000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000001',
     'a1000000-0000-0000-0000-000000000002', 9100.00, DATE '2026-07-14', 'Office Mart', 'Exam paper stock', 'CASH',
     '20000000-0000-0000-0000-000000000002'),
    ('a2000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000001',
     'a1000000-0000-0000-0000-000000000004', 26500.00, DATE '2026-07-20', 'City Fuels', 'Bus diesel', 'CARD',
     '20000000-0000-0000-0000-000000000002'),
    ('a2000000-0000-0000-0000-000000000009', '20000000-0000-0000-0000-000000000001',
     'a1000000-0000-0000-0000-000000000001', 20100.00, DATE '2026-08-08', 'BESCOM', 'August electricity', 'BANK_TRANSFER',
     '20000000-0000-0000-0000-000000000002'),
    ('a2000000-0000-0000-0000-00000000000a', '20000000-0000-0000-0000-000000000001',
     'a1000000-0000-0000-0000-000000000003', 8400.00, DATE '2026-08-19', 'CoolAir', 'AC service', 'UPI',
     '20000000-0000-0000-0000-000000000002'),
    ('a2000000-0000-0000-0000-00000000000b', '20000000-0000-0000-0000-000000000001',
     'a1000000-0000-0000-0000-000000000005', 15000.00, DATE '2026-09-02', 'Contract staff', 'Exam invigilation overtime', 'BANK_TRANSFER',
     '20000000-0000-0000-0000-000000000002'),
    ('a2000000-0000-0000-0000-00000000000c', '20000000-0000-0000-0000-000000000001',
     'a1000000-0000-0000-0000-000000000001', 18800.00, DATE '2026-09-09', 'BESCOM', 'September electricity', 'BANK_TRANSFER',
     '20000000-0000-0000-0000-000000000002')
ON CONFLICT (id) DO NOTHING;

INSERT INTO staff_salary_structure
    (id, school_id, staff_type, staff_id, basic, hra, allowances_json, deductions_json, effective_from, status)
VALUES
    ('a3000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
     'TEACHING', '30000000-0000-0000-0000-000000000041',
     35000.00, 14000.00,
     '[{"name":"Dearness Allowance","amount":5000},{"name":"Transport","amount":2000}]',
     '[{"name":"Provident Fund","amount":4200},{"name":"Professional Tax","amount":200}]',
     DATE '2026-04-01', 'ACTIVE'),
    ('a3000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001',
     'NON_TEACHING', '90000000-0000-0000-0000-000000000001',
     28000.00, 8000.00,
     '[{"name":"Transport","amount":1500}]',
     '[{"name":"Provident Fund","amount":3360},{"name":"Professional Tax","amount":200}]',
     DATE '2026-04-01', 'ACTIVE')
ON CONFLICT (school_id, staff_type, staff_id, effective_from) DO NOTHING;

INSERT INTO payroll_run
    (id, school_id, year, month, status, processed_at, published_at, processed_by, published_by)
VALUES
    ('a4000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
     2026, 8, 'PUBLISHED', TIMESTAMPTZ '2026-08-28 10:00:00+05:30', TIMESTAMPTZ '2026-08-29 11:00:00+05:30',
     '20000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002'),
    ('a4000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001',
     2026, 9, 'PROCESSED', TIMESTAMPTZ '2026-09-28 10:00:00+05:30', NULL,
     '20000000-0000-0000-0000-000000000002', NULL)
ON CONFLICT (school_id, year, month) DO NOTHING;

INSERT INTO payslip
    (id, school_id, payroll_run_id, staff_type, staff_id, user_id, employee_no, staff_name,
     designation, department, basic, hra, allowances_json, deductions_json, gross, total_deductions, net)
VALUES
    ('a5000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
     'a4000000-0000-0000-0000-000000000001', 'TEACHING', '30000000-0000-0000-0000-000000000041',
     '20000000-0000-0000-0000-000000000003', 'EMP001', 'Asha Sharma', 'Primary Teacher', 'Primary',
     35000.00, 14000.00,
     '[{"name":"Dearness Allowance","amount":5000},{"name":"Transport","amount":2000}]',
     '[{"name":"Provident Fund","amount":4200},{"name":"Professional Tax","amount":200}]',
     56000.00, 4400.00, 51600.00),
    ('a5000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001',
     'a4000000-0000-0000-0000-000000000001', 'NON_TEACHING', '90000000-0000-0000-0000-000000000001',
     NULL, 'NTS001', 'Ravi Verma', 'Accountant', 'Administration',
     28000.00, 8000.00,
     '[{"name":"Transport","amount":1500}]',
     '[{"name":"Provident Fund","amount":3360},{"name":"Professional Tax","amount":200}]',
     37500.00, 3560.00, 33940.00),
    ('a5000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001',
     'a4000000-0000-0000-0000-000000000002', 'TEACHING', '30000000-0000-0000-0000-000000000041',
     '20000000-0000-0000-0000-000000000003', 'EMP001', 'Asha Sharma', 'Primary Teacher', 'Primary',
     35000.00, 14000.00,
     '[{"name":"Dearness Allowance","amount":5000},{"name":"Transport","amount":2000}]',
     '[{"name":"Provident Fund","amount":4200},{"name":"Professional Tax","amount":200}]',
     56000.00, 4400.00, 51600.00),
    ('a5000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000001',
     'a4000000-0000-0000-0000-000000000002', 'NON_TEACHING', '90000000-0000-0000-0000-000000000001',
     NULL, 'NTS001', 'Ravi Verma', 'Accountant', 'Administration',
     28000.00, 8000.00,
     '[{"name":"Transport","amount":1500}]',
     '[{"name":"Provident Fund","amount":3360},{"name":"Professional Tax","amount":200}]',
     37500.00, 3560.00, 33940.00)
ON CONFLICT (payroll_run_id, staff_type, staff_id) DO NOTHING;
