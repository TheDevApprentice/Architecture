# 10. Ops: HA, Backup, Observabilité

## HA
- Keycloak stateless (scale horizontal) derrière Traefik, sticky sessions conseillées si pas d’invalidation distribuée.
- Base Postgres HA (repmgr) + Pgpool déjà fournie.

## Backups
- DB: utiliser des sauvegardes Postgres (ex: pgBackRest). Export JSON des realms comme complément (config).

## Monitoring
- Métriques Prometheus (si image/extension), logs, santé HTTP.
- Surveiller erreurs OIDC, latence Token/Authorization endpoints.

## Migrations/MAJ
- Tester en staging (compat schémas DB, thèmes, providers). Lire les notes de version Keycloak.
