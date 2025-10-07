# Dedicated Postgres for Keycloak
FROM postgres:16-alpine

# Copy initialization scripts (if any)
# Any .sql or .sh here will be executed on first container start
COPY initdb/ /docker-entrypoint-initdb.d/

# No extra packages by default; keep it lean
