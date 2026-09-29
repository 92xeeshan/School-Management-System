-- =====================================================================
-- V7: Demo records so every class/section has data after launch.
-- 2 students per section, extra teachers, fees, notices, today's attendance.
-- Password for extra login accounts: Admin@123
-- =====================================================================

SET app.bypass_rls = 'true';

-- Extra classes / sections (Class 6-B, Class 7 A/B)
INSERT INTO school_class (id, school_id, name, code, sort_order)
SELECT '30000000-0000-0000-0000-000000000013', '20000000-0000-0000-0000-000000000001', 'Class 7', 'C7', 7
WHERE NOT EXISTS (SELECT 1 FROM school_class WHERE id = '30000000-0000-0000-0000-000000000013');

INSERT INTO section (id, school_id, class_id, name, capacity)
SELECT v.id, '20000000-0000-0000-0000-000000000001', v.class_id, v.name, 40
FROM (VALUES
    ('30000000-0000-0000-0000-000000000024'::uuid, '30000000-0000-0000-0000-000000000012'::uuid, 'B'),
    ('30000000-0000-0000-0000-000000000025'::uuid, '30000000-0000-0000-0000-000000000013'::uuid, 'A'),
    ('30000000-0000-0000-0000-000000000026'::uuid, '30000000-0000-0000-0000-000000000013'::uuid, 'B')
) AS v(id, class_id, name)
WHERE NOT EXISTS (SELECT 1 FROM section s WHERE s.id = v.id);

INSERT INTO subject (id, school_id, name, code, type)
SELECT '30000000-0000-0000-0000-000000000035', '20000000-0000-0000-0000-000000000001', 'Computer Science', 'CS', 'CORE'
WHERE NOT EXISTS (SELECT 1 FROM subject WHERE id = '30000000-0000-0000-0000-000000000035');

-- Extra teacher login accounts
INSERT INTO app_user (id, school_id, username, email, phone, password_hash, first_name, last_name, locale, status)
SELECT v.id, '20000000-0000-0000-0000-000000000001', v.username, v.email, v.phone,
       '$2b$10$wq8FQUi7QD0Rf.nDwpLLTu2bI5p8Ob3Yrf1JfTkl7O8n/TkV7o34K',
       v.first_name, v.last_name, 'en', 'ACTIVE'
FROM (VALUES
    ('20000000-0000-0000-0000-000000000201'::uuid, 'teacher2', 'teacher2@demo.school', '9876500004', 'Meera',  'Joshi'),
    ('20000000-0000-0000-0000-000000000204'::uuid, 'teacher3', 'teacher3@demo.school', '9876500007', 'Vikram', 'Singh'),
    ('20000000-0000-0000-0000-000000000206'::uuid, 'teacher4', 'teacher4@demo.school', '9876500009', 'Nalini', 'Iyer'),
    ('20000000-0000-0000-0000-000000000207'::uuid, 'teacher5', 'teacher5@demo.school', '9876500010', 'David',  'Dsouza'),
    ('20000000-0000-0000-0000-000000000208'::uuid, 'teacher6', 'teacher6@demo.school', '9876500014', 'Priya',  'Menon')
) AS v(id, username, email, phone, first_name, last_name)
WHERE NOT EXISTS (SELECT 1 FROM app_user u WHERE u.id = v.id);

INSERT INTO user_role (school_id, user_id, role_id)
SELECT '20000000-0000-0000-0000-000000000001', v.user_id, '00000000-0000-0000-0000-000000000003'
FROM (VALUES
    ('20000000-0000-0000-0000-000000000201'::uuid),
    ('20000000-0000-0000-0000-000000000204'::uuid),
    ('20000000-0000-0000-0000-000000000206'::uuid),
    ('20000000-0000-0000-0000-000000000207'::uuid),
    ('20000000-0000-0000-0000-000000000208'::uuid)
) AS v(user_id)
WHERE NOT EXISTS (
    SELECT 1 FROM user_role ur WHERE ur.user_id = v.user_id AND ur.role_id = '00000000-0000-0000-0000-000000000003'
);

INSERT INTO teacher_profile (id, school_id, user_id, employee_no, first_name, last_name, email, phone, designation, qualification, join_date, status)
SELECT v.id, '20000000-0000-0000-0000-000000000001', v.user_id, v.emp, v.first_name, v.last_name,
       v.email, v.phone, v.designation, v.qualification, v.join_date, 'ACTIVE'
