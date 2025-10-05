FROM quay.io/keycloak/keycloak:latest

# Place custom providers/themes here later if needed
# COPY providers/ /opt/keycloak/providers/
# COPY themes/ /opt/keycloak/themes/
# Use custom entrypoint; compose supplies command ["start"]
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start"]