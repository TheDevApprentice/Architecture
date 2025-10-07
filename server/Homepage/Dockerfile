# server/Homepage/Dockerfile
FROM ghcr.io/gethomepage/homepage:latest

# Build-time defaults (override with --build-arg TZ=...)
ARG TZ=${TZ}
ARG NODE_ENV=${NODE_ENV}
ARG HOMEPAGE_ALLOWED_HOSTS=${HOMEPAGE_ALLOWED_HOSTS}

# Runtime environment
ENV TZ=${TZ} \
    NODE_ENV=${NODE_ENV} \
    HOMEPAGE_ALLOWED_HOSTS=${HOMEPAGE_ALLOWED_HOSTS}

# Install minimal packages (tzdata, CA certs, curl), set timezone, and create non-root user
# Note: base is Alpine-based, so use apk
RUN apk add --no-cache tzdata ca-certificates curl \
  && cp /usr/share/zoneinfo/${TZ} /etc/localtime \
  && echo ${TZ} > /etc/timezone \
  && addgroup -g 1000 app \
  && adduser -D -u 1000 -G app app \
  && mkdir -p /app/public/images /app/public/icons /app/config \
  && chown -R app:app /app

# Copy the configuration and assets into the image
# Make sure these paths exist in the repo:
# - server/Homepage/config/
# - server/Homepage/public/images/
# - server/Homepage/public/icons/
COPY --chown=app:app config/ /app/config/
COPY --chown=app:app images/ /app/public/images/
COPY --chown=app:app icons/ /app/public/icons/

# Optional: container healthcheck (Homepage serves on 3000)
# You can also configure the healthcheck in docker-compose instead.
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD curl -fsS http://127.0.0.1:3000/ || exit 1

# Drop privileges
USER app

# Homepage listens on 3000 by default
EXPOSE 3000