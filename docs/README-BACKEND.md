# School Management System - Backend (Spring Boot)

REST API for the School Management System: JWT auth + refresh tokens, RBAC,
multi-tenant data isolation via PostgreSQL Row-Level Security, attendance,
fees, notices and academics modules.

**Stack:** Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA,
PostgreSQL 15, Redis 7, Flyway, MinIO (optional), JasperReports, OpenAPI/Swagger.

---

## 1. Required software to install locally

| Software        | Version   | Purpose                                  | Download                                            |
|-----------------|-----------|------------------------------------------|-----------------------------------------------------|
| JDK             | 21 (LTS)  | Run/build the Spring Boot app            | https://adoptium.net (Temurin 21)                   |
| Maven           | 3.9+      | Build tool                               | https://maven.apache.org/download.cgi               |
| PostgreSQL      | 15        | Main database                            | https://www.postgresql.org/download/                |
| Redis           | 7         | Permission cache, session support        | https://redis.io/download                           |
| MinIO           | Latest    | File storage (optional, fees/notices PDFs and attachments) | https://min.io/download                    |
| Git             | Latest    | Clone the repository                     | https://git-scm.com/downloads                       |

> MinIO is optional for basic login/testing. Skip it if you do not need file
> upload, PDF receipts or notice attachments.

---

## 2. Required settings on your local system

The backend never hardcodes secrets. Everything is configured through
environment variables or defaults in `src/main/resources/application*.yml`.

### 2.1 PostgreSQL (Windows and Linux)

Start PostgreSQL, then create the three roles and one database exactly as
shown. Open a `psql` session as a superuser (usually `postgres`):

```sql
CREATE ROLE schoolms LOGIN PASSWORD 'schoolms';
CREATE ROLE app_rls  LOGIN PASSWORD 'app_rls';
CREATE ROLE app_admin LOGIN PASSWORD 'app_admin';

CREATE DATABASE schoolms OWNER schoolms;
```

What each role does:

| Role       | Used by                                                       |
|------------|---------------------------------------------------------------|
| `schoolms` | Flyway migrations (schema owner, creates tables/grants/RLS)   |
| `app_rls`  | The running application datasource (tenant-isolated by RLS)   |
| `app_admin`| Debugging / manual DBA work with RLS disabled                 |

On **Windows** you can run the same SQL from `psql` (add
`C:\Program Files\PostgreSQL\15\bin` to `PATH`) or use pgAdmin's query tool.

> No other setup is needed: Flyway creates every table, indexes, RLS
> policies, grants and demo seed data automatically on first startup.
>
> To use an external database (M2), set `POSTGRES_HOST`, `POSTGRES_PORT`,
> `POSTGRES_DB`, `POSTGRES_USER` and `POSTGRES_PASSWORD` in `.env` (see
> `.env.example`). Optional overrides: `SPRING_DATASOURCE_URL` and
> `SPRING_FLYWAY_URL`. Then run `scripts/apply-db.sh` or start the backend
> so Flyway migrates.

### 2.2 Redis (Windows and Linux)

Start Redis on the default port `6379` with no password:

- **Linux/macOS:** `redis-server` (or the distro service)
- **Windows:** use the official Memurai/Redis Windows build, or run Redis
  inside the Docker container described in `docs/DOCKER-SETUP.md`.

### 2.3 MinIO (optional, Windows and Linux)

Start MinIO with access key / secret `minioadmin` / `minioadmin` (the app
defaults). The backend creates the bucket automatically on startup. If you
change the credentials, set `MINIO_ACCESS_KEY` and `MINIO_SECRET_KEY`.

### 2.4 Environment variables

All variables have sensible local defaults, so the backend runs out of the
box if you created the database/roles exactly as in 2.1. Set these to
override the defaults:

