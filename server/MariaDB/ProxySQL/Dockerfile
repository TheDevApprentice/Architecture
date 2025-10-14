# Utilise l'image officielle de ProxySQL comme base
FROM proxysql/proxysql:latest

# Copie du script de mise à jour OpenSSL
COPY update_openssl.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/update_openssl.sh

# Copie du script de vérification Galera
COPY scripts/galera_check.sh /var/lib/proxysql/
RUN chmod +x /var/lib/proxysql/galera_check.sh

# Copie du fichier de configuration personnalisé
COPY proxysql.cnf /etc/proxysql.cnf

# Point d'entrée modifié pour exécuter le script avant ProxySQL
ENTRYPOINT ["/usr/local/bin/update_openssl.sh"]
CMD ["/usr/bin/proxysql", "-f", "--idle-threads", "-c", "/etc/proxysql.cnf"]
