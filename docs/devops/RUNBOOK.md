# System Runbook and Incident Response

**Scope:** Single-host SchoolMS (Compose or local JVM). No Grafana/Datadog is bundled.

---

## 1. Monitoring and Alerting Setup

| Signal | Where | Threshold (suggested) |
|---|---|---|
| Liveness | `GET http://localhost:8080/actuator/health` | Must return `{"status":"UP"}` |
| API process | `docker compose ps` / systemd | All services `running` |
| PostgreSQL | `pg_isready -U schoolms` | Ready |
| MinIO | console :9001 or `mc ready` | Ready if PDFs are required |
| Application logs | `docker compose logs -f backend` or stdout | No repeating 500s |
| Hikari | Default max pool 10 | Connection wait / timeout in logs |
| Disk | Host `df -h` | Volumes `pgdata`, `miniodata` |

Actuator exposes **only** `health` and `info`. `health` is public; `info` requires auth.

There are no shipped dashboards or pager rules. Operators should add host-level CPU/memory/disk alerts if they run production.

---

## 2. Incident Severity Matrix

| Severity | Definition | Response intent | Escalation |
|---|---|---|---|
| Sev 1 | Total outage (API down, DB down, nobody can log in) | Immediate; restore process or DB | On-call operator / DBA |
| Sev 2 | Major feature down (fees, exams publish, certificates) while login works | Same business day | Module owner |
| Sev 3 | Degraded (slow queries, MinIO down so PDFs fail) | Next business day | Developer |
| Sev 4 | Cosmetic / docs / single-user data fix | Planned | Issue tracker |

Contacts are not stored in this repository. Maintain an internal roster.

---

## 3. Runbook Playbooks

### 3.1 Service will not start

1. `docker compose ps` and `docker compose logs backend`.
2. If Flyway: `role "app_rls" does not exist` → create roles from `docs/README-BACKEND.md`.
3. `Connection refused ... 5432` → start PostgreSQL, check `POSTGRES_HOST`.
4. Port in use → stop the other process or change Compose `ports`.

### 3.2 Health is DOWN / high error rate

1. Hit `/actuator/health`.
2. Confirm PostgreSQL `pg_isready`.
3. Check Hikari errors (`connection is not available`). Reduce concurrent load or raise `maximum-pool-size` only after measuring.
4. Restart backend **after** DB is healthy: `docker compose restart backend`.

### 3.3 High CPU / memory

1. Identify the process (`backend` vs `postgres`).
2. Look for runaway requests (bulk ZIP export, CSV import).
3. Restart the API process if it is leaked; capture logs first.
4. PostgreSQL: look for sequential scans on large lists; indexes are listed in `DATABASE_SCHEMA.md`.

### 3.4 Login failures

| Symptom | Action |
|---|---|
| 401 bad credentials | Confirm demo password `Admin@123` or reset hash |
| 403 inactive / TC | User status or TC deactivation; see certificates |
| 403 CORS | Add origin to `CORS_ALLOWED_ORIGINS` |
| Frontend login error, API up | Confirm proxy `/api` → 8080 |
| Token loop | Clear `localStorage` key `schoolms.auth` |

### 3.5 MinIO / PDF failures

1. `docker compose logs minio`.
2. Confirm `MINIO_ENDPOINT` from the backend network (`http://minio:9000` in Compose).
3. Login/SIS can continue without MinIO; receipts and certificate files cannot.

### 3.6 Wrong tenant data (suspected leak)

1. Treat as Sev 1/2 security incident.
2. Confirm the user is not `SUPER_ADMIN` (RLS bypass).
3. Verify `app.school_id` GUC on the connection.
4. Do not disable RLS to “fix” a bug.

---

## 4. Database Backup and Disaster Recovery

### Backup (manual)

```bash
docker compose exec postgres pg_dump -U schoolms schoolms > backup-$(date +%Y%m%d).sql
```

Frequency suggestion: daily for any environment with real student data; keep at least 7 days.

MinIO: snapshot the `miniodata` volume or `mc mirror`.

### Restore

1. Stop backend.
2. Restore SQL into an empty database as role `schoolms`.
3. Start backend; Flyway should see current version and not re-apply.
4. Smoke: login `admin` / health endpoint / one student list.

### Point-in-time recovery

Not configured (no WAL archiving in Compose). Enable PostgreSQL PITR on the operator’s managed instance if required.

### Restore validation

- `GET /actuator/health` → UP
- Login seeded or known admin
- Row counts for `student`, `fee_payment` match the backup notes
- Open one published marksheet PDF if MinIO was restored
