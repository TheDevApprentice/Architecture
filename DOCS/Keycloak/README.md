# 🔐 Keycloak - Documentation Complète

## 📋 Sommaire

1. [Vue d'ensemble](#-vue-densemble)
2. [Configuration des Realms](#-configuration-des-realms)
3. [Clients et Intégrations](#-clients-et-intégrations)
4. [Groupes et Permissions](#-groupes-et-permissions)
5. [Thèmes Personnalisés](#-thèmes-personnalisés)
6. [Déploiement](#-déploiement)
7. [API REST Admin](#-api-rest-admin)
8. [Sécurité](#-sécurité)
9. [Dépannage](#-dépannage)

---

## 🎯 Vue d'ensemble

Keycloak est la solution **Identity and Access Management (IAM)** de l'infrastructure, fournissant:

- **Single Sign-On (SSO)** - Authentification unique pour tous les services
- **OpenID Connect** - Standard moderne d'authentification
- **OAuth 2.0** - Autorisation déléguée
- **Gestion centralisée** - Utilisateurs, groupes, rôles
- **Thèmes personnalisés** - Interface brandée par service

### 🏗️ Architecture

```
┌────────────────────────────────────────────┐
│         Keycloak Container                 │
├────────────────────────────────────────────┤
│                                            │
│  ┌──────────────────────────────────────┐ │
│  │  Keycloak Server (Quarkus)           │ │
│  │  Port 8080                           │ │
│  └──────────────────────────────────────┘ │
│                                            │
│  ┌──────────────────────────────────────┐ │
│  │  Realms Configuration                │ │
│  │  ├─> master  (admin realm)           │ │
│  │  └─> internal (app realm)            │ │
│  └──────────────────────────────────────┘ │
│                                            │
│  ┌──────────────────────────────────────┐ │
│  │  Custom Themes                       │ │
│  │  ├─> internal  (default)             │ │
│  │  ├─> jenkins   (CI/CD)               │ │
│  │  ├─> minio     (storage)             │ │
│  │  └─> master    (admin)               │ │
│  └──────────────────────────────────────┘ │
│                                            │
└─────────────────┬──────────────────────────┘
                  │
                  │ PostgreSQL Protocol
                  ▼
┌────────────────────────────────────────────┐
│      PostgreSQL Database                   │
│      (Dedicated Keycloak DB)               │
│      Network: keycloaknet                  │
└────────────────────────────────────────────┘
```

### 🔑 Fonctionnalités Clés

- **✅ Import automatique des realms** - Configuration as Code
- **🎨 Thèmes personnalisés** - 4 thèmes brandés
- **🔐 Brute force protection** - Protection contre les attaques
- **🌍 Multilingue** - Support EN/FR
- **📧 SMTP configuré** - Emails de reset password
- **⏱️ Token lifecycle** - Configuration fine des durées
- **🔄 Remember Me** - Sessions persistantes

---

## 📂 Structure de la Documentation

### 📄 Fichiers de Documentation

| Fichier | Description |
|---------|-------------|
| **[CONFIGURATION.md](./CONFIGURATION.md)** | Configuration des realms, clients, groupes, roles |
| **[DEPLOYMENT.md](./DEPLOYMENT.md)** | Guide de déploiement et prérequis |
| **[THEMES.md](./THEMES.md)** | Documentation des thèmes personnalisés |
| **[API.md](./API.md)** | API REST Admin et exemples d'utilisation |
| **[SECURITY.md](./SECURITY.md)** | Configuration de sécurité et meilleures pratiques |
| **[TROUBLESHOOTING.md](./TROUBLESHOOTING.md)** | Solutions aux problèmes courants |

---

## 🏢 Configuration des Realms

### Realms Disponibles

#### 1️⃣ Master Realm
- **Objectif:** Administration Keycloak
- **Accès:** Administrateurs uniquement
- **Users:** Admin bootstrap
- **Thème:** `master`

#### 2️⃣ Internal Realm
- **Objectif:** Applications internes SSO
- **Accès:** Utilisateurs de l'organisation
- **Clients:** Jenkins, MinIO, Grafana, etc.
- **Groupes:** IT, Jenkins
- **Thème:** `internal` (défaut), personnalisé par client

### Configuration Realm Internal

```json
{
  "realm": "internal",
  "enabled": true,
  "displayName": "Internal",
  "displayNameHtml": "Internal <small>SSO</small>",
  
  "loginTheme": "internal",
  "accountTheme": "keycloak.v3",
  "adminTheme": "keycloak.v2",
  
  "internationalizationEnabled": true,
  "supportedLocales": ["en", "fr"],
  "defaultLocale": "en",
  
  "bruteForceProtected": true,
  "failureFactor": 5,
  "maxFailureWaitSeconds": 900,
  
  "accessTokenLifespan": 300,
  "ssoSessionIdleTimeout": 1800,
  "ssoSessionMaxLifespan": 36000
}
```

**Voir [CONFIGURATION.md](./CONFIGURATION.md) pour les détails complets**

---

## 🔌 Clients et Intégrations

### Clients Préconfigurés

| Client | Type | Protocole | Service Cible | Thème |
|--------|------|-----------|---------------|-------|
| **jenkins** | Confidential | OIDC | Jenkins CI/CD | `jenkins` |
| **jenkins-automation** | Service Account | OIDC | Jenkins Automation | - |
| **minio** | Confidential | OIDC | MinIO Storage | `minio` |
| **grafana** | Public | OIDC | Grafana Monitoring | `internal` |

### Configuration Client Exemple (Jenkins)

```json
{
  "clientId": "jenkins",
  "secret": "${KC_SECRET_JENKINS}",
  "protocol": "openid-connect",
  "publicClient": false,
  "standardFlowEnabled": true,
  "serviceAccountsEnabled": false,
  "redirectUris": [
    "http://${JENKINS_URL}/*",
    "http://${JENKINS_URL}/securityRealm/finishLogin"
  ],
  "webOrigins": ["http://${JENKINS_URL}"],
  "attributes": {
    "login_theme": "jenkins",
    "pkce.code.challenge.method": "S256"
  }
}
```

**Voir [CONFIGURATION.md](./CONFIGURATION.md) pour tous les clients**

---

## 👥 Groupes et Permissions

### Groupes Définis

#### Groupe `IT`
```json
{
  "name": "IT",
  "realmRoles": ["config_service", "view_service"],
  "clientRoles": {
    "jenkins": ["admin"]
  }
}
```

**Permissions:**
- Administration complète des services
- Configuration système
- Accès Jenkins admin

#### Groupe `Jenkins`
```json
{
  "name": "Jenkins",
  "realmRoles": ["view_service"],
  "clientRoles": {
    "jenkins": ["user"]
  }
}
```

**Permissions:**
- Lecture des services
- Accès Jenkins standard
- Build/Deploy jobs

### Realm Roles

| Role | Description | Groupe |
|------|-------------|--------|
| **config_service** | Configure internal services | IT |
| **view_service** | View-only internal services | IT, Jenkins |

**Voir [CONFIGURATION.md](./CONFIGURATION.md) pour le système de permissions complet**

---

## 🎨 Thèmes Personnalisés

### Thèmes Disponibles

```
themes/
├── internal/         # Thème par défaut realm internal
│   └── login/
├── jenkins/          # Thème spécifique Jenkins
│   └── login/
├── minio/            # Thème spécifique MinIO
│   └── login/
└── master/           # Thème admin realm master
    └── login/
```

### Personnalisation

Chaque thème peut avoir:
- **Logos** - Branding personnalisé
- **Couleurs** - Palette de l'application
- **Templates** - Layouts personnalisés
- **Messages** - Traductions FR/EN

**Exemple d'utilisation:**
```json
{
  "clientId": "jenkins",
  "attributes": {
    "login_theme": "jenkins"
  }
}
```

**Voir [THEMES.md](./THEMES.md) pour la documentation complète**

---

## 🚀 Déploiement

### Prérequis

- Docker + Docker Compose
- PostgreSQL Database
- Variables d'environnement configurées

### Configuration Docker Compose

```yaml
services:
  keycloak-db:
    build: ./server/Keycloak/db
    environment:
      - POSTGRES_DB=keycloak
      - POSTGRES_USER=keycloak
      - POSTGRES_PASSWORD=${KC_DB_PASSWORD}
    networks:
      - keycloaknet
  
  keycloak:
    build: ./server/Keycloak
    environment:
      - KC_DB=postgres
      - KC_DB_URL=jdbc:postgresql://keycloak-db:5432/keycloak
      - KC_DB_USERNAME=keycloak
      - KC_DB_PASSWORD=${KC_DB_PASSWORD}
      - KC_HOSTNAME=${KC_HOSTNAME}
      - KEYCLOAK_ADMIN=${KC_BOOTSTRAP_ADMIN_USERNAME}
      - KEYCLOAK_ADMIN_PASSWORD=${KC_BOOTSTRAP_ADMIN_PASSWORD}
    ports:
      - "8080:8080"
    networks:
      - proxy
      - keycloaknet
    depends_on:
      - keycloak-db
```

### Variables d'Environnement

```bash
# Database
KC_DB_PASSWORD=<secure-password>

# Keycloak Admin
KC_BOOTSTRAP_ADMIN_USERNAME=admin
KC_BOOTSTRAP_ADMIN_PASSWORD=<admin-password>

# Hostname
KC_HOSTNAME=keycloak.local

# Client Secrets (generated)
KC_SECRET_JENKINS=<jenkins-secret>
KC_SECRET_JENKINS_AUTOMATION=<automation-secret>
KC_SECRET_MINIO=<minio-secret>
```

**Voir [DEPLOYMENT.md](./DEPLOYMENT.md) pour le guide complet**

---

## 🔧 API REST Admin

### Endpoints Principaux

#### Authentication
```bash
# Get Admin Token
curl -X POST "http://keycloak.local/realms/master/protocol/openid-connect/token" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=<password>" \
  -d "grant_type=password"
```

#### User Management
```bash
# List Users
curl -H "Authorization: Bearer <token>" \
  "http://keycloak.local/admin/realms/internal/users"

# Create User
curl -X POST \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"username":"jdoe","email":"jdoe@company.com","enabled":true}' \
  "http://keycloak.local/admin/realms/internal/users"
```

#### Groups
```bash
# List Groups
curl -H "Authorization: Bearer <token>" \
  "http://keycloak.local/admin/realms/internal/groups"
```

**Voir [API.md](./API.md) pour la documentation API complète**

---

## 🔐 Sécurité

### Fonctionnalités de Sécurité

#### Brute Force Protection
```json
{
  "bruteForceProtected": true,
  "failureFactor": 5,
  "maxFailureWaitSeconds": 900,
  "quickLoginCheckMilliSeconds": 1000
}
```

- **5 tentatives** avant blocage temporaire
- **15 minutes** de blocage après échec
- **Protection automatique** contre les attaques

#### Token Lifecycle
```json
{
  "accessTokenLifespan": 300,           // 5 minutes
  "ssoSessionIdleTimeout": 1800,        // 30 minutes
  "ssoSessionMaxLifespan": 36000        // 10 heures
}
```

#### Password Policy (Configurable)
- Longueur minimum: 12 caractères
- Au moins 1 majuscule
- Au moins 1 minuscule
- Au moins 1 chiffre
- Au moins 1 caractère spécial

**Voir [SECURITY.md](./SECURITY.md) pour les meilleures pratiques**

---

## 🛠️ Dépannage

### Problèmes Courants

#### 1. Keycloak ne démarre pas

```bash
# Vérifier logs
docker logs keycloak

# Vérifier DB
docker exec keycloak-db psql -U keycloak -c "\l"
```

#### 2. Erreur "Database not ready"

```bash
# Vérifier connexion DB
docker exec keycloak-db pg_isready

# Restart Keycloak
docker-compose restart keycloak
```

#### 3. Import de realm échoue

```bash
# Vérifier syntaxe JSON
docker exec keycloak cat /opt/keycloak/data/import/02-internal.json | jq

# Forcer re-import
docker-compose down keycloak
docker-compose up -d keycloak
```

**Voir [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) pour plus de solutions**

---

## 📊 Monitoring

### Health Checks

```bash
# Keycloak health
curl http://keycloak.local/health

# Realm discovery
curl http://keycloak.local/realms/internal/.well-known/openid-configuration
```

### Métriques

- **Active Sessions** - Via Admin Console
- **Login Rate** - Events → Login
- **Failed Logins** - Events → Login Error
- **Token Issuance** - Metrics endpoint

---

## 🔄 Workflow Typique

### Authentification SSO

```
1. User → Service (ex: Jenkins)
   └─> Redirect to Keycloak

2. User → Keycloak Login Page
   └─> Enter credentials
   └─> Theme personnalisé (ex: jenkins theme)

3. Keycloak → Validate credentials
   └─> Check brute force protection
   └─> Generate tokens (ID + Access)

4. Keycloak → Redirect to Service
   └─> With authorization code

5. Service → Exchange code for tokens
   └─> Validate JWT
   └─> Extract user info + groups

6. Service → Create session
   └─> User authenticated ✅
```

### Gestion Utilisateur via API

```
1. Get Admin Token
   └─> POST /realms/master/protocol/openid-connect/token

2. Create User
   └─> POST /admin/realms/internal/users

3. Add to Group
   └─> PUT /admin/realms/internal/users/{id}/groups/{groupId}

4. Set Password
   └─> PUT /admin/realms/internal/users/{id}/reset-password
```

---

## 📚 Ressources

### Documentation Officielle
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Admin REST API](https://www.keycloak.org/docs-api/latest/rest-api/)
- [Server Administration Guide](https://www.keycloak.org/docs/latest/server_admin/)

### Liens Internes
- [Architecture Globale](../ARCHITECTURE.md)
- [Documentation Jenkins](../Jenkins/README.md)
- [Documentation Traefik](../Traefik/README.md)

---

## 🤝 Contribution

### Structure des Fichiers

```
server/Keycloak/
├── Dockerfile                    # Image Keycloak
├── config/
│   ├── realms/                   # Realm exports
│   │   ├── master.json
│   │   └── internal.json
│   └── themes/                   # Thèmes personnalisés
│       ├── internal/
│       ├── jenkins/
│       ├── minio/
│       └── master/
└── db/
    ├── Dockerfile                # PostgreSQL dédié
    └── initdb/                   # Scripts d'initialisation
        ├── 001-init.sql
        └── 002-extensions.sql
```

---

## 📝 Changelog

### Version 0.1.0 (Initial Release)

- ✅ 2 Realms (master + internal)
- ✅ 4 Clients préconfigurés (Jenkins, Jenkins-automation, MinIO, Grafana)
- ✅ 2 Groupes (IT, Jenkins)
- ✅ 4 Thèmes personnalisés
- ✅ Brute force protection
- ✅ Import automatique realms
- ✅ PostgreSQL dédié
- ✅ Multilingue (EN/FR)

---

**📖 Pour commencer, consultez [DEPLOYMENT.md](./DEPLOYMENT.md)**
