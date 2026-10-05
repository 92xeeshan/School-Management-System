# Product Requirements Document (PRD / SRS)

**Product:** School Management System (SchoolMS)
**Companion docs:** `BRD.md`, `USER_STORIES_SCOPE.md`, `docs/api/API_SPECIFICATION.md`
**Roles in code:** `SUPER_ADMIN`, `ADMIN`, `TEACHER`, `PARENT`, `STUDENT` (no `PRINCIPAL`)

Requirement IDs (`PR-xx`) map to test cases in `docs/qa/TEST_CASES.md`.

---

## 1. System Overview and User Personas

### 1.1 System overview

SchoolMS is a browser SPA (Angular 19) talking to a Spring Boot REST API under `/api/`. PostgreSQL stores all business data. Flyway owns schema. MinIO stores optional files. Authentication is JWT access tokens (15 minutes) plus opaque rotating refresh tokens (30 days). Authorization is permission codes on `@PreAuthorize`, not Spring `hasRole`.

### 1.2 Personas and permissions

Navigation is **permission-based**. Role names are labels; APIs check permission codes.

| Persona | Login (demo, password `Admin@123`) | Typical permission set |
|---|---|---|
| Super Admin | `superadmin` | All permissions + `SCHOOL_READ` / `SCHOOL_MANAGE`; RLS bypass |
| Admin | `admin` | All school-level permissions including exam manage, staff CRUD, certificate approve |
| Teacher | `teacher` (Asha Sharma, EMP001, class teacher Class 5-A) | Read students/classes; mark attendance; exam read/mark; certificate generate/review |
| Parent | `parent` (Rajesh Kumar, guardian of ADM0001) | Timetable, attendance, fees, notices, events, report/marksheet read, certificate request |
| Student | `student` (Aarav Kumar, ADM0001) | Timetable, attendance, notices, events, admit/report/marksheet read, certificate request |

See `docs/security/SECURITY.md` for the full RBAC matrix.

### 1.3 Non-goals in the current UI

- Syllabus and academic-terms tabs are placeholders.
- Examinations analytics and re-evaluation tabs are placeholders.
- There is no dedicated Users admin screen in the Angular app; user APIs exist for admin/super-admin clients.
- Parents do not have `ADMIT_CARD_READ`.

---

## 2. Functional Specifications

### PR-01 Authentication and session

- Login with username or email and password (`POST /api/auth/login`).
- Response envelope `{ success, message, data }` with `accessToken`, `refreshToken`, `expiresInSeconds`, `user` (id, schoolId, username, roles, permissions, locale).
- Inactive users: 403 `auth.user_inactive`. Transfer-certificate deactivation: 403 `auth.tc_deactivated`. Bad credentials: 401.
- Access JWT claims: `sub` (user UUID), `schoolId`, `roles`, `locale`, issuer `schoolms`.
- Frontend stores session in `localStorage` key `schoolms.auth`. On 401, interceptor refreshes once via `POST /api/auth/refresh`.
- Logout: `POST /api/auth/logout?refreshToken=` revokes the hashed refresh token.
- `GET /api/auth/me` hydrates profile after reload.
- Locale update: `PUT /api/users/me/locale`.

**Acceptance (GWT)**

- Given a seeded `admin` account, when login succeeds, then the client receives tokens and is redirected to `/dashboard`.
- Given an expired access token and a valid refresh token, when an API call returns 401, then the interceptor refreshes and retries once.
- Given a deactivated user, when they login, then the API returns 403 and the login page shows the inactive/TC message.

### PR-02 Multi-tenancy and schools

- Tenant discriminator `school_id` on tenant tables; RLS policy `tenant_isolation`.
- Runtime DB role `app_rls`; Flyway owner `schoolms`.
- JWT `schoolId` is applied as `app.school_id`; `SUPER_ADMIN` sets `app.bypass_rls=true`.
- `GET /api/schools/current` for the signed-in school; list/create/patch schools require `SCHOOL_READ` / `SCHOOL_MANAGE`.

