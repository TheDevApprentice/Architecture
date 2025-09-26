#!/bin/bash
set -euo pipefail

# Configure replication on the replica using GTID auto-positioning.
# This script runs only when the data directory is empty (first startup).

REPL_USER="${REPL_USER:-repl}"
REPL_PASSWORD="${REPL_PASSWORD:-replpass}"
PRIMARY_HOST="${PRIMARY_HOST:-maxscale}"
PRIMARY_PORT="${PRIMARY_PORT:-3306}"
ROOT_PWD="${MARIADB_ROOT_PASSWORD:-${MYSQL_ROOT_PASSWORD:-}}"

if [ -z "$ROOT_PWD" ]; then
  echo "[replica-init] ERROR: MARIADB_ROOT_PASSWORD not set" >&2
  exit 1
fi

# Wait for MaxScale binlogrouter TCP to be reachable (it's not a SQL server, ping won't work)
echo "[replica-init] Waiting for primary ${PRIMARY_HOST}:${PRIMARY_PORT} (TCP)..."
for i in {1..60}; do
  if bash -lc "</dev/tcp/${PRIMARY_HOST}/${PRIMARY_PORT}" >/dev/null 2>&1; then
    echo "[replica-init] Primary TCP endpoint is reachable."
    break
  fi
  sleep 2
  if [ $i -eq 60 ]; then
    echo "[replica-init] ERROR: Primary TCP endpoint not reachable after timeout." >&2
    exit 1
  fi
done

# Give the primary a few seconds to finish any init scripts (e.g. creating repl user)
sleep 5

# Configure replication on this replica
cat <<SQL | mariadb -uroot -p"$ROOT_PWD"
STOP SLAVE; RESET SLAVE ALL;
CHANGE MASTER TO
  MASTER_HOST='${PRIMARY_HOST}',
  MASTER_PORT=${PRIMARY_PORT},
  MASTER_USER='${REPL_USER}',
  MASTER_PASSWORD='${REPL_PASSWORD}',
  MASTER_USE_GTID=slave_pos,
  MASTER_SSL=0,
  MASTER_SSL_VERIFY_SERVER_CERT=0;
START SLAVE;
SHOW SLAVE STATUS\G
SQL

echo "[replica-init] Replication configured and started."
