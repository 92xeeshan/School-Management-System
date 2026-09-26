# School Management System - AI Instructions Context (GEMINI.md)

This file serves as the foundational instruction context and architectural guide for AI interactions on the School Management System codebase. It defines development guidelines, build commands, testing strategies, and the technical architecture of both the backend and frontend modules.

---

## 1. Project Overview

The **School Management System** is a secure, highly scalable, multi-tenant academic and administrative management application. It supports multiple independent school organizations (tenants) isolated safely within a single shared database.

### 1.1 Technical Stack
* **Backend:** Spring Boot 3.x / Java 21 / Spring Security (JWT access + DB-backed refresh tokens) / Spring Data JPA / Caffeine Cache / Flyway Migrations / Swagger / MinIO S3 Object Storage (optional for attachments) / JasperReports (PDF receipt generation).
* **Frontend:** Angular 19 (utilizing Standalone Components, Angular Signals, and Reactive Forms) / Angular Material / ngx-translate (i18n support).
* **Database:** PostgreSQL 15+ leveraging Row-Level Security (RLS) for absolute tenant isolation.

### 1.2 Multi-Tenant Architecture & Database RLS
The project employs a **Shared-Database, Shared-Schema** approach.
* **Row-Level Security (RLS):** Every tenant-specific table contains a `school_id` UUID discriminator column. Row access is strictly limited via PostgreSQL RLS policies.
* **Database Roles:**
  * `schoolms`: Flyway migration user. Owns all tables, triggers, and creates DB-level grants/RLS policies.
  * `app_rls`: Runtime application database role. Operating queries under this user strictly filters results by the evaluated `app.school_id` session config.
  * `app_admin`: DBA/Superuser bypass role with RLS bypassed (used for platform-level management).
* **Dynamic Session Injection (`TenantAwareHikariDataSource`):**
  Upon checking out a database connection from the Hikari pool, the custom `TenantAwareHikariDataSource` class injects the thread's current `TenantContext` properties into the session:
  ```sql
  SELECT set_config('app.school_id', '<UUID>', false), set_config('app.bypass_rls', 'false', false)
  ```
  PostgreSQL RLS policies evaluate `current_setting('app.school_id')` to enforce data isolation in real-time.

---

## 2. Building and Running

### 2.1 Quick Start (Using Docker Compose)
Spins up PostgreSQL, MinIO, Backend, and Frontend containers:
```bash
cp .env.example .env
docker compose up --build
```

### 2.2 Local Standalone Run (Without Docker)

#### 2.2.1 Prerequisites
1. **Java:** JDK 21 (Temurin 21 recommended)
2. **Build Tools:** Maven 3.9+ & Node.js 20+ (with npm 10+)
3. **Services:** Running instances of PostgreSQL 15+ and optionally MinIO.

#### 2.2.2 PostgreSQL Setup
Connect as a PostgreSQL superuser (`postgres`) and execute:
```sql
CREATE ROLE schoolms LOGIN PASSWORD 'schoolms';
CREATE ROLE app_rls  LOGIN PASSWORD 'app_rls';
CREATE ROLE app_admin LOGIN PASSWORD 'app_admin';

CREATE DATABASE schoolms OWNER schoolms;
```
*Note: Flyway will automatically construct tables, create indices, configure RLS, and seed data on backend start.*

#### 2.2.3 Backend Dev Mode Setup
Configure required environment variables (sensible defaults are configured for local environment) and run:
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### 2.2.4 Frontend Dev Mode Setup
Install dependencies and run the local Angular dev server:
```bash
cd frontend
npm install
npm start
```
*Note: The frontend dev server utilizes `proxy.conf.json` to proxy any requests matching `/api` to the backend on `http://localhost:8080`.*

#### 2.2.5 Launch Scripts
Alternatively, use the dual-launch convenience scripts to start both apps:
* **Windows:** Double-click `scripts\run-windows.bat`
* **Linux/macOS:** Run `./scripts/run-linux.sh`

---

## 3. Testing Strategies

Comprehensive automated testing is expected for all code updates.

### 3.1 Backend Tests (JUnit + Spring Boot Test)
* Runs all unit and integration tests:
  ```bash
  cd backend
  mvn test
  ```
* Test configurations are loaded automatically via Maven test-profile parameters.

### 3.2 Frontend Tests (Karma + Jasmine)
* Runs the suite of Karma unit tests:
  ```bash
  cd frontend
  npm test
  ```

---

## 4. Development Conventions

### 4.1 General Code Conventions & Standards
* **No Bypass Hacks:** Never use direct casts, reflection, or bypass standard framework models unless explicitly instructed. Standard OOP delegation, composition, and type guards are always preferred.
* **Secrets Protection:** Do not log, print, or commit any credentials, tokens, `.env` details, or local database secrets. Use environment placeholders where appropriate.

### 4.2 Backend (Spring Boot) Conventions
* **Base Classes:** All JPA entity files must inherit from `com.schoolms.common.BaseEntity` to inherit the default UUID primary key format and standard auditing fields (`created_at`, `updated_at`).
* **Design Layers:** Keep controller, service, and database repository logic cleanly segregated. Validate incoming parameters using Jakarta Validation annotations at the controller layer.
* **Flyway Migrations:** All DB schema modifications must occur exclusively via new Flyway migration scripts under `backend/src/main/resources/db/migration/`. Never alter existing migration files.
* **Caffeine Cache:** Authority/permission checks are aggressively cached in Caffeine. Clear/invalidate cached authorities if dynamic permission adjustments occur.

### 4.3 Frontend (Angular) Conventions
* **Standalone First:** New Angular components, directives, and pipes must be structured as standalone.
* **Signals:** Prefer Angular Signals (`signal`, `computed`, `effect`) for modern state tracking and reactivity over old class-property bindings.
* **Proxy Routing:** Do not hardcode API base endpoints. Always route traffic relatively to `/api/*` and let `proxy.conf.json` proxy the request.
* **Localization (i18n):** User-facing text must be externalized to localized i18n key bundles. Always keep `backend/src/main/resources/i18n/messages_*.properties` sync-aligned with frontend `frontend/src/assets/i18n/*.json` (supporting `en`, `hi`, and `ur`).

---

## 5. Deployment & Production

* **Production Packaging:**
  * **Backend JAR Compilation:**
    ```bash
    cd backend
    mvn -DskipTests package
    ```
    This builds the executable JAR file in `backend/target/schoolms-backend-0.1.0-SNAPSHOT.jar`.
  * **Frontend Production Build:**
    ```bash
    cd frontend
    npm run build
    ```
    This outputs optimized static production assets to `frontend/dist/frontend/browser` suitable for serving via Nginx or CDN.
* **Database Migrations:** Schema-changing operations in production are initiated strictly by Flyway when starting the backend jar with the production migration user `schoolms`.
