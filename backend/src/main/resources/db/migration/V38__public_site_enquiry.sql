-- =====================================================================
-- V38: Public school landing (brochure) and admission enquiry inbox.
-- school_id tenancy only. No branches. ADMIN / SUPER_ADMIN permissions.
-- =====================================================================
SET app.bypass_rls = 'true';

INSERT INTO permission (code, module, name, description)
SELECT * FROM (VALUES
    ('PUBLIC_SITE_MANAGE', 'website', 'Manage public site', 'Edit the public school landing page'),
    ('ENQUIRY_READ',       'enquiry', 'Read enquiries',     'View admission enquiry inbox'),
    ('ENQUIRY_MANAGE',     'enquiry', 'Manage enquiries',   'Update enquiry status and notes')
) AS p(code, module, name, description)
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = p.code);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code IN ('PUBLIC_SITE_MANAGE', 'ENQUIRY_READ', 'ENQUIRY_MANAGE')
  AND r.code IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

ALTER TABLE notifications DROP CONSTRAINT IF EXISTS chk_notifications_category;
ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_category
    CHECK (category IN ('EXAM', 'ATTENDANCE', 'FEE', 'ASSIGNMENT', 'CERTIFICATE', 'LEAVE', 'ENQUIRY'));

CREATE TABLE public_site (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    published       BOOLEAN NOT NULL DEFAULT true,
    hero_image_url  VARCHAR(500),
    principal_name  VARCHAR(120),
    updated_by      UUID REFERENCES app_user(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_public_site_school UNIQUE (school_id)
);

CREATE TABLE public_site_copy (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    locale              VARCHAR(10) NOT NULL,
    tagline             VARCHAR(300),
    about               TEXT,
    principal_message   TEXT,
    facilities          TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_public_site_copy UNIQUE (school_id, locale),
    CONSTRAINT chk_public_site_copy_locale CHECK (locale IN ('en', 'hi', 'ur'))
);
CREATE INDEX idx_public_site_copy_school ON public_site_copy (school_id);

CREATE TABLE enquiry (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id    UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    parent_name  VARCHAR(120) NOT NULL,
    phone        VARCHAR(20) NOT NULL,
    email        VARCHAR(120),
    class_id     UUID REFERENCES school_class(id) ON DELETE SET NULL,
    class_name   VARCHAR(80),
    message      VARCHAR(1000),
    status       VARCHAR(20) NOT NULL DEFAULT 'NEW',
    notes        VARCHAR(1000),
    reviewed_by  UUID REFERENCES app_user(id),
    reviewed_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_enquiry_status CHECK (status IN ('NEW', 'CONTACTED', 'CLOSED'))
);
CREATE INDEX idx_enquiry_school_status ON enquiry (school_id, status, created_at DESC);
CREATE INDEX idx_enquiry_school_created ON enquiry (school_id, created_at DESC);

DO $$
DECLARE
    tbl TEXT;
BEGIN
    FOREACH tbl IN ARRAY ARRAY['public_site', 'public_site_copy', 'enquiry']
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

INSERT INTO public_site (id, school_id, published, principal_name)
VALUES (
    'e1000000-0000-0000-0000-000000000001',
    '20000000-0000-0000-0000-000000000001',
    true,
    'Dr. Meera Joshi'
)
ON CONFLICT DO NOTHING;

INSERT INTO public_site_copy (id, school_id, locale, tagline, about, principal_message, facilities)
VALUES
    (
        'e1000000-0000-0000-0000-000000000011',
        '20000000-0000-0000-0000-000000000001',
        'en',
        'Quality education for every child',
        'Demo Public School is a co-educational day school. We combine strong academics with sports, arts and values so every student can thrive.',
        'Welcome to Demo Public School. We partner with families to help every child grow in knowledge, character and confidence.',
        E'Smart classrooms\nScience laboratories\nLibrary and reading room\nSports ground\nSchool transport'
    ),
    (
        'e1000000-0000-0000-0000-000000000012',
        '20000000-0000-0000-0000-000000000001',
        'hi',
        'हर बच्चे के लिए गुणवत्तापूर्ण शिक्षा',
        'डेमो पब्लिक स्कूल एक सह-शिक्षा दिवसीय विद्यालय है। हम मजबूत शिक्षा को खेल, कला और मूल्यों के साथ जोड़ते हैं ताकि प्रत्येक छात्र आगे बढ़ सके।',
        'डेमो पब्लिक स्कूल में आपका स्वागत है। हम परिवारों के साथ मिलकर प्रत्येक बच्चे के ज्ञान, चरित्र और आत्मविश्वास को बढ़ाते हैं।',
        E'स्मार्ट कक्षाएँ\nविज्ञान प्रयोगशालाएँ\nपुस्तकालय\nखेल का मैदान\nविद्यालय परिवहन'
    ),
    (
        'e1000000-0000-0000-0000-000000000013',
        '20000000-0000-0000-0000-000000000001',
        'ur',
        'ہر بچے کے لیے معیاری تعلیم',
        'ڈیمو پبلک اسکول ایک مخلوط دن کا اسکول ہے۔ ہم مضبوط تعلیم کو کھیل، فنون اور اقدار کے ساتھ جوڑتے ہیں تاکہ ہر طالب علم ترقی کر سکے۔',
        'ڈیمو پبلک اسکول میں خوش آمدید۔ ہم خاندانوں کے ساتھ مل کر ہر بچے کے علم، کردار اور اعتماد کو بڑھاتے ہیں۔',
        E'سمارٹ کلاس رومز\nسائنس لیبز\nلائبریری\nکھیلوں کا میدان\nاسکول ٹرانسپورٹ'
    )
ON CONFLICT DO NOTHING;
