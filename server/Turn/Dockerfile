FROM coturn/coturn:latest

# Optional: set a default timezone via build-arg, can be overridden at build time
ARG TZ=UTC
ENV TZ=${TZ}

# Default runtime configuration (can be overridden via environment in compose)
ENV COTURN_PUBLIC_IP=127.0.0.1 \
    COTURN_REALM=local \
    COTURN_USER=devuser \
    COTURN_PASS=devpass

# Document commonly used ports
EXPOSE 3478 3478/udp

# Provide a sensible default command that mirrors your docker-compose flags
# You can still override this in docker-compose if needed.
CMD ["sh","-lc","exec turnserver \
  --no-cli \
  --no-tls \
  --fingerprint \
  --lt-cred-mech \
  --realm=${COTURN_REALM} \
  --user=${COTURN_USER}:${COTURN_PASS} \
  --external-ip=${COTURN_PUBLIC_IP} \
  --listening-port=3478 \
  --listening-ip=0.0.0.0 \
  --min-port=49160 --max-port=49200 \
  --verbose"]