FROM (VALUES
    ('30000000-0000-0000-0000-000000000042'::uuid, '20000000-0000-0000-0000-000000000201'::uuid, 'EMP002', 'Meera',  'Joshi',  'teacher2@demo.school', '9876500004', 'English Teacher',   'M.A. English',     DATE '2021-07-01'),
    ('30000000-0000-0000-0000-000000000043'::uuid, '20000000-0000-0000-0000-000000000204'::uuid, 'EMP003', 'Vikram', 'Singh',  'teacher3@demo.school', '9876500007', 'Science Teacher',   'M.Sc Physics',     DATE '2019-04-15'),
    ('30000000-0000-0000-0000-000000000044'::uuid, '20000000-0000-0000-0000-000000000206'::uuid, 'EMP004', 'Nalini', 'Iyer',   'teacher4@demo.school', '9876500009', 'Hindi Teacher',     'M.A. Hindi',       DATE '2022-01-10'),
    ('30000000-0000-0000-0000-000000000045'::uuid, '20000000-0000-0000-0000-000000000207'::uuid, 'EMP005', 'David',  'Dsouza', 'teacher5@demo.school', '9876500010', 'Computer Teacher',  'BCA',              DATE '2023-08-01'),
    ('30000000-0000-0000-0000-000000000046'::uuid, '20000000-0000-0000-0000-000000000208'::uuid, 'EMP006', 'Priya',  'Menon',  'teacher6@demo.school', '9876500014', 'Class Teacher',     'B.Ed',             DATE '2024-06-01')
) AS v(id, user_id, emp, first_name, last_name, email, phone, designation, qualification, join_date)
WHERE NOT EXISTS (SELECT 1 FROM teacher_profile t WHERE t.id = v.id);

INSERT INTO teacher_subject (school_id, teacher_id, subject_id)
SELECT '20000000-0000-0000-0000-000000000001', v.teacher_id, v.subject_id
FROM (VALUES
    ('30000000-0000-0000-0000-000000000042'::uuid, '30000000-0000-0000-0000-000000000032'::uuid),
    ('30000000-0000-0000-0000-000000000043'::uuid, '30000000-0000-0000-0000-000000000033'::uuid),
    ('30000000-0000-0000-0000-000000000044'::uuid, '30000000-0000-0000-0000-000000000034'::uuid),
    ('30000000-0000-0000-0000-000000000045'::uuid, '30000000-0000-0000-0000-000000000035'::uuid),
    ('30000000-0000-0000-0000-000000000046'::uuid, '30000000-0000-0000-0000-000000000031'::uuid)
) AS v(teacher_id, subject_id)
WHERE NOT EXISTS (
    SELECT 1 FROM teacher_subject ts
    WHERE ts.teacher_id = v.teacher_id AND ts.subject_id = v.subject_id
);

INSERT INTO teacher_section (school_id, teacher_id, section_id, academic_year_id, is_class_teacher)
SELECT '20000000-0000-0000-0000-000000000001', v.teacher_id, v.section_id,
       '30000000-0000-0000-0000-000000000001', true
FROM (VALUES
    ('30000000-0000-0000-0000-000000000042'::uuid, '30000000-0000-0000-0000-000000000022'::uuid),
    ('30000000-0000-0000-0000-000000000043'::uuid, '30000000-0000-0000-0000-000000000023'::uuid),
    ('30000000-0000-0000-0000-000000000044'::uuid, '30000000-0000-0000-0000-000000000024'::uuid),
    ('30000000-0000-0000-0000-000000000045'::uuid, '30000000-0000-0000-0000-000000000025'::uuid),
    ('30000000-0000-0000-0000-000000000046'::uuid, '30000000-0000-0000-0000-000000000026'::uuid)
) AS v(teacher_id, section_id)
WHERE NOT EXISTS (
    SELECT 1 FROM teacher_section ts
    WHERE ts.teacher_id = v.teacher_id AND ts.section_id = v.section_id
      AND ts.academic_year_id = '30000000-0000-0000-0000-000000000001'
);

