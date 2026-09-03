#!/usr/bin/env bash
# =====================================================================
# School Management System - Run UI + Backend (Linux / macOS)
# =====================================================================
# Usage:
#     chmod +x scripts/run-linux.sh
#     ./scripts/run-linux.sh
#
# Requirements (see docs/README-BACKEND.md and docs/README-FRONTEND.md):
#     JDK 21, Maven 3.9+, Node.js 20+, PostgreSQL 15, Redis 7
#     PostgreSQL and Redis must already be running.
# =====================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# ---------------------------------------------------------------------
# Load optional .env (database/redis credentials etc.)
# ---------------------------------------------------------------------
if [ -f "$PROJECT_DIR/.env" ]; then
    echo "[run] Loading $PROJECT_DIR/.env"
    set -a
    # shellcheck disable=SC1091
    source "$PROJECT_DIR/.env"
    set +a
fi

# ---------------------------------------------------------------------
# Prerequisite checks
# ---------------------------------------------------------------------
for cmd in java mvn node npm psql; do
    if ! command -v "$cmd" >/dev/null 2>&1; then
        echo "[run] ERROR: '$cmd' not found on PATH. See docs/README-BACKEND.md."
        exit 1
    fi
done

PGHOST="${POSTGRES_HOST:-localhost}"
PGPORT="${POSTGRES_PORT:-5432}"
if ! pg_isready -h "$PGHOST" -p "$PGPORT" -q 2>/dev/null; then
    echo "[run] WARNING: PostgreSQL does not seem to be running on ${PGHOST}:${PGPORT}."
    echo "[run] The backend will still start, but login will fail until the DB is up."
fi

BACKEND_PID=""
FRONTEND_PID=""

# ---------------------------------------------------------------------
# Trap: stop both servers when the script exits
# ---------------------------------------------------------------------
cleanup() {
    echo ""
    echo "[run] Stopping servers..."
    if [ -n "$BACKEND_PID" ] && kill -0 "$BACKEND_PID" 2>/dev/null; then
        kill "$BACKEND_PID" 2>/dev/null || true
    fi
    if [ -n "$FRONTEND_PID" ] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
        kill "$FRONTEND_PID" 2>/dev/null || true
    fi
    echo "[run] Done."
}
trap cleanup EXIT INT TERM

# ---------------------------------------------------------------------
# Start backend (Spring Boot, port 8080)
# ---------------------------------------------------------------------
echo "[run] Starting backend on http://localhost:8080"
(
    cd "$PROJECT_DIR/backend"
    mvn spring-boot:run -Dspring-boot.run.profiles=dev
) &
BACKEND_PID=$!

# ---------------------------------------------------------------------
# Start frontend (Angular dev server, port 4200)
# ---------------------------------------------------------------------
echo "[run] Installing frontend dependencies (first run only) and starting UI on http://localhost:4200"
if [ ! -d "$PROJECT_DIR/frontend/node_modules" ]; then
    (cd "$PROJECT_DIR/frontend" && npm install)
fi
(
    cd "$PROJECT_DIR/frontend"
    npm start
) &
FRONTEND_PID=$!

# ---------------------------------------------------------------------
# Wait for both servers to accept connections
# ---------------------------------------------------------------------
echo "[run] Waiting for services to come up..."
for i in $(seq 1 60); do
    BACKEND_UP=""
    FRONTEND_UP=""
    if curl -sf -o /dev/null http://localhost:8080/actuator/health 2>/dev/null; then
        BACKEND_UP="yes"
    fi
    if curl -sf -o /dev/null http://localhost:4200 2>/dev/null; then
        FRONTEND_UP="yes"
    fi
    if [ -n "$BACKEND_UP" ] && [ -n "$FRONTEND_UP" ]; then
        break
    fi
    sleep 2
done

echo ""
echo "================================================================"
echo "  School Management System is running"
echo "  UI:      http://localhost:4200"
echo "  API:     http://localhost:8080"
echo "  Swagger: http://localhost:8080/swagger-ui.html"
echo "  Demo login (any of these / password Admin@123):"
echo "    superadmin | admin | teacher | parent | student"
echo "  Press Ctrl+C to stop both servers."
echo "================================================================"

wait
