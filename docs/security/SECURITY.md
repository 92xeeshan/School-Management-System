# Security, Compliance, and Access Control

---

## 1. Role-Based Access Control Matrix

Roles in Flyway V2: `SUPER_ADMIN`, `ADMIN`, `TEACHER`, `STUDENT`, `PARENT`. **No `PRINCIPAL` role.** Certificate approval is `CERTIFICATE_APPROVE` on ADMIN / SUPER_ADMIN.

APIs check **permission codes**. UI nav uses the same codes (`hasAnyPermission`).

| Permission | SUPER_ADMIN | ADMIN | TEACHER | STUDENT | PARENT |
|---|---|---|---|---|---|
| `USER_*`, `ROLE_READ`, `ROLE_ASSIGN` | yes | yes | no | no | no |
| `STUDENT_READ` | yes | yes | yes | no | no |
| `STUDENT_CREATE/UPDATE/DELETE/IMPORT` | yes | yes | no | no | no |
| `GUARDIAN_*` | yes | yes | no | no | no |
| `CLASS_*`, `SECTION_*`, `SUBJECT_*` create/update | yes | yes | read-only CLASS/SECTION/SUBJECT | no | no |
| `TIMETABLE_READ` | yes | yes | yes | yes | yes |
| `TIMETABLE_MANAGE` | yes | yes | no | no | no |
| `ATTENDANCE_MARK` | yes | yes | yes | no | no |
| `ATTENDANCE_READ` | yes | yes | yes | yes | yes |
| `FEE_READ` | yes | yes | no | no | yes |
| `FEE_STRUCTURE_MANAGE`, `FEE_PAYMENT_RECORD` | yes | yes | no | no | no |
| `FEE_RECEIPT_VIEW` | yes | yes | no | no | yes |
| `NOTICE_READ` | yes | yes | yes | yes | yes |
| `NOTICE_CREATE/PUBLISH/DELETE` | yes | yes | no | no | no |
| `DASHBOARD_VIEW` | yes | yes | yes | yes | yes |
| `SCHOOL_READ`, `SCHOOL_MANAGE` | yes | no | no | no | no |
| `EXAM_READ` | yes | yes | yes | no | no |
| `EXAM_MANAGE` | yes | yes | no | no | no |
| `EXAM_MARK` | yes | yes | yes | no | no |
| `EVENT_READ` | yes | yes | yes | yes | yes |
| `EVENT_MANAGE` | yes | yes | no | no | no |
| `STAFF_READ` | yes | yes | yes | no | no |
| `STAFF_CREATE/UPDATE/DELETE` | yes | yes | no | no | no |
| `ADMIT_CARD_READ` | yes | yes | yes | yes | **no** |
| `REPORT_CARD_READ` | yes | yes | yes | yes | yes |
| `MARKSHEET_READ` | yes | yes | yes | yes | yes |
| `MARKSHEET_MANAGE` | yes | yes | no | no | no |
| `CERTIFICATE_READ` | yes | yes | yes | yes | yes |
| `CERTIFICATE_GENERATE` | yes | yes | yes | no | no |
| `CERTIFICATE_MANAGE`, `CERTIFICATE_APPROVE` | yes | yes | no | no | no |

SUPER_ADMIN also bypasses RLS (`school_id` NULL). ADMIN is tenant-scoped with the same school-level permissions except school platform APIs.

Class-teacher extra rules are **in services** (student profile edit, certificate request review), not extra roles.

Nav mismatch: Certificates sidenav requires `CERTIFICATE_GENERATE` or `CERTIFICATE_APPROVE`, while the route also allows `CERTIFICATE_READ`. Students/parents use **Downloads → Certificates**.

---

## 2. Data Protection Policies

| Control | Implementation |
|---|---|
| Passwords | BCrypt (`BCryptPasswordEncoder`) |
| Access tokens | JWT HMAC-SHA, 15 minutes, issuer `schoolms` |
| Refresh tokens | 32-byte hex, stored SHA-256, 30 days, rotated on refresh, revoked on logout |
| Tenant isolation | PostgreSQL RLS + `school_id`; JWT stamps `app.school_id` |
| Transport | TLS is the reverse-proxy/operator’s job; app listens HTTP 8080 |
| At rest | No column-level AES. Use disk/volume encryption on PostgreSQL and MinIO |
| Secrets | Env vars; prod `JWT_SECRET` and MinIO keys have no safe defaults |
| CSRF | Disabled (stateless Bearer API) |
| CORS | Allowlist `CORS_ALLOWED_ORIGINS`; credentials true |
| Uploads | 10 MB file / 20 MB request |
| Swagger | Public in current `SecurityConfig` — disable or protect in real production |
| Actuator | `health` public; `info` authenticated; no heapdump/env exposure |

Key management: rotate `JWT_SECRET` by deploying a new secret (invalidates access tokens; refresh still works until expiry unless you also revoke `refresh_token` rows).

---

## 3. Audit Logging Specifications

There is **no** dedicated audit table or SIEM export.

| Event | What is stored |
|---|---|
| Login | `app_user.last_login_at` |
| Mutations | `created_at` / `updated_at`; actor ids such as `marked_by`, `recorded_by`, `issued_by_user_id` |
| Refresh revoke | `refresh_token.revoked_at` |
| Certificate / marksheet workflow | Status columns and request history fields |
| Application logs | `com.schoolms` DEBUG (dev) / INFO (prod) |

Logged security-relevant HTTP outcomes: 401/403 via `RestAuthEntryPoint` / `GlobalExceptionHandler`. Retention of log files is the host’s concern. Suggested retention for production logs: 90 days.

---

## 4. Compliance Standards

SchoolMS handles student education records. Alignment is **design intent**, not a certification.

| Standard | Fit | Gaps |
|---|---|---|
| FERPA-like access | RBAC + parent/student scoped reads | No official FERPA program, no directory-info workflow |
| GDPR-like | Tenant isolation, hashed refresh tokens, deactivate vs delete | No DSR export/erasure job, no DPIA, no EU hosting default |
| SOC 2 | Change control via PRs | No CI evidence, no formal incident tracker in-app |
| HIPAA | **Not applicable** (not a health covered entity) | Do not store PHI claiming HIPAA |
| PCI DSS | Card numbers are **not** stored; `CARD` is a payment method enum | Do not capture PAN |

Do not claim certified compliance in marketing until an audit is completed.

### Operator checklist

- Change demo passwords before any real school data
- Set a unique `JWT_SECRET`
- Close Swagger if the API is on the public internet
- Restrict CORS
- Enable HTTPS
- Backup PostgreSQL
- Keep MinIO credentials off the public console
