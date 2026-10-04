# Test Strategy and Test Plan

**Product:** School Management System
**Related:** `TEST_CASES.md`, `docs/product/PRD.md`

---

## 1. Scope of Testing

### Included

| Type | What | How it is done today |
|---|---|---|
| Unit | Domain services | JUnit 5 under `backend/src/test/java` |
| Authorization logic | Permission / class-teacher scope | Service tests (certificates, marksheets, students) |
| Build verification | Angular production compile | `npm run build` |
| Manual / smoke | Login, nav, one happy path per module | Local or preview URL |
| Localization smoke | Switch en/hi/ur | Manual |
| Multi-tenant smoke | school_id on create; SUPER_ADMIN bypass | Implicit in services + RLS |

### Excluded / not automated yet

| Type | Status |
|---|---|
| Controller / MockMvc slice tests | None |
| True integration tests with PostgreSQL Testcontainers | None |
| End-to-end (Playwright/Cypress) | None |
| Performance / K6 | None |
| Dedicated security scan (DAST) | None |
| Accessibility (axe) | None |
| Frontend unit tests | Only `app.component.spec.ts`; `ng test` not a gate |

Do not invent CI jobs. Quality is currently developer-run.

---

## 2. Test Automation Stack

| Layer | Tool | Command | Trigger |
|---|---|---|---|
| Backend unit | JUnit 5, Mockito, `spring-boot-starter-test`, `spring-security-test` | `cd backend && mvn test` | Local, before PR |
| Frontend unit | Karma + Jasmine | `cd frontend && npm test` | Optional |
| Frontend build | Angular CLI | `cd frontend && npm run build` | After UI changes |
| API contract | springdoc | Manual `/swagger-ui.html` | When endpoints change |

There is no Jest, PyTest, Cypress, Playwright, or K6 in this repository.

### Existing backend test classes

| Class | Covers |
|---|---|
| `AuthServiceTest` | Login success/failure, inactive, TC-deactivated |
| `StudentServiceTest` | Create, duplicate admission, deactivate, enroll, profile edit rules |
| `AcademicsServiceTest` | Class/year/subject CRUD, capacity, conflicts |
| `TimetableServiceTest` | Teacher/room conflict |
| `GradingSchemeServiceTest` | Duplicate name, activate |
| `StaffServiceTest` | Teaching/non-teaching, class teacher |
| `DashboardServiceTest` | Aggregates |
| `ExamScheduleServiceTest` | Overlaps, validate |
| `ExamMarksServiceTest` | Totals, lock, import |
| `AdmitCardServiceTest` | Own vs other, unpublished export |
| `ReportCardServiceTest` | Publish visibility, export |
| `MarksheetServiceTest` | Submit/approve, unpublished redaction |
| `SchoolEventServiceTest` | Visibility, export |
| `NotificationServiceTest` | Inbox isolation, mark read |
| `CertificateServiceTest` | Generate, numbers, TC |
| `CertificateRequestServiceTest` | Submit, duplicate, review scope, approve/reject |
| `CertificateLifecycleServiceTest` | TC deactivation |

Helper: `com.schoolms.TestSecurity`.

---

## 3. Test Data Setup

| Source | Use |
|---|---|
| Flyway V3 + later seeds | Demo school `DEMO`, users `superadmin`/`admin`/`teacher`/`parent`/`student`, password `Admin@123` |
| Students ADM0001–ADM0006 | ADM0001 linked to user `student` (V21) |
| Teacher EMP001 | Asha Sharma, class teacher 5-A |
| Non-teaching NTS001–NTS005 | No logins |
| Unit tests | In-memory mocks / fixtures inside test classes — not the live DB |
| Third-party APIs | None to mock |
| Production copies | Do not load production student data into laptops. If needed, sanitize names and IDs first |

New tests should not depend on live MinIO.

---

## 4. Quality Exit Criteria

**Current (realistic for this repo)**

- `mvn test` green for touched backend modules
- `npm run build` green for frontend changes
- No new 500s on smoke login + one module path
- RLS not disabled to make a test pass
- New permissions seeded and documented

**Aspirational (not enforced)**

- Greater than 80% backend line coverage
- 0 open critical defects on the issue
- 100% of Must-Have user stories in `USER_STORIES_SCOPE.md` have an RTM row in `TEST_CASES.md`
- E2E suite on login, attendance save, fee payment, marksheet publish, certificate request

A release is **not** blocked on Playwright or coverage tooling until those exist.
