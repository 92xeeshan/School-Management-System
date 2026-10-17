-- =====================================================================
-- V32: Inventory / asset management (school supplies and lab equipment).
-- =====================================================================
SET app.bypass_rls = 'true';

INSERT INTO permission (code, module, name, description)
SELECT * FROM (VALUES
    ('INVENTORY_READ',   'inventory', 'Read inventory',   'View inventory items, categories, and stock history'),
    ('INVENTORY_MANAGE', 'inventory', 'Manage inventory', 'Create, update, and delete inventory items and stock movements'),
    ('INVENTORY_IMPORT', 'inventory', 'Import inventory', 'Bulk import inventory items from CSV')
) AS p(code, module, name, description)
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = p.code);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code IN ('INVENTORY_READ', 'INVENTORY_MANAGE', 'INVENTORY_IMPORT')
  AND r.code IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

CREATE TABLE inventory_category (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    name        VARCHAR(80) NOT NULL,
    description VARCHAR(300),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_inventory_category_school_name UNIQUE (school_id, name)
);
CREATE INDEX idx_inventory_category_school ON inventory_category(school_id);

CREATE TABLE inventory_item (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    category_id         UUID NOT NULL REFERENCES inventory_category(id) ON DELETE RESTRICT,
    name                VARCHAR(160) NOT NULL,
    sku                 VARCHAR(60),
    unit                VARCHAR(30) NOT NULL DEFAULT 'PCS',
    quantity            INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reorder_threshold   INTEGER NOT NULL DEFAULT 0 CHECK (reorder_threshold >= 0),
    purchase_date       DATE,
    purchase_cost       NUMERIC(12,2) CHECK (purchase_cost IS NULL OR purchase_cost >= 0),
    vendor              VARCHAR(120),
    location            VARCHAR(120),
    condition           VARCHAR(20) NOT NULL DEFAULT 'GOOD'
                        CHECK (condition IN ('NEW', 'GOOD', 'DAMAGED', 'UNDER_REPAIR', 'RETIRED')),
    assigned_to_user_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
    assigned_room       VARCHAR(120),
    created_by          UUID REFERENCES app_user(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_inventory_item_school_sku UNIQUE (school_id, sku)
);
CREATE INDEX idx_inventory_item_school ON inventory_item(school_id);
CREATE INDEX idx_inventory_item_category ON inventory_item(school_id, category_id);
CREATE INDEX idx_inventory_item_location ON inventory_item(school_id, location);
CREATE INDEX idx_inventory_item_condition ON inventory_item(school_id, condition);

CREATE TABLE inventory_transaction (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    item_id             UUID NOT NULL REFERENCES inventory_item(id) ON DELETE CASCADE,
    type                VARCHAR(20) NOT NULL
                        CHECK (type IN ('IN', 'OUT', 'CHECKOUT', 'CHECKIN')),
    quantity            INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reason              VARCHAR(240),
    assigned_to_user_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
    room                VARCHAR(120),
    transaction_date    DATE NOT NULL DEFAULT CURRENT_DATE,
    created_by          UUID REFERENCES app_user(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_inventory_txn_item ON inventory_transaction(item_id, transaction_date DESC, created_at DESC);
CREATE INDEX idx_inventory_txn_school ON inventory_transaction(school_id, transaction_date DESC);

ALTER TABLE inventory_category ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_category FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON inventory_category
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE inventory_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_item FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON inventory_item
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE inventory_transaction ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_transaction FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON inventory_transaction
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

GRANT SELECT, INSERT, UPDATE, DELETE ON inventory_category TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON inventory_category TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON inventory_item TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON inventory_item TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON inventory_transaction TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON inventory_transaction TO app_admin;

INSERT INTO inventory_category (id, school_id, name, description) VALUES
    ('b1000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
     'Lab Equipment', 'Science and computer lab equipment'),
    ('b1000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001',
     'Furniture', 'Classrooms, offices, and common areas'),
    ('b1000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001',
     'Stationery', 'Consumable classroom and office supplies'),
    ('b1000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000001',
     'Sports Gear', 'Playground and indoor sports equipment')
ON CONFLICT (school_id, name) DO NOTHING;

INSERT INTO inventory_item
    (id, school_id, category_id, name, sku, unit, quantity, reorder_threshold,
     purchase_date, purchase_cost, vendor, location, condition, created_by)
VALUES
    ('b2000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
     'b1000000-0000-0000-0000-000000000001', 'Digital projector', 'LAB-PRJ-001', 'PCS', 2, 1,
     DATE '2025-06-12', 28500.00, 'AV Systems', 'Lab 1', 'GOOD',
     '20000000-0000-0000-0000-000000000002'),
    ('b2000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001',
     'b1000000-0000-0000-0000-000000000003', 'A4 copier paper', 'STN-PAP-A4', 'REAM', 8, 12,
     DATE '2026-03-02', 2400.00, 'Office Mart', 'Store room', 'NEW',
     '20000000-0000-0000-0000-000000000002'),
    ('b2000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001',
     'b1000000-0000-0000-0000-000000000002', 'Student desk', 'FUR-DSK-001', 'PCS', 40, 10,
     DATE '2024-04-18', 52000.00, 'School Furnishers', 'Class 5A', 'GOOD',
     '20000000-0000-0000-0000-000000000002'),
    ('b2000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000001',
     'b1000000-0000-0000-0000-000000000004', 'Football', 'SPT-FB-001', 'PCS', 3, 6,
     DATE '2026-01-20', 1800.00, 'Sports Hub', 'Sports store', 'GOOD',
     '20000000-0000-0000-0000-000000000002')
ON CONFLICT (school_id, sku) DO NOTHING;

INSERT INTO inventory_transaction
    (id, school_id, item_id, type, quantity, reason, room, transaction_date, created_by)
VALUES
    ('b3000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
     'b2000000-0000-0000-0000-000000000001', 'IN', 2, 'Initial purchase', 'Lab 1', DATE '2025-06-12',
     '20000000-0000-0000-0000-000000000002'),
    ('b3000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001',
     'b2000000-0000-0000-0000-000000000002', 'IN', 20, 'Opening stock', 'Store room', DATE '2026-03-02',
     '20000000-0000-0000-0000-000000000002'),
    ('b3000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001',
     'b2000000-0000-0000-0000-000000000002', 'OUT', 12, 'Issued to office', 'Admin office', DATE '2026-04-15',
     '20000000-0000-0000-0000-000000000002')
ON CONFLICT DO NOTHING;
