-- =====================================================================
-- V16: Dashboard calendar events + awards/star students.
-- Adds EVENT_READ/EVENT_MANAGE permissions, school_event + award tables
-- with tenant RLS, and demo data for the redesigned dashboard.
-- =====================================================================
SET app.bypass_rls = 'true';

-- Permissions ---------------------------------------------------------
INSERT INTO permission (code, module, name, description)
SELECT * FROM (VALUES
    ('EVENT_READ',   'calendar', 'Read events',    'View school calendar events'),
    ('EVENT_MANAGE', 'calendar', 'Manage events',  'Create, edit and delete school calendar events')
) AS p(code, module, name, description)
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = p.code);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code = 'EVENT_READ'
  AND r.code IN ('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT', 'PARENT')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE p.code = 'EVENT_MANAGE'
  AND r.code IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- Calendar events -----------------------------------------------------
CREATE TABLE school_event (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    title             VARCHAR(200) NOT NULL,
    description       VARCHAR(1000),
    event_type        VARCHAR(20) NOT NULL DEFAULT 'EVENT'
                      CHECK (event_type IN ('HOLIDAY', 'EXAM', 'MEETING', 'EVENT', 'ACTIVITY', 'OTHER')),
    start_date        DATE NOT NULL,
    end_date          DATE,
    all_day           BOOLEAN NOT NULL DEFAULT true,
    start_time        TIME,
    end_time          TIME,
    location          VARCHAR(200),
    visibility_scope  VARCHAR(20) NOT NULL DEFAULT 'SCHOOL_WIDE'
                      CHECK (visibility_scope IN ('SCHOOL_WIDE', 'CLASS_WIDE', 'STAFF')),
    class_id          UUID REFERENCES school_class(id) ON DELETE CASCADE,
    section_id        UUID REFERENCES section(id) ON DELETE CASCADE,
    created_by        UUID,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_school_event_range CHECK (end_date IS NULL OR end_date >= start_date)
);
CREATE INDEX idx_school_event_school_date ON school_event(school_id, start_date);

