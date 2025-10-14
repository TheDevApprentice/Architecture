# Architecture Overview - Keycloak & Jenkins Automation

## 🏗️ Architecture globale

```
┌─────────────────────────────────────────────────────────────────┐
│                         Traefik (Reverse Proxy)                  │
│                    http://auth.localhost                         │
│                    http://jenkins.localhost                      │
└────────────┬────────────────────────────────┬───────────────────┘
             │                                │
             ▼                                ▼
    ┌────────────────┐              ┌─────────────────┐
    │   Keycloak     │◄─────────────┤    Jenkins      │
    │                │   OIDC Auth  │                 │
    │  Realms:       │              │  Pipelines:     │
    │  - master      │              │  - User Mgmt    │
    │  - internal    │              │  - Onboarding   │
    │                │              │  - Testing      │
    │  Clients:      │              │                 │
    │  - jenkins     │              │  Shared Lib:    │
    │  - jenkins-    │              │  - keycloakAuth │
    │    automation  │              │  - keycloakUser │
    └────────┬───────┘              └─────────┬───────┘
             │                                │
             ▼                                │
    ┌────────────────┐                       │
    │  PostgreSQL    │                       │
    │  (Keycloak DB) │                       │
    └────────────────┘                       │
                                             │
                                             ▼
                                    ┌─────────────────┐
                                    │  Webhook Trigger│
                                    │  (HR System,    │
                                    │   API, etc.)    │
                                    └─────────────────┘
```

## 🔐 Flux d'authentification

### 1. Utilisateur accède à Jenkins

```
User Browser
    │
    ├─► http://jenkins.localhost
    │
    ▼
Jenkins (détecte non-authentifié)
    │
    ├─► Redirect to Keycloak
    │   http://auth.localhost/realms/internal/protocol/openid-connect/auth
    │
    ▼
Keycloak Login Page (theme: jenkins)
    │
    ├─► User enters credentials
    │
    ▼
Keycloak validates & creates session
    │
    ├─► Redirect back to Jenkins with authorization code
    │
    ▼
Jenkins exchanges code for tokens
    │
    ├─► Access Token + ID Token + Refresh Token
    │
    ▼
Jenkins validates token & extracts claims
    │
    ├─► username, email, groups, roles
    │
    ▼
Jenkins creates session & applies authorization
    │
    ├─► Group "IT" → Admin permissions
    ├─► Group "Jenkins" → User permissions
    │
    ▼
User accesses Jenkins with appropriate permissions
```

## 🤖 Flux d'automatisation (User Management)

### Scénario 1: Création manuelle via Jenkins UI

```
Admin Jenkins
    │
    ├─► Ouvre pipeline "Keycloak-User-Management"
    │
    ├─► Remplit les paramètres (CREATE_USER)
    │
    ▼
Pipeline démarre
    │
    ├─► Stage: Get Access Token
    │   │
    │   ├─► keycloakAuth.getServiceAccountToken()
    │   │   │
    │   │   ├─► POST /realms/internal/protocol/openid-connect/token
    │   │   │   grant_type=client_credentials
    │   │   │   client_id=jenkins-automation
    │   │   │   client_secret=***
    │   │   │
    │   │   └─► Returns: access_token (JWT)
    │   │
    │   └─► Token stocké dans env.ACCESS_TOKEN
    │
    ├─► Stage: Execute Action (CREATE_USER)
    │   │
    │   ├─► keycloakUser.createUser()
    │   │   │
    │   │   ├─► Generate password (if not provided)
    │   │   │
    │   │   ├─► POST /admin/realms/internal/users
    │   │   │   Authorization: Bearer {access_token}
    │   │   │   Body: {username, email, firstName, ...}
    │   │   │
    │   │   ├─► Returns: HTTP 201 Created
    │   │   │
    │   │   ├─► GET /admin/realms/internal/users?username=X
    │   │   │   (to get user ID)
    │   │   │
    │   │   └─► Returns: user_id
    │   │
    │   └─► User created successfully
    │
    ├─► Stage: Add to Group (if specified)
    │   │
    │   ├─► keycloakUser.addUserToGroup()
    │   │   │
    │   │   ├─► GET /admin/realms/internal/groups
    │   │   │   (find group by name)
    │   │   │
    │   │   ├─► PUT /admin/realms/internal/users/{userId}/groups/{groupId}
    │   │   │
    │   │   └─► Returns: HTTP 204 No Content
    │   │
    │   └─► User added to group
    │
    └─► Pipeline completes
        │
        └─► Admin receives generated password in logs
```

