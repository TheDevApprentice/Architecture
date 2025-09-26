#!/bin/sh
set -euo pipefail

# Start MinIO server in background with passed args (default from CMD)
/usr/bin/minio "$@" &
MINIO_PID=$!

# Wait for MinIO to be responsive, then configure bucket
echo "[init] Waiting for MinIO to be ready..."
RETRIES=60
SLEEP=2

# Use mc to probe readiness by trying to set alias repeatedly
until /usr/local/bin/mc alias set local http://127.0.0.1:9000 "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}" >/dev/null 2>&1; do
  RETRIES=$((RETRIES-1)) || true
  if [ "$RETRIES" -le 0 ]; then
    echo "[init] MinIO not ready after waiting; exiting"
    exit 1
  fi
  sleep "$SLEEP"
done

echo "[init] MinIO is ready. Ensuring bucket exists and is public-read..."
# Create bucket if missing
/usr/local/bin/mc mb -p local/"${S3_BUCKET:-bucket}" || true
# Enable anonymous download (public-read)
/usr/local/bin/mc anonymous set download local/"${S3_BUCKET:-bucket}" || true

# Show buckets for visibility
/usr/local/bin/mc ls local || true

# Bring MinIO process to foreground
wait "$MINIO_PID"
