-- =====================================================================
-- V31: User credential management — first-login flags, lockout,
-- token versioning, security questions, and credential audit.
-- =====================================================================
SET app.bypass_rls = 'true';

ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS questions_set BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until TIMESTAMPTZ;

CREATE TABLE security_question (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(40) NOT NULL UNIQUE,
    sort_order  SMALLINT NOT NULL DEFAULT 0,
    status      VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE user_security_answer (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id    UUID REFERENCES school(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    question_id  UUID NOT NULL REFERENCES security_question(id) ON DELETE CASCADE,
    answer_hash  VARCHAR(100) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_security_question UNIQUE (user_id, question_id)
);
CREATE INDEX idx_user_security_answer_user ON user_security_answer(user_id);

CREATE TABLE password_reset_challenge (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id    UUID REFERENCES school(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash   VARCHAR(64) NOT NULL UNIQUE,
    expires_at   TIMESTAMPTZ NOT NULL,
    consumed_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_password_reset_user ON password_reset_challenge(user_id);

CREATE TABLE credential_audit (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID REFERENCES school(id) ON DELETE CASCADE,
    actor_user_id   UUID REFERENCES app_user(id) ON DELETE SET NULL,
    target_user_id  UUID REFERENCES app_user(id) ON DELETE SET NULL,
    action          VARCHAR(40) NOT NULL,
    detail          VARCHAR(300),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_credential_audit_school ON credential_audit(school_id, created_at DESC);
CREATE INDEX idx_credential_audit_target ON credential_audit(target_user_id, created_at DESC);

INSERT INTO security_question (id, code, sort_order) VALUES
    ('a1000000-0000-0000-0000-000000000001', 'MOTHER_MAIDEN', 1),
    ('a1000000-0000-0000-0000-000000000002', 'FIRST_PET', 2),
    ('a1000000-0000-0000-0000-000000000003', 'BIRTH_CITY', 3),
    ('a1000000-0000-0000-0000-000000000004', 'FAVORITE_TEACHER', 4),
    ('a1000000-0000-0000-0000-000000000005', 'FIRST_SCHOOL', 5)
ON CONFLICT (code) DO NOTHING;

ALTER TABLE user_security_answer ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_security_answer FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON user_security_answer
    FOR ALL
    USING (school_id IS NULL OR school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id IS NULL OR school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE password_reset_challenge ENABLE ROW LEVEL SECURITY;
ALTER TABLE password_reset_challenge FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON password_reset_challenge
    FOR ALL
    USING (school_id IS NULL OR school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id IS NULL OR school_id = app_current_school_id() OR app_bypass_rls());

ALTER TABLE credential_audit ENABLE ROW LEVEL SECURITY;
ALTER TABLE credential_audit FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON credential_audit
    FOR ALL
    USING (school_id IS NULL OR school_id = app_current_school_id() OR app_bypass_rls())
    WITH CHECK (school_id IS NULL OR school_id = app_current_school_id() OR app_bypass_rls());

GRANT SELECT, INSERT, UPDATE, DELETE ON security_question TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON security_question TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON user_security_answer TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON user_security_answer TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON password_reset_challenge TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON password_reset_challenge TO app_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON credential_audit TO app_rls;
GRANT SELECT, INSERT, UPDATE, DELETE ON credential_audit TO app_admin;

DROP FUNCTION IF EXISTS auth_find_user(TEXT);

CREATE OR REPLACE FUNCTION auth_find_user(p_login TEXT)
RETURNS TABLE (
    id UUID, school_id UUID, username VARCHAR, password_hash VARCHAR,
    status VARCHAR, locale VARCHAR, first_name VARCHAR, last_name VARCHAR, email VARCHAR,
    must_change_password BOOLEAN, questions_set BOOLEAN, token_version INTEGER,
    failed_login_attempts INTEGER, locked_until TIMESTAMPTZ
)
LANGUAGE sql
SECURITY DEFINER
AS $$
    SELECT id, school_id, username, password_hash, status, locale, first_name, last_name, email,
           must_change_password, questions_set, token_version, failed_login_attempts, locked_until
    FROM app_user
    WHERE username = p_login OR email = p_login
    LIMIT 1;
$$;

REVOKE ALL ON FUNCTION auth_find_user(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION auth_find_user(TEXT) TO app_rls;
