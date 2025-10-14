#!/bin/sh
set -eu

# Optional debug
# set -x

# Inputs expected (provide via compose):
#   KC_URL_INTERNAL            e.g. ${KC_URL_INTERNAL}
#   KC_REALM          e.g. internal
#   KC_ADMIN_USER     e.g. ${KC_BOOTSTRAP_ADMIN_USERNAME}
#   KC_ADMIN_PASSWORD e.g. ${KC_BOOTSTRAP_ADMIN_PASSWORD}
#   OIDC_CLIENT_ID    e.g. jenkins
# If OIDC_CLIENT_SECRET is already set, we skip fetching.

fetch_secret() {
  echo "[entrypoint] OIDC_CLIENT_SECRET not provided; fetching from Keycloak..."

  : "${KC_URL_INTERNAL:?KC_URL_INTERNAL is required}"
  : "${KC_REALM:?KC_REALM is required}"
  : "${KC_ADMIN_USER:?KC_ADMIN_USER is required}"
  : "${KC_ADMIN_PASSWORD:?KC_ADMIN_PASSWORD is required}"
  : "${OIDC_CLIENT_ID:?OIDC_CLIENT_ID is required}"

  # Wait for Keycloak to be reachable
  ATTEMPTS=60
  until curl -fsS "http://${KC_URL_INTERNAL}/realms/${KC_REALM}/.well-known/openid-configuration" > /dev/null 2>&1 || [ $ATTEMPTS -le 0 ]; do
    echo "[entrypoint] Waiting for Keycloak at http://${KC_URL_INTERNAL}/realms/${KC_REALM}/.well-known/openid-configuration (${ATTEMPTS})..."
    ATTEMPTS=$((ATTEMPTS - 1))
    sleep 2
  done
  if [ $ATTEMPTS -le 0 ]; then
    echo "[entrypoint] Keycloak not reachable; cannot fetch client secret" >&2
    return 1
  fi

  # Get admin access token (master realm)
  ACCESS_TOKEN=$(curl -fsS \
    -d "grant_type=password" \
    -d "client_id=admin-cli" \
    -d "username=${KC_ADMIN_USER}" \
    -d "password=${KC_ADMIN_PASSWORD}" \
    "http://${KC_URL_INTERNAL}/realms/master/protocol/openid-connect/token" | jq -r .access_token)

  if [ -z "${ACCESS_TOKEN}" ] || [ "${ACCESS_TOKEN}" = "null" ]; then
    echo "[entrypoint] Failed to obtain admin access token" >&2
    return 1
  fi

  # Resolve client internal ID
  CLIENT_ID=$(curl -fsS -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "http://${KC_URL_INTERNAL}/admin/realms/${KC_REALM}/clients?clientId=${OIDC_CLIENT_ID}" | jq -r '.[0].id')

  if [ -z "${CLIENT_ID}" ] || [ "${CLIENT_ID}" = "null" ]; then
    echo "[entrypoint] Client ${OIDC_CLIENT_ID} not found in realm ${KC_REALM}" >&2
    return 1
  fi

  # Fetch secret
  SECRET=$(curl -fsS -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "http://${KC_URL_INTERNAL}/admin/realms/${KC_REALM}/clients/${CLIENT_ID}/client-secret" | jq -r .value)

  if [ -z "${SECRET}" ] || [ "${SECRET}" = "null" ]; then
    echo "[entrypoint] Failed to retrieve client secret for ${OIDC_CLIENT_ID}" >&2
    return 1
  fi

  export OIDC_CLIENT_SECRET="${SECRET}"
  echo "[entrypoint] Retrieved OIDC client secret for ${OIDC_CLIENT_ID}."
}

# Fetch OIDC client secret if not provided
if [ -z "${OIDC_CLIENT_SECRET}" ]; then
  echo "[entrypoint] OIDC_CLIENT_SECRET not set, fetching from Keycloak..."
  fetch_secret || {
    echo "[entrypoint] WARNING: Failed to fetch OIDC client secret. Jenkins may not be able to authenticate with Keycloak." >&2
  }
else
  echo "[entrypoint] OIDC_CLIENT_SECRET already set."
fi

echo "[entrypoint] Starting Jenkins..."
exec /usr/local/bin/jenkins.sh