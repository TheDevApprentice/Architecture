# 🚀 Jenkins - Documentation Complète

## 📋 Sommaire

1. [Vue d'ensemble](#-vue-densemble)
2. [Architecture et Configuration](#-architecture-et-configuration)
3. [Pipelines d'Automatisation](#-pipelines-dautomatisation)
4. [Bibliothèque Partagée Keycloak](#-bibliothèque-partagée-keycloak)
5. [Configuration JCasC (Jenkins as Code)](#-configuration-jcasc)
6. [Plugins Installés](#-plugins-installés)
7. [Déploiement et Utilisation](#-déploiement-et-utilisation)
8. [Sécurité et Authentification](#-sécurité-et-authentification)
9. [Guide de Dépannage](#-guide-de-dépannage)

---

## 🎯 Vue d'ensemble

Cette instance Jenkins est configurée pour **automatiser la gestion des utilisateurs Keycloak** et fournir une plateforme CI/CD complète. Elle s'intègre nativement avec Keycloak pour l'authentification SSO et propose des pipelines prêts à l'emploi pour l'onboarding d'employés et la gestion des comptes.

### Fonctionnalités Clés

- **✅ Configuration as Code (JCasC)** - Configuration complète via YAML
- **🔐 Authentification SSO via OpenId Connect** - OpenID Connect (OIDC)
- **📦 Pipelines préconfigurés** - Gestion utilisateurs, onboarding, tests
- **📚 Bibliothèque partagée** - Fonctions réutilisables pour Keycloak API
- **🎨 Vues préconfigurés** - Organisation automatique des jobs
- **🔧 Outils intégrés** - Docker, Git, DB clients, SSH

---

## 📂 Structure de la Documentation

### 📄 Fichiers de Documentation

| Fichier | Description |
|---------|-------------|
| **[ARCHITECTURE.md](./ARCHITECTURE.md)** | Architecture technique détaillée et diagrammes |
| **[CONFIGURATION.md](./CONFIGURATION.md)** | Guide de configuration JCasC et variables d'environnement |
| **[PIPELINES.md](./PIPELINES.md)** | Documentation complète des 3 pipelines |
| **[SHARED_LIBRARY.md](./SHARED_LIBRARY.md)** | API de la bibliothèque partagée Keycloak |
| **[DEPLOYMENT.md](./DEPLOYMENT.md)** | Guide de déploiement et prérequis |
| **[SECURITY.md](./SECURITY.md)** | Configuration de sécurité et meilleures pratiques |
| **[TROUBLESHOOTING.md](./TROUBLESHOOTING.md)** | Solutions aux problèmes courants |

---

## 🏗️ Architecture et Configuration

### Composants Principaux

```
┌────────────────────────────────────────────────────┐
│                 JENKINS CONTAINER                  │
│                                                    │
│  ┌──────────────────────────────────────────┐    │
│  │        Jenkins Core (LTS JDK21)          │    │
│  │    - Web UI (Port 8080)                  │    │
│  │    - Inbound Agents (Port 50000)         │    │
│  └──────────────────────────────────────────┘    │
│                                                    │
│  ┌──────────────────────────────────────────┐    │
│  │       Configuration as Code (JCasC)      │    │
│  │    - Keycloak OIDC SSO                   │    │
│  │    - Matrix Authorization                │    │
│  │    - System Settings                     │    │
│  └──────────────────────────────────────────┘    │
│                                                    │
│  ┌──────────────────────────────────────────┐    │
│  │           Plugins (56 plugins)           │    │
│  │    - Pipeline & Workflow                 │    │
│  │    - OIDC Authentication                 │    │
│  │    - Docker, Git, SSH                    │    │
│  │    - Generic Webhook Trigger             │    │
│  └──────────────────────────────────────────┘    │
│                                                    │
│  ┌──────────────────────────────────────────┐    │
│  │      Keycloak Shared Library             │    │
│  │    - keycloakAuth.groovy                 │    │
│  │    - keycloakUser.groovy                 │    │
│  └──────────────────────────────────────────┘    │
│                                                    │
│  ┌──────────────────────────────────────────┐    │
│  │           3 Pipeline Jobs                │    │
│  │    - Keycloak User Management            │    │
│  │    - Employee Onboarding Webhook         │    │
│  │    - Test Keycloak Integration           │    │
│  └──────────────────────────────────────────┘    │
│                                                    │
│  ┌──────────────────────────────────────────┐    │
│  │              Auto Views                  │    │
│  │    - Keycloak Management                 │    │
│  │    - Integration Tests                   │    │
│  └──────────────────────────────────────────┘    │
│                                                    │
│  ┌──────────────────────────────────────────┐    │
│  │         Integrated Tools                 │    │
│  │    - Docker CLI + Compose                │    │
│  │    - Git, SSH, curl, jq                  │    │
│  │    - PostgreSQL & MariaDB clients        │    │
│  └──────────────────────────────────────────┘    │
└────────────────────────────────────────────────────┘
           │                          │
           │                          │
    ┌──────▼──────┐          ┌───────▼────────┐
    │  Keycloak   │          │  Docker Socket │
    │  (OIDC SSO) │          │  (CI/CD Builds)│
    └─────────────┘          └────────────────┘
```

---

## 🔄 Pipelines d'Automatisation

### 1️⃣ Keycloak User Management

Pipeline interactif pour la gestion manuelle des utilisateurs Keycloak.

**Actions Disponibles:**
- ✅ Créer un utilisateur
- ✏️ Mettre à jour un utilisateur
- 🗑️ Supprimer un utilisateur
- 🔑 Réinitialiser le mot de passe
- 👥 Ajouter à un groupe
- 📋 Lister les utilisateurs

**Utilisation:**
```
Jenkins UI → Keycloak-User-Management → Build with Parameters
```

### 2️⃣ Employee Onboarding Webhook

Pipeline automatisé déclenché par webhook pour l'onboarding d'employés.

**Fonctionnalités:**
- ✅ Création automatique de compte
- 📧 Email de bienvenue avec credentials
- 👥 Attribution automatique aux groupes
- 🔄 Mise à jour si l'utilisateur existe déjà

**Déclenchement:**
```bash
curl -X POST \
  http://jenkins.local/generic-webhook-trigger/invoke?token=employee-onboarding-secret-token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jdoe",
    "email": "john.doe@company.com",
    "firstName": "John",
    "lastName": "Doe",
    "department": "IT",
    "role": "developer",
    "realm": "internal"
  }'
```

### 3️⃣ Test Keycloak Integration

Suite de tests complète pour valider l'intégration Keycloak.

**Tests Exécutés:**
1. Connectivité Keycloak
2. Authentification Service Account
3. Validation de token
4. Listing des utilisateurs
5. Création d'utilisateur test
6. Mise à jour d'utilisateur
7. Réinitialisation de mot de passe
8. Attribution à un groupe
9. Suppression d'utilisateur

**Voir [PIPELINES.md](./PIPELINES.md) pour la documentation complète**

---

## 📚 Bibliothèque Partagée Keycloak

Bibliothèque Groovy réutilisable pour interagir avec l'API Keycloak.

### Modules Disponibles

#### `keycloakAuth.groovy`
- `getServiceAccountToken()` - Obtenir un token via client credentials
- `getAdminToken()` - Obtenir un token admin via username/password
- `validateToken()` - Valider un access token

#### `keycloakUser.groovy`
- `createUser()` - Créer un nouvel utilisateur
- `updateUser()` - Mettre à jour un utilisateur existant
- `deleteUser()` - Supprimer un utilisateur
- `resetPassword()` - Réinitialiser le mot de passe
- `addUserToGroup()` - Ajouter un utilisateur à un groupe
- `listUsers()` - Lister tous les utilisateurs d'un realm
- `getUserId()` - Obtenir l'ID d'un utilisateur
- `getGroupId()` - Obtenir l'ID d'un groupe
- `generatePassword()` - Générer un mot de passe sécurisé

**Voir [SHARED_LIBRARY.md](./SHARED_LIBRARY.md) pour l'API complète**

---

## ⚙️ Configuration JCasC

Jenkins est configuré entièrement via **Configuration as Code (JCasC)** avec le fichier `jenkins.yaml`.

### Sections Principales

```yaml
jenkins:
  securityRealm: oic         # Keycloak OIDC SSO
  authorizationStrategy:     # Matrix-based permissions
    - group: IT → Admin
    - group: Jenkins → Standard permissions
  
unclassified:
  location:
    url: http://${JENKINS_URL}
```

### Variables d'Environnement Requises

| Variable | Description | Exemple |
|----------|-------------|---------|
| `JENKINS_URL` | URL publique de Jenkins | `jenkins.local` |
| `KC_URL_INTERNAL` | URL interne Keycloak | `keycloak:8080` |
| `KC_REALM` | Realm Keycloak | `internal` |
| `OIDC_CLIENT_ID` | Client ID Jenkins dans Keycloak | `jenkins` |
| `OIDC_CLIENT_SECRET` | Secret du client (auto-fetch) | `***` |
| `KC_CLIENT_ID_JENKINS_AUTOMATION` | Client automation | `jenkins-automation` |
| `KC_SECRET_JENKINS_AUTOMATION` | Secret automation | `***` |

**Voir [CONFIGURATION.md](./CONFIGURATION.md) pour plus de détails**

---

## 🔌 Plugins Installés

### Catégories de Plugins

#### **Core Pipeline & Utilities** (5)
- workflow-aggregator
- pipeline-utility-steps
- durable-task
- pipeline-stage-view

#### **SCM & GitHub** (3)
- git
- github
- github-branch-source

#### **Docker** (1)
- docker-workflow

#### **SSH & Remote Execution** (3)
- ssh-agent
- sshd
- publish-over-ssh

#### **Webhooks & Triggers** (1)
- generic-webhook-trigger

#### **Security & Credentials** (4)
- credentials
- credentials-binding
- plain-credentials
- matrix-auth

#### **Quality & Tests** (2)
- junit
- warnings-ng

#### **Notifications** (2)
- mailer
- email-ext

#### **UX & Logs** (4)
- timestamper
- ansicolor
- build-timeout
- throttle-concurrents

#### **Configuration as Code** (2)
- configuration-as-code
- job-dsl

#### **Authentication (Keycloak)** (2)
- oic-auth
- oidc-provider

**Total: 29+ plugins (avec dépendances)**

---

## 🚀 Déploiement et Utilisation

### Prérequis

- Docker + Docker Compose
- Keycloak configuré avec:
  - Realm `internal`
  - Client `jenkins` (OIDC confidential)
  - Client `jenkins-automation` (service account)
  - Groupes `IT` et `Jenkins`

### Déploiement via Docker Compose

```yaml
services:
  jenkins:
    build: ./server/Jenkins
    container_name: jenkins
    ports:
      - "8080:8080"
      - "50000:50000"
    environment:
      - JENKINS_URL=jenkins.local
      - KC_URL_INTERNAL=keycloak:8080
      - KC_REALM=internal
      - OIDC_CLIENT_ID=jenkins
      - KC_CLIENT_ID_JENKINS_AUTOMATION=jenkins-automation
      - KC_SECRET_JENKINS_AUTOMATION=${KC_SECRET}
    volumes:
      - jenkins_home:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock
    networks:
      - proxy
      - dbnet
```

### Premier Démarrage

1. **Démarrer Jenkins:**
   ```bash
   docker-compose up -d jenkins
   ```

2. **Vérifier les logs:**
   ```bash
   docker-compose logs -f jenkins
   ```

3. **Accéder à Jenkins:**
   ```
   http://jenkins.local:8080
   ```

4. **Se connecter via Keycloak:**
   - Cliquer sur "Login with Keycloak"
   - Utiliser les credentials Keycloak

**Voir [DEPLOYMENT.md](./DEPLOYMENT.md) pour le guide complet**

---

## 🔐 Sécurité et Authentification

### Authentification OIDC (Keycloak)

Jenkins utilise **OpenID Connect** pour l'authentification SSO via Keycloak.

#### Configuration OIDC

```yaml
securityRealm:
  oic:
    clientId: "${OIDC_CLIENT_ID}"
    clientSecret: "${OIDC_CLIENT_SECRET}"
    wellKnownOpenIDConfigurationUrl: 
      "http://${KC_URL_INTERNAL}/realms/${KC_REALM}/.well-known/openid-configuration"
    userNameField: "preferred_username"
    groupsFieldName: "groups"
```

### Autorisation Matrix-Based

Les permissions sont attribuées par groupe Keycloak:

| Groupe Keycloak | Permissions Jenkins |
|-----------------|---------------------|
| **IT** | Administrateur complet |
| **Jenkins** | Lecture, Build, Configuration des jobs |

### Récupération Automatique du Secret

Le script `entrypoint.sh` récupère automatiquement le `OIDC_CLIENT_SECRET` depuis Keycloak au démarrage si non fourni.

**Voir [SECURITY.md](./SECURITY.md) pour les détails complets**

---

## 🛠️ Guide de Dépannage

### Problèmes Courants

#### 1. Erreur "Failed to obtain client secret"

**Cause:** Keycloak non accessible ou credentials invalides

**Solution:**
```bash
# Vérifier la connectivité
docker exec jenkins curl http://keycloak:8080/realms/internal/.well-known/openid-configuration

# Vérifier les variables d'environnement
docker exec jenkins env | grep KC_
```

#### 2. Pipelines ne se créent pas au démarrage

**Cause:** Scripts init.groovy.d non exécutés

**Solution:**
```bash
# Vérifier les logs au démarrage
docker logs jenkins | grep "Creating Keycloak Automation Pipeline Jobs"

# Relancer manuellement
docker exec jenkins groovy /usr/share/jenkins/ref/init.groovy.d/01-create-pipeline-jobs.groovy
```

#### 3. Erreur "Access token invalid"

**Cause:** Token expiré ou realm incorrect

**Solution:**
- Vérifier que le realm est correct (`internal`)
- Régénérer un token
- Vérifier les permissions du service account

#### 4. Webhook ne déclenche pas le pipeline

**Cause:** Token incorrect ou plugin non configuré

**Solution:**
```bash
# Vérifier le webhook trigger
curl -X POST "http://jenkins.local:8080/generic-webhook-trigger/invoke?token=employee-onboarding-secret-token" \
  -H "Content-Type: application/json" \
  -d '{"username": "test", "email": "test@test.com"}'
```

**Voir [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) pour plus de solutions**

---

## 📊 Métriques et Monitoring

### Points de Surveillance

- **Santé Jenkins:** `http://jenkins.local:8080/metrics`
- **Statut des builds:** Dashboard Jenkins
- **Logs:** `docker logs jenkins -f`
- **Connexion Keycloak:** Vérifier OIDC well-known endpoint

---

## 🔄 Workflow Typique

### Onboarding d'un Nouvel Employé

```
1. Système RH → Webhook Jenkins
                    ↓
2. Jenkins → Parse payload
                    ↓
3. Jenkins → Determine group (IT/Jenkins)
                    ↓
4. Jenkins → Get Keycloak token
                    ↓
5. Jenkins → Check if user exists
                    ↓
6. Jenkins → Create/Update user
                    ↓
7. Jenkins → Add to group
                    ↓
8. Jenkins → Send welcome email
                    ↓
9. Jenkins → Notify HR
```

### Test de l'Intégration

```bash
# 1. Accéder à Jenkins
http://jenkins.local:8080

# 2. Lancer le pipeline de test
Jenkins UI → Test-Keycloak-Integration → Build Now

# 3. Vérifier les résultats
Console Output → All tests should pass ✅
```

---

## 📚 Ressources Additionnelles

### Documentation Officielle

- [Jenkins Configuration as Code](https://github.com/jenkinsci/configuration-as-code-plugin)
- [Keycloak Admin REST API](https://www.keycloak.org/docs-api/latest/rest-api/index.html)
- [OIDC Authentication Plugin](https://plugins.jenkins.io/oic-auth/)
- [Generic Webhook Trigger](https://plugins.jenkins.io/generic-webhook-trigger/)

### Liens Internes

- [Architecture Globale](../ARCHITECTURE.md)
- [Documentation Keycloak](../Keycloak/README.md)
- [Documentation Traefik](../Traefik/README.md)

---

## 🤝 Contribution et Support

### Structure des Fichiers

```
server/Jenkins/
├── Dockerfile                    # Image Jenkins personnalisée
└── config/
    ├── plugins.txt               # Liste des plugins
    ├── jenkins.yaml              # Configuration JCasC
    ├── entrypoint.sh             # Script de démarrage
    ├── init.groovy.d/            # Scripts d'initialisation
    │   ├── 01-create-pipeline-jobs.groovy
    │   └── 02-create-views-jobs.groovy
    ├── pipelines/                # Jenkinsfiles
    │   ├── keycloak-user-management.jenkinsfile
    │   ├── employee-onboarding-webhook.jenkinsfile
    │   └── test-keycloak-integration.jenkinsfile
    └── shared-library/           # Bibliothèque partagée
        └── vars/
            ├── keycloakAuth.groovy
            └── keycloakUser.groovy
```

---

## 📝 Changelog

### Version 0.1.0 (Initial Release)

- ✅ Configuration JCasC complète
- ✅ Intégration Keycloak OIDC
- ✅ 3 pipelines d'automatisation
- ✅ Bibliothèque partagée Keycloak
- ✅ Vues automatiques
- ✅ Récupération automatique des secrets
- ✅ 29+ plugins préinstallés

---

## ⚖️ Licence

Ce projet fait partie de l'architecture DevOps globale.

---

**📖 Pour commencer rapidement, consultez [DEPLOYMENT.md](./DEPLOYMENT.md)**