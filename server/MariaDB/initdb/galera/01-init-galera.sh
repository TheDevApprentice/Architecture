#!/bin/bash
# Script d'initialisation pour Galera Cluster
# ?? placer dans ./server/MariaDB/initdb/galera/

set -euo pipefail

echo "[galera-init] Initialisation du cluster Galera..."

# Utiliser mariadb (le client officiel de MariaDB) au lieu de mysql
MYSQL_CMD="mariadb"

# Cr??ation des utilisateurs n??cessaires
$MYSQL_CMD -uroot -p"${MARIADB_ROOT_PASSWORD}" <<SQL
-- Utilisateur pour SST (State Snapshot Transfer)
CREATE USER IF NOT EXISTS 'sst_user'@'%' IDENTIFIED BY 'sst_password';
GRANT RELOAD, LOCK TABLES, PROCESS, REPLICATION CLIENT ON *.* TO 'sst_user'@'%';

-- Utilisateur de monitoring pour ProxySQL
CREATE USER IF NOT EXISTS 'monitor'@'%' IDENTIFIED BY 'monitor';
GRANT PROCESS, REPLICATION CLIENT, REPLICATION SLAVE, SLAVE MONITOR ON *.* TO 'monitor'@'%';

-- Utilisateur de backup/replication si n??cessaire
CREATE USER IF NOT EXISTS 'repl'@'%' IDENTIFIED BY 'replpass';
GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'repl'@'%';

-- Compte MaxScale pour lecture des m??tadonn??es d'authentification (MariaDBAuth)
CREATE USER IF NOT EXISTS 'maxscale'@'%' IDENTIFIED BY 'maxscale';
GRANT SELECT ON mysql.* TO 'maxscale'@'%';
GRANT SHOW DATABASES ON *.* TO 'maxscale'@'%';
GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'maxscale'@'%';

-- Utilisateur applicatif avec permissions ??tendues
GRANT ALL PRIVILEGES ON chat.* TO 'chat'@'%';

FLUSH PRIVILEGES;
SQL

echo "[galera-init] Utilisateurs cr????s avec succ??s."

# V??rification du statut Galera (optionnel car peut ??chouer)
$MYSQL_CMD -uroot -p"${MARIADB_ROOT_PASSWORD}" -e "SHOW STATUS LIKE 'wsrep%';" 2>/dev/null || echo "[galera-init] Impossible de v??rifier le statut Galera pour le moment."

echo "[galera-init] Initialisation termin??e."