### Scénario 2: Onboarding automatisé via Webhook

```
HR System / API
    │
    ├─► POST http://jenkins.localhost/generic-webhook-trigger/invoke
    │   ?token=employee-onboarding-secret-token
    │   
    │   Body: {
    │     "username": "jdoe",
    │     "email": "john.doe@company.com",
    │     "firstName": "John",
    │     "lastName": "Doe",
    │     "department": "IT",
    │     "role": "developer"
    │   }
    │
    ▼
Jenkins Generic Webhook Trigger
    │
    ├─► Parse JSON payload
    │
    ├─► Extract variables (username, email, etc.)
    │
    ├─► Trigger pipeline "Employee-Onboarding-Webhook"
    │
    ▼
Pipeline démarre
    │
    ├─► Stage: Parse Webhook Payload
    │   └─► Validate required fields
    │
    ├─► Stage: Determine Group Assignment
    │   │
    │   ├─► Map department to group
    │   │   IT → "IT" group
    │   │   Development → "Jenkins" group
    │   │
    │   └─► env.targetGroup = "IT"
    │
    ├─► Stage: Get Keycloak Access Token
    │   └─► (same as manual flow)
    │
    ├─► Stage: Check if User Exists
    │   │
    │   ├─► Try to get user ID
    │   │
    │   ├─► If found: env.userExists = 'true'
    │   └─► If not found: env.userExists = 'false'
    │
    ├─► Stage: Create User Account (if not exists)
    │   │
    │   ├─► Generate secure password
    │   │
    │   ├─► Create user in Keycloak
    │   │
    │   └─► Store password for email
    │
    ├─► Stage: Update Existing User (if exists)
    │   └─► Update user information
    │
    ├─► Stage: Assign to Group
    │   └─► Add user to determined group
    │
    ├─► Stage: Send Welcome Email
    │   │
    │   ├─► Prepare email with credentials
    │   │
    │   └─► Send via Email Extension plugin
    │
    └─► Stage: Notify HR
        └─► Send confirmation
```

## 🔑 Gestion des secrets et tokens

### Service Account Token Flow

```
Jenkins Pipeline
    │
    ├─► keycloakAuth.getServiceAccountToken()
    │
    ▼
Keycloak Token Endpoint
    │
    ├─► Validates client credentials
    │   (client_id + client_secret)
    │
    ├─► Checks client permissions
    │   (serviceAccountsEnabled: true)
    │
    ├─► Generates JWT access token
    │   │
    │   ├─► Header: {"alg": "RS256", "typ": "JWT"}
    │   │
    │   ├─► Payload: {
    │   │     "sub": "service-account-jenkins-automation",
    │   │     "azp": "jenkins-automation",
    │   │     "realm_access": {
    │   │       "roles": ["manage-users", "view-users", ...]
    │   │     },
    │   │     "exp": 1234567890,
    │   │     "iat": 1234567600
    │   │   }
    │   │
    │   └─► Signature: RS256(header + payload, private_key)
    │
    └─► Returns: {
          "access_token": "eyJhbGc...",
          "token_type": "Bearer",
          "expires_in": 300
        }
```

### Token Usage

```
Jenkins → Keycloak API
    │
    ├─► GET/POST/PUT/DELETE /admin/realms/{realm}/...
    │   Header: Authorization: Bearer eyJhbGc...
    │
    ▼
Keycloak API Gateway
    │
    ├─► Validates JWT signature
    │
    ├─► Checks token expiration
    │
    ├─► Verifies required permissions
    │   (e.g., manage-users for POST /users)
    │
    ├─► If valid: Process request
    │
    └─► If invalid: Return 401 Unauthorized
```

