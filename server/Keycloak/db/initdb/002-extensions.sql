-- Switch to keycloak DB and enable useful extensions
\connect "keycloak"

CREATE EXTENSION IF NOT EXISTS pgcrypto;
