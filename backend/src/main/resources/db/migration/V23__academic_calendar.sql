-- =====================================================================
-- V23: Academic calendar expansions for issue #20.
-- Adds PTM/SPORTS/NOTICE types, ROLE audience, and extra demo events.
-- Class-scoped seed follows ADM0001 live enrollment (same pattern as V22).
-- =====================================================================
SET app.bypass_rls = 'true';

ALTER TABLE school_event DROP CONSTRAINT IF EXISTS school_event_event_type_check;
ALTER TABLE school_event ADD CONSTRAINT school_event_event_type_check
    CHECK (event_type IN (
        'HOLIDAY', 'EXAM', 'MEETING', 'EVENT', 'ACTIVITY', 'OTHER',
        'PTM', 'SPORTS', 'NOTICE'
    ));

ALTER TABLE school_event DROP CONSTRAINT IF EXISTS school_event_visibility_scope_check;
ALTER TABLE school_event ADD CONSTRAINT school_event_visibility_scope_check
    CHECK (visibility_scope IN ('SCHOOL_WIDE', 'CLASS_WIDE', 'STAFF', 'ROLE'));

ALTER TABLE school_event ADD COLUMN IF NOT EXISTS audience_role VARCHAR(20);

UPDATE school_event
SET event_type = 'PTM'
WHERE id = '80000000-0000-0000-0000-000000000003'
  AND event_type = 'MEETING';

UPDATE school_event
SET event_type = 'SPORTS'
WHERE id = '80000000-0000-0000-0000-000000000004'
  AND event_type = 'ACTIVITY';

WITH target AS (
    SELECT e.school_id, sec.class_id, e.section_id, c.name AS class_name, sec.name AS section_name
    FROM student s
    JOIN student_enrollment e ON e.student_id = s.id AND e.status = 'ACTIVE'
    JOIN academic_year y ON y.id = e.academic_year_id AND y.is_current
    JOIN section sec ON sec.id = e.section_id
    JOIN school_class c ON c.id = sec.class_id
    WHERE s.admission_no = 'ADM0001'
    LIMIT 1
)
INSERT INTO school_event (id, school_id, title, description, event_type, start_date, end_date,
                          all_day, start_time, end_time, location, visibility_scope, class_id, section_id, created_by)
SELECT
    '80000000-0000-0000-0000-000000000008',
    t.school_id,
    t.class_name || ' ' || t.section_name || ' Science Fair Prep',
    'Section-only briefing for the science exhibition.',
    'NOTICE',
    CURRENT_DATE + 2,
    CURRENT_DATE + 2,
    false,
    '14:00',
    '15:00',
    t.class_name || ' ' || t.section_name,
    'CLASS_WIDE',
    t.class_id,
    t.section_id,
    '20000000-0000-0000-0000-000000000002'
FROM target t
ON CONFLICT (id) DO NOTHING;

INSERT INTO school_event (id, school_id, title, description, event_type, start_date, end_date,
                          all_day, start_time, end_time, location, visibility_scope, class_id, section_id, created_by)
VALUES
    ('80000000-0000-0000-0000-000000000009', '20000000-0000-0000-0000-000000000001',
     'Republic Day Holiday', 'School remains closed for Republic Day.',
     'HOLIDAY', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int, 1, 26),
     make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int, 1, 26),
     true, NULL, NULL, NULL, 'SCHOOL_WIDE', NULL, NULL,
     '20000000-0000-0000-0000-000000000002'),
    ('80000000-0000-0000-0000-000000000010', '20000000-0000-0000-0000-000000000001',
     'Independence Day Holiday', 'School remains closed for Independence Day.',
     'HOLIDAY', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int, 8, 15),
     make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int, 8, 15),
     true, NULL, NULL, NULL, 'SCHOOL_WIDE', NULL, NULL,
     '20000000-0000-0000-0000-000000000002')
ON CONFLICT (id) DO NOTHING;

INSERT INTO school_event (id, school_id, title, description, event_type, start_date, end_date,
                          all_day, start_time, end_time, location, visibility_scope, audience_role, created_by)
VALUES
    ('80000000-0000-0000-0000-000000000011', '20000000-0000-0000-0000-000000000001',
     'Parent Orientation Evening', 'Orientation for parents of newly admitted students.',
     'PTM', CURRENT_DATE + 9, CURRENT_DATE + 9, false, '17:00', '18:30', 'Main Auditorium',
     'ROLE', 'PARENT', '20000000-0000-0000-0000-000000000002')
ON CONFLICT (id) DO NOTHING;
