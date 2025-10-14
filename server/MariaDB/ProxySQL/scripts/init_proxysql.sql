-- Script d'initialisation ProxySQL
-- À placer dans ./server/MariaDB/ProxySQL/scripts/init_proxysql.sql

-- Charger la configuration dans le runtime
LOAD MYSQL SERVERS TO RUNTIME;
LOAD MYSQL USERS TO RUNTIME;
LOAD MYSQL QUERY RULES TO RUNTIME;
LOAD ADMIN VARIABLES TO RUNTIME;
LOAD MYSQL VARIABLES TO RUNTIME;

-- Sauvegarder la configuration sur disque
SAVE MYSQL SERVERS TO DISK;
SAVE MYSQL USERS TO DISK;
SAVE MYSQL QUERY RULES TO DISK;
SAVE ADMIN VARIABLES TO DISK;
SAVE MYSQL VARIABLES TO DISK;

-- Vérifier les serveurs
SELECT hostgroup_id, hostname, port, status, weight FROM mysql_servers ORDER BY hostgroup_id, hostname;

-- Vérifier les utilisateurs
SELECT username, default_hostgroup, max_connections FROM mysql_users;

-- Vérifier les règles de routage
SELECT rule_id, match_pattern, destination_hostgroup, apply FROM mysql_query_rules WHERE active=1 ORDER BY rule_id;

-- Activer le monitoring
UPDATE global_variables SET variable_value='monitor' WHERE variable_name='mysql-monitor_username';
UPDATE global_variables SET variable_value='monitor' WHERE variable_name='mysql-monitor_password';
LOAD MYSQL VARIABLES TO RUNTIME;
SAVE MYSQL VARIABLES TO DISK;

-- Forcer une vérification de connectivité
SELECT * FROM monitor.mysql_server_connect_log ORDER BY time_start_us DESC LIMIT 10;
SELECT * FROM monitor.mysql_server_ping_log ORDER BY time_start_us DESC LIMIT 10;