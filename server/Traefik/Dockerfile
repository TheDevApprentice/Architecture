# server/Traefik/Dockerfile
ARG NODE_ENV

FROM traefik:v2.11 as traefik

FROM traefik as build_dev
ONBUILD COPY traefik.dev.yml /etc/traefik/traefik.yml

FROM traefik as build_production
ONBUILD COPY traefik.yml /etc/traefik/traefik.yml
# Ensure acme storage path exists (volume will persist it)
RUN mkdir -p /letsencrypt && touch /letsencrypt/acme.json && chmod 600 /letsencrypt/acme.json

FROM build_${NODE_ENV}

# Expose HTTP/HTTPS
EXPOSE 80 443

# Default CMD (Traefik already uses /etc/traefik/traefik.yml by default)
CMD ["--configFile=/etc/traefik/traefik.yml"]