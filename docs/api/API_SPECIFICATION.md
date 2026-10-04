# API Specification

**Base path:** `/api`
**Live OpenAPI:** `GET /v3/api-docs` (Swagger UI: `/swagger-ui.html`)
**Style:** REST JSON unless noted (PDF/ZIP are `application/octet-stream` or `application/pdf`)
**Auth:** Bearer JWT on all routes except login, refresh, Swagger, and `GET /actuator/health`

This document is the inventory of **implemented** Spring controllers. For request/response schemas generated from code, use springdoc.

---

## 1. Authentication and Authorization

### Headers

| Header | Required | Description |
|---|---|---|
| `Authorization` | Yes except public routes | `Bearer <accessToken>` |
| `Content-Type` | JSON writes | `application/json` |
| `Accept-Language` | Optional | `en`, `hi`, `ur` for API messages |
| `Accept` | Optional | `application/json` or PDF types on exports |

### Token exchange

1. `POST /api/auth/login` with `{ "username", "password" }` (username **or** email).
2. Store `data.accessToken` (JWT, 15 minutes) and `data.refreshToken` (opaque hex, 30 days).
3. Send the access token on subsequent calls.
4. On 401, `POST /api/auth/refresh` with `{ "refreshToken" }`. Old refresh token is revoked (rotation).
5. Logout: `POST /api/auth/logout?refreshToken=` (authenticated).

JWT issuer `schoolms`. Claims: `sub` (user UUID), `schoolId`, `roles`, `locale`, `iat`, `exp`. HMAC-SHA with `JWT_SECRET`.

Authorities are **permission codes** (`hasAuthority('STUDENT_READ')`), not `ROLE_*`.

Public matchers: `/api/auth/login`, `/api/auth/refresh`, `/swagger-ui/**`, `/v3/api-docs/**`, `/swagger-ui.html`, `/actuator/health`, `/error`.

---

## 2. Global Response Formats and Errors

### Success envelope

Most JSON endpoints:

```json
{
  "success": true,
  "message": "optional i18n key or text",
  "data": {}
}
```

Lists may use `data` as an array or a paged object (`PagedResponse`).

### Error envelope (`ApiErrorResponse`)

```json
{
  "timestamp": "2026-10-04T12:00:00Z",
  "status": 400,
  "code": "validation.failed",
  "message": "localized text",
  "fieldErrors": [
    { "field": "username", "code": "NotBlank", "message": "must not be blank" }
  ]
}
```

| HTTP | Typical `code` | When |
|---|---|---|
| 400 | `validation.failed`, `error.bad_request`, business codes | Bean validation / `BusinessException` |
| 401 | `auth.unauthorized`, `auth.bad_credentials` | Missing/invalid JWT or login |
| 403 | `auth.access_denied`, `auth.user_inactive`, `auth.tc_deactivated` | Permission or inactive |
| 404 | `resource.not_found`, `error.not_found` | Missing entity |
| 405 | `error.method_not_allowed` | Wrong method |
| 409 | `error.conflict` | Unique constraint |
| 500 | `error.internal` | Unhandled |

Binary export errors still return this JSON with the corresponding status.

---

## 3. Endpoint Reference Inventory

Examples use `http://localhost:8080`. Replace tokens. Paths are relative to the host; they include `/api`.

### 3.1 Auth — `/api/auth`

#### POST `/api/auth/login`

Request:

```json
{ "username": "admin", "password": "Admin@123" }
```

200 `data`:

```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<hex>",
  "expiresInSeconds": 900,
  "user": {
    "id": "uuid",
    "schoolId": "uuid",
    "username": "admin",
    "roles": ["ADMIN"],
    "permissions": ["STUDENT_READ"],
    "locale": "en"
  }
}
```

400/401/403 as in error table.

#### POST `/api/auth/refresh`

Request: `{ "refreshToken": "<hex>" }`. 200 same as login. 401 invalid/expired refresh.

#### POST `/api/auth/logout?refreshToken=<hex>`

Requires Bearer. 200 `{ "success": true, "message": "auth.logout_success" }`.

#### GET `/api/auth/me`

200 user profile (names/email filled). 401 without token.

---

### 3.2 Users — `/api/users`

