# Keycloak Service Account Permissions

## jenkins-automation Service Account

The `jenkins-automation` service account requires the following roles from the `realm-management` client to execute all test pipelines:

### Currently Assigned ✅
- `manage-users` - Create, update, delete users
- `view-users` - List and view user details
- `view-clients` - List and view client details
- `query-clients` - Query client information
- `query-groups` - Query group information
- `query-users` - Query user information

### Missing (Required for Client Management) ❌
- `manage-clients` - Create, update, delete clients

## How to Add Missing Permissions

1. Login to Keycloak Admin Console
2. Navigate to: **Realm Settings → Clients → jenkins-automation**
3. Go to **Service Account Roles** tab
4. Select **realm-management** from the **Client Roles** dropdown
5. Assign the following **Available Roles**:
   - ✅ `manage-clients`

## Test Pipeline Requirements

| Pipeline | Required Roles |
|----------|---------------|
| `Test-Keycloak-User-Management` | `manage-users`, `view-users`, `query-users` |
| `Test-Keycloak-Group-Management` | `manage-users`, `view-users`, `query-groups`, `query-users` |
| `Test-Keycloak-Client-Management` | `manage-clients`, `view-clients`, `query-clients` |
| `Test-Keycloak-Session-Management` | `manage-users`, `view-users`, `view-clients`, `query-clients` |

## Verification

After adding permissions, verify by running:
```bash
# Get service account token
curl -X POST http://keycloak:8080/realms/internal/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=jenkins-automation" \
  -d "client_secret=YOUR_SECRET"

# Decode the token to verify roles
# Check the "resource_access.realm-management.roles" array
```
