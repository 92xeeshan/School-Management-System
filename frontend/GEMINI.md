# GEMINI.md — School Management System Frontend

## 0. Agent Contract

This file is the persistent context and engineering rules for this repository.

Before changing code:

1. Read this file.
2. Read `NewUI.md` when working on UI modernization.
3. Inspect the relevant existing implementation before editing.
4. Follow existing architecture/API/contracts unless explicitly asked to change them.

**Execution rule:** Work on ONE requested task/sub-task at a time. Do not continue to another task automatically. After completing the requested task, validate, summarize, and STOP. Continue only after explicit user confirmation.

**Never:**

* invent API endpoints, models, permissions, or backend behavior
* replace real API behavior with mock/fake data unless explicitly requested
* bypass authentication/authorization
* remove existing validation, loading/error handling, pagination, or business logic
* migrate architecture/framework without explicit request
* rewrite unrelated files
* duplicate existing theme/i18n/design-system infrastructure
* silently change business behavior during UI work

Prefer the smallest maintainable change that satisfies the request.

---

# 1. Project

School Management System frontend.

Primary domains:

`Dashboard · Students · Staff · Academics · Examinations · Attendance · Fees · Notices · Authentication`

Backend is a REST API exposed through `/api/*`.

This is an existing production-oriented application being incrementally modernized. **Preserve existing behavior unless the task explicitly changes behavior.**

---

# 2. Stack

* Angular 19
* Standalone components
* TypeScript 5.7
* RxJS 7.8
* Angular Material/CDK 19
* Material 3 theming
* SCSS
* ngx-translate 16
* Docker + Nginx

Do not introduce another UI framework, CSS framework, state-management library, or Angular module architecture unless explicitly requested.

---

# 3. Repository Map

```text
src/
├── app/
│   ├── core/
│   │   ├── auth/             # authentication, guards, interceptor
│   │   ├── models/           # shared/domain models
│   │   └── theme/            # theme state
│   ├── features/
│   │   ├── academics/
│   │   ├── attendance/
│   │   ├── dashboard/
│   │   ├── examinations/
│   │   ├── fees/
│   │   ├── login/
│   │   ├── notices/
│   │   ├── staff/
│   │   └── students/
│   ├── i18n/                 # locale configuration/service
│   ├── layout/
│   │   └── main-layout/      # authenticated application shell
│   ├── app.config.ts
│   ├── app.routes.ts
│   └── app.component.*
├── assets/
│   └── i18n/                 # en.json, hi.json, ur.json
├── styles/
│   └── _theme.scss           # Material/custom theme
├── styles.scss               # global styles
└── main.ts
```

Important files:

```text
src/app/app.routes.ts
src/app/app.config.ts
src/app/layout/main-layout/
src/app/core/auth/
src/app/core/theme/
src/app/i18n/
src/styles.scss
src/styles/_theme.scss
src/assets/i18n/
```

Before modifying a feature, inspect its entire feature directory plus relevant service/model/route/translation dependencies.

---

# 4. Angular Conventions

Use the existing Angular 19 standalone architecture.

Prefer:

* standalone components
* lazy-loaded routes
* `ChangeDetectionStrategy.OnPush`
* Angular control flow: `@if`, `@for`, `@switch`
* existing DI/services
* existing route guards/interceptors
* existing shared models/utilities

Do not:

* introduce NgModules
* duplicate services
* create unnecessary abstractions
* perform broad refactors during focused UI tasks

---

# 5. Routes / Features

Main routes:

```text
/login
/dashboard

/students
/students/:id

/staff

/academics
/academics/classes
/academics/subjects
/academics/timetable
/academics/syllabus
/academics/examinations
/academics/terms

/examinations
/examinations/schedules
/examinations/marks
/examinations/report-cards
/examinations/analytics
/examinations/reevaluation

/attendance
/fees
/notices
```

Authenticated application routes are protected by `AuthGuard`; feature access may additionally use `PermissionGuard`.

Never remove or weaken route protection to make a UI feature work.

---

# 6. Authentication & Authorization

Authentication implementation:

```text
src/app/core/auth/auth.service.ts
src/app/core/auth/auth.guard.ts
src/app/core/auth/permission.guard.ts
src/app/core/auth/auth.interceptor.ts
```

API:

```text
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
```

Session key:

```text
schoolms.auth
```

Session contains access token, refresh token, expiration, and user.

Authenticated requests use:

```text
Authorization: Bearer <access-token>
```

Existing refresh/401 handling must remain intact.

Permissions are enforced in the existing authorization infrastructure. Examples:

```text
DASHBOARD_VIEW
STUDENT_READ
STAFF_READ
CLASS_READ
SUBJECT_READ
TIMETABLE_READ
EXAM_READ
EXAM_MANAGE
ATTENDANCE_READ
ATTENDANCE_MARK
FEE_READ
FEE_RECEIPT_VIEW
NOTICE_READ
```

