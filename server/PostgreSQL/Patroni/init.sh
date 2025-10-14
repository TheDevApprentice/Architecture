#!/bin/bash
set -e

# Ce script s'exécute lors du bootstrap initial du cluster
# Similaire aux scripts dans /docker-entrypoint-initdb.d de MariaDB

psql -v ON_ERROR_STOP=1 --username "$PATRONI_SUPERUSER_USERNAME" <<-EOSQL
    -- Create replication user if not exists (pour monitoring style ProxySQL)
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'monitor') THEN
            CREATE USER monitor WITH PASSWORD 'monitor';
        END IF;
    END
    \$\$;
    
    GRANT pg_monitor TO monitor;
    
    -- Extensions utiles (équivalent aux features MariaDB)
    CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
    CREATE EXTENSION IF NOT EXISTS pgcrypto;
EOSQL

echo "PostgreSQL cluster initialized successfully!"