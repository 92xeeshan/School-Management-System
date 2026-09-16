-- =====================================================================
-- V17: Demo attendance for the last 5 operational school days so the
-- dashboard attendance trend chart renders meaningful data.
-- =====================================================================
SET app.bypass_rls = 'true';

WITH days AS (
    SELECT d::date AS day
    FROM generate_series(CURRENT_DATE - INTERVAL '10 days', CURRENT_DATE, INTERVAL '1 day') AS d
    WHERE EXTRACT(ISODOW FROM d) < 6
    ORDER BY d DESC
    LIMIT 5
),
pairs AS (
    SELECT s.id AS student_id, s.school_id, e.section_id, days.day
    FROM student s
    JOIN student_enrollment e ON e.student_id = s.id AND e.status = 'ACTIVE'
    JOIN academic_year ay ON ay.id = e.academic_year_id AND ay.is_current
    CROSS JOIN days
    WHERE s.school_id = '20000000-0000-0000-0000-000000000001'
      AND s.status = 'ACTIVE'
)
INSERT INTO attendance (school_id, student_id, section_id, attendance_date, status, marked_by)
SELECT school_id,
       student_id,
       section_id,
       day,
       CASE
           WHEN abs(hashtext(student_id::text || day::text)) % 100 < 83 THEN 'PRESENT'
           WHEN abs(hashtext(student_id::text || day::text)) % 100 < 91 THEN 'ABSENT'
           ELSE 'LATE'
       END,
       '20000000-0000-0000-0000-000000000003'
FROM pairs
ON CONFLICT (student_id, attendance_date) DO NOTHING;
