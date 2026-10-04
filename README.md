# School Management System

Multi-tenant school operations platform: students, staff, academics, attendance, fees, exams, certificates, notices, and calendar.

**Stack:** Spring Boot 3.5 / Java 21 / PostgreSQL 15 / Spring Security JWT / Caffeine / MinIO / JasperReports / Angular 19 + Material M3 + ngx-translate / Flyway / Docker Compose.

Roles in code: `SUPER_ADMIN`, `ADMIN`, `TEACHER`, `PARENT`, `STUDENT`. There is no `PRINCIPAL` role.

---

## Documentation

| Topic | Path |
|---|---|
| Business / product | `docs/product/BRD.md`, `docs/product/PRD.md`, `docs/product/USER_STORIES_SCOPE.md` |
| Architecture | `docs/architecture/ARCHITECTURE.md`, `docs/architecture/DATABASE_SCHEMA.md` |
| API | `docs/api/API_SPECIFICATION.md` (live: `/swagger-ui.html`) |
| Contributing | `CONTRIBUTING.md` |
| Deploy / runbook | `docs/devops/DEPLOYMENT.md`, `docs/devops/RUNBOOK.md` |
| Security / RBAC | `docs/security/SECURITY.md` |
| QA | `docs/qa/TEST_PLAN.md`, `docs/qa/TEST_CASES.md` |
| End users | `docs/user-guides/USER_MANUAL.md`, `docs/user-guides/ADMIN_GUIDE.md` |
| Changelog | `CHANGELOG.md` |
| Backend setup | `docs/README-BACKEND.md` |
| Frontend setup | `docs/README-FRONTEND.md` |
| Docker | `docs/DOCKER-SETUP.md` |
| UI flow (roles) | `docs/UI-FLOW.md` |

---

## 1. Prerequisites and tooling

| Software | Version | Purpose |
|---|---|---|
| Git | latest | Clone |
| JDK | 21 LTS | Backend |
| Maven | 3.9+ | Backend build |
| Node.js | 20+ (npm 10+) | Frontend |
| PostgreSQL | 15 | Database (if not using Compose) |
| Docker Desktop / Docker Engine + Compose v2 | latest | Optional all-in-one |
| MinIO | latest | Optional files/PDFs |

Angular CLI global install is not required (`npx` / local `node_modules`).

---

## 2. Environment setup

```bash
git clone https://github.com/92xeeshan/School-Management-System.git
cd School-Management-System
cp .env.example .env
```

`.env.example` keys: `POSTGRES_*`, `MINIO_ROOT_*`, `JWT_SECRET`. Defaults are fine for local demo. Change secrets before any real data.

### PostgreSQL roles (non-Docker)

```sql
CREATE ROLE schoolms LOGIN PASSWORD 'schoolms';
CREATE ROLE app_rls  LOGIN PASSWORD 'app_rls';
CREATE ROLE app_admin LOGIN PASSWORD 'app_admin';
CREATE DATABASE schoolms OWNER schoolms;
```

Flyway (user `schoolms`) creates tables, RLS, grants, and demo data on first backend start.

Point at an external database with `POSTGRES_HOST` / `POSTGRES_PORT` / `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` or `SPRING_DATASOURCE_URL`. Optional: `scripts/apply-db.sh`.

---

## 3. Local run and build

### Docker Compose (recommended)

```bash
docker compose up --build
```

| Service | URL |
|---|---|
| Frontend | http://localhost:4200 |
| Backend | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| MinIO console | http://localhost:9001 (`minioadmin` / `minioadmin`) |

The frontend nginx proxies `/api/` to the backend. Stop with Ctrl+C or `docker compose down`.

### Without Docker

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

```bash
cd frontend
npm install
npm start
```

Dev server proxies `/api` to `http://localhost:8080` (`frontend/src/proxy.conf.json`).

Together: `scripts/run-linux.sh` or `scripts/run-windows.bat`.

### Production-like build

```bash
cd backend && mvn -DskipTests package
java -jar target/schoolms-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

```bash
cd frontend && npm run build
```

Output: `frontend/dist/frontend/browser`. Prod profile **requires** `JWT_SECRET` (and MinIO keys if used).

---

## 4. Testing

```bash
cd backend && mvn test
cd frontend && npm run build
cd frontend && npm test
```

`mvn test` is the meaningful automated gate (service unit tests). Frontend Karma tests are minimal. See `docs/qa/TEST_PLAN.md`.

There is no `npm run lint` script.

---

## 5. Demo accounts

Password for every seeded account: `Admin@123`.

| Username | Role |
|---|---|
| `superadmin` | Super Admin (all schools, RLS bypass) |
| `admin` | School Admin |
| `teacher` | Teacher (Asha Sharma, EMP001, Class 5-A) |
| `parent` | Parent of ADM0001 |
| `student` | Student Aarav Kumar, ADM0001 |

---

## 6. Modules

- Auth (JWT + refresh), RBAC permission codes, PostgreSQL RLS multi-tenancy
- Students, guardians, enrollment, staff
- Academics, timetable, grading schemes
- Attendance, fees, notices, calendar
- Exams, admit cards, report cards, marksheets (publish lock)
- Certificates (generate, request, class-teacher review, admin approve)
- Dashboards, in-app notifications
- Light/dark Material M3 UI; en / hi / ur

---

## 7. Troubleshooting FAQ

| Problem | Fix |
|---|---|
| Flyway `role "app_rls" does not exist` | Create roles in section 2 |
| `Connection refused` port 5432 | Start PostgreSQL; check `POSTGRES_HOST` |
| Frontend login fails | Backend must be on 8080; wait for Flyway; check `/actuator/health` |
| 403 on API from the browser | Add the UI origin to `CORS_ALLOWED_ORIGINS` |
| API 404 from the UI | Proxy `/api` not reaching backend (`proxy.conf.json` / nginx) |
| Port 4200 or 8080 busy | Stop the other process or change Compose `ports` |
| 401 after idle | Refresh token expired (30 days) or revoked; log in again |
| JWT errors in prod | Set `JWT_SECRET` (32+ characters) |
| PDFs/uploads fail | Start MinIO; set `MINIO_ENDPOINT` |

More: `docs/README-BACKEND.md`, `docs/README-FRONTEND.md`, `docs/devops/RUNBOOK.md`.