| Method | Path | Permission | Notes |
|---|---|---|---|
| GET | `/api/users` | `USER_READ` | School users |
| GET | `/api/users/{id}` | `USER_READ` | |
| POST | `/api/users` | `USER_CREATE` | Create + roles |
| PATCH | `/api/users/{id}` | `USER_UPDATE` | |
| POST | `/api/users/{id}/roles` | `ROLE_ASSIGN` | |
| DELETE | `/api/users/{id}/roles` | `ROLE_ASSIGN` | |
| PUT | `/api/users/me/locale` | authenticated | `{ "locale": "hi" }` |

---

### 3.3 Schools — `/api/schools`

| Method | Path | Permission |
|---|---|---|
| GET | `/api/schools` | `SCHOOL_READ` |
| GET | `/api/schools/current` | authenticated |
| GET | `/api/schools/{id}` | `SCHOOL_READ` |
| POST | `/api/schools` | `SCHOOL_MANAGE` |
| PATCH | `/api/schools/{id}` | `SCHOOL_MANAGE` |

---

### 3.4 Students — `/api/students`

| Method | Path | Permission | Purpose |
|---|---|---|---|
| GET | `/api/students` | `STUDENT_READ` | Paginated search |
| GET | `/api/students/by-section` | `STUDENT_READ` | Query section |
| GET | `/api/students/{id}` | `STUDENT_READ` | |
| GET | `/api/students/{id}/profile` | `STUDENT_READ` | Full profile |
| PUT | `/api/students/{id}/profile` | `STUDENT_UPDATE` or `STUDENT_READ` | Class-teacher rules in service |
| GET | `/api/students/{id}/guardians` | `STUDENT_READ` | |
| POST | `/api/students` | `STUDENT_CREATE` | |
| PUT | `/api/students/{id}` | `STUDENT_UPDATE` | |
| POST | `/api/students/{id}/enroll` | `STUDENT_UPDATE` | |
| POST | `/api/students/{id}/guardians` | `GUARDIAN_CREATE` | Link existing |
| POST | `/api/students/{id}/guardians/new` | `GUARDIAN_CREATE` | Create + link |
| DELETE | `/api/students/{id}/guardians/{guardianId}` | `GUARDIAN_UPDATE` | Unlink |
| PATCH | `/api/students/{id}/deactivate` | `STUDENT_DELETE` | |
| POST | `/api/students/import` | `STUDENT_IMPORT` | CSV multipart |

Guardians collection: `GET/POST /api/guardians`, `PATCH /api/guardians/{id}` (`GUARDIAN_READ/CREATE/UPDATE`).

200 example (get student `data` shape, abbreviated):

```json
{
  "id": "uuid",
  "admissionNo": "ADM0001",
  "firstName": "Aarav",
  "lastName": "Kumar",
  "status": "ACTIVE"
}
```

400 validation, 404 missing, 409 duplicate admission number.

---

### 3.5 Academics

| Method | Path | Permission |
|---|---|---|
| GET/POST | `/api/academic-years` | `CLASS_READ` / `CLASS_CREATE` |
| GET/POST | `/api/classes` | `CLASS_READ` / `CLASS_CREATE` |
| PUT/DELETE | `/api/classes/{id}` | `CLASS_UPDATE` |
| GET/POST | `/api/sections` | `SECTION_READ` / `SECTION_CREATE` |
| DELETE | `/api/sections/{id}` | `CLASS_UPDATE` or `SECTION_UPDATE` |
| GET/POST | `/api/subjects` | `SUBJECT_READ` / `SUBJECT_CREATE` |
| PUT | `/api/subjects/{id}` | `SUBJECT_UPDATE` |
| GET/POST | `/api/teachers` | `CLASS_READ` / `CLASS_CREATE` |
| GET | `/api/teachers/{id}` | `SECTION_READ` |
| GET | `/api/timetable` | `TIMETABLE_READ` or `CLASS_READ` or `SECTION_READ` |
| POST/PUT/DELETE | `/api/timetable`, `/api/timetable/{id}` | `TIMETABLE_MANAGE` |
| GET | `/api/grading-schemes` | `EXAM_READ` or `CLASS_READ` |
| POST/PUT | `/api/grading-schemes`, `/{id}` | `EXAM_MANAGE` |
| PUT | `/api/grading-schemes/{id}/activate` | `EXAM_MANAGE` |
| PUT | `/api/grading-schemes/{id}/deactivate` | `EXAM_MANAGE` |