## 📊 Modèle de données

### Realm Internal - Structure

```
Realm: internal
│
├─► Users
│   ├─► hugo (IT group)
│   ├─► fred (Jenkins group)
│   ├─► test (no group)
│   └─► service-account-jenkins-automation
│
├─► Groups
│   ├─► IT
│   │   ├─► Realm Roles: config_service, view_service
│   │   └─► Client Roles (jenkins): admin
│   │
│   └─► Jenkins
│       ├─► Realm Roles: view_service
│       └─► Client Roles (jenkins): user
│
├─► Realm Roles
│   ├─► config_service
│   ├─► view_service
│   └─► manage-users (composite)
│
├─► Clients
│   ├─► jenkins (OIDC, standard flow)
│   │   ├─► Client ID: jenkins
│   │   ├─► Secret: pbVa7IZU...
│   │   ├─► Redirect URIs: http://jenkins.localhost/*
│   │   ├─► Protocol Mappers: groups
│   │   └─► Client Roles: admin, user
│   │
│   └─► jenkins-automation (Service Account)
│       ├─► Client ID: jenkins-automation
│       ├─► Secret: automation-secret...
│       ├─► Service Account: enabled
│       └─► Permissions: manage-users, view-users, ...
│
└─► Events (enabled)
    ├─► User Events: LOGIN, LOGOUT, UPDATE_PASSWORD, ...
    └─► Admin Events: CREATE_USER, UPDATE_USER, ...
```

## 🔒 Matrice de permissions

| Utilisateur/Groupe | Realm Roles | Jenkins Roles | Keycloak Admin | API Access |
|-------------------|-------------|---------------|----------------|------------|
| **hugo** (IT) | config_service, view_service | admin | ✅ | ✅ |
| **fred** (Jenkins) | view_service | user | ❌ | ❌ |
| **test** | - | - | ❌ | ❌ |
| **service-account-jenkins-automation** | - | - | manage-users, view-users, query-users, query-groups | ✅ |

## 📁 Structure des fichiers

```
Architecture/
│
├─── .env                                    # Variables d'environnement
│
├─── 15-docker-compose.Infra.dev.security.yml
│    └─── Keycloak + PostgreSQL
│
├─── 16-docker-compose.Infra.dev.cicd.yml
│    └─── Jenkins
│
├─── server/
│    │
│    ├─── Keycloak/
│    │    ├─── Dockerfile
│    │    ├─── config/
│    │    │    ├─── realms/
│    │    │    │    ├─── master.json        # Realm master (admin)
│    │    │    │    └─── internal.json      # Realm internal (services)
│    │    │    └─── themes/
│    │    │         ├─── master/
│    │    │         ├─── internal/
│    │    │         ├─── jenkins/
│    │    │         └─── minio/
│    │    └─── db/
│    │         └─── Dockerfile              # PostgreSQL pour Keycloak
│    │
│    └─── jenkins/
│         ├─── Dockerfile                   # Copie pipelines & shared-library
│         ├─── config/
│         │    ├─── jenkins.yaml            # JCasC configuration
│         │    ├─── plugins.txt             # Liste des plugins
│         │    ├─── entrypoint.sh           # Script de démarrage
│         │    ├─── README.md               # Doc configuration
│         │    │
│         │    ├─── pipelines/              # Copiés dans l'image Docker
│         │    │    ├─── keycloak-user-management.jenkinsfile
│         │    │    ├─── employee-onboarding-webhook.jenkinsfile
│         │    │    ├─── test-keycloak-integration.jenkinsfile
│         │    │    └─── README.md
│         │    │
│         │    └─── shared-library/         # Copiée dans l'image Docker
│         │         └─── vars/
│         │              ├─── keycloakAuth.groovy    # Fonctions auth
│         │              └─── keycloakUser.groovy    # Fonctions user mgmt
│         │
│         └─── data/                        # Volume persistant (runtime)
│
└─── DOCS/
     ├─── KEYCLOAK_SECURITY_CONFIG.md
     ├─── QUICK_START_JENKINS_KEYCLOAK.md
     └─── ARCHITECTURE_OVERVIEW.md (ce fichier)
```

