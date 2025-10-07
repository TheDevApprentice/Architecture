# 02. Déploiement Docker

## Services
- `keycloak` dans `14-docker-compose.Infra.devSecurity.yml`.
- DB Postgres HA + Pgpool: `12-docker-compose.Infra.devDB.postgres.yml`.

## Démarrer
- DB: `docker compose -f 12-docker-compose.Infra.devDB.postgres.yml up -d`
- Keycloak: `docker compose -f 14-docker-compose.Infra.devSecurity.yml up -d --build`
- Accès: `http://auth.localhost:8081`

## Variables principales (Keycloak)
- `KC_DB_TYPE=postgres`, `KC_DB_URL=jdbc:postgresql://pgpool:5432/keycloak`
- `KC_DB_USERNAME`, `KC_DB_PASSWORD`
- `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD`
- `KC_PROXY=edge`, `KC_HOSTNAME`

## Réseaux/ports
- Pgpool: 5432 (localhost)
- Keycloak: 8081 (dev), routage Traefik via `proxy`

## Santé et logs
- Vérifier les healthchecks des nœuds Postgres et Pgpool.
- `docker logs keycloak -f`

## Persistance
- Volumes: `keycloak_data` pour les données; Postgres: volumes par nœud.
