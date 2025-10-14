# Architecture sur 4 serveurs (recommandée)

## Placement recommandé
- Serveur A: etcd + Patroni
- Serveur B: etcd + Patroni
- Serveur C: etcd + Patroni
- Serveur D: HAProxy + monitoring (+ éventuel replica `nofailover` pour sauvegardes)

## Diagramme
```mermaid
flowchart LR
  subgraph DCS[etcd (quorum 2/3)]
    E1[etcd A]
    E2[etcd B]
    E3[etcd C]
  end
  subgraph DB[Patroni PostgreSQL]
    P1[Patroni A]
    P2[Patroni B]
    P3[Patroni C]
  end
  LB[HAProxy D]
  Clients[Clients]

  E1<-->E2
  E2<-->E3
  E1<-->E3

  P1<-->E1
  P2<-->E2
  P3<-->E3

  LB-->P1
  LB-->P2
  LB-->P3
  Clients-->LB
```

## Variantes
- HAProxy redondé: ajouter un 2e HAProxy (ex. sur B) + VIP (keepalived).
- Séparation forte: etcd dédiés A/B/C, DB sur B/C/D; plus de ressources/ops.

## Scalabilité
- Lecture: ajouter des replicas; HAProxy `:5433` les load-balance.
- Écriture: un seul leader; scale vertical ou sharding/logical replication si besoin.