-- Awards / accolades --------------------------------------------------
CREATE TABLE award (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    student_id        UUID REFERENCES student(id) ON DELETE CASCADE,
    academic_year_id  UUID REFERENCES academic_year(id) ON DELETE SET NULL,
    title             VARCHAR(200) NOT NULL,
    category          VARCHAR(40) NOT NULL DEFAULT 'ACADEMIC'
                      CHECK (category IN ('ACADEMIC', 'SPORTS', 'ARTS', 'CULTURAL', 'BEHAVIOUR', 'OTHER')),
    badge             VARCHAR(40) NOT NULL DEFAULT 'GOLD'
                      CHECK (badge IN ('GOLD', 'SILVER', 'BRONZE', 'STAR')),
    description       VARCHAR(500),
    awarded_date      DATE NOT NULL DEFAULT CURRENT_DATE,
    points            INTEGER NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_award_school_date ON award(school_id, awarded_date);
CREATE INDEX idx_award_student ON award(student_id);

-- Row level security --------------------------------------------------
ALTER TABLE school_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE school_event FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON school_event
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE award ENABLE ROW LEVEL SECURITY;
ALTER TABLE award FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON award
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

GRANT SELECT, INSERT, UPDATE, DELETE ON school_event TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON school_event TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON award TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON award TO app_admin;

-- Demo students (adds gender diversity for the dashboard breakdown) ----
INSERT INTO student (id, school_id, user_id, admission_no, first_name, last_name,
                     date_of_birth, gender, admission_date, status)
VALUES
    ('40000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', NULL,
     'ADM0002', 'Priya',  'Sharma', '2014-08-22', 'FEMALE', '2025-04-01', 'ACTIVE'),
    ('40000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001', NULL,
     'ADM0003', 'Rohan',  'Verma',  '2014-02-14', 'MALE',   '2025-04-01', 'ACTIVE'),
    ('40000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000001', NULL,
     'ADM0004', 'Ananya', 'Iyer',   '2014-11-30', 'FEMALE', '2025-04-01', 'ACTIVE'),
    ('40000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000001', NULL,
     'ADM0005', 'Kabir',  'Singh',  '2013-06-05', 'MALE',   '2025-04-01', 'ACTIVE'),
    ('40000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000001', NULL,
     'ADM0006', 'Fatima', 'Khan',   '2013-09-18', 'FEMALE', '2025-04-01', 'ACTIVE')
ON CONFLICT (school_id, admission_no) DO NOTHING;

INSERT INTO student_enrollment (id, school_id, student_id, section_id, academic_year_id,
                                roll_number, enrollment_date, status)
VALUES
    ('40000000-0000-0000-0000-000000000022', '20000000-0000-0000-0000-000000000001',
     '40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000021',
     '30000000-0000-0000-0000-000000000001', 2, '2025-04-01', 'ACTIVE'),
    ('40000000-0000-0000-0000-000000000023', '20000000-0000-0000-0000-000000000001',
     '40000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000021',
     '30000000-0000-0000-0000-000000000001', 3, '2025-04-01', 'ACTIVE'),
    ('40000000-0000-0000-0000-000000000024', '20000000-0000-0000-0000-000000000001',
     '40000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000022',
     '30000000-0000-0000-0000-000000000001', 1, '2025-04-01', 'ACTIVE'),
    ('40000000-0000-0000-0000-000000000025', '20000000-0000-0000-0000-000000000001',
     '40000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000023',
     '30000000-0000-0000-0000-000000000001', 1, '2025-04-01', 'ACTIVE'),
    ('40000000-0000-0000-0000-000000000026', '20000000-0000-0000-0000-000000000001',
     '40000000-0000-0000-0000-000000000006', '30000000-0000-0000-0000-000000000023',
     '30000000-0000-0000-0000-000000000001', 2, '2025-04-01', 'ACTIVE')
ON CONFLICT (student_id, academic_year_id) DO NOTHING;

-- Demo awards ---------------------------------------------------------
INSERT INTO award (id, school_id, student_id, academic_year_id, title, category, badge,
                   description, awarded_date, points)
VALUES
    ('70000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
     '40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
     'Mathematics Olympiad Champion', 'ACADEMIC', 'GOLD',
     'First place in the inter-school mathematics olympiad', CURRENT_DATE - 12, 98),
    ('70000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001',
     '40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001',
     'Best Science Project', 'ACADEMIC', 'GOLD',
     'Outstanding working model on renewable energy', CURRENT_DATE - 9, 95),
    ('70000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001',
     '40000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000001',
     'Annual Art Competition Winner', 'ARTS', 'SILVER',
     'Winner of the annual landscape painting competition', CURRENT_DATE - 7, 90),
    ('70000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000001',
     '40000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000001',
     'Inter-school Football MVP', 'SPORTS', 'GOLD',
     'Most valuable player in the district football league', CURRENT_DATE - 4, 89),
    ('70000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000001',
     '40000000-0000-0000-0000-000000000006', '30000000-0000-0000-0000-000000000001',
     'Spelling Bee Runner-up', 'ACADEMIC', 'BRONZE',
     'Runner-up in the state level spelling bee', CURRENT_DATE - 2, 85);

-- Demo calendar events (relative dates keep the preview alive) ---------
INSERT INTO school_event (id, school_id, title, description, event_type, start_date, end_date,
                          all_day, start_time, end_time, location, visibility_scope, created_by)
VALUES
    ('80000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
     'Weekly Staff Meeting', 'Coordination meeting for all teaching and support staff.',
     'MEETING', CURRENT_DATE, CURRENT_DATE, false, '16:00', '17:00', 'Staff Room', 'STAFF',
     '20000000-0000-0000-0000-000000000002'),
    ('80000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001',
     'Mid-Term Examinations', 'Mid-term examinations for all classes. Timetable shared separately.',
     'EXAM', CURRENT_DATE + 3, CURRENT_DATE + 7, true, NULL, NULL, 'Examination Halls', 'SCHOOL_WIDE',
     '20000000-0000-0000-0000-000000000002'),
    ('80000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001',
     'Parent-Teacher Meeting', 'Discuss progress and exam preparation with parents.',
     'MEETING', CURRENT_DATE + 5, CURRENT_DATE + 5, false, '09:30', '12:30', 'Main Auditorium', 'SCHOOL_WIDE',
     '20000000-0000-0000-0000-000000000002'),
    ('80000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000001',
     'Annual Sports Day', 'Inter-house athletics and team sports finals.',
     'ACTIVITY', CURRENT_DATE + 12, CURRENT_DATE + 12, true, NULL, NULL, 'School Ground', 'SCHOOL_WIDE',
     '20000000-0000-0000-0000-000000000002'),
    ('80000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000001',
     'Science Exhibition', 'Students showcase science and robotics projects.',
     'EVENT', CURRENT_DATE - 2, CURRENT_DATE - 2, true, NULL, NULL, 'Science Block', 'SCHOOL_WIDE',
     '20000000-0000-0000-0000-000000000002'),
    ('80000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000001',
     'Founder''s Day Holiday', 'School remains closed for Founder''s Day.',
     'HOLIDAY', CURRENT_DATE + 18, CURRENT_DATE + 18, true, NULL, NULL, NULL, 'SCHOOL_WIDE',
     '20000000-0000-0000-0000-000000000002'),
    ('80000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000001',
     'Term Break', 'End of term break for all students and staff.',
     'HOLIDAY', CURRENT_DATE + 32, CURRENT_DATE + 40, true, NULL, NULL, NULL, 'SCHOOL_WIDE',
     '20000000-0000-0000-0000-000000000002');
