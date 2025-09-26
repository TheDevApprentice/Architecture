 # server/MariaDB/dockerfile
 # MariaDB image for local development.
 # The schema is initialized by the Node server; no init SQL here.
 
 FROM mariadb:11.4
 
 # Environment variables are expected to be supplied via docker-compose:
 # - MYSQL_DATABASE (or MARIADB_DATABASE)
 # - MYSQL_USER (or MARIADB_USER)
 # - MYSQL_PASSWORD (or MARIADB_PASSWORD)
 # - MYSQL_ROOT_PASSWORD (or MARIADB_ROOT_PASSWORD)
 
 # Expose the default MariaDB port
 EXPOSE 3306
 
 # Persistent data volume
 VOLUME ["/var/lib/mysql"]
 
 # The default entrypoint/cmd from the official image starts mysqld.
 