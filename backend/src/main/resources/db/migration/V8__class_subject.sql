CREATE TABLE class_subject (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id  UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    class_id   UUID NOT NULL REFERENCES school_class(id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES subject(id) ON DELETE CASCADE,
    CONSTRAINT uq_class_subject UNIQUE (class_id, subject_id)
);

ALTER TABLE class_subject ENABLE ROW LEVEL SECURITY;
ALTER TABLE class_subject FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON class_subject
    FOR ALL
    USING (school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls());

GRANT SELECT, INSERT, UPDATE, DELETE ON class_subject TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON class_subject TO app_admin;