**Do not hard-code role assumptions in components when the existing permission system can be used.**

Navigation visibility must remain permission-aware.

---

# 7. API Contract

Use relative `/api/...` URLs.

Development proxy:

```text
/api → http://localhost:8080
```

Production Nginx:

```text
/api/ → http://backend:8080
```

Never hard-code `localhost:8080` in application services.

Common response contracts:

```ts
interface ApiResponse<T> {
  success: boolean;
  data: T;
  timestamp: string;
}
```

```ts
interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
```

```ts
interface ApiError {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  fieldErrors: Array<{
    field: string;
    code: string;
    message: string;
  }> | null;
}
```

Existing API areas include:

```text
Dashboard:
  /api/dashboard
  /api/events
  /api/notices/published

Students:
  /api/students
  /api/students/{id}
  /api/students/{id}/profile
  /api/students/{id}/deactivate
  /api/students/{id}/enroll
  /api/students/by-section

Staff:
  /api/staff/teachers
  /api/staff/teachers/{id}
  /api/staff/teachers/{id}/deactivate
  /api/staff/teachers/{id}/class-teacher
  /api/staff/non-teaching
  /api/staff/non-teaching/{id}
  /api/staff/non-teaching/{id}/deactivate
  /api/staff/sections

Academics:
  /api/classes
  /api/classes/{id}
  /api/sections
  /api/sections/{id}
  /api/subjects
  /api/subjects/{id}
  /api/academic-years
  /api/timetable
  /api/timetable/{id}
  /api/grading-schemes
  /api/grading-schemes/{id}/activate
  /api/grading-schemes/{id}/deactivate

Examinations:
  /api/exam-marks
  /api/exam-marks/options
  /api/exam-marks/import
  /api/exam-marks/template
  /api/exam-marks/lock

Attendance:
  /api/attendance/mark
  /api/attendance/section

Fees:
  /api/fees/students/{studentId}/assignments
  /api/fees/students/{studentId}/payments
  /api/fees/assignments/{assignmentId}/installments
  /api/fees/payments

Notices:
  /api/notices
  /api/notices/published
```

These are existing contracts, not a license to invent related endpoints. Always inspect the actual service before modifying API usage.

---

# 8. Internationalization

Library:

`ngx-translate`

Translations:

```text
src/assets/i18n/en.json
src/assets/i18n/hi.json
src/assets/i18n/ur.json
```

Locales:

```text
en = English = LTR
hi = Hindi   = LTR
ur = Urdu    = RTL
```

Locale infrastructure:

```text
src/app/i18n/i18n.config.ts
src/app/i18n/locale.service.ts
```

`LocaleService` controls language persistence, browser fallback, `<html lang>`, `<html dir>`, RTL state, and translation language.

Rules:

* all new user-facing text must be translated
* update all three locale files when adding translation keys
* never assume English text width
* test long Hindi/Urdu labels
* do not break interpolation/pluralization
* preserve existing translation-key structure where possible

Do not hard-code user-facing English in templates/components.

---

# 9. RTL

Urdu requires RTL.

Existing RTL state uses the document direction and an `html.rtl` class.

CSS must support both directions.

Prefer logical CSS:

```text
margin-inline
padding-inline
inset-inline
border-inline
text-align: start/end
```

Avoid unnecessary physical-direction rules:

```text
margin-left/right
padding-left/right
left/right
```

unless the property genuinely represents a physical direction.

Verify RTL for:

* sidebar/navigation
* toolbar
* icons
* forms
* tables
* pagination
* dialogs
* tabs
* badges
* spacing/alignment

Never implement RTL as an afterthought.

---

# 10. Theme System

Existing theme infrastructure:

```text
src/styles/_theme.scss
src/styles.scss
src/app/core/theme/theme.service.ts
```

Themes:

* Light
* Dark

Existing custom semantic tokens include:

```text
--color-primary
--color-primary-soft
--color-bg
--color-border
--color-muted
--color-sidebar
--color-sidebar-text
--radius
```

Use the existing Material 3/theme-token infrastructure.

Do not create a second theme system.

Do not scatter arbitrary colors throughout components.

New UI must work in both light and dark modes.

When adding a semantic design token, add it centrally rather than duplicating values across components.

---

# 11. Existing UI Language

Existing reusable concepts include:

```text
.card
.btn
.btn-primary
.btn-block
.btn-ghost

.badge
.badge-success
.badge-danger
.badge-warning
.badge-muted

.alert
.alert-error
```

Reuse or systematically evolve existing patterns.

Do not create multiple competing button/card/badge conventions.

Angular Material should be preferred for interactive/form controls where appropriate.

---

# 12. Main Layout

Main application shell:

`src/app/layout/main-layout/`

Contains:

* sidebar
* brand
* permission-aware navigation
* top toolbar
* language selector
* user information/avatar
* logout
* router outlet

