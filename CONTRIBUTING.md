# Contributing to School Management System

## 1. Repository Structure

```
.
├── backend/                 Spring Boot API (Java 21)
│   ├── src/main/java/com/schoolms/   Domain packages (auth, student, exam, ...)
│   ├── src/main/resources/
│   │   ├── application.yml           Default config
│   │   ├── application-dev.yml
│   │   ├── application-prod.yml
│   │   ├── db/migration/             Flyway V1–V28
│   │   └── i18n/                     API messages en/hi/ur
│   └── src/test/java/com/schoolms/   Service unit tests
├── frontend/                Angular 19 SPA
│   ├── src/app/core/        Auth, theme, API models
│   ├── src/app/features/    One folder per module
│   ├── src/app/layout/      Shell, page-header, empty-state
│   ├── src/assets/i18n/     en.json, hi.json, ur.json
│   ├── src/styles/          M3 theme partials
│   ├── src/proxy.conf.json  Dev /api → localhost:8080
│   └── nginx.conf           Prod /api/ reverse proxy
├── db/                      Optional SQL snapshots (not a substitute for Flyway)
├── scripts/                 run-linux.sh, run-windows.bat, apply-db.sh
├── docs/                    Product, architecture, API, devops, QA, user guides
├── docker-compose.yml
└── .env.example
```

Place new backend code in the matching `com.schoolms.<domain>` package. Place new Angular screens under `frontend/src/app/features/<domain>/`. Do not add a `PRINCIPAL` role; map “principal” actions to `ADMIN` permissions such as `CERTIFICATE_APPROVE`.

## 2. Coding Standards and Style Guide

### Backend

- Java 21, Spring Boot 3.5. Controllers stay thin; business rules live in `*Service`.
- Schema changes: add the next Flyway version (`V29__...sql`). Never enable Hibernate `ddl-auto`.
- Tenant tables need `school_id` and an RLS policy (copy the `tenant_isolation` pattern from V4).
- Authorize with `@PreAuthorize("hasAuthority('CODE')")` using permission codes, not `hasRole`.
- Return `ApiResponse` for JSON; throw `BusinessException` / `ResourceNotFoundException` / `AuthException`.
- i18n keys in `messages_en.properties` (and hi/ur). Do not hardcode user-facing English in services.
- Tests: JUnit 5 service tests next to the domain (`mvn test` from `backend/`).

### Frontend

- Standalone components, typed models, `/api` relative URLs only.
- Route protection: `AuthGuard` + `PermissionGuard` with `data.permissions`.
- Nav items in `main-layout.component.ts` must use the same permission codes as the API.
- Reuse `PageHeaderComponent`, `EmptyStateComponent`, `.data-table`, `.card-toolbar`.
- Print documents (report card, marksheet, admit card) stay on white backgrounds.
- Add strings to `en.json`, `hi.json`, and `ur.json` together.
- Theme tokens: `var(--color-*)` / Material M3. Do not introduce a second palette.

There is no ESLint/Prettier/Checkstyle config in the repo yet. Match neighboring files (2-space TS, 4-space Java).

### Naming

| Kind | Pattern |
|---|---|
| Permission codes | `MODULE_ACTION` e.g. `FEE_PAYMENT_RECORD` |
| Flyway | `V{n}__snake_description.sql` |
| Angular routes | kebab-case paths |
| i18n keys | `module.camelOrDot` e.g. `nav.dashboard` |

## 3. Git Workflow and Commit Conventions

**Branches**

```
feature/{issue_id}-{short-description}
fix/{issue_id}-{short-description}
```

Examples: `feature/63-documentation`, `feature/36-ui-modernization`.

Do not commit on another issue’s branch. Leave unrelated untracked files unstaged.

**Commits (Conventional Commits)**

```
feat: add student certificate request workflow
fix: seed class-scoped calendar events
docs: add product and architecture documentation
chore: update submodule reference
```

Optional trailer used on this project:

```
Co-authored-by: monkeycode-ai <monkeycode-ai@chaitin.com>
```

**Merge:** open a GitHub pull request into `main`. Prefer squash or rebase so `main` stays linear. Do not force-push `main`.

## 4. Pull Request Checklist

- [ ] Branch named `feature/{id}-...` or `fix/{id}-...`
- [ ] Scope limited to the issue; no drive-by refactors
- [ ] Flyway migration included if the schema changed
- [ ] Permissions seeded if a new API authority was added
- [ ] i18n updated (en/hi/ur) for new UI strings
- [ ] `mvn test` in `backend/` passes for touched services
- [ ] `npm run build` in `frontend/` if UI changed
- [ ] Print layouts still white where required
- [ ] Docs updated under `docs/` when behavior or APIs change
- [ ] No secrets in commits (use `.env.example` placeholders)
- [ ] Unrelated leftovers (`TransferCertificateGuard.java`, extra `*.jrxml`, unwired guards) not staged unless they are the task