---

### 3.6 Attendance — `/api/attendance`

| Method | Path | Permission |
|---|---|---|
| GET | `/api/attendance/sessions` | `ATTENDANCE_READ` |
| POST | `/api/attendance/mark` | `ATTENDANCE_MARK` |
| GET | `/api/attendance/section` | `ATTENDANCE_READ` |
| GET | `/api/attendance/students/{studentId}` | `ATTENDANCE_READ` or `STUDENT_READ` |
| GET | `/api/attendance/students/{studentId}/summary` | same |

Mark body (conceptual): section, date, list of `{ studentId, status }` where status is `PRESENT|ABSENT|LATE|LEAVE`.

---

### 3.7 Fees — `/api/fees`

| Method | Path | Permission |
|---|---|---|
| GET/POST | `/api/fees/categories` | `FEE_READ` / `FEE_STRUCTURE_MANAGE` |
| GET/POST | `/api/fees/structures` | `FEE_READ` / `FEE_STRUCTURE_MANAGE` |
| POST | `/api/fees/assignments` | `FEE_STRUCTURE_MANAGE` |
| GET | `/api/fees/students/{studentId}/assignments` | `FEE_READ` or `STUDENT_READ` |
| GET | `/api/fees/assignments/{assignmentId}/installments` | same |
| POST | `/api/fees/payments` | `FEE_PAYMENT_RECORD` |
| GET | `/api/fees/students/{studentId}/payments` | `FEE_READ` or `FEE_RECEIPT_VIEW` or `STUDENT_READ` |
| GET | `/api/fees/payments/{id}` | same |

Payment methods: `CASH`, `CARD`, `UPI`, `BANK_TRANSFER`.

---

### 3.8 Notices — `/api/notices`

| Method | Path | Auth |
|---|---|---|
| GET | `/api/notices` | `NOTICE_READ` or `STUDENT_READ` |
| GET | `/api/notices/published` | authenticated |
| GET | `/api/notices/unread-count` | authenticated |
| GET | `/api/notices/{id}` | authenticated (marks read) |
| POST | `/api/notices` | `NOTICE_CREATE` |
| POST | `/api/notices/{id}/publish` | `NOTICE_PUBLISH` |
| POST | `/api/notices/{id}/archive` | `NOTICE_PUBLISH` or `NOTICE_DELETE` |
| POST | `/api/notices/{id}/read` | authenticated |

---

### 3.9 Dashboard, events, notifications

| Method | Path | Permission |
|---|---|---|
| GET | `/api/dashboard` | `DASHBOARD_VIEW` |
| GET | `/api/events` | `EVENT_READ` |
| GET | `/api/events/upcoming` | `EVENT_READ` |
| GET | `/api/events/options` | `EVENT_READ` |
| GET | `/api/events/export` | `EVENT_MANAGE` (holiday PDF) |
| POST | `/api/events` | `EVENT_MANAGE` |
| PUT | `/api/events/{id}` | `EVENT_MANAGE` |
| DELETE | `/api/events/{id}` | `EVENT_MANAGE` |
| GET | `/api/notifications` | authenticated |
| GET | `/api/notifications/unread-count` | authenticated |
| PATCH | `/api/notifications/{id}/read` | authenticated |
| PATCH | `/api/notifications/read-all` | authenticated |

---

### 3.10 Staff — `/api/staff`

| Method | Path | Permission |
|---|---|---|
| GET/POST | `/api/staff/teachers` | `STAFF_READ` / `STAFF_CREATE` |
| PUT | `/api/staff/teachers/{id}` | `STAFF_UPDATE` |
| PUT | `/api/staff/teachers/{id}/class-teacher` | `STAFF_UPDATE` |
| PATCH | `/api/staff/teachers/{id}/deactivate` | `STAFF_DELETE` |
| GET/POST | `/api/staff/non-teaching` | `STAFF_READ` / `STAFF_CREATE` |
| PUT | `/api/staff/non-teaching/{id}` | `STAFF_UPDATE` |
| PATCH | `/api/staff/non-teaching/{id}/deactivate` | `STAFF_DELETE` |
| GET | `/api/staff/sections` | `STAFF_READ` |

---

### 3.11 Examinations

