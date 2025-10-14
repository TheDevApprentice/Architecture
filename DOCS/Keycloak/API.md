# 🔧 API REST Admin Keycloak

## 📋 Table des Matières

- [Authentification](#authentification)
- [User Management](#user-management)
- [Groups](#groups)
- [Roles](#roles)
- [Clients](#clients)

---

## Authentification

### Get Admin Token (Master Realm)

```bash
curl -X POST "http://keycloak.local/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=<admin-password>" \
  -d "grant_type=password"
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cC...",
  "expires_in": 60,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cC...",
  "token_type": "Bearer"
}
```

### Get Service Account Token

```bash
curl -X POST "http://keycloak.local/realms/internal/protocol/openid-connect/token" \
  -d "client_id=jenkins-automation" \
  -d "client_secret=<client-secret>" \
  -d "grant_type=client_credentials"
```

---

## User Management

### List Users

```bash
curl -H "Authorization: Bearer <token>" \
  "http://keycloak.local/admin/realms/internal/users?max=100"
```

**Response:**
```json
[
  {
    "id": "f3a8...",
    "username": "jdoe",
    "email": "john.doe@company.com",
    "firstName": "John",
    "lastName": "Doe",
    "enabled": true,
    "emailVerified": true
  }
]
```

### Get User by Username

```bash
curl -H "Authorization: Bearer <token>" \
  "http://keycloak.local/admin/realms/internal/users?username=jdoe&exact=true"
```

### Create User

```bash
curl -X POST \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jdoe",
    "email": "john.doe@company.com",
    "firstName": "John",
    "lastName": "Doe",
    "enabled": true,
    "emailVerified": false,
    "credentials": [{
      "type": "password",
      "value": "SecurePass123!",
      "temporary": true
    }],
    "requiredActions": ["UPDATE_PASSWORD"],
    "attributes": {
      "locale": ["en"]
    }
  }' \
  "http://keycloak.local/admin/realms/internal/users"
```

**Response:** `HTTP 201 Created`
**Location header:** `/admin/realms/internal/users/{user-id}`

### Update User

```bash
curl -X PUT \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@newcompany.com",
    "firstName": "Jonathan",
    "emailVerified": true
  }' \
  "http://keycloak.local/admin/realms/internal/users/{user-id}"
```

### Delete User

```bash
curl -X DELETE \
  -H "Authorization: Bearer <token>" \
  "http://keycloak.local/admin/realms/internal/users/{user-id}"
```

### Reset Password

```bash
curl -X PUT \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "password",
    "value": "NewPassword123!",
    "temporary": false
  }' \
  "http://keycloak.local/admin/realms/internal/users/{user-id}/reset-password"
```

### Send Email Actions

```bash
# Send password reset email
curl -X PUT \
  -H "Authorization: Bearer <token>" \
  "http://keycloak.local/admin/realms/internal/users/{user-id}/execute-actions-email" \
  -H "Content-Type: application/json" \
  -d '["UPDATE_PASSWORD"]'
```

---

## Groups

### List Groups

```bash
curl -H "Authorization: Bearer <token>" \
  "http://keycloak.local/admin/realms/internal/groups"
```

**Response:**
```json
[
  {
    "id": "abc123",
    "name": "IT",
    "path": "/IT"
  },
  {
    "id": "def456",
    "name": "Jenkins",
    "path": "/Jenkins"
  }
]
```

### Get Group by Name

```bash
# List and filter by name
curl -H "Authorization: Bearer <token>" \
  "http://keycloak.local/admin/realms/internal/groups" | jq '.[] | select(.name=="IT")'
```

### Add User to Group

```bash
curl -X PUT \
  -H "Authorization: Bearer <token>" \
  "http://keycloak.local/admin/realms/internal/users/{user-id}/groups/{group-id}"
```

### Remove User from Group

```bash
curl -X DELETE \
  -H "Authorization: Bearer <token>" \
  "http://keycloak.local/admin/realms/internal/users/{user-id}/groups/{group-id}"
```

### Get User Groups

```bash
curl -H "Authorization: Bearer <token>" \
  "http://keycloak.local/admin/realms/internal/users/{user-id}/groups"
```

---

## Roles

### List Realm Roles

```bash
curl -H "Authorization: Bearer <token>" \
  "http://keycloak.local/admin/realms/internal/roles"
```

### List Client Roles

```bash
curl -H "Authorization: Bearer <token>" \
  "http://keycloak.local/admin/realms/internal/clients/{client-id}/roles"
```

### Add Realm Role to User

```bash
curl -X POST \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "id": "role-id",
      "name": "config_service"
    }
  ]' \
  "http://keycloak.local/admin/realms/internal/users/{user-id}/role-mappings/realm"
```

---

## Clients

### List Clients

```bash
curl -H "Authorization: Bearer <token>" \
  "http://keycloak.local/admin/realms/internal/clients"
```

### Get Client by clientId

```bash
curl -H "Authorization: Bearer <token>" \
  "http://keycloak.local/admin/realms/internal/clients?clientId=jenkins"
```

### Get Client Secret

```bash
curl -H "Authorization: Bearer <token>" \
  "http://keycloak.local/admin/realms/internal/clients/{client-uuid}/client-secret"
```

**Response:**
```json
{
  "type": "secret",
  "value": "abc123def456..."
}
```

### Regenerate Client Secret

```bash
curl -X POST \
  -H "Authorization: Bearer <token>" \
  "http://keycloak.local/admin/realms/internal/clients/{client-uuid}/client-secret"
```

---

## Exemples Complets

### Script: Create User + Add to Group

```bash
#!/bin/bash
KC_URL="http://keycloak.local"
REALM="internal"

# 1. Get admin token
TOKEN=$(curl -s -X POST "${KC_URL}/realms/master/protocol/openid-connect/token" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=${ADMIN_PASSWORD}" \
  -d "grant_type=password" | jq -r .access_token)

# 2. Create user
LOCATION=$(curl -s -i -X POST \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jdoe",
    "email": "john.doe@company.com",
    "enabled": true,
    "credentials": [{"type":"password","value":"Pass123!","temporary":true}]
  }' \
  "${KC_URL}/admin/realms/${REALM}/users" | grep -i location | awk '{print $2}' | tr -d '\r')

USER_ID=$(basename ${LOCATION})
echo "Created user ID: ${USER_ID}"

# 3. Get group ID
GROUP_ID=$(curl -s -H "Authorization: Bearer ${TOKEN}" \
  "${KC_URL}/admin/realms/${REALM}/groups" | jq -r '.[] | select(.name=="Jenkins") | .id')

# 4. Add user to group
curl -X PUT \
  -H "Authorization: Bearer ${TOKEN}" \
  "${KC_URL}/admin/realms/${REALM}/users/${USER_ID}/groups/${GROUP_ID}"

echo "User added to Jenkins group"
```

### Script: Bulk Password Reset

```bash
#!/bin/bash
USERS=("user1" "user2" "user3")

for USERNAME in "${USERS[@]}"; do
  # Get user ID
  USER_ID=$(curl -s -H "Authorization: Bearer ${TOKEN}" \
    "${KC_URL}/admin/realms/${REALM}/users?username=${USERNAME}&exact=true" | jq -r '.[0].id')
  
  # Send reset email
  curl -X PUT \
    -H "Authorization: Bearer ${TOKEN}" \
    "${KC_URL}/admin/realms/${REALM}/users/${USER_ID}/execute-actions-email" \
    -H "Content-Type: application/json" \
    -d '["UPDATE_PASSWORD"]'
  
  echo "Reset email sent to ${USERNAME}"
done
```

---

## Rate Limiting

⚠️ **Attention:** L'API Keycloak peut être rate-limitée. Respecter:

- Max 100 requêtes/minute par IP
- Utiliser pagination pour listes volumineuses
- Mettre en cache les tokens (durée: 60s)

---

## Pagination

```bash
# Page 1 (0-99)
curl "http://keycloak.local/admin/realms/internal/users?first=0&max=100"

# Page 2 (100-199)
curl "http://keycloak.local/admin/realms/internal/users?first=100&max=100"
```

---

**⬅️ Retour au [README](./README.md)**
