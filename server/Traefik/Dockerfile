# server/Traefik/Dockerfile
FROM traefik:v2.11

# Copy static and dynamic configuration
COPY traefik.yml /etc/traefik/traefik.yml
COPY dynamic/ /etc/traefik/dynamic/

# Ensure acme storage path exists (volume will persist it)
RUN mkdir -p /letsencrypt && touch /letsencrypt/acme.json && chmod 600 /letsencrypt/acme.json

# Expose HTTP/HTTPS
EXPOSE 80 443

# Default CMD (Traefik already uses /etc/traefik/traefik.yml by default)
CMD ["--configFile=/etc/traefik/traefik.yml"]