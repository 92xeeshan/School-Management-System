-- =====================================================================
-- V3: Seed platform super admin + demo school and demo users.
-- Demo password for all seeded accounts: Admin@123 (bcrypt below).
-- =====================================================================

INSERT INTO app_user (id, school_id, username, email, password_hash, first_name, last_name, locale, status)
VALUES
    ('10000000-0000-0000-0000-000000000001', NULL, 'superadmin',
     'superadmin@schoolms.local',
     '$2b$10$wq8FQUi7QD0Rf.nDwpLLTu2bI5p8Ob3Yrf1JfTkl7O8n/TkV7o34K',
     'Platform', 'Admin', 'en', 'ACTIVE');

INSERT INTO user_role (school_id, user_id, role_id)
VALUES (NULL, '10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001');

-- Demo school ------------------------------------------------------------
INSERT INTO school (id, code, name, currency, default_locale)
VALUES ('20000000-0000-0000-0000-000000000001', 'DEMO', 'Demo Public School', 'INR', 'en');

-- Demo users (school DEMO)
INSERT INTO app_user (id, school_id, username, email, password_hash, first_name, last_name, locale, status)
VALUES
    ('20000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', 'admin',
     'admin@demo.school', '$2b$10$wq8FQUi7QD0Rf.nDwpLLTu2bI5p8Ob3Yrf1JfTkl7O8n/TkV7o34K',
     'School', 'Admin', 'en', 'ACTIVE'),
    ('20000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001', 'teacher',
     'teacher@demo.school', '$2b$10$wq8FQUi7QD0Rf.nDwpLLTu2bI5p8Ob3Yrf1JfTkl7O8n/TkV7o34K',
     'Asha', 'Sharma', 'en', 'ACTIVE'),
    ('20000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000001', 'parent',
     'parent@demo.school', '$2b$10$wq8FQUi7QD0Rf.nDwpLLTu2bI5p8Ob3Yrf1JfTkl7O8n/TkV7o34K',
     'Rajesh', 'Kumar', 'en', 'ACTIVE'),
    ('20000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000001', 'student',
     'student@demo.school', '$2b$10$wq8FQUi7QD0Rf.nDwpLLTu2bI5p8Ob3Yrf1JfTkl7O8n/TkV7o34K',
     'Aarav', 'Kumar', 'en', 'ACTIVE');

INSERT INTO user_role (school_id, user_id, role_id) VALUES
    ('20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002'),
    ('20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000003'),
    ('20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000005'),
    ('20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000004');

-- Demo academic setup -----------------------------------------------------
INSERT INTO academic_year (id, school_id, name, start_date, end_date, is_current) VALUES
    ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
     '2025-26', '2025-04-01', '2026-03-31', true);

INSERT INTO school_class (id, school_id, name, code, sort_order) VALUES
    ('30000000-0000-0000-0000-000000000011', '20000000-0000-0000-0000-000000000001', 'Class 5', 'C5', 5),
    ('30000000-0000-0000-0000-000000000012', '20000000-0000-0000-0000-000000000001', 'Class 6', 'C6', 6);

INSERT INTO section (id, school_id, class_id, name, capacity) VALUES
    ('30000000-0000-0000-0000-000000000021', '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000011', 'A', 40),
    ('30000000-0000-0000-0000-000000000022', '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000011', 'B', 40),
    ('30000000-0000-0000-0000-000000000023', '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000012', 'A', 40);

INSERT INTO subject (id, school_id, name, code, type) VALUES
    ('30000000-0000-0000-0000-000000000031', '20000000-0000-0000-0000-000000000001', 'Mathematics', 'MATH', 'CORE'),
    ('30000000-0000-0000-0000-000000000032', '20000000-0000-0000-0000-000000000001', 'English',    'ENG',  'CORE'),
    ('30000000-0000-0000-0000-000000000033', '20000000-0000-0000-0000-000000000001', 'Science',    'SCI',  'CORE'),
    ('30000000-0000-0000-0000-000000000034', '20000000-0000-0000-0000-000000000001', 'Hindi',      'HIN',  'CORE');

INSERT INTO teacher_profile (id, school_id, user_id, employee_no, first_name, last_name, email, phone, designation)
VALUES ('30000000-0000-0000-0000-000000000041', '20000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000003', 'EMP001', 'Asha', 'Sharma', 'teacher@demo.school',
        '9876500001', 'Primary Teacher');

INSERT INTO teacher_subject (school_id, teacher_id, subject_id) VALUES
    ('20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000041', '30000000-0000-0000-0000-000000000031');

INSERT INTO teacher_section (school_id, teacher_id, section_id, academic_year_id, is_class_teacher) VALUES
    ('20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000041',
     '30000000-0000-0000-0000-000000000021', '30000000-0000-0000-0000-000000000001', true);

-- Demo student (Aarav) -----------------------------------------------------
INSERT INTO student (id, school_id, user_id, admission_no, first_name, last_name, date_of_birth, gender, admission_date)
VALUES ('40000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000005', 'ADM0001', 'Aarav', 'Kumar', '2014-05-10', 'MALE', '2025-04-01');

INSERT INTO guardian (id, school_id, user_id, first_name, last_name, relationship, email, phone)
VALUES ('40000000-0000-0000-0000-000000000011', '20000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000004', 'Rajesh', 'Kumar', 'FATHER', 'parent@demo.school', '9876500002');

INSERT INTO student_guardian (school_id, student_id, guardian_id, relationship, is_primary)
VALUES ('20000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001',
        '40000000-0000-0000-0000-000000000011', 'FATHER', true);

INSERT INTO student_enrollment (id, school_id, student_id, section_id, academic_year_id, roll_number, status)
VALUES ('40000000-0000-0000-0000-000000000021', '20000000-0000-0000-0000-000000000001',
        '40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000021',
        '30000000-0000-0000-0000-000000000001', 1, 'ACTIVE');

-- Demo fee setup ------------------------------------------------------------
INSERT INTO fee_category (id, school_id, name, code) VALUES
    ('50000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Tuition Fee', 'TUITION'),
    ('50000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', 'Transport Fee', 'TRANSPORT');

INSERT INTO fee_structure (id, school_id, class_id, academic_year_id, category_id, amount, frequency, due_day)
VALUES ('50000000-0000-0000-0000-000000000011', '20000000-0000-0000-0000-000000000001',
        '30000000-0000-0000-0000-000000000011', '30000000-0000-0000-0000-000000000001',
        '50000000-0000-0000-0000-000000000001', 1500.00, 'MONTHLY', 10);

INSERT INTO student_fee_assignment (id, school_id, student_id, fee_structure_id, status)
VALUES ('50000000-0000-0000-0000-000000000021', '20000000-0000-0000-0000-000000000001',
        '40000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000011', 'ACTIVE');

-- Demo notice -----------------------------------------------------------------
INSERT INTO notice (id, school_id, title, body, author_id, visibility_scope, priority, publish_at, status)
VALUES ('60000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
        'Welcome to the new academic year', 'School reopens on 1 April. All students are requested to attend.',
        '20000000-0000-0000-0000-000000000002', 'SCHOOL_WIDE', 'NORMAL', now(), 'PUBLISHED');
