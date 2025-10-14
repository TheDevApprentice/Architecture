# Opérations courantes

## Démarrage/Arrêt
- Démarrer: `docker compose -f 15-docker-compose.Infra.devDB.postgres.yml up -d`
- Arrêter: `docker compose -f 15-docker-compose.Infra.devDB.postgres.yml down` (ou `stop`/`start` par service)

## Tests rapides
- Leader (RW): `psql -h 127.0.0.1 -p 5432 -U postgres -c "select pg_is_in_recovery()"` → `false`
- Replicas (RO): `psql -h 127.0.0.1 -p 5433 -U postgres -c "select pg_is_in_recovery()"` → `true`
- Stats HAProxy: `http://127.0.0.1:7000/`

## Failover
1. Arrêter le leader courant.
2. Observer la promotion via `patronictl list` et HAProxy.
3. Redémarrer le nœud arrêté; il rejoint en replica (rewind/basebackup si besoin).

## Rejoin en échec
- `patronictl list`
- `patronictl reinit <scope> <node>` pour forcer une **réinitialisation** depuis le leader.

## Tous les Patroni down
1. S’assurer qu’etcd (quorum) est up.
2. Démarrer un nœud Patroni → il devient leader après recovery/TTL.
3. Démarrer les autres → rejoin.

## Notes pratiques
- Si vous avez stoppé explicitement un service avec `stop`, utilisez `start` (et pas `up`) pour le relancer.
- Les volumes persistants évitent la perte de données.