## 🚀 Déploiement

### Ordre de démarrage

1. **Infrastructure de base** (Traefik, Registry)
   ```bash
   docker compose -f 11-docker-compose.Infra.dev.yml up -d
   ```

2. **Sécurité** (Keycloak + PostgreSQL)
   ```bash
   docker compose -f 15-docker-compose.Infra.dev.security.yml up -d
   ```
   - Attend que Keycloak soit prêt (~30s)
   - Import automatique des realms au démarrage

3. **CI/CD** (Jenkins)
   ```bash
   docker compose -f 16-docker-compose.Infra.dev.cicd.yml up -d
   ```
   - Jenkins démarre et se configure via JCasC
   - OIDC configuré automatiquement

### Vérification

```bash
# Keycloak
curl http://auth.localhost/realms/internal/.well-known/openid-configuration

# Jenkins
curl http://jenkins.localhost

# Test auth
# 1. Ouvrir http://jenkins.localhost
# 2. Redirection vers Keycloak
# 3. Login avec hugo/changeMe123!
# 4. Redirection vers Jenkins avec session
```

## 🔄 Cycle de vie d'un utilisateur

```
1. CRÉATION
   ├─► Via webhook (onboarding automatique)
   ├─► Via Jenkins UI (création manuelle)
   └─► Via Keycloak Admin Console (création directe)
   
2. ACTIVATION
   ├─► Email de bienvenue envoyé
   ├─► Mot de passe temporaire fourni
   └─► requiredActions: UPDATE_PASSWORD, UPDATE_PROFILE
   
3. PREMIÈRE CONNEXION
   ├─► User se connecte à Jenkins
   ├─► Redirection vers Keycloak
   ├─► Forcé de changer le mot de passe
   └─► Forcé de compléter le profil
   
4. UTILISATION
   ├─► Accès aux services selon les groupes
   ├─► Sessions SSO entre services
   └─► Refresh tokens pour sessions longues
   
5. MODIFICATION
   ├─► Update via Jenkins pipeline
   ├─► Self-service via Keycloak Account Console
   └─► Admin via Keycloak Admin Console
   
6. DÉSACTIVATION
   ├─► Update user: enabled = false
   ├─► Sessions révoquées
   └─► Accès bloqué immédiatement
   
7. SUPPRESSION
   ├─► Delete via Jenkins pipeline
   ├─► Soft delete (keep audit trail)
   └─► Hard delete (remove completely)
```

## 📈 Scalabilité et performance

### Keycloak
- **Database:** PostgreSQL dédié
- **Cache:** Infinispan (embedded)
- **Sessions:** Database-backed
- **Horizontal scaling:** Possible avec load balancer

### Jenkins
- **Agents:** Support pour agents distribués (port 50000)
- **Pipelines:** Exécution parallèle possible
- **Shared Library:** Chargée une fois, réutilisée

### Optimisations futures
- [ ] Keycloak clustering (HA)
- [ ] Redis pour cache Keycloak
- [ ] Jenkins agents Kubernetes
- [ ] CDN pour thèmes statiques

## 🛡️ Sécurité en profondeur

### Couche 1: Réseau
- Segmentation réseau (proxy, keycloaknet)
- Traefik comme point d'entrée unique
- Pas d'exposition directe des services

### Couche 2: Authentification
- OIDC/OAuth2 standard
- MFA supporté (TOTP)
- Brute force protection

### Couche 3: Autorisation
- RBAC via groupes et rôles
- Fine-grained permissions
- Service accounts avec permissions minimales

### Couche 4: Audit
- Tous les événements loggés
- Admin events détaillés
- Rétention 30 jours

### Couche 5: Secrets
- Credentials Jenkins
- Docker secrets (prod)
- Rotation régulière

## 📚 Références

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Jenkins Pipeline](https://www.jenkins.io/doc/book/pipeline/)
- [OIDC Specification](https://openid.net/connect/)
- [OAuth 2.0](https://oauth.net/2/)
