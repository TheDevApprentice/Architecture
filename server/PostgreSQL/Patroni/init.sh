#!/bin/bash
set -e

# Ce script s'exécute lors du bootstrap initial du cluster
# Similaire aux scripts dans /docker-entrypoint-initdb.d de MariaDB

psql -v ON_ERROR_STOP=1 --username "$PATRONI_SUPERUSER_USERNAME" <<-EOSQL
    -- Create database if not exists
    -- SELECT 'CREATE DATABASE ${POSTGRES_DB:-keycloak}'
    -- WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${POSTGRES_DB:-keycloak}')\gexec
    
    -- Create user if not exists
    --  DO \$\$
    -- BEGIN
    --     IF NOT EXISTS (SELECT FROM pg_user WHERE usename = '${POSTGRES_USER:-user}') THEN
    --         CREATE USER "${POSTGRES_USER:-user}" WITH PASSWORD '${POSTGRES_PASSWORD:-user}';
    --     END IF;
    -- END
    -- \$\$;
    
    -- Grant privileges
    -- GRANT ALL PRIVILEGES ON DATABASE "${POSTGRES_DB:-keycloak}" TO "${POSTGRES_USER:-user}";
    
    -- Connect to the database and grant schema privileges
    -- \c ${POSTGRES_DB:-keycloak}
    -- GRANT ALL ON SCHEMA public TO "${POSTGRES_USER:-user}";
    -- ALTER DATABASE "${POSTGRES_DB:-keycloak}" OWNER TO "${POSTGRES_USER:-user}";
    
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