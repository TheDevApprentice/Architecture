# FAQ

- **Que se passe-t-il si tous les nœuds Patroni sont down puis je redémarre un seul nœud ?**
  - etcd garde l’état. Le premier nœud redémarré lit etcd, acquiert le lock et devient leader; les autres rejoignent en replicas (rewind/basebackup). Pas de perte de données avec volumes persistants.

- **Pourquoi j’ai dû parfois “recréer” un nœud pour qu’il revienne ?**
  - `up -d` ne relance pas un conteneur stoppé explicitement; utiliser `docker compose start`. Si rejoin échoue (divergence), faire `patronictl reinit <cluster> <node>`.

- **Que deviennent les checks HAProxy 503 ?**
  - Normal: `/primary` retourne 200 uniquement sur le leader, 503 sinon. `/replica` fait l’inverse. HAProxy s’en sert pour filtrer.

- **Et si 1 nœud etcd tombe (sur 3) ?**
  - Aucun impact majeur: quorum maintenu, élections toujours possibles.

- **Et si la majorité etcd tombe (≥2/3) ou tous etcd tombent ?**
  - Le leader courant continue tant qu’il vit. Aucune nouvelle élection/promotion possible jusqu’au retour du quorum etcd.

- **Chaque nœud Patroni doit-il parler à un etcd spécifique ?**
  - Non. `PATRONI_ETCD3_HOSTS` fournit plusieurs endpoints; Patroni bascule vers n’importe lequel disponible.

- **Comment vérifier rapidement le rôle exposé par HAProxy ?**
  - `psql -h 127.0.0.1 -p 5432 -U postgres -c "select pg_is_in_recovery()"` → `false` (leader)
  - `psql -h 127.0.0.1 -p 5433 -U postgres -c "select pg_is_in_recovery()"` → `true` (replicas)

- **Santé du conteneur HAProxy dans Compose ?**
  - Éviter `curl/wget` si absents; une alternative simple: `haproxy -c -f /usr/local/etc/haproxy/haproxy.cfg` (validation de config) ou sonde TCP sur `:7000`.
