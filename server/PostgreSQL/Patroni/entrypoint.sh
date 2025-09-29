#!/bin/bash
set -e

# Do NOT pre-populate PGDATA before Patroni bootstraps the cluster
# Only create subdirs if a valid cluster already exists
if [ -f /var/lib/postgresql/data/PG_VERSION ]; then
  # Cluster already initialized: it's safe to ensure aux dirs exist
  mkdir -p /var/lib/postgresql/data/wal_archive
  mkdir -p /var/lib/postgresql/data/pg_log
fi

# If PGDATA is non-empty (e.g., after basebackup), enforce correct perms/ownership
if [ "$(ls -A /var/lib/postgresql/data 2>/dev/null | wc -l)" -gt 0 ]; then
  chown -R postgres:postgres /var/lib/postgresql
  chmod 700 /var/lib/postgresql/data
fi

# Create pgpass file for authentication
cat > ${PATRONI_POSTGRESQL_PGPASS} <<EOF
*:*:*:${PATRONI_SUPERUSER_USERNAME}:${PATRONI_SUPERUSER_PASSWORD}
*:*:*:${PATRONI_REPLICATION_USERNAME}:${PATRONI_REPLICATION_PASSWORD}
*:*:*:${POSTGRES_USER}:${POSTGRES_PASSWORD}
EOF
chmod 600 ${PATRONI_POSTGRESQL_PGPASS}

# Wait for etcd to be ready
echo "Waiting for etcd cluster..."
until curl -s http://etcd1:2379/health > /dev/null 2>&1; do
  echo "Waiting for etcd..."
  sleep 2
done
echo "Etcd is ready!"

# Execute the command
exec "$@"