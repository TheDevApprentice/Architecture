# 🔐 Sécurité Jenkins - Guide Complet v0.2.0

**Version:** v0.2.0 - Keycloak Management Automation Suite  
**Date:** October 18, 2025

## 📋 Table des Matières

- [Authentification OIDC](#authentification-oidc)
- [Service Accounts](#service-accounts)
- [Gestion des Secrets dans les Pipelines](#gestion-des-secrets-dans-les-pipelines)
- [Autorisation Matrix](#autorisation-matrix)
- [Sécurisation Docker](#sécurisation-docker)
- [Meilleures Pratiques v0.2.0](#meilleures-pratiques-v020)
- [Audit et Monitoring](#audit-et-monitoring)

---

## 🆕 Nouveautés v0.2.0

### Améliorations de Sécurité

- ✅ **Service Account `jenkins-automation`** avec permissions minimales
- ✅ **Token-based authentication** (5-minute expiration)
- ✅ **Password encryption** - Jamais loggés ou exposés
- ✅ **Client secret masking** - Seulement les 4 derniers caractères affichés
- ✅ **Temporary files** pour payloads sensibles (auto-supprimés)
- ✅ **Confirmation gates** pour opérations destructives
- ✅ **DRY_RUN mode** pour tests sans side effects
- ✅ **Audit logging** de toutes les opérations critiques

---

## Authentification OIDC

### 🔐 Configuration SSO

Jenkins utilise **OpenID Connect** pour l'authentification via Keycloak.

#### Flux d'Authentification

```
1. User → Jenkins (http://jenkins.local:8080)
   └─> Redirect to Keycloak login

2. User → Keycloak (authentication)
   └─> Enter credentials

3. Keycloak → Generate tokens
   ├─> ID Token (user info)
   ├─> Access Token
   └─> Refresh Token

4. Keycloak → Redirect to Jenkins
   └─> With authorization code

5. Jenkins → Exchange code for tokens
   └─> Validate ID token signature
   └─> Extract user claims

6. Jenkins → Create session
   └─> Map groups to permissions
```

#### Claims Utilisés

| Claim | Usage | Exemple |
|-------|-------|---------|
| `preferred_username` | Username Jenkins | `jdoe` |
| `name` | Nom complet | `John Doe` |
| `email` | Email | `john.doe@company.com` |
| `groups` | Attribution permissions | `["IT", "Jenkins"]` |

#### Sécurité du Token

- **Validation signature:** JWT vérifié avec clés publiques Keycloak
- **Expiration:** Tokens expirés automatiquement rejetés
- **Refresh:** Session maintenue via refresh token
- **Révocation:** Déconnexion Keycloak révoque les tokens

### ⚠️ Configuration Production

**HTTPS Obligatoire en Production:**

```yaml
jenkins:
  securityRealm:
    oic:
      disableSslVerification: false  # CHANGER!
      serverConfiguration:
        wellKnown:
          wellKnownOpenIDConfigurationUrl: "https://${KC_URL}.com/realms/internal/.well-known/openid-configuration"
```

**Redirect URIs sécurisés:**
```
Valid Redirect URIs:
  - https://${JENKINS_URL}.com/*
  - https://${JENKINS_URL}.com/securityRealm/finishLogin
```

---

## Service Accounts

### 🤖 jenkins-automation Service Account

**Version:** v0.2.0  
**Purpose:** Automation de la gestion Keycloak via pipelines Jenkins

#### Configuration

```yaml
Client ID: jenkins-automation
Client Type: Confidential
Service Accounts Enabled: true
Authorization Enabled: false
Standard Flow: Disabled
Direct Access Grants: Disabled
```

#### Permissions Requises (Realm-Management)

Le service account doit avoir les rôles suivants pour fonctionner correctement:

| Rôle | Purpose | Pipelines Concernés |
|------|---------|---------------------|
| `manage-users` | Créer/modifier/supprimer utilisateurs | User Management |
| `view-users` | Lister et consulter utilisateurs | User Management, Audit |
| `manage-clients` | Créer/modifier/supprimer clients | Client Management |
| `view-clients` | Lister et consulter clients | Client Management, Audit |
| `query-clients` | Rechercher clients | Client Management |
| `query-groups` | Rechercher groupes | Group Management |
| `query-users` | Rechercher utilisateurs | All Management Pipelines |

#### Principe de Moindre Privilège

✅ **Ce qui est accordé:**
- Gestion des utilisateurs dans le realm `internal`
- Gestion des groupes et membres
- Gestion des clients OAuth2/OIDC
- Consultation des sessions
- Génération de rapports

❌ **Ce qui n'est PAS accordé:**
- Modification de la configuration du realm
- Gestion des Identity Providers
- Modification des rôles realm-management
- Accès au realm `master`
- Modification des politiques de sécurité

#### Authentification Token

```groovy
// Obtenir un access token
def token = keycloakAuth.getAccessToken(
    KC_URL_INTERNAL,
    KC_CLIENT_ID_JENKINS_AUTOMATION,
    KC_CLIENT_SECRET_JENKINS_AUTOMATION
)

// Token properties
// - Type: Bearer
// - Expiration: 5 minutes
// - Scope: Service account roles
// - Auto-cleanup: Oui (post-pipeline)
```

#### Rotation des Secrets

**Fréquence recommandée:** Tous les 90 jours

```bash
# 1. Générer nouveau secret dans Keycloak
Pipeline: Keycloak-Client-Management
ACTION: REGENERATE_SECRET
CLIENT_ID: jenkins-automation

# 2. Mettre à jour dans Jenkins
# Credentials → Update KC_SECRET_JENKINS_AUTOMATION

# 3. Tester
Pipeline: Test-Keycloak-User-Management
```

---

## Gestion des Secrets dans les Pipelines

### 🔒 Types de Secrets Gérés

#### 1. Passwords Utilisateurs

**Sécurité:**
- Type de paramètre: `password` (encrypted in Jenkins)
- Conversion: `.toString()` pour utilisation
- Logging: Jamais affiché dans les logs
- Transmission: Via fichiers temporaires uniquement
- Cleanup: Fichiers supprimés automatiquement

**Exemple:**
```groovy
parameters {
    password(
        name: 'PASSWORD',
        defaultValue: '',
        description: 'User password (leave empty for auto-generation)'
    )
}

stages {
    stage('Create User') {
        steps {
            script {
                // Conversion sécurisée
                def pwd = params.PASSWORD.toString()
                
                // Utilisation via fichier temporaire
                def tmpFile = "/tmp/pwd_${BUILD_NUMBER}.txt"
                sh "echo '${pwd}' > ${tmpFile}"
                
                // Appel API
                sh """
                    curl -X POST ... \
                      -d @${tmpFile}
                """
                
                // Cleanup immédiat
                sh "rm -f ${tmpFile}"
            }
        }
    }
}
```

#### 2. Client Secrets

**Sécurité:**
- Masking: Seulement les 4 derniers caractères affichés
- Stockage: Jenkins Credentials Store (encrypted)
- Transmission: Variables d'environnement (scope limité)
- Logs: Automatiquement masqués par Jenkins

**Exemple:**
```groovy
// Affichage sécurisé
def maskedSecret = "****${secret.substring(secret.length() - 4)}"
echo "Client secret: ${maskedSecret}"
// Output: Client secret: ****Xy9Z
```

#### 3. Access Tokens

**Sécurité:**
- Durée de vie: 5 minutes
- Scope: Limité aux rôles du service account
- Stockage: Variable temporaire (scope stage)
- Cleanup: Automatique en fin de pipeline

**Exemple:**
```groovy
environment {
    ACCESS_TOKEN = ''  // Initialisé vide
}

stages {
    stage('Get Token') {
        steps {
            script {
                ACCESS_TOKEN = keycloakAuth.getAccessToken(...)
            }
        }
    }
    
    stage('Use Token') {
        steps {
            script {
                // Utilisation du token
                keycloakUser.createUser(ACCESS_TOKEN, ...)
            }
        }
    }
}

post {
    always {
        script {
            // Cleanup automatique
            ACCESS_TOKEN = null
        }
    }
}
```

### 🛡️ Protection contre les Fuites

#### Dans les Logs

```groovy
// ❌ MAUVAIS - Secret visible
echo "Password: ${password}"

// ✅ BON - Secret masqué
echo "Password set successfully"
```

#### Dans les Fichiers Temporaires

```groovy
// ✅ BON - Cleanup automatique
try {
    def tmpFile = "/tmp/payload_${BUILD_NUMBER}.json"
    writeFile file: tmpFile, text: jsonPayload
    sh "curl -X POST -d @${tmpFile} ..."
} finally {
    sh "rm -f /tmp/payload_${BUILD_NUMBER}.json"
}
```

#### Dans les Paramètres URL

```groovy
// ❌ MAUVAIS - Secret dans l'URL
sh "curl https://api.com/users?password=${pwd}"

// ✅ BON - Secret dans le body
sh """
    curl -X POST https://api.com/users \
      -H 'Content-Type: application/json' \
      -d '{"password": "${pwd}"}'
"""
```

### 🔐 Confirmation Gates

Pour les opérations destructives ou sensibles, des gates de confirmation sont implémentés:

```groovy
parameters {
    booleanParam(
        name: 'CONFIRM_DELETE',
        defaultValue: false,
        description: '⚠️ Check to confirm deletion'
    )
}

stages {
    stage('Validate') {
        steps {
            script {
                if (!params.CONFIRM_DELETE) {
                    error("❌ Deletion not confirmed. Check CONFIRM_DELETE to proceed.")
                }
            }
        }
    }
}
```

**Opérations nécessitant confirmation:**
- DELETE_USER
- DELETE_GROUP (surtout avec membres)
- DELETE_CLIENT
- REGENERATE_SECRET
- REVOKE_USER_SESSIONS
- REVOKE_ALL_SESSIONS (double confirmation)

---

## Autorisation Matrix

### 👥 Modèle de Permissions

```
┌─────────────────────────────────────┐
│         Keycloak Groups             │
├─────────────────────────────────────┤
│  IT         → Admin complet         │
│  Jenkins    → User standard         │
│  Developers → Custom permissions    │
└─────────────────────────────────────┘
              │
              │ OIDC groups claim
              ▼
┌─────────────────────────────────────┐
│       Jenkins Authorization         │
├─────────────────────────────────────┤
│  Group IT:                          │
│    - Overall/Administer             │
│                                     │
│  Group Jenkins:                     │
│    - Overall/Read                   │
│    - Job/* (all job permissions)    │
│    - Agent/* (agent management)     │
└─────────────────────────────────────┘
```

### 🔒 Principe du Moindre Privilège

**Groupe IT (Admins uniquement):**
- Configuration système
- Gestion plugins
- Gestion credentials
- Accès console Groovy

**Groupe Jenkins (Développeurs):**
- Lire configuration (lecture seule)
- Créer/Modifier/Lancer jobs
- Gérer agents de build
- **Pas d'accès:** Configuration système, plugins, credentials globales

### 📋 Matrice Complète

```yaml
authorizationStrategy:
  globalMatrix:
    entries:
      - group:
          name: IT
          permissions:
            - Overall/Administer
            
      - group:
          name: Jenkins
          permissions:
            # Overall
            - Overall/Read
            
            # Agents
            - Agent/Connect
            - Agent/Create
            - Agent/Configure
            - Agent/Disconnect
            - Agent/Delete
            
            # Jobs
            - Job/Discover
            - Job/Read
            - Job/Create
            - Job/Configure
            - Job/Build
            - Job/Move
            - Job/Delete
            
            # Runs
            - Run/Delete
            - Run/Replay
            
            # Views
            - View/Read
            - View/Create
            - View/Configure
            - View/Delete
```

---

## Gestion des Secrets

### 🔑 Types de Secrets

| Secret | Stockage | Accès |
|--------|----------|-------|
| **OIDC_CLIENT_SECRET** | Auto-fetch au démarrage | Jenkins seulement |
| **KC_SECRET_JENKINS_AUTOMATION** | Variable d'environnement | Pipelines |
| **KC_ADMIN_PASSWORD** | Variable d'environnement | Entrypoint script |
| **User Passwords** | Affichés dans logs builds | ⚠️ Sensible |

### 🛡️ Auto-Fetch du Client Secret

Le script `entrypoint.sh` récupère automatiquement `OIDC_CLIENT_SECRET`:

**Avantages:**
- ✅ Pas besoin de stocker le secret en clair
- ✅ Rotation automatique si le secret change dans Keycloak
- ✅ Un seul secret à gérer manuellement (automation client)

**Sécurité:**
```bash
# Le secret est récupéré via API Admin Keycloak
# Nécessite KC_ADMIN_USER et KC_ADMIN_PASSWORD
# Exporté comme variable d'environnement éphémère
export OIDC_CLIENT_SECRET="${SECRET}"
```

### 🔐 Secrets dans les Pipelines

**❌ Mauvaise Pratique:**
```groovy
// NE JAMAIS hardcoder les secrets
def password = "MyPassword123!"
```

**✅ Bonne Pratique:**
```groovy
// Utiliser les variables d'environnement
environment {
    KC_CLIENT_SECRET = "${KC_SECRET_JENKINS_AUTOMATION}"
}

// Ou credentials binding
withCredentials([string(credentialsId: 'keycloak-secret', variable: 'SECRET')]) {
    // Utiliser ${SECRET}
}
```

### 🧹 Nettoyage des Secrets

**Toujours nettoyer dans post.always:**
```groovy
post {
    always {
        script {
            env.ACCESS_TOKEN = null
            env.PASSWORD = null
        }
    }
}
```

### 📝 Secrets dans les Logs

**⚠️ Attention:**
Les mots de passe générés sont affichés dans les logs de build:

```groovy
echo "Generated password: ${password}"  // Visible dans Console Output
```

**Solutions:**
1. Utiliser Jenkins Credentials Store
2. Envoyer les passwords par email uniquement
3. Activer log masking si possible

---

## Sécurisation Docker

### 🐳 Docker Socket

**Risque:**
```yaml
volumes:
  - /var/run/docker.sock:/var/run/docker.sock
```

Le container Jenkins a accès au daemon Docker de l'hôte = **Accès root équivalent**

**Implications:**
- Jenkins peut créer/supprimer des containers
- Jenkins peut accéder aux volumes de l'hôte
- Jenkins peut escalader les privilèges

**Mitigations:**

**1. User Namespace:**
```yaml
services:
  jenkins:
    user: jenkins:docker
    # Jenkins s'exécute en tant qu'utilisateur non-root
```

**2. Docker Socket Proxy:**
```yaml
services:
  docker-proxy:
    image: tecnativa/docker-socket-proxy
    environment:
      - CONTAINERS=1
      - IMAGES=1
      - NETWORKS=0
      - VOLUMES=0
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
  
  jenkins:
    environment:
      - DOCKER_HOST=tcp://docker-proxy:2375
```

**3. Limiter les Capabilities:**
```yaml
services:
  jenkins:
    cap_drop:
      - ALL
    cap_add:
      - CHOWN
      - SETUID
      - SETGID
```

### 🔒 Isolation Réseau

**Segmentation:**
```yaml
networks:
  proxy:      # Accès Traefik (externe)
  dbnet:      # Accès bases de données
  internal:   # Communication interne uniquement
```

**Principe:**
- Jenkins sur `proxy` + `dbnet`
- Pas d'accès direct depuis Internet
- Traefik comme reverse proxy

---

## Meilleures Pratiques

### 1️⃣ Authentification

- ✅ Toujours utiliser OIDC/SAML (jamais de comptes locaux)
- ✅ Activer 2FA dans Keycloak
- ✅ Forcer HTTPS en production
- ✅ Configurer session timeout
- ❌ Ne pas désactiver CSRF protection

### 2️⃣ Autorisation

- ✅ Principe du moindre privilège
- ✅ Groupes basés sur les rôles (RBAC)
- ✅ Auditer régulièrement les permissions
- ✅ Limiter les admins (groupe IT uniquement)
- ❌ Ne pas donner Overall/Administer aux développeurs

### 3️⃣ Credentials

- ✅ Utiliser Jenkins Credentials Store
- ✅ Rotation régulière des secrets
- ✅ Chiffrer les credentials au repos
- ✅ Limiter l'accès aux credentials par projet
- ❌ Ne jamais logger les credentials

### 4️⃣ Pipelines

- ✅ Exécuter les pipelines en sandbox mode
- ✅ Valider les entrées utilisateur
- ✅ Limiter les commandes shell autorisées
- ✅ Scanner le code des pipelines
- ❌ Ne pas faire confiance au code externe

### 5️⃣ Plugins

- ✅ Installer uniquement les plugins nécessaires
- ✅ Mettre à jour régulièrement
- ✅ Vérifier les vulnérabilités (CVE)
- ✅ Désactiver les plugins non utilisés
- ❌ Ne pas installer de plugins non vérifiés

### 6️⃣ Monitoring

- ✅ Activer l'audit logging
- ✅ Monitorer les tentatives de connexion
- ✅ Alerter sur les échecs d'authentification
- ✅ Surveiller l'utilisation des credentials
- ✅ Logs centralisés (ELK, Grafana Loki)

---

## Audit et Monitoring

### 📊 Logs à Surveiller

**Authentication Events:**
```
Successful login: username=jdoe, ip=192.168.1.10
Failed login: username=admin, ip=203.0.113.5
Logout: username=jdoe
```

**Authorization Events:**
```
Access denied: user=jdoe, resource=/configure
Permission granted: user=admin, action=Overall/Administer
```

**Credential Access:**
```
Credential accessed: id=keycloak-secret, user=jdoe, job=Test-Pipeline
```

### 🔍 Requêtes d'Audit

**Logs de connexion:**
```bash
docker logs jenkins | grep "login"
```

**Échecs d'authentification:**
```bash
docker logs jenkins | grep "Failed login" | tail -n 50
```

**Accès non autorisés:**
```bash
docker logs jenkins | grep "Access denied"
```

### 📈 Métriques de Sécurité

| Métrique | Seuil | Action |
|----------|-------|--------|
| Échecs de login | > 5/min | Alerte + Block IP |
| Tentatives admin | > 3 échecs | Alerte Security Team |
| Credential access | Anormal | Investiguer |
| Configuration changes | Tous | Audit log |

### 🚨 Alertes à Configurer

```yaml
# Exemple avec Prometheus Alertmanager
- alert: JenkinsFailedLogins
  expr: rate(jenkins_failed_login_total[5m]) > 5
  annotations:
    summary: "Trop de tentatives de connexion échouées"

- alert: JenkinsUnauthorizedAccess
  expr: rate(jenkins_access_denied_total[5m]) > 10
  annotations:
    summary: "Accès non autorisés détectés"
```

---

## Checklist de Sécurité

### 🔐 Configuration

- [ ] HTTPS activé (production)
- [ ] OIDC configuré avec Keycloak
- [ ] Groupes et permissions configurés
- [ ] Session timeout configuré
- [ ] CSRF protection activé

### 🔑 Credentials

- [ ] Aucun secret en clair dans le code
- [ ] Variables d'environnement utilisées
- [ ] Nettoyage des secrets dans post blocks
- [ ] Rotation des secrets planifiée

### 🐳 Docker

- [ ] Container non-root
- [ ] Docker socket sécurisé
- [ ] Networks isolés
- [ ] Volumes avec permissions restrictives

### 📊 Monitoring

- [ ] Audit logging activé
- [ ] Logs centralisés
- [ ] Alertes configurées
- [ ] Dashboard de monitoring

### 🔄 Maintenance

- [ ] Plugins à jour
- [ ] Scan de vulnérabilités régulier
- [ ] Audit des permissions trimestriel
- [ ] Tests de sécurité

---

## Incidents de Sécurité

### 🚨 Procédure en Cas d'Incident

**1. Détection:**
- Alertes automatiques
- Rapports utilisateurs
- Scan de vulnérabilités

**2. Isolation:**
```bash
# Arrêter Jenkins immédiatement
docker-compose stop jenkins

# Bloquer l'accès réseau
docker network disconnect proxy jenkins
```

**3. Investigation:**
```bash
# Examiner les logs
docker logs jenkins > incident-logs.txt

# Vérifier les accès
grep "Access denied" incident-logs.txt
grep "Failed login" incident-logs.txt
```

**4. Remédiation:**
- Révoquer credentials compromis
- Changer tous les secrets
- Patcher les vulnérabilités
- Mettre à jour les plugins

**5. Post-Incident:**
- Rapport d'incident
- Amélioration des contrôles
- Formation équipe

---

**⬅️ Retour au [README](./README.md)**
