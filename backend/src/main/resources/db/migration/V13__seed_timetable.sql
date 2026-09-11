SET app.bypass_rls = 'true';

INSERT INTO timetable_entry (school_id, section_id, academic_year_id, day_of_week, period_number, start_time, end_time, subject_id, teacher_id, room)
SELECT
    s.school_id,
    s.id,
    y.id,
    slot.day_of_week,
    slot.period_number,
    slot.start_time,
    slot.end_time,
    subj.id,
    teach.id,
    'Room ' || (100 + ((ns.idx + slot.period_number - 1) % 4) + 1)
FROM section s
JOIN academic_year y
  ON y.school_id = s.school_id AND y.is_current = true
JOIN (
    SELECT id, school_id, (ROW_NUMBER() OVER (PARTITION BY school_id ORDER BY id) - 1)::int AS idx
    FROM section
) ns ON ns.id = s.id
CROSS JOIN (
    VALUES
        (1::smallint, 1::smallint, '09:00'::time, '09:45'::time, 0),
        (1::smallint, 2::smallint, '09:45'::time, '10:30'::time, 1),
        (1::smallint, 3::smallint, '10:45'::time, '11:30'::time, 2),
        (2::smallint, 1::smallint, '09:00'::time, '09:45'::time, 3),
        (3::smallint, 1::smallint, '09:00'::time, '09:45'::time, 0)
) AS slot(day_of_week, period_number, start_time, end_time, subject_ord)
JOIN LATERAL (
    SELECT id
    FROM subject
    WHERE school_id = s.school_id
    ORDER BY name
    OFFSET slot.subject_ord
    LIMIT 1
) subj ON true
JOIN LATERAL (
    SELECT id
    FROM teacher_profile
    WHERE school_id = s.school_id
    ORDER BY first_name, last_name
    OFFSET (ns.idx + slot.subject_ord)
    LIMIT 1
) teach ON true
ON CONFLICT DO NOTHING;
