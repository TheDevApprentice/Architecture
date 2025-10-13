FROM quay.io/keycloak/keycloak:latest

ENV HOST=${HOST}

# Copy realm(s) to the Quarkus import directory
COPY config/realms/master.json  /opt/keycloak/data/import/01-master.json
COPY config/realms/internal.json /opt/keycloak/data/import/02-internal.json
COPY config/themes/ /opt/keycloak/themes/

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--import-realm", "--spi-theme-cache-themes=false", "--spi-theme-cache-templates=false"]