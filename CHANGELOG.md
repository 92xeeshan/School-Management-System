# Changelog

All notable changes to School Management System are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
The backend artifact version is `0.1.0-SNAPSHOT` (`backend/pom.xml`). Dates use the commit timeline on `main` / feature branches.

---

## [Unreleased]

### Added

- Documentation suite under `docs/` (product, architecture, API, devops, security, QA, user guides).
- Root `CONTRIBUTING.md`.

---

## [0.1.0-SNAPSHOT] — 2026-10-04

Snapshot of the application as of documentation issue #63, including work landed on feature branches (UI modernization #36, certificates #18/#73, and prior modules).

### Added

- Multi-tenant SchoolMS: Spring Boot 3.5 / Java 21 / PostgreSQL 15 / Flyway / Angular 19.
- JWT access tokens and rotating refresh tokens; permission-based RBAC; PostgreSQL RLS.
- i18n English, Hindi, Urdu (RTL); ngx-translate + MessageSource.
- Student information system, guardians, enrollment, CSV import.
- Staff management (teaching, class teacher, non-teaching).
- Academics: classes, sections, subjects, timetable, grading schemes.
- Attendance marking and history.
- Fees: structures, assignments, payments, PDF receipts.
- Notices with publish/archive and read receipts.
- Role dashboards with metrics, charts, and calendar widgets.
- Academic calendar with role/class-scoped events and holiday PDF export.
- Examinations: schedules with overlap validation, marks grid, lock, CSV/Excel import.
- Admit cards, report cards, marksheets with publish lock and PDF/ZIP export.
- Marksheet submit / approve / reject workflow.
- Certificates: templates, generate TC/bonafide/character/course-completion, student/parent request queue, class-teacher review, admin approval.
- In-app notifications with header bell.
- Angular Material M3 theme, light/dark mode, collapsible sidenav, shared page header and empty states.
- Docker Compose stack (PostgreSQL, MinIO, backend, frontend nginx `/api/` proxy).
- OpenAPI / Swagger UI.
- Demo seed users (`admin`, `teacher`, `parent`, `student`, `superadmin`) password `Admin@123`.

### Changed

- Permission cache moved from Redis to Caffeine.
- Frontend talks to the API only via relative `/api` (dev proxy + nginx).
- Student grid actions use icon buttons with tooltips.
- Report cards open in an on-page modal.

### Fixed

- ngx-translate JSON loaded from `/assets/i18n` before first render.
- Class-scoped calendar events seeded from ADM0001 enrollment.
- OnPush UI refresh after Caffeine change.
- External PostgreSQL via `POSTGRES_*` / `SPRING_DATASOURCE_*`.

### Deprecated

- Academics Syllabus and Terms tabs (placeholders).
- Examinations Analytics and Re-evaluation tabs (placeholders).
- `db/01_create_tables.sql` snapshots versus Flyway (Flyway is source of truth).

### Security

- BCrypt passwords; SHA-256 refresh tokens; HMAC JWT.
- RLS tenant isolation; SUPER_ADMIN explicit bypass.
- CORS allowlist; prod profile requires `JWT_SECRET`.
- TC issuance can deactivate student login (`auth.tc_deactivated`).

---

## Release metadata template

When cutting a numbered release, add a section:

```
## [X.Y.Z] — YYYY-MM-DD

### Added
### Changed
### Fixed
### Deprecated
### Security
```
