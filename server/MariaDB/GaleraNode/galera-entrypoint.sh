#!/usr/bin/env bash
set -euo pipefail

# This helper renders a Galera CNF from environment variables and then chains to the
# official MariaDB entrypoint (which will start mysqld).
#
# Usage in docker-compose for each Galera node:
#   entrypoint: ["/usr/local/bin/galera-entrypoint.sh"]
#   environment:
#     GALERA: "1"
#     NODE_ID: "1"                # server-id
#     NODE_NAME: "mariadb_galera1"
#     NODE_ADDRESS: "mariadb_galera1"  # hostname/IP for wsrep_node_address
#     CLUSTER_NAME: "cluster"
#     CLUSTER_SEEDS: "mariadb_galera1,mariadb_galera2,mariadb_galera3"
#     BOOTSTRAP: "0"              # set to "1" only to bootstrap a new cluster
#     SST_USER: "sst_user"
#     SST_PASSWORD: "sst_password"
#     GCACHE_SIZE: "256M"
#     BINLOG_EXPIRE_DAYS: "3"
#     BINLOG_MAX_SIZE: "100M"
#     GTID_DOMAIN_ID: "1"
#
# If GALERA != 1, this script simply chains to the default entrypoint.

: "${GALERA:=0}"

# Path to default entrypoint
DEFAULT_ENTRYPOINT="/usr/local/bin/docker-entrypoint.sh"
if [ ! -x "$DEFAULT_ENTRYPOINT" ]; then
  # Fallback for mariadb images where entrypoint is elsewhere
  DEFAULT_ENTRYPOINT="/usr/local/bin/entrypoint.sh"
fi

if [ "$GALERA" != "1" ]; then
  exec "$DEFAULT_ENTRYPOINT" "$@"
fi

# Read env with defaults
NODE_ID="${NODE_ID:-1}"
NODE_NAME="${NODE_NAME:-mariadb_node}"
NODE_ADDRESS="${NODE_ADDRESS:-127.0.0.1}"
CLUSTER_NAME="${CLUSTER_NAME:-cluster}"
CLUSTER_SEEDS="${CLUSTER_SEEDS:-}"
BOOTSTRAP="${BOOTSTRAP:-0}"
SST_USER="${SST_USER:-sst_user}"
SST_PASSWORD="${SST_PASSWORD:-sst_password}"
GCACHE_SIZE="${GCACHE_SIZE:-256M}"
GCACHE_PAGE_SIZE="${GCACHE_PAGE_SIZE:-128M}"
INNODB_BUFFER_POOL_SIZE="${INNODB_BUFFER_POOL_SIZE:-256M}"
INNODB_LOG_FILE_SIZE="${INNODB_LOG_FILE_SIZE:-64M}"
BINLOG_EXPIRE_DAYS="${BINLOG_EXPIRE_DAYS:-3}"
BINLOG_MAX_SIZE="${BINLOG_MAX_SIZE:-100M}"
GTID_DOMAIN_ID="${GTID_DOMAIN_ID:-1}"

# Compute wsrep_cluster_address
if [ "$BOOTSTRAP" = "1" ]; then
  WSREP_CLUSTER_ADDRESS="gcomm://"
else
  if [ -z "$CLUSTER_SEEDS" ]; then
    echo "[galera-entrypoint] ERROR: CLUSTER_SEEDS must be set when BOOTSTRAP=0" >&2
    exit 1
  fi
  WSREP_CLUSTER_ADDRESS="gcomm://$CLUSTER_SEEDS"
fi

# Render Galera CNF
CNF_DIR="/etc/mysql/mariadb.conf.d"
mkdir -p "$CNF_DIR"
GALERA_CNF="/etc/mysql/mariadb.conf.d/99-galera.cnf"

cat > "$GALERA_CNF" <<EOF
[mysqld]
# --- Base ---
server-id=${NODE_ID}
bind-address=0.0.0.0
skip-name-resolve=1

# --- Galera ---
wsrep_on=ON
wsrep_cluster_name="${CLUSTER_NAME}"
wsrep_cluster_address="${WSREP_CLUSTER_ADDRESS}"
wsrep_node_address="${NODE_ADDRESS}"
wsrep_node_name="${NODE_NAME}"
wsrep_provider=/usr/lib/galera/libgalera_smm.so

# SST
wsrep_sst_method=mariabackup
wsrep_sst_auth="${SST_USER}:${SST_PASSWORD}"

# Replication/Galera tuning
wsrep_slave_threads=4
wsrep_log_conflicts=ON
wsrep_retry_autocommit=3
log_slave_updates=ON

# GTID (MariaDB)
gtid_strict_mode=ON
gtid_domain_id=${GTID_DOMAIN_ID}

# GCache/Provider options
wsrep_provider_options="pc.recovery=TRUE;gcache.size=${GCACHE_SIZE};gcache.page_size=${GCACHE_PAGE_SIZE};gcache.recover=yes"

# InnoDB for Galera
default_storage_engine=InnoDB
innodb_buffer_pool_size=${INNODB_BUFFER_POOL_SIZE}
innodb_log_file_size=${INNODB_LOG_FILE_SIZE}
innodb_flush_log_at_trx_commit=0
innodb_flush_method=O_DIRECT
innodb_autoinc_lock_mode=2
innodb_doublewrite=1

# Binlog
binlog_format=ROW
binlog_row_image=FULL
binlog_checksum=CRC32
log_bin=mysql-bin
expire_logs_days=${BINLOG_EXPIRE_DAYS}
max_binlog_size=${BINLOG_MAX_SIZE}

# General
query_cache_type=0
query_cache_size=0

# Logging
slow_query_log=ON
slow_query_log_file=/var/log/mysql/slow.log
long_query_time=2
log_queries_not_using_indexes=ON

# Timeouts
wait_timeout=28800
interactive_timeout=28800
connect_timeout=30

# Connections
max_connections=1000
max_connect_errors=10000
max_allowed_packet=16M
EOF

echo "[galera-entrypoint] Rendered ${GALERA_CNF}:"
sed -n '1,120p' "$GALERA_CNF" || true

# Determine daemon binary name (MariaDB 10.x/11.x use 'mariadbd'; some builds accept 'mysqld')
DAEMON="mariadbd"
if ! command -v "$DAEMON" >/dev/null 2>&1; then
  if command -v mysqld >/dev/null 2>&1; then
    DAEMON="mysqld"
  fi
fi

if [ "$BOOTSTRAP" = "1" ]; then
  echo "[galera-entrypoint] BOOTSTRAP=1 -> starting $DAEMON with --wsrep-new-cluster"
  # Run the official entrypoint with explicit daemon command and the bootstrap flag
  exec "$DEFAULT_ENTRYPOINT" "$DAEMON" --wsrep-new-cluster
else
  echo "[galera-entrypoint] Chaining to MariaDB entrypoint (normal join) with $DAEMON..."
  # Ensure daemon is launched even if no CMD was provided in compose (so $@ may be empty)
  exec "$DEFAULT_ENTRYPOINT" "$DAEMON"
fi
