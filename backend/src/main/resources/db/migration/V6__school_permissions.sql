-- =====================================================================
-- V6: Platform-level school permissions. School administration is
-- super-admin only; expose it through the same permission-code matrix
-- used by every other module (authorities are permission codes, not
-- roles, so hasRole() is never used).
-- =====================================================================

INSERT INTO permission (code, module, name, description)
SELECT * FROM (VALUES
    ('SCHOOL_READ',   'school', 'Read schools',   'View school records'),
    ('SCHOOL_MANAGE', 'school', 'Manage schools', 'Create/update school records')
) AS p(code, module, name, description)
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = p.code);

INSERT INTO role_permission (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000001', id FROM permission
WHERE code IN ('SCHOOL_READ', 'SCHOOL_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = '00000000-0000-0000-0000-000000000001'
        AND rp.permission_id = permission.id);
