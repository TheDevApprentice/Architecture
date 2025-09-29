FROM postgres:16

# Install Patroni and dependencies (Debian-based for wider wheel availability)
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
       python3 \
       python3-pip \
       curl \
       bash \
       build-essential \
       libpq-dev \
       python3-dev \
    && pip3 install --no-cache-dir --break-system-packages \
       psycopg2-binary>=2.9 \
       "patroni[etcd3]" \
    && apt-get purge -y build-essential python3-dev \
    && apt-get autoremove -y \
    && rm -rf /var/lib/apt/lists/*

# Create directories
RUN mkdir -p /var/lib/postgresql/data \
    && chown -R postgres:postgres /var/lib/postgresql

# Copy configuration
COPY patroni.yml /etc/patroni.yml
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

USER postgres
WORKDIR /var/lib/postgresql

EXPOSE 5432 8008

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["patroni", "/etc/patroni.yml"]