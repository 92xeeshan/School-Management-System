ALTER TABLE subject
    ADD COLUMN weekly_periods INTEGER,
    ADD COLUMN practical BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE subject
    ADD CONSTRAINT chk_subject_status CHECK (status IN ('ACTIVE', 'INACTIVE'));

ALTER TABLE class_subject
    ADD COLUMN teacher_id UUID REFERENCES teacher_profile(id) ON DELETE SET NULL;

UPDATE subject SET weekly_periods = 6 WHERE code IN ('MATH', 'ENG');
UPDATE subject SET weekly_periods = 5, practical = true WHERE code = 'SCI';
UPDATE subject SET weekly_periods = 4 WHERE code = 'HIN';

UPDATE class_subject cs
SET teacher_id = ts.teacher_id
FROM teacher_subject ts
WHERE ts.school_id = cs.school_id
  AND ts.subject_id = cs.subject_id
  AND cs.teacher_id IS NULL;
