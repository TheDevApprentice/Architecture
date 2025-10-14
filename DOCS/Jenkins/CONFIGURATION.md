# ⚙️ Configuration Jenkins - JCasC et Variables

## 📋 Table des Matières

- [Configuration as Code (JCasC)](#configuration-as-code-jcasc)
- [Variables d'Environnement](#variables-denvironnement)
- [Configuration OIDC](#configuration-oidc)
- [Autorisation Matrix](#autorisation-matrix)
- [Scripts d'Initialisation](#scripts-dinitialisation)
- [Plugins](#plugins)

---

## Configuration as Code (JCasC)

Jenkins est configuré via le fichier `jenkins.yaml` utilisant le plugin **Configuration as Code**.

### 📍 Emplacement

```
server/Jenkins/config/jenkins.yaml
```

### 📄 Structure Complète

```yaml
jenkins:
  systemMessage: "Configured by JCasC dev - Welcome to Jenkins"
  
  securityRealm:
    oic:
      # Credentials
      clientId: "${OIDC_CLIENT_ID}"
      clientSecret: "${OIDC_CLIENT_SECRET}"
      
      # Claims mapping
      userIdStrategy: "caseInsensitive"
      userNameField: "preferred_username"
      groupsFieldName: "groups"
      groupIdStrategy: "caseInsensitive"
      fullNameFieldName: "name"
      emailFieldName: "email"
      
      # Logout behavior
      logoutFromOpenidProvider: true
      rootURLFromRequest: true
      sendScopesInTokenRequest: true
      postLogoutRedirectUrl: "http://${JENKINS_URL}"
      
      # Security (Dev only - HTTP)
      disableSslVerification: true
      
      # PKCE support
      properties:
        - "pkce"
      
      # OIDC discovery
      serverConfiguration:
        wellKnown:
          wellKnownOpenIDConfigurationUrl: "http://${KC_URL}/realms/${KC_REALM}/.well-known/openid-configuration"
          scopesOverride: "openid profile email"
  
  authorizationStrategy:
    globalMatrix:
      entries:
        # Groupe IT = Admin complet
        - group:
            name: IT
            permissions:
              - Overall/Administer
        
        # Groupe Jenkins = Permissions standard
        - group:
            name: Jenkins
            permissions:
              - Overall/Read
              - Agent/Connect
              - Agent/Create
              - Agent/Configure
              - Agent/Disconnect
              - Agent/Delete
              - Job/Discover
              - Job/Read
              - Job/Create
              - Job/Configure
              - Job/Build
              - Job/Move
              - Job/Delete
              - Run/Delete
              - Run/Replay
              - View/Read
              - View/Create
              - View/Configure
              - View/Delete

unclassified:
  location:
    adminAddress: "l'adresse n'est pas encore configurée <nobody@nowhere>"
    url: "http://${JENKINS_URL}"
```

---

## Variables d'Environnement

### 🔑 Variables Obligatoires

| Variable | Description | Exemple | Source |
|----------|-------------|---------|--------|
| **JENKINS_URL** | URL publique Jenkins | `jenkins.local` | Docker Compose |
| **KC_URL_INTERNAL** | URL interne Keycloak | `keycloak:8080` | Docker Compose |
| **KC_URL** | URL externe Keycloak (OIDC discovery) | `keycloak.local` | Docker Compose |
| **KC_REALM** | Realm Keycloak | `internal` | Docker Compose |
| **OIDC_CLIENT_ID** | Client ID pour SSO | `jenkins` | Docker Compose |
| **OIDC_CLIENT_SECRET** | Client Secret (auto-fetch) | `***` | Entrypoint script |

### 🤖 Variables Automation

| Variable | Description | Exemple |
|----------|-------------|---------|
| **KC_CLIENT_ID_JENKINS_AUTOMATION** | Client ID pour automation | `jenkins-automation` |
| **KC_SECRET_JENKINS_AUTOMATION** | Client Secret automation | `***` |

### 📝 Configuration Docker Compose

```yaml
services:
  jenkins:
    image: jenkins-custom:latest
    environment:
      # URLs
      - JENKINS_URL=jenkins.local
      - KC_URL_INTERNAL=keycloak:8080
      - KC_URL=keycloak.local
      
      # Realm
      - KC_REALM=internal
      
      # OIDC Client (SSO)
      - OIDC_CLIENT_ID=jenkins
      # OIDC_CLIENT_SECRET auto-récupéré via entrypoint.sh
      
      # Automation Client
      - KC_CLIENT_ID_JENKINS_AUTOMATION=jenkins-automation
      - KC_SECRET_JENKINS_AUTOMATION=${KC_SECRET_JENKINS_AUTOMATION}
      
      # Keycloak Admin (pour entrypoint)
      - KC_ADMIN_USER=${KC_BOOTSTRAP_ADMIN_USERNAME}
      - KC_ADMIN_PASSWORD=${KC_BOOTSTRAP_ADMIN_PASSWORD}
      
      # Java options
      - JAVA_OPTS=-Djenkins.install.runSetupWizard=false
      - TZ=Europe/Paris
```

---

## Configuration OIDC

### 🔐 Security Realm

Jenkins utilise le plugin **OpenID Connect Authentication** pour l'authentification SSO via Keycloak.

#### Configuration Keycloak Requise

**1. Créer le Client `jenkins`:**

```bash
# Dans l'interface Keycloak Admin
Realm: internal
Client ID: jenkins
Client Protocol: openid-connect
Access Type: confidential
Valid Redirect URIs: 
  - http://jenkins.local:8080/*
  - http://jenkins.local:8080/securityRealm/finishLogin
Web Origins: http://jenkins.local:8080
```

**2. Configurer les Mappers:**

```
Mapper: groups
Mapper Type: Group Membership
Token Claim Name: groups
Full group path: OFF
Add to ID token: ON
Add to access token: ON
Add to userinfo: ON
```

**3. Obtenir le Client Secret:**

```
Clients → jenkins → Credentials → Secret
```

### 🔍 Claims Mapping

| Claim Keycloak | Champ Jenkins | Configuration |
|----------------|---------------|---------------|
| `preferred_username` | Username | `userNameField` |
| `groups` | Groups | `groupsFieldName` |
| `name` | Full Name | `fullNameFieldName` |
| `email` | Email | `emailFieldName` |

### 🔄 Well-Known Discovery

Jenkins utilise l'endpoint OIDC discovery pour obtenir automatiquement la configuration:

```
http://keycloak.local/realms/internal/.well-known/openid-configuration
```

**Endpoints détectés:**
- `authorization_endpoint`
- `token_endpoint`
- `userinfo_endpoint`
- `end_session_endpoint`
- `jwks_uri`

### 🛡️ PKCE Support

**Proof Key for Code Exchange** est activé pour améliorer la sécurité:

```yaml
properties:
  - "pkce"
```

### ⚠️ Configuration Développement

```yaml
disableSslVerification: true
```

**À CHANGER en production!** Utiliser HTTPS avec certificats valides.

---

## Autorisation Matrix

### 👥 Groupes et Permissions

#### Groupe `IT` (Administrateurs)

```yaml
- group:
    name: IT
    permissions:
      - Overall/Administer
```

**Accès complet:**
- Configuration système
- Gestion des plugins
- Gestion des credentials
- Tous les jobs
- Tous les agents

#### Groupe `Jenkins` (Utilisateurs Standard)

```yaml
- group:
    name: Jenkins
    permissions:
      - Overall/Read
      - Agent/*
      - Job/*
      - Run/*
      - View/*
```

**Accès limité:**
- ✅ Lire la configuration
- ✅ Créer/Configurer/Lancer des jobs
- ✅ Gérer les agents
- ✅ Créer des vues
- ❌ Modifier la configuration système
- ❌ Gérer les plugins

### 📊 Matrice de Permissions

| Permission | IT | Jenkins |
|------------|----|----|
| **Overall/Administer** | ✅ | ❌ |
| **Overall/Read** | ✅ | ✅ |
| **Agent/Connect** | ✅ | ✅ |
| **Agent/Create** | ✅ | ✅ |
| **Agent/Configure** | ✅ | ✅ |
| **Agent/Delete** | ✅ | ✅ |
| **Job/Discover** | ✅ | ✅ |
| **Job/Read** | ✅ | ✅ |
| **Job/Create** | ✅ | ✅ |
| **Job/Configure** | ✅ | ✅ |
| **Job/Build** | ✅ | ✅ |
| **Job/Delete** | ✅ | ✅ |
| **Run/Delete** | ✅ | ✅ |
| **Run/Replay** | ✅ | ✅ |
| **View/Read** | ✅ | ✅ |
| **View/Create** | ✅ | ✅ |
| **View/Configure** | ✅ | ✅ |
| **Credentials/View** | ✅ | ❌ |
| **Credentials/Create** | ✅ | ❌ |

---

## Scripts d'Initialisation

Les scripts Groovy dans `init.groovy.d/` sont exécutés au démarrage de Jenkins.

### 📂 Structure

```
config/init.groovy.d/
├── 01-create-pipeline-jobs.groovy    # Création des pipelines
└── 02-create-views-jobs.groovy       # Création des vues
```

### 📝 01-create-pipeline-jobs.groovy

**Objectif:** Créer automatiquement les 3 pipelines Keycloak

**Fonctionnement:**
```groovy
import jenkins.model.Jenkins
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.plugin.JenkinsJobManagement

// Job DSL pour créer les pipelines
def jobDslScript = '''
pipelineJob('Keycloak-User-Management') {
    description('...')
    parameters { ... }
    definition {
        cps {
            script(readFileFromWorkspace('/usr/share/jenkins/ref/pipelines/keycloak-user-management.jenkinsfile'))
            sandbox(true)
        }
    }
}
// ... autres pipelines
'''

// Exécution
def workspace = new File('/usr/share/jenkins/ref')
def jobManagement = new JenkinsJobManagement(System.out, [:], workspace)
def result = new DslScriptLoader(jobManagement).runScript(jobDslScript)
```

**Pipelines créés:**
1. Keycloak-User-Management
2. Employee-Onboarding-Webhook
3. Test-Keycloak-Integration

### 📝 02-create-views-jobs.groovy

**Objectif:** Créer des vues pour organiser les pipelines

**Vues créées:**

**1. Integration Tests**
```groovy
def integrationView = new ListView("Integration Tests")
integrationView.includeRegex = "Test-.*"
```

**2. Keycloak Management**
```groovy
def keycloakView = new ListView("Keycloak Management")
keycloakView.includeRegex = "Keycloak-.*|Employee-.*"
```

---

## Plugins

### 📦 Installation

Les plugins sont installés via `plugins.txt` au build de l'image Docker.

```dockerfile
COPY ./config/plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt
```

### 📋 Liste Complète

#### Core Pipeline (5)
```
workflow-aggregator
pipeline-utility-steps
durable-task
pipeline-stage-view
```

#### SCM & GitHub (3)
```
git
github
github-branch-source
```

#### Docker (1)
```
docker-workflow
```

#### SSH & Remote (3)
```
ssh-agent
sshd
publish-over-ssh
```

#### Webhooks (1)
```
generic-webhook-trigger
```

#### Security & Credentials (4)
```
credentials
credentials-binding
plain-credentials
matrix-auth
```

#### Quality & Tests (2)
```
junit
warnings-ng
```

#### Notifications (2)
```
mailer
email-ext
```

#### UX & Logs (4)
```
timestamper
ansicolor
build-timeout
throttle-concurrents
```

#### Configuration as Code (2)
```
configuration-as-code
job-dsl
```

#### Authentication (2)
```
oic-auth
oidc-provider
```

### 🔄 Mise à Jour des Plugins

**Méthode 1: Rebuild l'image**
```bash
docker-compose build jenkins
docker-compose up -d jenkins
```

**Méthode 2: Via Interface Web**
```
Jenkins → Manage Jenkins → Manage Plugins → Updates
```

---

## Entrypoint Script

### 📍 Fichier

```
server/Jenkins/config/entrypoint.sh
```

### 🎯 Fonctionnalités

1. **Auto-fetch OIDC Client Secret**
   - Connecte à Keycloak avec admin credentials
   - Récupère le secret du client `jenkins`
   - Export comme `OIDC_CLIENT_SECRET`

2. **Health Check Keycloak**
   - Attend que Keycloak soit disponible
   - Vérifie l'endpoint `.well-known/openid-configuration`
   - Timeout après 60 tentatives (120 secondes)

3. **Démarrage Jenkins**
   - Exécute le script Jenkins officiel
   - Passe toutes les variables d'environnement

### 🔄 Flux d'Exécution

```
┌─────────────────────────────────────┐
│  Container Start                    │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  Check: OIDC_CLIENT_SECRET set?    │
└────────────┬────────────────────────┘
             │
        ┌────┴────┐
        │         │
       Yes       No
        │         │
        │         ▼
        │    ┌──────────────────────┐
        │    │  Wait for Keycloak   │
        │    │  (60 attempts)       │
        │    └──────────┬───────────┘
        │               │
        │               ▼
        │    ┌──────────────────────┐
        │    │  Get Admin Token     │
        │    └──────────┬───────────┘
        │               │
        │               ▼
        │    ┌──────────────────────┐
        │    │  Get Client ID       │
        │    └──────────┬───────────┘
        │               │
        │               ▼
        │    ┌──────────────────────┐
        │    │  Fetch Secret        │
        │    └──────────┬───────────┘
        │               │
        │               ▼
        │    ┌──────────────────────┐
        │    │  Export OIDC_SECRET  │
        │    └──────────┬───────────┘
        │               │
        └───────┬───────┘
                │
                ▼
     ┌──────────────────────┐
     │  Start Jenkins       │
     │  exec jenkins.sh     │
     └──────────────────────┘
```

### 📝 Extrait du Script

```bash
#!/bin/sh
set -eu

fetch_secret() {
  echo "[entrypoint] Fetching OIDC client secret from Keycloak..."
  
  # Wait for Keycloak
  ATTEMPTS=60
  until curl -fsS "http://${KC_URL_INTERNAL}/realms/${KC_REALM}/.well-known/openid-configuration" > /dev/null 2>&1; do
    echo "[entrypoint] Waiting for Keycloak (${ATTEMPTS})..."
    ATTEMPTS=$((ATTEMPTS - 1))
    sleep 2
  done
  
  # Get admin token
  ACCESS_TOKEN=$(curl -fsS \
    -d "grant_type=password" \
    -d "client_id=admin-cli" \
    -d "username=${KC_ADMIN_USER}" \
    -d "password=${KC_ADMIN_PASSWORD}" \
    "http://${KC_URL_INTERNAL}/realms/master/protocol/openid-connect/token" | jq -r .access_token)
  
  # Fetch secret
  SECRET=$(curl -fsS -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "http://${KC_URL_INTERNAL}/admin/realms/${KC_REALM}/clients/${CLIENT_ID}/client-secret" | jq -r .value)
  
  export OIDC_CLIENT_SECRET="${SECRET}"
}

# Fetch if not provided
if [ -z "${OIDC_CLIENT_SECRET}" ]; then
  fetch_secret
fi

# Start Jenkins
exec /usr/local/bin/jenkins.sh
```

---

## Personnalisation

### Ajouter un Nouveau Groupe

**1. Créer le groupe dans Keycloak:**
```
Realm: internal → Groups → Create Group
Name: HR
```

**2. Modifier jenkins.yaml:**
```yaml
authorizationStrategy:
  globalMatrix:
    entries:
      - group:
          name: HR
          permissions:
            - Overall/Read
            - Job/Read
            - Job/Build
```

**3. Rebuild et redéployer:**
```bash
docker-compose up -d jenkins
```

### Modifier le System Message

```yaml
jenkins:
  systemMessage: "Votre message personnalisé"
```

### Changer l'Admin Email

```yaml
unclassified:
  location:
    adminAddress: "admin@company.com"
```

---

**⬅️ Retour au [README](./README.md)**