Primary navigation:

```text
Dashboard
Students
Staff
Academics
Examinations
Attendance
Fees
Notices
```

Layout changes must preserve:

* routing
* authentication
* permissions
* localization
* RTL
* theme switching
* logout

---

# 13. UI Modernization

`NewUI.md` is the detailed modernization plan.

When working on UI modernization, treat `NewUI.md` as the task sequence and this file as the engineering constraints.

Current modernization areas:

1. Material 3 theme setup
2. Theme service/toggle
3. Layout/navigation redesign
4. Shared components/tables/data views
5. Global consistency + verification

**Do not execute multiple modernization tasks in one run.**

For each task:

```text
inspect → implement → validate → summarize → STOP
```

Wait for explicit confirmation before continuing.

---

# 14. UI/UX Requirements

Modernized UI should be:

* clean
* professional
* consistent
* responsive
* accessible
* data-oriented
* appropriate for school administration

Use consistent patterns for:

* page headers
* cards
* forms
* filters
* tables
* tabs
* dialogs
* pagination
* badges/status
* loading
* empty states
* errors

Avoid decorative UI that reduces usability.

Do not change business behavior for visual reasons.

---

# 15. Responsive Design

Support:

* desktop
* laptop
* tablet
* mobile

Pay particular attention to:

* sidebar
* toolbar
* tables
* filter bars
* forms
* dialogs
* dashboard cards
* tabs
* translated labels

Prevent accidental horizontal page overflow.

Large data tables may use intentional horizontal scrolling.

---

# 16. Accessibility

Maintain:

* semantic HTML
* keyboard navigation
* visible focus states
* accessible labels
* appropriate `aria-label`s
* accessible form errors
* sufficient contrast
* non-color-only status communication

Icons used as actions need accessible names.

Do not remove accessibility to achieve visual compactness.

---

# 17. Business Logic Invariant

The frontend represents real administrative workflows.

Preserve existing behavior for:

* CRUD
* forms
* validation
* API calls
* pagination
* filtering
* sorting
* loading/error states
* authentication
* authorization
* localization
* attendance
* fees
* examinations
* student/staff management
* notices
* dashboard data

If a requested visual change appears to require backend/business-rule changes, inspect and report the dependency rather than inventing behavior.

---

# 18. Development / Validation

Install:

```bash
npm install
```

Run:

```bash
npm start
```

Build:

```bash
npm run build
```

Test:

```bash
npm test
```

Watch:

```bash
npm run watch
```

After meaningful code changes, run the smallest relevant validation, and for completed sub-tasks normally run:

```bash
npm run build
```

Run tests when relevant.

Never report a task as validated if the build/test was not actually run.

If validation fails:

1. diagnose
2. fix issues caused by the current task
3. rerun validation
4. report unrelated pre-existing failures separately

---

# 19. Docker / Nginx

Deployment uses Docker + Nginx.

Preserve:

* Angular SPA fallback
* `/api/` backend proxy
* production routing behavior

Do not modify deployment configuration during UI work unless required.

---

# 20. Change Discipline

For every task:

### Before editing

* identify affected files
* inspect dependencies
* understand current behavior
* identify translations/theme/RTL implications
* identify authorization/API implications

### While editing

* keep scope narrow
* reuse existing infrastructure
* preserve behavior
* avoid unrelated formatting/refactors
* avoid speculative improvements

### After editing

* inspect diff
* build/test as appropriate
* verify translations if UI text changed
* verify RTL if layout changed
* verify dark mode if styling changed
* report files changed and validation result

---

# 21. Completion Report Format

At the end of each requested task, report:

```text
## Completed
- <short description>

## Files Changed
- path/to/file
- path/to/file

## Validation
- Build: PASS/FAIL
- Tests: PASS/FAIL/NOT RUN

## Notes
- <important implementation detail>
- <known issue, if any>

STOP — wait for explicit confirmation before starting another task.
```

Keep the report concise.

---

# 22. Decision Priority

When instructions conflict, prioritize:

1. Explicit current user request
2. Preserve existing functionality/security/API contracts
3. Authentication and authorization
4. Localization + RTL
5. Accessibility
6. Current task scope
7. Existing architecture/conventions
8. Design consistency
9. Visual polish

Do not sacrifice application behavior for visual changes.

---

# 23. Source of Truth

Use this hierarchy:

```text
Actual source code/config/API usage
        ↓
GEMINI.md engineering constraints
        ↓
NewUI.md UI modernization sequence
        ↓
Agent assumptions
```

When documentation conflicts with actual code, inspect the code and report the discrepancy before making a risky change.

Do not assume undocumented behavior.

---

# 24. Final Rule

**Understand first. Change minimally. Preserve behavior. Validate. Stop.**

For UI modernization:

**ONE sub-task → ONE implementation → validation → report → STOP → user confirmation.**