-- Two students per section (Aarav already in 5-A)
INSERT INTO student (id, school_id, admission_no, first_name, last_name, date_of_birth, gender, blood_group, admission_date, status)
SELECT v.id, '20000000-0000-0000-0000-000000000001', v.adm, v.first_name, v.last_name,
       v.dob, v.gender, v.blood, DATE '2025-04-01', 'ACTIVE'
FROM (VALUES
    ('40000000-0000-0000-0000-000000000002'::uuid, 'ADM0002', 'Diya',   'Sharma', DATE '2014-08-15', 'FEMALE', 'O+'),
    ('40000000-0000-0000-0000-000000000003'::uuid, 'ADM0003', 'Rohan',  'Gupta',  DATE '2014-01-22', 'MALE',   'A+'),
    ('40000000-0000-0000-0000-000000000004'::uuid, 'ADM0004', 'Ananya', 'Verma',  DATE '2013-11-03', 'FEMALE', 'AB+'),
    ('40000000-0000-0000-0000-000000000005'::uuid, 'ADM0005', 'Kabir',  'Khan',   DATE '2013-07-19', 'MALE',   'B-'),
    ('40000000-0000-0000-0000-000000000006'::uuid, 'ADM0006', 'Ishaan', 'Patel',  DATE '2013-03-08', 'MALE',   'O+'),
    ('40000000-0000-0000-0000-000000000007'::uuid, 'ADM0007', 'Meera',  'Nair',   DATE '2013-09-21', 'FEMALE', 'A+'),
    ('40000000-0000-0000-0000-000000000008'::uuid, 'ADM0008', 'Arjun',  'Reddy',  DATE '2013-12-02', 'MALE',   'B+'),
    ('40000000-0000-0000-0000-000000000009'::uuid, 'ADM0009', 'Sara',   'Ali',    DATE '2012-06-14', 'FEMALE', 'O-'),
    ('40000000-0000-0000-0000-000000000010'::uuid, 'ADM0010', 'Vihaan', 'Joshi',  DATE '2012-02-27', 'MALE',   'A-'),
    ('40000000-0000-0000-0000-000000000011'::uuid, 'ADM0011', 'Zara',   'Khan',   DATE '2012-10-05', 'FEMALE', 'B+'),
    ('40000000-0000-0000-0000-000000000012'::uuid, 'ADM0012', 'Advait', 'Rao',    DATE '2012-04-18', 'MALE',   'AB+')
) AS v(id, adm, first_name, last_name, dob, gender, blood)
WHERE NOT EXISTS (SELECT 1 FROM student s WHERE s.id = v.id);

INSERT INTO guardian (id, school_id, first_name, last_name, relationship, phone, occupation)
SELECT v.id, '20000000-0000-0000-0000-000000000001', v.first_name, v.last_name, v.rel, v.phone, v.occ
FROM (VALUES
    ('40000000-0000-0000-0000-000000000012'::uuid, 'Sunita', 'Sharma', 'MOTHER', '9876500005', 'Teacher'),
    ('40000000-0000-0000-0000-000000000013'::uuid, 'Amit',   'Gupta',  'FATHER', '9876500011', 'Engineer'),
    ('40000000-0000-0000-0000-000000000014'::uuid, 'Suresh', 'Verma',  'FATHER', '9876500012', 'Doctor'),
    ('40000000-0000-0000-0000-000000000015'::uuid, 'Imran',  'Khan',   'FATHER', '9876500013', 'Advocate'),
    ('40000000-0000-0000-0000-000000000016'::uuid, 'Rakesh', 'Patel',  'FATHER', '9876500015', 'Merchant'),
    ('40000000-0000-0000-0000-000000000017'::uuid, 'Lakshmi','Nair',   'MOTHER', '9876500016', 'Nurse'),
    ('40000000-0000-0000-0000-000000000018'::uuid, 'Kiran',  'Reddy',  'FATHER', '9876500017', 'Banker'),
    ('40000000-0000-0000-0000-000000000019'::uuid, 'Farah',  'Ali',    'MOTHER', '9876500018', 'Designer'),
    ('40000000-0000-0000-0000-000000000020'::uuid, 'Nikhil', 'Joshi',  'FATHER', '9876500019', 'Professor'),
    ('40000000-0000-0000-0000-000000000021'::uuid, 'Sameer', 'Khan',   'FATHER', '9876500020', 'Consultant'),
    ('40000000-0000-0000-0000-000000000022'::uuid, 'Anita',  'Rao',    'MOTHER', '9876500021', 'Accountant')
) AS v(id, first_name, last_name, rel, phone, occ)
WHERE NOT EXISTS (SELECT 1 FROM guardian g WHERE g.id = v.id);

