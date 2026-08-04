-- =====================================================================
-- COMPLETE DDL FOR ALL TABLES - School Management System
-- =====================================================================
-- Run this first to create every table in the database:
--     psql -U schoolms -d schoolms -f 01_create_tables.sql
--
-- Notes:
--   * Conventions: UUID primary keys (gen_random_uuid, built into
--     PostgreSQL 13+), school_id on every tenant table, VARCHAR enums with
--     CHECK constraints, TIMESTAMPTZ audit columns on every entity table.
--   * RLS helper functions are created here; the application enables the
--     Row-Level Security policies at runtime via its Flyway migration.
--   * Seed data (roles, permissions, sample records) lives in
--     db/02_insert_records.sql.
-- =====================================================================

-- RLS helpers ----------------------------------------------------------
CREATE OR REPLACE FUNCTION app_set_school_id(tenant_id UUID)
RETURNS void
LANGUAGE sql
AS $$
  SELECT set_config('app.school_id', tenant_id::text, false);
$$;

CREATE OR REPLACE FUNCTION app_current_school_id()
RETURNS UUID
LANGUAGE sql
STABLE
AS $$
  SELECT NULLIF(current_setting('app.school_id', true), '')::uuid;
$$;

CREATE OR REPLACE FUNCTION app_bypass_rls()
RETURNS boolean
LANGUAGE sql
STABLE
AS $$
  SELECT current_setting('app.bypass_rls', true) = 'true';
$$;

-- =====================================================================
-- Global tables (no school_id)
-- =====================================================================

