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
ENV S3_BUCKET=bucket

# Expose ports (documentary; compose will map)
EXPOSE 9000 9001

# Use our entrypoint to start minio then init bucket
ENTRYPOINT ["/usr/local/bin/minio-entrypoint.sh"]
CMD ["server", "/data", "--console-address", ":9001"]
