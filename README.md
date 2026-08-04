# School Management System

Multi-tenant (RLS) school management system.

**Stack:** Spring Boot 3 / Java 21 / PostgreSQL 15 / Spring Security JWT / Redis / MinIO / JasperReports / Angular 18 + Material + ngx-translate / Flyway / Docker Compose.

## Modules

- Auth & user management (JWT + refresh tokens, RBAC with extensible role/permission model)
- Multi-language UI (English, Hindi, Urdu with RTL) — switchable at runtime, stored per user
- Student information system (profiles, guardians, class/section assignment, bulk CSV import)
- Academics (class, section, subject, timetable)
- Daily attendance with reports
- Fees (structure per class/category, manual payments, PDF receipts)
- Notices with role-based visibility
- Role-specific dashboards (admin, teacher, parent, student)

## Quick start (local dev with Docker Compose)

```bash
cp .env.example .env
docker compose up --build
```

| Service  | URL                          |
|----------|------------------------------|
| Frontend | http://localhost:4200        |
| Backend  | http://localhost:8080        |
| Swagger  | http://localhost:8080/swagger-ui.html |
| MinIO    | http://localhost:9001        |

## Running without Docker

Requirements: JDK 21, Maven 3.9+, Node 20+, PostgreSQL 15, Redis 7.

```bash
# database
createdb schoolms

# backend (dev profile)
cd backend && ./mvnw spring-boot:run

# frontend
cd frontend && npm install && npm start
```

Environment variables are provided via `application-dev.yml`/`application-prod.yml`; no secrets are hardcoded.

## Default accounts

Seeded by Flyway (`V3__seed_super_admin.sql`); credentials in `application-dev.yml`.

## Architecture notes

- Shared-DB multi-tenancy with a `school_id` discriminator on every tenant table and PostgreSQL Row-Level Security policies enforced at the database layer.
- JWT access token (short-lived) + DB-backed refresh token. Authorities are resolved from the `role_permission` table and cached in Redis.
- Backend and frontend messages are localized (Spring MessageSource / ngx-translate) for en/hi/ur.
