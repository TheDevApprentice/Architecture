# 🔐 Sécurité Jenkins - Guide Complet

## 📋 Table des Matières

- [Authentification OIDC](#authentification-oidc)
- [Autorisation Matrix](#autorisation-matrix)
- [Gestion des Secrets](#gestion-des-secrets)
- [Sécurisation Docker](#sécurisation-docker)
- [Meilleures Pratiques](#meilleures-pratiques)
- [Audit et Monitoring](#audit-et-monitoring)

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
