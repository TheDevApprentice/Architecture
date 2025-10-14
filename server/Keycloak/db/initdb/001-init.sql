-- Keycloak database initialization
-- Note: The official Postgres image will create the database and user from
-- POSTGRES_DB, POSTGRES_USER, and POSTGRES_PASSWORD automatically.
-- This script runs against the POSTGRES_DB database and can be used to
-- enable extensions or set additional settings.

-- Useful extension for Keycloak password hashing and functions
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Placeholders for future tweaks (schemas, privileges, etc.)
-- Example: ALTER SCHEMA public OWNER TO keycloak;
