# server/MariaDB/GaleraNode/dockerfile
# MariaDB image for local development.
# The schema is initialized by the Node server; no init SQL here.

FROM mariadb:11.4

# Environment variables are expected to be supplied via docker-compose:
# - MYSQL_DATABASE (or MARIADB_DATABASE)
# - MYSQL_USER (or MARIADB_USER)
# - MYSQL_PASSWORD (or MARIADB_PASSWORD)
# - MYSQL_ROOT_PASSWORD (or MARIADB_ROOT_PASSWORD)

# Add a tiny entrypoint helper that can render a Galera CNF from env vars when GALERA=1
COPY galera-entrypoint.sh /usr/local/bin/galera-entrypoint.sh
RUN chmod +x /usr/local/bin/galera-entrypoint.sh

# Expose the default MariaDB port
EXPOSE 3306

# Persistent data volume
VOLUME ["/var/lib/mysql"]

# The default entrypoint/cmd from the official image starts mysqld.
# We will chain through docker-compose by setting entrypoint to /usr/local/bin/galera-entrypoint.sh