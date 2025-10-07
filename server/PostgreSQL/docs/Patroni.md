# Patroni — Configuration et fonctionnement

## Variables clés (extraits du compose)
- `PATRONI_SCOPE`: nom du cluster (ex: `postgres-cluster`).
- `PATRONI_NAMESPACE`: namespace etcd (ex: `/service`).
- `PATRONI_ETCD3_HOSTS`: endpoints etcd (ex: `etcd1:2379,etcd2:2379,etcd3:2379`).
- `PATRONI_POSTGRESQL_LISTEN`/`CONNECT_ADDRESS`: écoute et adresse annonce PG.
- `PATRONI_RESTAPI_LISTEN`/`CONNECT_ADDRESS`: API REST Patroni (ex: `:8008`).
- Bootstrap DCS:
  - `PATRONI_BOOTSTRAP_DCS_TTL=30`
  - `PATRONI_BOOTSTRAP_DCS_LOOP_WAIT=10`
  - `PATRONI_BOOTSTRAP_DCS_RETRY_TIMEOUT=30`
  - `PATRONI_BOOTSTRAP_DCS_SYNCHRONOUS_MODE=true`
  - `PATRONI_BOOTSTRAP_DCS_SYNCHRONOUS_MODE_STRICT=false`
  - `PATRONI_BOOTSTRAP_DCS_POSTGRESQL_USE_PG_REWIND=true`

## Cycle de vie d’un nœud Patroni
1. Lit l’état etcd pour le `scope`/`namespace`.
2. S’il n’y a pas de leader valide, tente d’acquérir le **lock** (clé avec TTL).
3. Le leader renouvelle périodiquement le lock; les replicas suivent en streaming.
4. Si le leader disparaît et que le TTL expire, une **élection** a lieu; un replica se promeut.
5. Un ancien leader qui revient se **rejoindra** en replica. `pg_rewind` est utilisé si divergence, sinon basebackup.

## Modes synchrones
- `SYNCHRONOUS_MODE=true, SYNCHRONOUS_MODE_STRICT=false` (recommandé dev/standard): le leader accepte les commits même sans standby synchro; Patroni nomme un standby synchro dès qu’il est dispo.
- `SYNCHRONOUS_MODE_STRICT=true`: pas de commit tant qu’un standby synchro n’est pas présent (cohérence renforcée, disponibilité moindre).

## API REST Patroni (utilisée par HAProxy)
- `GET /health` — santé de base
- `GET /primary` — 200 si nœud est leader, sinon 503
- `GET /replica` — 200 si nœud est replica, sinon 503

## Rejoin/Réinitialisation d’un replica
- Vérifier l’état: `patronictl list`
- Forcer une réinit si rejoin échoue: `patronictl reinit <scope> <node>`

## Points importants
- Chaque nœud essaie les endpoints d’`ETCD3_HOSTS` jusqu’à succès.
- Les volumes persistants évitent la perte de données lors des redémarrages.
- Sans quorum etcd, aucune nouvelle élection n’est possible; le leader courant continue tant qu’il vit.
