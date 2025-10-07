# etcd — Rôle et scénarios

## Rôle (DCS)
- Stocke l’état du cluster Patroni: lock leader, membres, config, historique.
- Fournit **consensus** via Raft. Les écritures nécessitent un **quorum**.

## Quorum et tailles de cluster
- Toujours un nombre impair: 3 ou 5.
- Avec 3 nœuds: quorum = 2.

## TTL/Lock leader
- Le leader Patroni renouvelle une clé éphémère avec **TTL**.
- Si non renouvelée (panne), la clé expire → nouvelle élection possible.

## Pannes
- 1 nœud etcd down (sur 3): **OK**. Quorum maintenu, élections possibles.
- Majorité etcd perdue (≥2 down) ou **tous etcd down**:
  - Le leader PG courant continue tant qu’il tourne.
  - Pas de nouvelle promotion/élection possible jusqu’au retour du quorum.

## Bonnes pratiques
- 3 nœuds etcd sur 3 hôtes distincts (souvent colocalisés avec Patroni).
- Pas de nombres pairs; pas de 2 etcd sur le même serveur.
- Volumes persistants pour etcd.