| Variable                       | Default                              | Description                          |
|--------------------------------|--------------------------------------|--------------------------------------|
| `SPRING_DATASOURCE_URL`        | `jdbc:postgresql://localhost:5432/schoolms` | JDBC URL (set this to the M2 host to use M2 DB) |
| `SPRING_DATASOURCE_USERNAME`   | `app_rls`                            | Runtime DB role                     |
| `SPRING_DATASOURCE_PASSWORD`   | `app_rls`                            | Runtime DB password                 |
| `SPRING_FLYWAY_URL`            | same as datasource URL               | Flyway JDBC URL                     |
| `SPRING_FLYWAY_USER`           | `schoolms`                           | Migration DB role                   |
| `SPRING_FLYWAY_PASSWORD`       | `schoolms`                           | Migration DB password               |
| `POSTGRES_HOST`                | `localhost`                          | Host used by `db/` SQL scripts      |
| `POSTGRES_PORT`                | `5432`                               | Port used by `db/` SQL scripts      |
| `POSTGRES_DB`                  | `schoolms`                           | Database name                       |
| `POSTGRES_USER`                | `schoolms`                           | Schema-owner role for SQL scripts   |
| `POSTGRES_PASSWORD`            | `schoolms`                           | Schema-owner password               |
| `SPRING_DATA_REDIS_HOST`       | `localhost`                          | Redis host                          |
| `JWT_SECRET`                   | dev-only placeholder (see yml)       | JWT signing secret (32+ chars)      |
| `MINIO_ENDPOINT`               | `http://localhost:9000`              | MinIO endpoint                      |
| `CORS_ALLOWED_ORIGINS`         | `http://localhost:4200,...`          | Comma-separated allowed origins     |

How to set variables:

- **Windows (PowerShell):** `setx JWT_SECRET "your-long-secret"`
- **Windows (CMD):** `set JWT_SECRET=your-long-secret` (current session)
- **Linux/macOS:** `export JWT_SECRET="your-long-secret"`

---

## 3. How to run the application

### 3.1 Prerequisites ready?

- PostgreSQL is running and the roles/database from section 2.1 exist.
- Redis is running on `localhost:6379`.

### 3.2 Run in dev mode (recommended)

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Wait for the log line `Started SchoolManagementApplication`. Flyway applies
the schema migrations and seeds demo data on first startup.

### 3.3 Build and run a JAR

```bash
cd backend
mvn -DskipTests package
java -jar target/schoolms-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

### 3.4 One-click script (Windows / Linux)

Use the provided launcher that starts the backend and the frontend together:

- Linux/macOS: `./scripts/run-linux.sh`
- Windows: double-click `scripts\run-windows.bat`

### 3.5 Verify it is running

| Check                        | Command                                              | Expected              |
|------------------------------|------------------------------------------------------|-----------------------|
| Health                       | `curl http://localhost:8080/actuator/health`         | `{"status":"UP"}`     |
| Swagger UI                   | open http://localhost:8080/swagger-ui.html in a browser | OpenAPI page      |
| Login                        | `curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"Admin@123"}'` | JSON with `accessToken` |

### 3.6 Default demo accounts

Seeded by Flyway. Password for every account is `Admin@123`:

| Username     | Role        |
|--------------|-------------|
| `superadmin` | Super Admin |
| `admin`      | Admin       |
| `teacher`    | Teacher     |
| `parent`     | Parent      |
| `student`    | Student     |

### 3.7 Useful endpoints

| Endpoint                          | Description                     |
|-----------------------------------|---------------------------------|
| `/api/auth/login`                 | Login (returns access + refresh)|
| `/api/auth/refresh`               | Refresh an expired access token |
| `/api/dashboard`                  | Role dashboard summary          |
| `/api/students`                   | Students CRUD                   |
| `/api/classes` `/api/sections` `/api/subjects` `/api/timetable` | Academics APIs |
| `/api/attendance`                 | Attendance APIs                 |
| `/api/fees`                       | Fees APIs                       |
| `/api/notices`                    | Notices APIs                    |
| `/api/schools`                    | Schools (super admin)           |
| `/api/users`                      | Users (admin)                   |

---

## 4. Troubleshooting

| Problem                          | Solution                                            |
|----------------------------------|-----------------------------------------------------|
| Flyway migration fails with `role "app_rls" does not exist` | Create the roles from section 2.1 |
| `Connection refused ... 5432`    | PostgreSQL is not running / wrong port              |
| `Connection refused ... 6379`    | Redis is not running                                |
| Login returns 403                | Add your frontend origin to `CORS_ALLOWED_ORIGINS`  |
| 401 on a working session         | Access token expired; the UI auto-refreshes it      |