INSERT INTO student_guardian (school_id, student_id, guardian_id, relationship, is_primary)
SELECT '20000000-0000-0000-0000-000000000001', v.student_id, v.guardian_id, v.rel, true
FROM (VALUES
    ('40000000-0000-0000-0000-000000000002'::uuid, '40000000-0000-0000-0000-000000000012'::uuid, 'MOTHER'),
    ('40000000-0000-0000-0000-000000000003'::uuid, '40000000-0000-0000-0000-000000000013'::uuid, 'FATHER'),
    ('40000000-0000-0000-0000-000000000004'::uuid, '40000000-0000-0000-0000-000000000014'::uuid, 'FATHER'),
    ('40000000-0000-0000-0000-000000000005'::uuid, '40000000-0000-0000-0000-000000000015'::uuid, 'FATHER'),
    ('40000000-0000-0000-0000-000000000006'::uuid, '40000000-0000-0000-0000-000000000016'::uuid, 'FATHER'),
    ('40000000-0000-0000-0000-000000000007'::uuid, '40000000-0000-0000-0000-000000000017'::uuid, 'MOTHER'),
    ('40000000-0000-0000-0000-000000000008'::uuid, '40000000-0000-0000-0000-000000000018'::uuid, 'FATHER'),
    ('40000000-0000-0000-0000-000000000009'::uuid, '40000000-0000-0000-0000-000000000019'::uuid, 'MOTHER'),
    ('40000000-0000-0000-0000-000000000010'::uuid, '40000000-0000-0000-0000-000000000020'::uuid, 'FATHER'),
    ('40000000-0000-0000-0000-000000000011'::uuid, '40000000-0000-0000-0000-000000000021'::uuid, 'FATHER'),
    ('40000000-0000-0000-0000-000000000012'::uuid, '40000000-0000-0000-0000-000000000022'::uuid, 'MOTHER')
) AS v(student_id, guardian_id, rel)
WHERE NOT EXISTS (
    SELECT 1 FROM student_guardian sg
    WHERE sg.student_id = v.student_id AND sg.guardian_id = v.guardian_id
);

INSERT INTO student_enrollment (id, school_id, student_id, section_id, academic_year_id, roll_number, status)
SELECT v.id, '20000000-0000-0000-0000-000000000001', v.student_id, v.section_id,
       '30000000-0000-0000-0000-000000000001', v.roll, 'ACTIVE'
FROM (VALUES
    ('40000000-0000-0000-0000-000000000102'::uuid, '40000000-0000-0000-0000-000000000002'::uuid, '30000000-0000-0000-0000-000000000021'::uuid, 2),
    ('40000000-0000-0000-0000-000000000103'::uuid, '40000000-0000-0000-0000-000000000003'::uuid, '30000000-0000-0000-0000-000000000022'::uuid, 1),
    ('40000000-0000-0000-0000-000000000104'::uuid, '40000000-0000-0000-0000-000000000004'::uuid, '30000000-0000-0000-0000-000000000022'::uuid, 2),
    ('40000000-0000-0000-0000-000000000105'::uuid, '40000000-0000-0000-0000-000000000005'::uuid, '30000000-0000-0000-0000-000000000023'::uuid, 1),
    ('40000000-0000-0000-0000-000000000106'::uuid, '40000000-0000-0000-0000-000000000006'::uuid, '30000000-0000-0000-0000-000000000023'::uuid, 2),
    ('40000000-0000-0000-0000-000000000107'::uuid, '40000000-0000-0000-0000-000000000007'::uuid, '30000000-0000-0000-0000-000000000024'::uuid, 1),
    ('40000000-0000-0000-0000-000000000108'::uuid, '40000000-0000-0000-0000-000000000008'::uuid, '30000000-0000-0000-0000-000000000024'::uuid, 2),
    ('40000000-0000-0000-0000-000000000109'::uuid, '40000000-0000-0000-0000-000000000009'::uuid, '30000000-0000-0000-0000-000000000025'::uuid, 1),
    ('40000000-0000-0000-0000-000000000110'::uuid, '40000000-0000-0000-0000-000000000010'::uuid, '30000000-0000-0000-0000-000000000025'::uuid, 2),
    ('40000000-0000-0000-0000-000000000111'::uuid, '40000000-0000-0000-0000-000000000011'::uuid, '30000000-0000-0000-0000-000000000026'::uuid, 1),
    ('40000000-0000-0000-0000-000000000112'::uuid, '40000000-0000-0000-0000-000000000012'::uuid, '30000000-0000-0000-0000-000000000026'::uuid, 2)
) AS v(id, student_id, section_id, roll)
WHERE NOT EXISTS (SELECT 1 FROM student_enrollment e WHERE e.id = v.id);