**Schedules** `/api/exam-schedules` — all `EXAM_MANAGE`: GET `/options`, GET `/`, POST `/validate`, POST `/`, PUT `/{id}`, PATCH `/{id}/status`, DELETE `/{id}`.

**Marks** `/api/exam-marks`

| Method | Path | Permission |
|---|---|---|
| GET | `/options`, `/`, `/template` | `EXAM_READ` or `EXAM_MARK` or `EXAM_MANAGE` |
| POST | `/` | `EXAM_MARK` or `EXAM_MANAGE` |
| PUT | `/lock` | `EXAM_MANAGE` |
| POST | `/import` | `EXAM_MARK` or `EXAM_MANAGE` (multipart) |

**Admit cards** `/api/admit-cards` — all `ADMIT_CARD_READ`: GET `/options`, `/students`, `/mine`, `/{studentId}`; POST `/export/pdf`, `/export/zip`.

**Report cards** `/api/report-cards` — all `REPORT_CARD_READ`: same pattern as admit cards.

**Marksheets** `/api/marksheets`

| Method | Path | Permission |
|---|---|---|
| GET | `/options`, `/students`, `/mine`, `/{studentId}` | `MARKSHEET_READ` |
| POST | `/submit` | `EXAM_MARK` or `MARKSHEET_MANAGE` |
| POST | `/approve`, `/reject` | `MARKSHEET_MANAGE` |
| PUT | `/publish` | `MARKSHEET_MANAGE` |
| POST | `/export/pdf`, `/export/zip` | `MARKSHEET_READ` |

PDF/ZIP: 200 file body with `Content-Disposition`. 400 if unpublished when export requires publish. 401/403 as usual. 500 on report engine failure.

---

### 3.12 Certificates — `/api/certificates`

| Method | Path | Permission | Purpose |
|---|---|---|---|
| GET/POST | `/templates` | READ/GENERATE/MANAGE vs `CERTIFICATE_MANAGE` | Templates |
| PUT | `/templates/{id}` | `CERTIFICATE_MANAGE` | |
| POST | `/generate` | `CERTIFICATE_GENERATE` | Staff generate |
| POST | `/requests` | `CERTIFICATE_READ` | JSON or multipart |
| GET | `/requests/mine` | `CERTIFICATE_READ` | |
| GET | `/requests` | READ or GENERATE or APPROVE | Staff queue |
| POST | `/requests/{id}/review` | `CERTIFICATE_GENERATE` | Class-teacher forward |
| POST | `/requests/{id}/cancel` | READ or GENERATE | |
| POST | `/requests/{id}/approve` | `CERTIFICATE_APPROVE` | |
| POST | `/requests/{id}/reject` | `CERTIFICATE_APPROVE` | |
| GET | `/` | `CERTIFICATE_READ` | Issued register |
| GET | `/mine` | `CERTIFICATE_READ` | |
| GET | `/student/{studentId}` | `CERTIFICATE_READ` | |
| POST | `/{id}/approve` | `CERTIFICATE_APPROVE` | Approve draft |
| GET | `/{id}/download` | `CERTIFICATE_READ` | PDF |

Types: `TC`, `BONAFIDE`, `CHARACTER`, `COURSE_COMPLETION`.

Request example:

```json
{
  "certificateType": "BONAFIDE",
  "reason": "Bank account opening"
}
```

200 `data` includes status (`SUBMITTED`, `TEACHER_REVIEWED`, approved/rejected) and flags `canReview`, `canApprove`, `canReject`, `canCancel`, `canDownload`. 400 duplicate pending request. 403 out-of-section teacher.

---

## 4. Status code cheat sheet per call

For every JSON endpoint:

- **200** — success envelope or file
- **400** — validation / business rule (`code` explains)
- **401** — missing/expired JWT (or bad login)
- **403** — authenticated but missing permission or inactive
- **404** — unknown id
- **409** — unique constraint
- **500** — unhandled

Public login additionally returns **401** `auth.bad_credentials` and **403** inactive/TC.

---

## 5. Integration notes

- Frontend always uses relative `/api/...` (dev proxy and nginx keep the prefix).
- CORS allowlist: `CORS_ALLOWED_ORIGINS` (defaults include `http://localhost:4200` and `https://*.monkeycode-ai.live`). Credentials allowed. Exposed header: `Content-Disposition`.
- No GraphQL. No webhooks. No third-party OAuth in this version.