CREATE TABLE school (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(50)  NOT NULL UNIQUE,
    name                VARCHAR(200) NOT NULL,
    address             VARCHAR(500),
    phone               VARCHAR(50),
    email               VARCHAR(120),
    logo_url            VARCHAR(500),
    currency            VARCHAR(10)  NOT NULL DEFAULT 'INR',
    default_locale      VARCHAR(10)  NOT NULL DEFAULT 'en',
    timezone            VARCHAR(60)  NOT NULL DEFAULT 'Asia/Kolkata',
    academic_start_month SMALLINT    NOT NULL DEFAULT 4,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE role (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(300),
    is_system   BOOLEAN      NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE permission (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(100) NOT NULL UNIQUE,
    module      VARCHAR(50)  NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description VARCHAR(300),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE role_permission (
    role_id       UUID NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permission(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- =====================================================================
-- Tenant tables (school_id FK + RLS in V4)
-- =====================================================================

CREATE TABLE app_user (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id     UUID REFERENCES school(id) ON DELETE CASCADE,
    username      VARCHAR(60) NOT NULL,
    email         VARCHAR(120),
    phone         VARCHAR(30),
    password_hash VARCHAR(100) NOT NULL,
    first_name    VARCHAR(60) NOT NULL,
    last_name     VARCHAR(60),
    locale        VARCHAR(10) NOT NULL DEFAULT 'en',
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                  CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED')),
    last_login_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_school_username UNIQUE (school_id, username),
    CONSTRAINT uq_user_school_email   UNIQUE (school_id, email)
);

CREATE TABLE user_role (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID REFERENCES school(id) ON DELETE CASCADE,
    user_id   UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role_id   UUID NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_role UNIQUE (school_id, user_id, role_id)
);

CREATE TABLE refresh_token (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash  VARCHAR(128) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    device_info VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_token_user ON refresh_token(user_id);

-- Students / guardians ------------------------------------------------
CREATE TABLE student (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id     UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    user_id       UUID REFERENCES app_user(id),
    admission_no  VARCHAR(30) NOT NULL,
    first_name    VARCHAR(60) NOT NULL,
    last_name     VARCHAR(60),
    date_of_birth DATE,
    gender        VARCHAR(10) CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    blood_group   VARCHAR(5),
    religion      VARCHAR(30),
    nationality   VARCHAR(60) DEFAULT 'IN',
    admission_date DATE NOT NULL DEFAULT CURRENT_DATE,
    photo_url     VARCHAR(500),
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                  CHECK (status IN ('ACTIVE', 'INACTIVE', 'ALUMNI', 'TRANSFERRED')),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_student_school_admission UNIQUE (school_id, admission_no)
);
CREATE INDEX idx_student_school_name ON student(school_id, last_name);

CREATE TABLE guardian (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id     UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    user_id       UUID REFERENCES app_user(id),
    first_name    VARCHAR(60) NOT NULL,
    last_name     VARCHAR(60),
    relationship  VARCHAR(20) NOT NULL DEFAULT 'GUARDIAN'
                  CHECK (relationship IN ('FATHER', 'MOTHER', 'GUARDIAN', 'OTHER')),
    email         VARCHAR(120),
    phone         VARCHAR(30),
    occupation    VARCHAR(100),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE student_guardian (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id    UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    student_id   UUID NOT NULL REFERENCES student(id) ON DELETE CASCADE,
    guardian_id  UUID NOT NULL REFERENCES guardian(id) ON DELETE CASCADE,
    relationship VARCHAR(20) NOT NULL DEFAULT 'GUARDIAN'
                 CHECK (relationship IN ('FATHER', 'MOTHER', 'GUARDIAN', 'OTHER')),
    is_primary   BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uq_student_guardian UNIQUE (school_id, student_id, guardian_id)
);

-- Academics ------------------------------------------------------------
CREATE TABLE academic_year (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    name        VARCHAR(30) NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    is_current  BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_academic_year_school_name UNIQUE (school_id, name)
);

CREATE TABLE school_class (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    name        VARCHAR(50) NOT NULL,
    code        VARCHAR(20),
    sort_order  INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_class_school_name UNIQUE (school_id, name)
);

CREATE TABLE section (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    class_id    UUID NOT NULL REFERENCES school_class(id) ON DELETE CASCADE,
    name        VARCHAR(20) NOT NULL,
    capacity    INTEGER NOT NULL DEFAULT 40,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_section_class_name UNIQUE (class_id, name)
);

CREATE TABLE subject (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    name        VARCHAR(80) NOT NULL,
    code        VARCHAR(20),
    type        VARCHAR(10) NOT NULL DEFAULT 'CORE' CHECK (type IN ('CORE', 'ELECTIVE')),
    description VARCHAR(300),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_subject_school_name UNIQUE (school_id, name)
);

CREATE TABLE teacher_profile (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id      UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    user_id        UUID REFERENCES app_user(id),
    employee_no    VARCHAR(30) NOT NULL,
    first_name     VARCHAR(60) NOT NULL,
    last_name      VARCHAR(60),
    email          VARCHAR(120),
    phone          VARCHAR(30),
    designation    VARCHAR(100),
    qualification  VARCHAR(150),
    join_date      DATE,
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                   CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_teacher_school_emp UNIQUE (school_id, employee_no)
);

CREATE TABLE teacher_subject (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id  UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    teacher_id UUID NOT NULL REFERENCES teacher_profile(id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES subject(id) ON DELETE CASCADE,
    CONSTRAINT uq_teacher_subject UNIQUE (school_id, teacher_id, subject_id)
);

CREATE TABLE teacher_section (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    teacher_id      UUID NOT NULL REFERENCES teacher_profile(id) ON DELETE CASCADE,
    section_id      UUID NOT NULL REFERENCES section(id) ON DELETE CASCADE,
    academic_year_id UUID NOT NULL REFERENCES academic_year(id) ON DELETE CASCADE,
    is_class_teacher BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uq_teacher_section UNIQUE (school_id, teacher_id, section_id, academic_year_id)
);

CREATE TABLE student_enrollment (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id        UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    student_id       UUID NOT NULL REFERENCES student(id) ON DELETE CASCADE,
    section_id       UUID NOT NULL REFERENCES section(id) ON DELETE CASCADE,
    academic_year_id UUID NOT NULL REFERENCES academic_year(id) ON DELETE CASCADE,
    roll_number      INTEGER,
    enrollment_date  DATE NOT NULL DEFAULT CURRENT_DATE,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                     CHECK (status IN ('ACTIVE', 'PROMOTED', 'LEFT')),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_enrollment_student_year UNIQUE (student_id, academic_year_id)
);
CREATE INDEX idx_enrollment_section ON student_enrollment(school_id, section_id, academic_year_id);

CREATE TABLE timetable_entry (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id        UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    section_id       UUID NOT NULL REFERENCES section(id) ON DELETE CASCADE,
    academic_year_id UUID NOT NULL REFERENCES academic_year(id) ON DELETE CASCADE,
    day_of_week      SMALLINT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    period_number    SMALLINT NOT NULL,
    start_time       TIME NOT NULL,
    end_time         TIME NOT NULL,
    subject_id       UUID REFERENCES subject(id) ON DELETE SET NULL,
    teacher_id       UUID REFERENCES teacher_profile(id) ON DELETE SET NULL,
    room             VARCHAR(50),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_timetable_slot UNIQUE (section_id, academic_year_id, day_of_week, period_number)
);

-- Attendance ------------------------------------------------------------
CREATE TABLE attendance_session (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    section_id        UUID NOT NULL REFERENCES section(id) ON DELETE CASCADE,
    academic_year_id  UUID NOT NULL REFERENCES academic_year(id) ON DELETE CASCADE,
    attendance_date   DATE NOT NULL,
    subject_id        UUID REFERENCES subject(id) ON DELETE SET NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                      CHECK (status IN ('PENDING', 'PARTIAL', 'COMPLETE')),
    marked_by         UUID REFERENCES app_user(id),
    marked_at         TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_attendance_session UNIQUE (section_id, attendance_date, subject_id)
);

CREATE TABLE attendance (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    student_id        UUID NOT NULL REFERENCES student(id) ON DELETE CASCADE,
    section_id        UUID NOT NULL REFERENCES section(id) ON DELETE CASCADE,
    attendance_date   DATE NOT NULL,
    status            VARCHAR(10) NOT NULL CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'LEAVE')),
    remark            VARCHAR(300),
    marked_by         UUID REFERENCES app_user(id),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_attendance_student_date UNIQUE (student_id, attendance_date)
);
CREATE INDEX idx_attendance_section_date ON attendance(school_id, section_id, attendance_date);

-- Fees ------------------------------------------------------------------
CREATE TABLE fee_category (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    name        VARCHAR(80) NOT NULL,
    code        VARCHAR(30) NOT NULL,
    description VARCHAR(300),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_fee_category_school UNIQUE (school_id, code)
);

CREATE TABLE fee_structure (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    class_id          UUID NOT NULL REFERENCES school_class(id) ON DELETE CASCADE,
    academic_year_id  UUID NOT NULL REFERENCES academic_year(id) ON DELETE CASCADE,
    category_id       UUID NOT NULL REFERENCES fee_category(id) ON DELETE CASCADE,
    amount            NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    frequency         VARCHAR(10) NOT NULL DEFAULT 'MONTHLY'
                      CHECK (frequency IN ('MONTHLY', 'QUARTERLY', 'ANNUAL')),
    due_day           SMALLINT,
    applicable_from   DATE,
    applicable_to     DATE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_fee_structure UNIQUE (class_id, academic_year_id, category_id)
);

CREATE TABLE student_fee_assignment (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id          UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    student_id         UUID NOT NULL REFERENCES student(id) ON DELETE CASCADE,
    fee_structure_id   UUID NOT NULL REFERENCES fee_structure(id) ON DELETE CASCADE,
    discount_type      VARCHAR(10) CHECK (discount_type IN ('FIXED', 'PERCENT')),
    discount_amount    NUMERIC(12,2),
    status             VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_student_fee UNIQUE (student_id, fee_structure_id)
);

CREATE TABLE fee_installment (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id                 UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    student_fee_assignment_id UUID NOT NULL REFERENCES student_fee_assignment(id) ON DELETE CASCADE,
    due_date                  DATE NOT NULL,
    amount_due                NUMERIC(12,2) NOT NULL CHECK (amount_due >= 0),
    amount_paid               NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (amount_paid >= 0),
    status                    VARCHAR(10) NOT NULL DEFAULT 'PENDING'
                              CHECK (status IN ('PENDING', 'PAID', 'PARTIAL', 'OVERDUE', 'WAIVED')),
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_fee_installment UNIQUE (student_fee_assignment_id, due_date)
);

CREATE TABLE fee_payment (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id                 UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    receipt_no                VARCHAR(30) NOT NULL,
    student_id                UUID NOT NULL REFERENCES student(id) ON DELETE CASCADE,
    student_fee_assignment_id UUID REFERENCES student_fee_assignment(id) ON DELETE SET NULL,
    amount_paid               NUMERIC(12,2) NOT NULL CHECK (amount_paid > 0),
    paid_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    payment_method            VARCHAR(20) NOT NULL DEFAULT 'CASH'
                              CHECK (payment_method IN ('CASH', 'CARD', 'UPI', 'BANK_TRANSFER')),
    reference_no              VARCHAR(80),
    remarks                   VARCHAR(300),
    recorded_by               UUID REFERENCES app_user(id),
    pdf_url                   VARCHAR(500),
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_fee_payment_receipt UNIQUE (school_id, receipt_no)
);
CREATE INDEX idx_fee_payment_student ON fee_payment(school_id, student_id);

-- Notices ----------------------------------------------------------------
CREATE TABLE notice (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id        UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    title            VARCHAR(200) NOT NULL,
    body             TEXT NOT NULL,
    author_id        UUID NOT NULL REFERENCES app_user(id),
    visibility_scope VARCHAR(20) NOT NULL DEFAULT 'SCHOOL_WIDE'
                     CHECK (visibility_scope IN ('SCHOOL_WIDE', 'CLASS_WIDE')),
    class_id         UUID REFERENCES school_class(id) ON DELETE SET NULL,
    section_id       UUID REFERENCES section(id) ON DELETE SET NULL,
    priority         VARCHAR(10) NOT NULL DEFAULT 'NORMAL'
                     CHECK (priority IN ('NORMAL', 'IMPORTANT', 'URGENT')),
    publish_at       TIMESTAMPTZ,
    expires_at       TIMESTAMPTZ,
    status           VARCHAR(10) NOT NULL DEFAULT 'DRAFT'
                     CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notice_school_status ON notice(school_id, status, publish_at);

CREATE TABLE notice_attachment (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    notice_id   UUID NOT NULL REFERENCES notice(id) ON DELETE CASCADE,
    file_url    VARCHAR(500) NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    file_type   VARCHAR(100),
    file_size   BIGINT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE notice_read_receipt (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL REFERENCES school(id) ON DELETE CASCADE,
    notice_id UUID NOT NULL REFERENCES notice(id) ON DELETE CASCADE,
    user_id   UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    read_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_notice_read UNIQUE (school_id, notice_id, user_id)
);
