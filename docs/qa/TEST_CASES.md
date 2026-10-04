# Test Cases and Requirements Traceability Matrix

IDs: `TC-xxx`. Requirement IDs from `docs/product/PRD.md` (`PR-xx`) and `BRD.md` (`BR-xx`).
Status column is for execution records (leave blank until run).

---

## 1. Requirements Traceability Matrix

| Requirement | Test cases |
|---|---|
| PR-01 Auth | TC-001, TC-002, TC-003, TC-004, TC-005 |
| PR-02 Tenancy | TC-010, TC-011 |
| PR-03 Students | TC-020, TC-021, TC-022, TC-023 |
| PR-04 Staff | TC-030, TC-031 |
| PR-05 Academics | TC-040, TC-041, TC-042 |
| PR-06 Attendance | TC-050, TC-051 |
| PR-07 Fees | TC-060, TC-061 |
| PR-08 Notices | TC-070 |
| PR-09 Calendar / dashboard / notifications | TC-080, TC-081, TC-082 |
| PR-10 Examinations | TC-090, TC-091, TC-092, TC-093, TC-094 |
| PR-11 Certificates | TC-100, TC-101, TC-102, TC-103, TC-104 |
| PR-12 Presentation | TC-110, TC-111 |
| NFR-03 Security | TC-003, TC-120, TC-121 |
| BR-03 RLS | TC-010 |

---

## 2. Detailed Test Scenarios

### Auth

#### TC-001 Login success
- **Requirement:** PR-01
- **Preconditions:** Flyway seeded; backend up
- **Steps:** POST `/api/auth/login` with `admin` / `Admin@123`
- **Expected:** 200, `data.accessToken`, `refreshToken`, roles contains `ADMIN`
- **Pass/Fail:**

#### TC-002 Bad password
- **Preconditions:** User exists
- **Steps:** Login with wrong password
- **Expected:** 401 `auth.bad_credentials`; no token
- **Pass/Fail:**

#### TC-003 Missing Bearer
- **Steps:** GET `/api/dashboard` without Authorization
- **Expected:** 401 `auth.unauthorized`
- **Pass/Fail:**

#### TC-004 Refresh rotation
- **Preconditions:** Valid refresh token
- **Steps:** POST `/api/auth/refresh`; reuse old refresh token
- **Expected:** First call 200 new pair; second call 401 invalid refresh
- **Pass/Fail:**

#### TC-005 Teacher login UI
- **Steps:** Open `/login`, sign in `teacher` / `Admin@123`
- **Expected:** Redirect `/dashboard`; sidenav shows attendance, not fee manage
- **Pass/Fail:**

### Tenancy

#### TC-010 School scope on students
- **Preconditions:** Two schools or SUPER_ADMIN vs ADMIN
- **Steps:** Login as `admin`; GET `/api/students`
- **Expected:** Only Demo Public School rows; ADM0001 visible
- **Pass/Fail:**

#### TC-011 Super admin bypass
- **Steps:** Login `superadmin`; GET `/api/schools`
- **Expected:** 200 list; `SCHOOL_READ` allowed
- **Pass/Fail:**

### Students

#### TC-020 Create unique admission
- **Permission:** `STUDENT_CREATE`
- **Steps:** POST student with new admission no
- **Expected:** 200; appears in GET list
- **Pass/Fail:**

#### TC-021 Duplicate admission
- **Steps:** Create with `ADM0001` again
- **Expected:** Business/409 error; original row unchanged
- **Pass/Fail:**

#### TC-022 Deactivate
- **Steps:** PATCH deactivate as admin
- **Expected:** Status not ACTIVE; student cannot use linked login if deactivated via TC flow
- **Pass/Fail:**

#### TC-023 Teacher cannot create
- **Steps:** As `teacher`, POST `/api/students`
- **Expected:** 403
- **Pass/Fail:**

### Staff

#### TC-030 Create teacher unique emp no
- **Steps:** POST `/api/staff/teachers` with new `employeeNo`
- **Expected:** 200; duplicate EMP001 fails
- **Pass/Fail:**

#### TC-031 Assign class teacher
- **Steps:** PUT class-teacher for section 5-A
- **Expected:** Teacher listed as class teacher; certificate review scoped to that section
- **Pass/Fail:**

### Academics

#### TC-040 Create class + section
- **Steps:** POST class, POST section with capacity
- **Expected:** Visible on `/academics/classes`; delete blocked if students enrolled
- **Pass/Fail:**

#### TC-041 Timetable clash
- **Steps:** Two entries same teacher overlapping time
- **Expected:** API validation error; no second row
- **Pass/Fail:**

#### TC-042 Grading scheme activate
- **Steps:** Create scheme, activate
- **Expected:** Only one ACTIVE per class
- **Pass/Fail:**

