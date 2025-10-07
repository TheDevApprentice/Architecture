# Stage 1: get mc binary from official image (fast and reliable)
FROM minio/mc:latest AS mc

# Stage 2: final MinIO image with built-in bucket initialization
FROM minio/minio:latest

# Copy the mc client from the mc image to avoid network downloads at build time
COPY --from=mc /usr/bin/mc /usr/local/bin/mc
RUN chmod +x /usr/local/bin/mc

# Copy entrypoint script
COPY init.sh /usr/local/bin/minio-entrypoint.sh
RUN chmod +x /usr/local/bin/minio-entrypoint.sh

# Default environment (can be overridden by compose)

ENV MINIO_ROOT_USER=${MINIO_ROOT_USER}
ENV MINIO_ROOT_PASSWORD=${MINIO_ROOT_PASSWORD}
# ENV S3_BUCKET=bucket
# ENV S3_BUCKETS=bucket1,bucket2
# ENV MINIO_PUBLIC=true


# Expose ports (documentary; compose will map)
EXPOSE 9000 9001

# Use our entrypoint to start minio then init bucket
ENTRYPOINT ["/usr/local/bin/minio-entrypoint.sh"]
CMD ["server", "/data", "--console-address", ":9001"]
