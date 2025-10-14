# HAProxy — Routage et checks

## Rôles
- Expose deux entrées:
  - `:5432` → backend `postgres_primary_backend` (Leader uniquement)
  - `:5433` → backend `postgres_replicas_backend` (Replicas uniquement)
- Stats UI: `:7000`

## Health checks côté HAProxy
Dans `server/PostgreSQL/Patroni/haproxy.cfg`:
- Primary:
  - `option httpchk GET /primary`
  - `http-check expect status 200`
  - `server patroniX ... check port 8008`
- Replicas:
  - `option httpchk GET /replica`
  - `http-check expect status 200`
  - `server patroniX ... check port 8008`

Les 503 observés sur un rôle « non-conforme » sont attendus et servent justement à filtrer les nœuds.

## Healthcheck du conteneur HAProxy (compose)
- On a retiré la dépendance à `curl/wget` (non présents selon image). Si besoin, un check minimal peut utiliser `haproxy -c -f ...` (validation de config) ou une sonde TCP sur `:7000`.

## Points clés
- Aucun `httpchk` sur les lignes `server` (HAProxy 2.8) — checks définis au niveau du backend.
- Les checks interrogent l’API Patroni (`:8008`).
