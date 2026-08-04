-- =====================================================================
-- V4: Enable Row-Level Security on every table that carries a school_id.
-- Policy: rows are visible/manipulable only when
--   school_id = app_current_school_id()  (set from the JWT per connection)
--   OR app.bypass_rls = 'true'           (super-admin / platform context)
-- Any future table with a school_id column must enable RLS in its own
-- migration using the same helper functions.
-- =====================================================================

-- Login happens before any JWT/tenant context exists, so the username lookup
-- must bypass RLS. The function is SECURITY DEFINER (runs as owner). Because
-- the app_user table is NOT force-flagged with RLS, the table owner (schoolms)
-- is exempt from its policy, so the function can search across tenants for the
-- login credential. The runtime role app_rls is still tenant-scoped by the
-- policy on app_user.
CREATE OR REPLACE FUNCTION auth_find_user(p_login TEXT)
RETURNS TABLE (
    id UUID, school_id UUID, username VARCHAR, password_hash VARCHAR,
    status VARCHAR, locale VARCHAR, first_name VARCHAR, last_name VARCHAR, email VARCHAR
)
LANGUAGE sql
SECURITY DEFINER
AS $$
    SELECT id, school_id, username, password_hash, status, locale, first_name, last_name, email
    FROM app_user
    WHERE username = p_login OR email = p_login
    LIMIT 1;
$$;

REVOKE ALL ON FUNCTION auth_find_user(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION auth_find_user(TEXT) TO app_rls;

-- RLS policies -----------------------------------------------------------
DO $$
DECLARE
    t text;
BEGIN
    FOR t IN
        SELECT table_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND column_name = 'school_id'
        GROUP BY table_name
        ORDER BY table_name
    LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', t);
        -- app_user is intentionally NOT forced: the SECURITY DEFINER
        -- auth_find_user() login function runs as the table owner (schoolms)
        -- and must be able to search credentials across tenants.
        IF t <> 'app_user' THEN
            EXECUTE format('ALTER TABLE public.%I FORCE ROW LEVEL SECURITY', t);
        END IF;
        EXECUTE format($p$
            CREATE POLICY tenant_isolation ON public.%I
            FOR ALL
            USING (school_id = app_current_school_id() OR app_bypass_rls())
            WITH CHECK (school_id = app_current_school_id() OR app_bypass_rls())
        $p$, t);
    END LOOP;
END $$;
