-- =====================================================================
-- V19: Exam schedule builder with rooms, invigilators, and publish state.
-- =====================================================================
SET app.bypass_rls = 'true';

CREATE TABLE exam_schedule (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    academic_year_id  UUID NOT NULL REFERENCES academic_year(id) ON DELETE CASCADE,
    class_id          UUID NOT NULL REFERENCES school_class(id) ON DELETE CASCADE,
    section_id        UUID NOT NULL REFERENCES section(id) ON DELETE CASCADE,
    subject_id        UUID NOT NULL REFERENCES subject(id) ON DELETE CASCADE,
    exam_term         VARCHAR(40) NOT NULL,
    exam_date         DATE NOT NULL,
    start_time        TIME NOT NULL,
    end_time          TIME NOT NULL,
    room              VARCHAR(50),
    max_marks         NUMERIC(8,2) NOT NULL DEFAULT 100,
    pass_marks        NUMERIC(8,2) NOT NULL DEFAULT 33,
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_exam_schedule UNIQUE (school_id, academic_year_id, section_id, subject_id, exam_term),
    CONSTRAINT chk_exam_schedule_term CHECK (exam_term IN ('QUIZ', 'UNIT', 'MIDTERM', 'TERM', 'FINAL', 'CONTINUOUS')),
    CONSTRAINT chk_exam_schedule_status CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT chk_exam_schedule_time CHECK (start_time < end_time),
    CONSTRAINT chk_exam_schedule_marks CHECK (max_marks > 0 AND pass_marks >= 0 AND pass_marks <= max_marks)
);
CREATE INDEX idx_exam_schedule_school_date ON exam_schedule(school_id, exam_date, start_time);
CREATE INDEX idx_exam_schedule_school_room ON exam_schedule(school_id, room);

CREATE TABLE exam_schedule_invigilator (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    exam_schedule_id  UUID NOT NULL REFERENCES exam_schedule(id) ON DELETE CASCADE,
    teacher_id        UUID NOT NULL REFERENCES teacher_profile(id) ON DELETE CASCADE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_exam_schedule_invigilator UNIQUE (exam_schedule_id, teacher_id)
);
CREATE INDEX idx_exam_schedule_invigilator_teacher ON exam_schedule_invigilator(school_id, teacher_id);

ALTER TABLE exam_schedule ENABLE ROW LEVEL SECURITY;
ALTER TABLE exam_schedule FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON exam_schedule
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE exam_schedule_invigilator ENABLE ROW LEVEL SECURITY;
ALTER TABLE exam_schedule_invigilator FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON exam_schedule_invigilator
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

GRANT SELECT, INSERT, UPDATE, DELETE ON exam_schedule TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON exam_schedule TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON exam_schedule_invigilator TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON exam_schedule_invigilator TO app_admin;

INSERT INTO exam_schedule (
    school_id, academic_year_id, class_id, section_id, subject_id, exam_term,
    exam_date, start_time, end_time, room, max_marks, pass_marks, status)
SELECT
    y.school_id,
    y.id,
    s.class_id,
    s.id,
    subj.id,
    'TERM',
    slot.exam_date,
    slot.start_time,
    slot.end_time,
    COALESCE(NULLIF(s.room, ''), 'Room ' || (100 + ns.idx)),
    slot.max_marks,
    slot.pass_marks,
    slot.status
FROM academic_year y
JOIN (
    SELECT id, school_id, class_id, room,
           (ROW_NUMBER() OVER (PARTITION BY school_id ORDER BY class_id, name) - 1)::int AS idx
    FROM section
) ns ON ns.school_id = y.school_id
JOIN section s ON s.id = ns.id
JOIN LATERAL (
    SELECT id
    FROM subject
    WHERE school_id = y.school_id
    ORDER BY name
    OFFSET (ns.idx % GREATEST((SELECT COUNT(*) FROM subject WHERE school_id = y.school_id), 1))
    LIMIT 1
) subj ON true
JOIN (
    VALUES
        (0, DATE '2026-09-15', '09:00'::time, '11:00'::time, 100::numeric, 33::numeric, 'PUBLISHED'),
        (1, DATE '2026-09-16', '09:00'::time, '11:00'::time, 100::numeric, 33::numeric, 'PUBLISHED'),
        (2, DATE '2026-09-15', '13:00'::time, '15:00'::time, 100::numeric, 33::numeric, 'PUBLISHED'),
        (3, DATE '2026-09-17', '09:00'::time, '11:00'::time, 80::numeric, 27::numeric, 'DRAFT')
) AS slot(idx, exam_date, start_time, end_time, max_marks, pass_marks, status)
  ON slot.idx = ns.idx
WHERE y.is_current = true
ON CONFLICT (school_id, academic_year_id, section_id, subject_id, exam_term) DO NOTHING;

INSERT INTO exam_schedule_invigilator (school_id, exam_schedule_id, teacher_id)
SELECT s.school_id, s.id, t.id
FROM exam_schedule s
JOIN LATERAL (
    SELECT id
    FROM teacher_profile
    WHERE school_id = s.school_id AND status = 'ACTIVE'
    ORDER BY first_name, last_name
    LIMIT 1
) t ON true
WHERE s.status = 'PUBLISHED'
  AND NOT EXISTS (
      SELECT 1 FROM exam_schedule_invigilator i
      WHERE i.exam_schedule_id = s.id
        AND i.teacher_id = t.id);
