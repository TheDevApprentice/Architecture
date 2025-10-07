# Dedicated Postgres for Keycloak
FROM postgres:16-alpine

# Copy initialization scripts (if any)
# Any .sql or .sh here will be executed on first container start
COPY initdb/ /docker-entrypoint-initdb.d/

# No extra packages by default; keep it lean

# Healthcheck: align with compose to report readiness when Postgres accepts connections
HEALTHCHECK --interval=10s --timeout=5s --start-period=20s --retries=10 \
  CMD pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" -h localhost || exit 1
