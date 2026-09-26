SET app.bypass_rls = 'true';

ALTER TABLE marksheet
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS submitted_by UUID,
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS rejected_by UUID,
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS class_rank INTEGER;

UPDATE marksheet
SET status = 'PUBLISHED'
WHERE published = true
  AND (status IS NULL OR status = 'DRAFT');

ALTER TABLE marksheet DROP CONSTRAINT IF EXISTS chk_marksheet_status;
ALTER TABLE marksheet ADD CONSTRAINT chk_marksheet_status
    CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'PUBLISHED', 'REJECTED'));

CREATE INDEX IF NOT EXISTS idx_marksheet_status
    ON marksheet (school_id, academic_year_id, section_id, exam_term, status);