### PR-03 Students and guardians

- Paginated student list, search, section filter.
- Create / update / deactivate; unique `(school_id, admission_no)`.
- Profile view/edit with class-teacher vs admin rules.
- Guardians: list, create, link, unlink.
- Enroll student in class/section for an academic year.
- CSV import (`STUDENT_IMPORT`).
- UI: `/students`, `/students/:id`.

### PR-04 Staff

- Teaching staff CRUD, class-teacher assignment, deactivate.
- Non-teaching staff CRUD and deactivate.
- Unique `(school_id, employee_no)`.
- UI: `/staff/teachers`, `/staff/non-teaching`. Permissions `STAFF_*`.

### PR-05 Academics

- Academic years, classes, sections (capacity, room), subjects, class-subject mapping.
- Timetable entries unique per section/year/day/period; teacher/room conflict checks.
- Grading schemes, grade boundaries, assessment weightages; one ACTIVE scheme per class.
- UI: `/academics/*`. Syllabus and terms: coming soon.

### PR-06 Attendance

- Mark Present / Absent / Late / Leave for a section and date (`ATTENDANCE_MARK`).
- Sessions and per-student history/summary (`ATTENDANCE_READ`).
- Unique attendance per student per date.

### PR-07 Fees

- Categories, structures (class + year + category, frequency MONTHLY/QUARTERLY/ANNUAL).
- Assign to students with optional discount; installments PENDING/PARTIAL/PAID/OVERDUE/WAIVED.
- Record payment CASH/CARD/UPI/BANK_TRANSFER; unique receipt number; PDF receipt URL.
- Parents: `FEE_READ`, `FEE_RECEIPT_VIEW`. Students do not have fee permissions in V2 seed.

### PR-08 Notices

- Create draft, publish, archive. Audience scopes (school / teachers / parents).
- Published list for authenticated users; unread count; mark read.
- UI composer for `NOTICE_CREATE`.

### PR-09 Calendar and dashboard

- Events with type, date range, visibility (role/class). Holidays PDF export for `EVENT_MANAGE`.
- Dashboard KPIs, gender mix, attendance trend, star students, agenda, notices, widget customize (`schoolms.dashboard.widgets`).
- In-app notifications: list, unread count, mark one/all read. Header bell.

### PR-10 Examinations

- Schedules with overlap validation, publish status, invigilators (`EXAM_MANAGE`).
- Marks grid: draft/save/submit, lock/unlock/deadline (`EXAM_MANAGE`), CSV/Excel import (`EXAM_MARK`).
- Admit cards and report cards: roster, mine, PDF/ZIP; print only if published.
- Marksheets: submit, approve, reject, publish/lock, PDF/ZIP (`MARKSHEET_READ` / `MARKSHEET_MANAGE`).
- UI: `/examinations/*`, `/downloads/marksheet`. Analytics and re-evaluation: placeholders.

### PR-11 Certificates

Types: `TC`, `BONAFIDE`, `CHARACTER`, `COURSE_COMPLETION`.

- Templates (header/footer, requires-approval) — `CERTIFICATE_MANAGE`.
- Staff generate draft — `CERTIFICATE_GENERATE`.
- Student/parent request (JSON or multipart supporting file) — `CERTIFICATE_READ`.
- Class-teacher review/forward — `CERTIFICATE_GENERATE`.
- Admin approve/reject issued drafts and requests — `CERTIFICATE_APPROVE`.
- Sequential certificate numbers; TC can deactivate student + user.
- Downloads: `/certificates` (staff queue requires generate/approve in nav), `/downloads/certificates` (self-service).

### PR-12 Presentation

- Languages: English (ltr), Hindi (ltr), Urdu (rtl). Persisted `schoolms.locale`.
- Material M3 theme, light/dark (`schoolms.theme`), collapsible sidenav (`schoolms.sidenav.collapsed`).
- Shared page header, empty state, data tables.
- Print sheets remain white (`#fff`).

