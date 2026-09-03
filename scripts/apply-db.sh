#!/usr/bin/env bash
# Apply db/ SQL scripts using POSTGRES_* env vars (M2 or local).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

if [ -f "$PROJECT_DIR/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    source "$PROJECT_DIR/.env"
    set +a
fi

export PGHOST="${POSTGRES_HOST:-localhost}"
export PGPORT="${POSTGRES_PORT:-5432}"
export PGDATABASE="${POSTGRES_DB:-schoolms}"
export PGUSER="${POSTGRES_USER:-schoolms}"
export PGPASSWORD="${POSTGRES_PASSWORD:-schoolms}"

if ! command -v psql >/dev/null 2>&1; then
    echo "[db] ERROR: psql not found on PATH."
    exit 1
fi

echo "[db] Applying schema to ${PGHOST}:${PGPORT}/${PGDATABASE} as ${PGUSER}"
psql -v ON_ERROR_STOP=1 -f "$PROJECT_DIR/db/01_create_tables.sql"
echo "[db] Applying seed data"
psql -v ON_ERROR_STOP=1 -f "$PROJECT_DIR/db/02_insert_records.sql"
echo "[db] Done."
