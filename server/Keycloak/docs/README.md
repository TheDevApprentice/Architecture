# Documentation Keycloak (Projet)

Cette documentation couvre l’utilisation de Keycloak dans ce dépôt et son intégration dans vos applications (front + API), du développement à la prod.

## Table des matières
- 01-architecture.md
- 02-deploiement-docker.md
- 03-oidc-oauth2-flows.md
- 04-configuration-realm-clients.md
- 05-roles-groupes-autorisations.md
- 06-federation-identites-idp.md
- 07-themes-branding.md
- 08-integration-node-express-option-b.md
- 09-securite-bonnes-pratiques.md
- 10-ops-ha-backup-observabilite.md
- 11-troubleshooting.md

## Points clés du dépôt
- Compose: `14-docker-compose.Infra.devSecurity.yml` (service `keycloak`, labels Traefik, réseaux `proxy`/`dbnet`).
- Base de données: Postgres HA via `12-docker-compose.Infra.devDB.postgres.yml` (Pgpool + repmgr), URL JDBC: `jdbc:postgresql://pgpool:5432/keycloak`.
- Dockerfile Keycloak: `server/Keycloak/Dockerfile` (base image, customisations possibles: thèmes, providers, conf). 

## Public cible
- Développeurs front/back
- Ops/SRE
- Sécurité (IAM)

## Conventions
- Realm applicatif distinct de `master`.
- En dev: HTTP possible; en prod: HTTPS strict via Traefik, `KC_PROXY=edge`, `KC_HOSTNAME`.

http://auth.localhost/admin/master/console/
http://auth.localhost/realms/master/account
http://auth.localhost/realms/internal/account
