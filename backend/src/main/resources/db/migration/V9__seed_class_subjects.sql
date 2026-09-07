INSERT INTO class_subject (school_id, class_id, subject_id)
SELECT c.school_id, c.id, s.id
FROM school_class c
JOIN subject s ON s.school_id = c.school_id
WHERE c.id IN (
        '30000000-0000-0000-0000-000000000011',
        '30000000-0000-0000-0000-000000000012',
        '30000000-0000-0000-0000-000000000013'
    )
ON CONFLICT DO NOTHING;
