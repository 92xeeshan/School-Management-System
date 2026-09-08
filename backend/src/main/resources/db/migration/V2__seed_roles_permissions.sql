-- =====================================================================
-- V2: Seed roles, permissions and the role->permission matrix
-- Deterministic UUIDs only for the 5 system roles.
-- =====================================================================

INSERT INTO role (id, code, name, description, is_system) VALUES
    ('00000000-0000-0000-0000-000000000001', 'SUPER_ADMIN', 'Super Admin', 'Platform administrator across all schools', true),
    ('00000000-0000-0000-0000-000000000002', 'ADMIN',       'Admin',       'School administrator', true),
    ('00000000-0000-0000-0000-000000000003', 'TEACHER',     'Teacher',     'Teaching staff', true),
    ('00000000-0000-0000-0000-000000000004', 'STUDENT',     'Student',     'Enrolled student', true),
    ('00000000-0000-0000-0000-000000000005', 'PARENT',      'Parent',      'Guardian of a student', true);

INSERT INTO permission (code, module, name, description) VALUES
    ('USER_READ',              'user',       'Read users',              'View user accounts'),
    ('USER_CREATE',            'user',       'Create users',            'Create user accounts'),
    ('USER_UPDATE',            'user',       'Update users',            'Update user accounts'),
    ('USER_DELETE',            'user',       'Delete users',            'Deactivate/delete user accounts'),
    ('ROLE_READ',              'role',       'Read roles',              'View roles and permissions'),
    ('ROLE_ASSIGN',            'role',       'Assign roles',            'Assign roles to users'),
    ('STUDENT_READ',           'student',    'Read students',           'View student profiles'),
    ('STUDENT_CREATE',         'student',    'Create students',         'Add student profiles'),
    ('STUDENT_UPDATE',         'student',    'Update students',         'Edit student profiles'),
    ('STUDENT_DELETE',         'student',    'Delete students',         'Remove student profiles'),
    ('STUDENT_IMPORT',         'student',    'Import students',         'Bulk CSV import of students'),
    ('GUARDIAN_READ',          'guardian',   'Read guardians',          'View guardian records'),
    ('GUARDIAN_CREATE',        'guardian',   'Create guardians',        'Add guardian records'),
    ('GUARDIAN_UPDATE',        'guardian',   'Update guardians',        'Edit guardian records'),
    ('CLASS_READ',             'academics',  'Read classes',            'View classes'),
    ('CLASS_CREATE',           'academics',  'Create classes',          'Add classes'),
    ('CLASS_UPDATE',           'academics',  'Update classes',          'Edit classes'),
    ('SECTION_READ',           'academics',  'Read sections',           'View sections'),
    ('SECTION_CREATE',         'academics',  'Create sections',         'Add sections'),
    ('SECTION_UPDATE',         'academics',  'Update sections',         'Edit sections'),
    ('SUBJECT_READ',           'academics',  'Read subjects',           'View subjects'),
    ('SUBJECT_CREATE',         'academics',  'Create subjects',         'Add subjects'),
    ('SUBJECT_UPDATE',         'academics',  'Update subjects',         'Edit subjects'),
    ('TIMETABLE_READ',         'timetable',  'Read timetable',          'View timetables'),
    ('TIMETABLE_MANAGE',       'timetable',  'Manage timetable',        'Create/edit timetable entries'),
    ('ATTENDANCE_MARK',        'attendance', 'Mark attendance',         'Record daily attendance'),
    ('ATTENDANCE_READ',        'attendance', 'Read attendance',         'View attendance records'),
    ('FEE_READ',               'fee',        'Read fees',               'View fee structures and payments'),
    ('FEE_STRUCTURE_MANAGE',   'fee',        'Manage fee structure',    'Create/edit fee structures'),
    ('FEE_PAYMENT_RECORD',     'fee',        'Record payments',         'Record manual fee payments'),
    ('FEE_RECEIPT_VIEW',       'fee',        'View receipts',           'View/download payment receipts'),
    ('NOTICE_READ',            'notice',     'Read notices',            'View notices'),
    ('NOTICE_CREATE',          'notice',     'Create notices',          'Draft notices'),
    ('NOTICE_PUBLISH',         'notice',     'Publish notices',         'Publish/draft/archive notices'),
    ('NOTICE_DELETE',          'notice',     'Delete notices',          'Delete notices'),
    ('EVENT_READ',             'calendar',   'Read calendar events',    'View academic calendar and events'),
    ('EVENT_MANAGE',           'calendar',   'Manage calendar events',  'Create/edit/delete events and holidays'),
    ('DASHBOARD_VIEW',         'dashboard',  'View dashboard',          'Access role dashboard');

-- role -> permission matrix --------------------------------------------
-- SUPER_ADMIN: all permissions (platform + school level)
INSERT INTO role_permission (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000001', id FROM permission;

-- ADMIN: all school-level permissions
INSERT INTO role_permission (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000002', id FROM permission;

-- TEACHER
INSERT INTO role_permission (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000003', id FROM permission
WHERE code IN ('STUDENT_READ', 'CLASS_READ', 'SECTION_READ', 'SUBJECT_READ',
               'TIMETABLE_READ', 'ATTENDANCE_MARK', 'ATTENDANCE_READ',
               'NOTICE_READ', 'EVENT_READ', 'DASHBOARD_VIEW');

-- STUDENT
INSERT INTO role_permission (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000004', id FROM permission
WHERE code IN ('TIMETABLE_READ', 'ATTENDANCE_READ', 'NOTICE_READ', 'EVENT_READ', 'DASHBOARD_VIEW');

-- PARENT
INSERT INTO role_permission (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000005', id FROM permission
WHERE code IN ('TIMETABLE_READ', 'ATTENDANCE_READ', 'NOTICE_READ',
               'FEE_READ', 'FEE_RECEIPT_VIEW', 'EVENT_READ', 'DASHBOARD_VIEW');
