# Business Requirements Document (BRD)

**Product:** School Management System (SchoolMS)
**Version:** 0.1.0-SNAPSHOT
**Status:** As-built (documents the shipped application)
**Audience:** School operators, product owners, engineering

---

## 1. Executive Summary and Project Vision

### 1.1 Problem statement

K-12 schools still split student records, attendance, fees, exams, and certificates across spreadsheets, paper registers, and disconnected tools. That creates duplicate data, weak access control, delayed parent communication, and error-prone exam publishing.

### 1.2 Strategic objectives

SchoolMS is a multi-tenant web application that gives one school (or a platform operator spanning many schools) a single system for:

- Student information, guardians, and class/section enrollment
- Staff (teaching and non-teaching) and academics (classes, subjects, timetable)
- Daily attendance, fee collection, notices, and academic calendar
- Examination schedules, marks, admit cards, report cards, and marksheets
- Transfer / bonafide / character / course-completion certificates with approval
- Role-based dashboards and in-app notifications

The product is designed for Indian school operations (INR, Asia/Kolkata, en/hi/ur UI) but is not limited to a single board.

### 1.3 Vision

A permission-driven school operating system where every role (super admin, school admin, teacher, parent, student) sees only the data they are allowed to act on, with tenant isolation enforced in PostgreSQL Row-Level Security.

---

## 2. Stakeholder and Persona Matrix

| Persona | Role code | Primary goals | Typical actions |
|---|---|---|---|
| Platform operator | `SUPER_ADMIN` | Onboard schools, inspect all tenants | Create/update schools, platform users |
| School administrator | `ADMIN` | Run the school day to day | Students, staff, academics, fees, exams, certificates, notices |
| Class / subject teacher | `TEACHER` | Teach and record outcomes | Mark attendance, enter marks, review certificate requests, view assigned sections |
| Parent / guardian | `PARENT` | Track a child | Attendance, fees, notices, calendar, report cards, certificate requests |
| Student | `STUDENT` | Self-serve academic records | Timetable, attendance, notices, admit/report cards, certificate requests |

There is **no `PRINCIPAL` role**. Certificate “principal approval” is the `CERTIFICATE_APPROVE` permission, granted to `ADMIN` and `SUPER_ADMIN`.

Supporting stakeholders: engineering (Spring Boot / Angular), school IT (PostgreSQL, Docker), and accounts staff (non-teaching records; fee collection via admin).

---

## 3. Business Requirements and Features

Prioritized by operational impact. Requirement IDs are reused in `PRD.md` and `docs/qa/TEST_CASES.md`.

| ID | Capability | Business impact |
|---|---|---|
| BR-01 | Secure login with JWT access + rotating refresh tokens | Protects student and fee data |
| BR-02 | Permission-based RBAC (not role-name checks in APIs) | Least privilege per school |
| BR-03 | Shared-DB multi-tenancy with `school_id` + RLS | Isolate schools on one database |
| BR-04 | Student SIS (CRUD, guardians, enrollment, CSV import) | Master student record |
| BR-05 | Staff management (teachers, class teachers, non-teaching) | HR and class assignment |
| BR-06 | Academics (years, classes, sections, subjects, timetable) | Timetable and class structure |
| BR-07 | Daily attendance mark and history | Compliance and parent visibility |
| BR-08 | Fee structures, assignments, payments, PDF receipts | Revenue collection |
| BR-09 | Notices with audience and publish/archive | School communication |
| BR-10 | Academic calendar with role/class visibility | Events and holidays |
| BR-11 | Exam schedules, marks entry, lock, CSV/Excel import | Assessment operations |
| BR-12 | Admit cards, report cards, marksheets with publish lock | Official result documents |
| BR-13 | Certificates (generate, request, class-teacher review, admin approve) | TC / bonafide workflow |
| BR-14 | Role dashboards and in-app notifications | Daily awareness |
| BR-15 | i18n (English, Hindi, Urdu RTL) and light/dark theme | Accessibility for staff/parents |
| BR-16 | File storage (MinIO) for PDFs and attachments | Receipts and supporting docs |

---

## 4. Key Performance Indicators (KPIs)

| KPI | Target (product intent) | How it is observed today |
|---|---|---|
| Time to mark a section’s attendance | Under 2 minutes for a typical section | Attendance grid + save |
| Fee receipt issued at collection | 100% of recorded payments get a receipt number | `fee_payment.receipt_no` |
| Unpublished results not visible to students/parents | 100% | Marksheet/report-card publish flags |
| Tenant data leak across schools | Zero | RLS policies + tests around school_id |
| Demo login success | Seeded accounts sign in with `Admin@123` | Flyway V3 |
| API authorization failures | 403 for missing permission, 401 for missing/expired JWT | Spring Security + `GlobalExceptionHandler` |
| Localization coverage | en / hi / ur for UI chrome and API messages | ngx-translate + MessageSource |

Formal production SLOs (uptime, p95 latency) are not instrumented yet; see `docs/devops/RUNBOOK.md`.

---

## 5. Constraints and Assumptions

### Constraints

- Single-region, single PostgreSQL instance; no Kubernetes or managed cloud is required to run the app.
- Runtime Java 21, Spring Boot 3.5, Angular 19, PostgreSQL 15.
- JWT HMAC secret must be set in production (`JWT_SECRET`, 32+ characters).
- Preview environments expose one port; the Angular dev server proxies `/api` to the backend.
- Print documents (report card, marksheet, admit card) stay on white backgrounds.
- No GraphQL, WebSockets, or message queue in the current architecture.

### Assumptions

- One logical school per `school` row; `SUPER_ADMIN` has `school_id` NULL and bypasses RLS.
- Demo data (Demo Public School, academic year 2025-26) is acceptable for development.
- MinIO is optional for login/SIS testing; PDFs and certificate attachments need it.
- FERPA/GDPR-style privacy is a design goal (tenant isolation, hashed refresh tokens); there is no certified compliance program yet.

### Out of scope for this BRD version

- Online payment gateways and SSO
- SMS/email gateways
- Syllabus and exam-term configuration UIs (placeholders)
- Exam analytics and re-evaluation modules (placeholders)
- Dedicated HIPAA alignment (not a health product)
