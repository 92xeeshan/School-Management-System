# Admin Configuration and System Management Guide

Audience: school **Admin** and platform **Super Admin**. APIs exist for users even when a dedicated Users screen is not in the Angular sidebar.

---

## 1. Tenant and System Provisioning

### First-time local/demo

1. Follow `docs/DOCKER-SETUP.md` or `docs/README-BACKEND.md`.
2. Flyway creates schema and seeds **Demo Public School** (`code=DEMO`).
3. Log in as `admin` / `Admin@123`.

### Super Admin — add a school

1. Log in as `superadmin`.
2. Use `POST /api/schools` (Swagger or API client) with code, name, timezone, locale, currency.
3. Create an admin user in that school (`POST /api/users` + `ROLE_ASSIGN` → `ADMIN`).
4. School admins cannot call school-manage APIs (`SCHOOL_MANAGE` is super-admin only).

### Organization parameters

Stored on `school`: address, phone, email, logo URL, currency (`INR`), default locale, timezone (`Asia/Kolkata`), academic start month, status `ACTIVE`/`SUSPENDED`.

There is no multi-domain custom hostname feature. The UI origin must be on the backend CORS list (`CORS_ALLOWED_ORIGINS`).

### Academic year

Create the year via academics APIs (`/api/academic-years`) and mark it current before enrolling students. Demo year: `2025-26`.

---

## 2. User Administration

### Roles

`SUPER_ADMIN`, `ADMIN`, `TEACHER`, `PARENT`, `STUDENT`. Do not create `PRINCIPAL`.

### Create a user (API)

`POST /api/users` with username, password, names, school, then `POST /api/users/{id}/roles`.

Link:

- Teacher → `teacher_profile.user_id` (staff screens)
- Student → `student.user_id`
- Parent → `guardian.user_id`

### Bulk import

Students: `POST /api/students/import` (CSV) with `STUDENT_IMPORT`. There is no bulk user CSV in the UI.

### Deactivate

- Students: Students grid delete/deactivate (`STUDENT_DELETE`) or Transfer Certificate approval (may set inactive / TC login error).
- Teachers / non-teaching: deactivate actions on Staff pages.
- Users: `PATCH /api/users/{id}` status `INACTIVE` / `LOCKED`.

Inactive users cannot log in (`auth.user_inactive`).

### Demo accounts to replace

Change or disable `Admin@123` before loading real data.

---

## 3. Global Integration Setup

| Integration | Status in this version | What you can do |
|---|---|---|
| SMTP / email | Not implemented | Do not expect password-reset mail |
| SMS | Not implemented | |
| Payment gateway | Not implemented | Record CASH/CARD/UPI/BANK_TRANSFER manually on Fees |
| SSO / OAuth | Not implemented | Username + password only |
| MinIO | Optional | Set `MINIO_*`; needed for PDFs and certificate attachments |
| OpenAPI | `/swagger-ui.html` | Protect or disable on the public internet |

Certificate templates (letterhead HTML, requires-approval flag): Certificates → Templates (`CERTIFICATE_MANAGE`).

Fee categories/structures: Fees module (`FEE_STRUCTURE_MANAGE`).

Exam grading: Academics → Examinations tab (`EXAM_MANAGE`).

---

## 4. System Audit Log Operations

There is **no** audit-log screen. To investigate:

1. Application logs (`docker compose logs backend`).
2. Table timestamps (`created_at`, `updated_at`) and actor columns (`recorded_by`, `marked_by`, `issued_by_user_id`).
3. `app_user.last_login_at`.
4. `refresh_token.revoked_at` for session revocation.
5. Certificate request status history and marksheet status.

Filter in SQL as role `app_admin` / `schoolms` (bypasses RLS as owner). Do not use `app_rls` for cross-tenant forensics.

---

## 5. Recommended office procedures

1. Create academic year, classes, sections, subjects, timetable.
2. Import or add students; assign guardians; create parent logins if needed.
3. Assign class teachers (Staff → teacher → class-teacher).
4. Define fee structures and assign to students.
5. Teachers mark attendance daily.
6. Build exam schedule, enter marks, **submit**, admin **approve/publish** before students can download.
7. Certificate requests: class teacher forwards → admin approves.
8. Publish notices rather than leaving drafts.

Print: only published admit cards, report cards, and marksheets. Keep printer on A4 portrait.