### Attendance

#### TC-050 Mark section
- **Steps:** As teacher, POST `/api/attendance/mark` PRESENT/ABSENT mix
- **Expected:** 200; GET section returns same statuses; unique per student/date
- **Pass/Fail:**

#### TC-051 Parent history
- **Steps:** Login `parent`; open attendance
- **Expected:** Child ADM0001 history only; no mark controls
- **Pass/Fail:**

### Fees

#### TC-060 Record payment
- **Steps:** As admin, POST `/api/fees/payments` CASH
- **Expected:** Receipt number; installment amount_paid increases
- **Pass/Fail:**

#### TC-061 Parent read
- **Steps:** Login parent; GET assignments/payments
- **Expected:** 200 own child; POST payment 403
- **Pass/Fail:**

### Notices and calendar

#### TC-070 Publish notice
- **Steps:** Create notice, POST publish; login as student GET published
- **Expected:** Student sees published, not draft
- **Pass/Fail:**

#### TC-080 Event visibility
- **Steps:** Create ROLE-scoped event; login other role
- **Expected:** Hidden when audience does not match
- **Pass/Fail:**

#### TC-081 Dashboard
- **Steps:** GET `/api/dashboard` as admin vs student
- **Expected:** 200 different widgets/KPIs; both need `DASHBOARD_VIEW`
- **Pass/Fail:**

#### TC-082 Notifications
- **Steps:** Trigger a notifying action; GET `/api/notifications`; PATCH read
- **Expected:** Unread count drops; other users’ inbox unchanged
- **Pass/Fail:**

### Exams

#### TC-090 Schedule overlap
- **Steps:** POST `/api/exam-schedules/validate` conflicting room
- **Expected:** Conflict reported; persist rejected
- **Pass/Fail:**

#### TC-091 Marks lock
- **Steps:** Save marks; PUT lock; teacher POST again
- **Expected:** Teacher blocked; admin can unlock
- **Pass/Fail:**

#### TC-092 Admit card unpublished
- **Steps:** Student GET admit card / export while unpublished
- **Expected:** Blocked or not printable
- **Pass/Fail:**

#### TC-093 Report card other student
- **Steps:** Student A requests student B’s report card
- **Expected:** 403
- **Pass/Fail:**

#### TC-094 Marksheet approve
- **Steps:** Teacher submit; admin approve + publish; student GET `/mine`
- **Expected:** Student sees published only; PDF export works
- **Pass/Fail:**

### Certificates

#### TC-100 Student request
- **Steps:** As `student`, POST `/api/certificates/requests` BONAFIDE
- **Expected:** Status SUBMITTED; appears in `/requests/mine`
- **Pass/Fail:**

#### TC-101 Duplicate pending
- **Steps:** Submit same type again
- **Expected:** 400 business error
- **Pass/Fail:**

#### TC-102 Class-teacher scope
- **Steps:** Teacher not class teacher of the section reviews request
- **Expected:** 403
- **Pass/Fail:**

#### TC-103 Admin approve
- **Steps:** POST approve as admin
- **Expected:** Issued certificate number; download PDF 200
- **Pass/Fail:**

#### TC-104 TC deactivates login
- **Steps:** Approve TC for a student with user account; login as that student
- **Expected:** 403 `auth.tc_deactivated` or inactive
- **Pass/Fail:**

### Presentation and security extras

#### TC-110 Locale Urdu RTL
- **Steps:** Switch language to Urdu
- **Expected:** `html[dir=rtl]`; labels from `ur.json`
- **Pass/Fail:**

#### TC-111 Theme persist
- **Steps:** Toggle dark theme, reload
- **Expected:** `html.theme-dark` remains (`schoolms.theme`)
- **Pass/Fail:**

#### TC-120 Student hits admin exam manage
- **Steps:** As student, POST `/api/exam-schedules`
- **Expected:** 403
- **Pass/Fail:**

#### TC-121 SQL injection on search
- **Steps:** Student search `%' OR 1=1 --`
- **Expected:** Parameterized query; no extra rows; 200 or empty
- **Pass/Fail:**

---

## 3. Coverage Boundaries

| Kind | Examples in this suite |
|---|---|
| Positive | TC-001, TC-020, TC-050, TC-060, TC-094, TC-100 |
| Negative / authz | TC-003, TC-023, TC-061, TC-093, TC-102, TC-120 |
| Validation / unique | TC-021, TC-030, TC-041, TC-101 |
| Security input | TC-121 |
| Publish locks | TC-092, TC-094 |
| Tenant | TC-010, TC-011 |

Automated counterparts exist for many certificate/exam/auth cases in `*ServiceTest`. UI cases (TC-005, TC-110, TC-111) are manual until E2E exists.