---

## 3. Non-Functional Requirements

| ID | Area | Requirement |
|---|---|---|
| NFR-01 | Performance | Hikari pool default 10. Caffeine permission cache TTL 5 minutes. No Redis. |
| NFR-02 | Availability | Single process; health at `GET /actuator/health` (public). No HA documented. |
| NFR-03 | Security | BCrypt passwords; SHA-256 refresh tokens; HMAC JWT; CORS allowlist; CSRF off (stateless). Prod JWT secret required. |
| NFR-04 | Data isolation | RLS on tenant tables; `SUPER_ADMIN` bypass only. |
| NFR-05 | i18n | API messages via MessageSource en/hi/ur; UI JSON files. |
| NFR-06 | Files | Multipart 10 MB file / 20 MB request. MinIO bucket `schoolms`. |
| NFR-07 | Observability | Actuator `health` + `info` only. App logs `com.schoolms` DEBUG in dev, INFO in prod. |
| NFR-08 | Compatibility | JDK 21, Maven 3.9+, Node 20+, PostgreSQL 15, modern Chromium/Firefox. |
| NFR-09 | Scalability | Vertical only in current design; shared DB multi-tenant. |
| NFR-10 | Print | Official PDFs via JasperReports; UI print/preview for admit/report/marksheet. |

---

## 4. Acceptance Criteria (module-level Given-When-Then)

### Students

- Given `STUDENT_CREATE`, when admin submits a new admission number, then the student appears in the list for that school only.
- Given a duplicate admission number in the same school, when create is called, then the API returns 409/business error.

### Attendance

- Given `ATTENDANCE_MARK` and a section roster, when statuses are saved, then records persist uniquely per student/date.
- Given a parent with `ATTENDANCE_READ`, when they open attendance, then they see their child’s history, not other sections.

### Fees

- Given `FEE_PAYMENT_RECORD`, when a payment is posted, then an installment updates and a receipt number is allocated.

### Exams

- Given unpublished marksheets, when a student calls `/api/marksheets/mine`, then unpublished results are redacted/blocked as implemented.
- Given `MARKSHEET_MANAGE`, when admin approves, then status becomes published and PDF export is allowed.

### Certificates

- Given a student with no pending request, when they submit a bonafide request, then it appears in the staff queue as `SUBMITTED`.
- Given a pending request of the same type, when they submit again, then the API rejects the duplicate.
- Given class-teacher scope, when another section’s request is reviewed, then the API denies it.
- Given `CERTIFICATE_APPROVE`, when admin approves a TC, then a certificate number is issued and the student may be deactivated.

### Tenancy

- Given two schools, when a user from school A lists students, then school B rows are not returned.

---

## 5. UI/UX Wireframe References

There are no Figma files in the repository. Interaction specs follow the shipped Angular routes.

| Flow | Route | Layout |
|---|---|---|
| Login | `/login` | Centered card, locale pills, theme-aware surface |
| Shell | `MainLayoutComponent` | Material sidenav + toolbar, profile menu, language select, theme toggle, notification bell |
| Lists | Students, fees, attendance, notices, staff, exams | `app-page-header`, `.card-toolbar`, `.data-table`, `app-empty-state` |
| Student profile | `/students/:id` | Sections: personal, academic, attendance; actions by permission |
| Certificates | `/certificates` | Tabs: templates, request queue, issued register |
| Downloads | `/downloads/marksheet`, `/downloads/certificates` | Filters, selection, publish actions, print/PDF |
| Print preview | Admit / report / marksheet modals | A4, white background |

RTL: `document.documentElement.dir = rtl` for Urdu; CDK `Dir` on `AppComponent`.

Mobile: sidenav over-mode at `max-width: 768px`; extra breakpoints at 480px in layout SCSS.
