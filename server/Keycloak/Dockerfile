FROM quay.io/keycloak/keycloak:latest

ENV HOST=${HOST}

# Copy realm(s) to the Quarkus import directory
COPY config/realms/ /opt/keycloak/data/import
COPY config/themes/ /opt/keycloak/themes/

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--import-realm", "--spi-theme-cache-themes=false", "--spi-theme-cache-templates=false"]