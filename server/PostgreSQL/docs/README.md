# Documentation PostgreSQL HA (Patroni + etcd + HAProxy)

Ce dossier documente la configuration, l’architecture et les opérations autour du cluster PostgreSQL basé sur Patroni (orchestration), etcd (DCS/consensus) et HAProxy (routage).

## Composants
- **Patroni**: supervise et orchestre PostgreSQL. Rôle Leader/Replica, failover, rejoin.
- **etcd**: magasin distribué (DCS) pour verrou leader et état du cluster.
- **HAProxy**: expose deux entrées:
  - `:5432` vers le Leader (lecture/écriture)
  - `:5433` vers les Replicas (lecture seule)
  - Stats sur `:7000`

## Fichiers et chemins utiles
- Compose: `15-docker-compose.Infra.devDB.postgres.yml`
- Patroni config: `server/PostgreSQL/Patroni/patroni.yml`
- HAProxy: `server/PostgreSQL/Patroni/haproxy.cfg`
- Scripts: `server/PostgreSQL/Patroni/entrypoint.sh`, `init.sh`

## Lecture recommandée
- `Patroni.md` — variables, modes, cycle de vie, rejoin
- `etcd.md` — quorum, TTL, scénarios de panne
- `HAProxy.md` — checks HTTP sur l’API Patroni et mapping ports
- `Operations.md` — procédures courantes et tests
- `Architecture-4-Servers.md` — placement recommandé sur 4 serveurs
- `FAQ.md` — questions/réponses issues des échanges

## Vue d’ensemble
```mermaid
flowchart LR
  subgraph DCS[etcd cluster (quorum=2/3)]
    E1[etcd1]
    E2[etcd2]
    E3[etcd3]
  end

  subgraph DB[Patroni PostgreSQL]
    P1[patroni1]
    P2[patroni2]
    P3[patroni3]
  end

  LB[HAProxy]
  Apps[Applications]

  E1<-->E2
  E2<-->E3
  E1<-->E3

  P1<-->E1
  P2<-->E2
  P3<-->E3

  LB-->P1
  LB-->P2
  LB-->P3
  Apps-->LB
```
