FROM registry:2

# Install apache2-utils for htpasswd command
USER root
RUN apk add --no-cache apache2-utils

# Copy entrypoint script
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Default environment variables
ENV REGISTRY_AUTH=htpasswd
ENV REGISTRY_AUTH_HTPASSWD_REALM="Registry Realm"
ENV REGISTRY_AUTH_HTPASSWD_PATH=/auth/htpasswd
ENV REGISTRY_HTTP_ADDR=:5000
ENV REGISTRY_STORAGE_DELETE_ENABLED=true
ENV REGISTRY_STORAGE=filesystem
ENV REGISTRY_STORAGE_FILESYSTEM_ROOTDIRECTORY=/var/lib/registry

# Registry credentials (to be overridden in docker-compose)
ENV REGISTRY_USERNAME=${REGISTRY_USERNAME}
ENV REGISTRY_PASSWORD=${REGISTRY_PASSWORD}

# Create auth directory
RUN mkdir -p /auth

# Expose port
EXPOSE 5000

# Use custom entrypoint
ENTRYPOINT ["/entrypoint.sh"]
CMD ["/etc/docker/registry/config.yml"]