INSERT INTO fee_structure (id, school_id, class_id, academic_year_id, category_id, amount, frequency, due_day)
SELECT v.id, '20000000-0000-0000-0000-000000000001', v.class_id,
       '30000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', v.amount, 'MONTHLY', 10
FROM (VALUES
    ('50000000-0000-0000-0000-000000000013'::uuid, '30000000-0000-0000-0000-000000000012'::uuid, 1600.00),
    ('50000000-0000-0000-0000-000000000016'::uuid, '30000000-0000-0000-0000-000000000013'::uuid, 1700.00)
) AS v(id, class_id, amount)
WHERE NOT EXISTS (SELECT 1 FROM fee_structure f WHERE f.id = v.id);

INSERT INTO student_fee_assignment (id, school_id, student_id, fee_structure_id, status)
SELECT v.id, '20000000-0000-0000-0000-000000000001', v.student_id, v.fee_id, 'ACTIVE'
FROM (VALUES
    ('50000000-0000-0000-0000-000000000022'::uuid, '40000000-0000-0000-0000-000000000002'::uuid, '50000000-0000-0000-0000-000000000011'::uuid),
    ('50000000-0000-0000-0000-000000000023'::uuid, '40000000-0000-0000-0000-000000000003'::uuid, '50000000-0000-0000-0000-000000000011'::uuid),
    ('50000000-0000-0000-0000-000000000024'::uuid, '40000000-0000-0000-0000-000000000004'::uuid, '50000000-0000-0000-0000-000000000011'::uuid),
    ('50000000-0000-0000-0000-000000000025'::uuid, '40000000-0000-0000-0000-000000000005'::uuid, '50000000-0000-0000-0000-000000000013'::uuid),
    ('50000000-0000-0000-0000-000000000026'::uuid, '40000000-0000-0000-0000-000000000006'::uuid, '50000000-0000-0000-0000-000000000013'::uuid),
    ('50000000-0000-0000-0000-000000000027'::uuid, '40000000-0000-0000-0000-000000000007'::uuid, '50000000-0000-0000-0000-000000000013'::uuid),
    ('50000000-0000-0000-0000-000000000028'::uuid, '40000000-0000-0000-0000-000000000009'::uuid, '50000000-0000-0000-0000-000000000016'::uuid)
) AS v(id, student_id, fee_id)
WHERE NOT EXISTS (SELECT 1 FROM student_fee_assignment a WHERE a.id = v.id);

INSERT INTO fee_payment (id, school_id, receipt_no, student_id, student_fee_assignment_id, amount_paid, paid_at, payment_method, remarks, recorded_by)
SELECT v.id, '20000000-0000-0000-0000-000000000001', v.receipt, v.student_id, v.assignment,
       v.amount, now() - (v.days || ' days')::interval, v.method, v.remarks,
       '20000000-0000-0000-0000-000000000002'
FROM (VALUES
    ('50000000-0000-0000-0000-000000000042'::uuid, 'RCPT-0002', '40000000-0000-0000-0000-000000000002'::uuid, '50000000-0000-0000-0000-000000000022'::uuid, 1500.00, 4, 'UPI',  'Feb tuition'),
    ('50000000-0000-0000-0000-000000000043'::uuid, 'RCPT-0003', '40000000-0000-0000-0000-000000000003'::uuid, '50000000-0000-0000-0000-000000000023'::uuid, 1500.00, 3, 'CASH', 'Feb tuition'),
    ('50000000-0000-0000-0000-000000000044'::uuid, 'RCPT-0004', '40000000-0000-0000-0000-000000000005'::uuid, '50000000-0000-0000-0000-000000000025'::uuid, 1600.00, 2, 'CARD', 'Feb tuition')
) AS v(id, receipt, student_id, assignment, amount, days, method, remarks)
WHERE NOT EXISTS (SELECT 1 FROM fee_payment p WHERE p.id = v.id);

