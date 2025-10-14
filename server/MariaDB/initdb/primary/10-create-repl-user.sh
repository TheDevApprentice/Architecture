#!/bin/bash
set -euo pipefail

# This script runs only on first initialization of the primary (empty data dir)
# It creates/ensures the replication user exists with REPLICATION SLAVE privilege.

REPL_USER="${REPL_USER:-repl}"
REPL_PASSWORD="${REPL_PASSWORD:-replpass}"
ROOT_PWD="${MARIADB_ROOT_PASSWORD:-${MYSQL_ROOT_PASSWORD:-}}"

if [ -z "$ROOT_PWD" ]; then
  echo "[primary-init] ERROR: MARIADB_ROOT_PASSWORD not set" >&2
  exit 1
fi

echo "[primary-init] Creating/ensuring replication user '$REPL_USER' exists..."
mariadb -uroot -p"$ROOT_PWD" <<SQL
CREATE USER IF NOT EXISTS '${REPL_USER}'@'%' IDENTIFIED BY '${REPL_PASSWORD}';
GRANT REPLICATION SLAVE ON *.* TO '${REPL_USER}'@'%';
FLUSH PRIVILEGES;
SQL

echo "[primary-init] Replication user ensured."