INSERT INTO notice (id, school_id, title, body, author_id, visibility_scope, priority, publish_at, status)
SELECT v.id, '20000000-0000-0000-0000-000000000001', v.title, v.body,
       '20000000-0000-0000-0000-000000000002', 'SCHOOL_WIDE', v.priority, now(), 'PUBLISHED'
FROM (VALUES
    ('60000000-0000-0000-0000-000000000002'::uuid, 'PTA Meeting', 'Parents meeting on 15 February at 10 AM in the auditorium.', 'IMPORTANT'),
    ('60000000-0000-0000-0000-000000000003'::uuid, 'Sports Day', 'Annual sports day will be held next Friday. Students must wear the sports uniform.', 'NORMAL'),
    ('60000000-0000-0000-0000-000000000005'::uuid, 'Fee Payment Reminder', 'Kindly pay pending fee installments before the due date.', 'URGENT')
) AS v(id, title, body, priority)
WHERE NOT EXISTS (SELECT 1 FROM notice n WHERE n.id = v.id);

INSERT INTO attendance (id, school_id, student_id, section_id, attendance_date, status, marked_by)
SELECT v.id, '20000000-0000-0000-0000-000000000001', v.student_id, v.section_id,
       CURRENT_DATE, v.status, '20000000-0000-0000-0000-000000000003'
FROM (VALUES
    ('60000000-0000-0000-0000-000000000101'::uuid, '40000000-0000-0000-0000-000000000001'::uuid, '30000000-0000-0000-0000-000000000021'::uuid, 'PRESENT'),
    ('60000000-0000-0000-0000-000000000102'::uuid, '40000000-0000-0000-0000-000000000002'::uuid, '30000000-0000-0000-0000-000000000021'::uuid, 'PRESENT'),
    ('60000000-0000-0000-0000-000000000103'::uuid, '40000000-0000-0000-0000-000000000003'::uuid, '30000000-0000-0000-0000-000000000022'::uuid, 'PRESENT'),
    ('60000000-0000-0000-0000-000000000104'::uuid, '40000000-0000-0000-0000-000000000004'::uuid, '30000000-0000-0000-0000-000000000022'::uuid, 'LATE'),
    ('60000000-0000-0000-0000-000000000105'::uuid, '40000000-0000-0000-0000-000000000005'::uuid, '30000000-0000-0000-0000-000000000023'::uuid, 'PRESENT'),
    ('60000000-0000-0000-0000-000000000106'::uuid, '40000000-0000-0000-0000-000000000006'::uuid, '30000000-0000-0000-0000-000000000023'::uuid, 'ABSENT'),
    ('60000000-0000-0000-0000-000000000107'::uuid, '40000000-0000-0000-0000-000000000007'::uuid, '30000000-0000-0000-0000-000000000024'::uuid, 'PRESENT'),
    ('60000000-0000-0000-0000-000000000108'::uuid, '40000000-0000-0000-0000-000000000008'::uuid, '30000000-0000-0000-0000-000000000024'::uuid, 'PRESENT'),
    ('60000000-0000-0000-0000-000000000109'::uuid, '40000000-0000-0000-0000-000000000009'::uuid, '30000000-0000-0000-0000-000000000025'::uuid, 'PRESENT'),
    ('60000000-0000-0000-0000-000000000110'::uuid, '40000000-0000-0000-0000-000000000010'::uuid, '30000000-0000-0000-0000-000000000025'::uuid, 'PRESENT'),
    ('60000000-0000-0000-0000-000000000111'::uuid, '40000000-0000-0000-0000-000000000011'::uuid, '30000000-0000-0000-0000-000000000026'::uuid, 'PRESENT'),
    ('60000000-0000-0000-0000-000000000112'::uuid, '40000000-0000-0000-0000-000000000012'::uuid, '30000000-0000-0000-0000-000000000026'::uuid, 'PRESENT')
) AS v(id, student_id, section_id, status)
WHERE NOT EXISTS (
    SELECT 1 FROM attendance a
    WHERE a.student_id = v.student_id AND a.attendance_date = CURRENT_DATE
);
