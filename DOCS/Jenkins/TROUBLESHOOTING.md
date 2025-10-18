# 🛠️ Dépannage Jenkins - Guide de Résolution v0.2.0

**Version:** v0.2.0 - Keycloak Management Automation Suite  
**Date:** October 18, 2025

## 📋 Table des Matières

- [Problèmes de Démarrage](#problèmes-de-démarrage)
- [Authentification OIDC](#authentification-oidc)
- [Pipelines v0.2.0](#pipelines-v020)
  - [Erreurs Communes](#erreurs-communes)
  - [Service Account](#service-account)
  - [JSON Parsing](#json-parsing)
  - [Password Parameters](#password-parameters)
  - [Tests d'Intégration](#tests-dintégration)
- [Intégration Keycloak](#intégration-keycloak)
- [Performance](#performance)
- [Docker](#docker)

---

## Problèmes de Démarrage

### ❌ Container ne démarre pas

**Symptômes:**
```bash
docker ps -a | grep jenkins
# Status: Exited (1)
```

**Diagnostic:**
```bash
docker logs jenkins
```

**Causes possibles:**

**1. Permissions sur le volume:**
```bash
# Vérifier permissions
docker exec jenkins ls -la /var/jenkins_home

# Fix
docker exec -u root jenkins chown -R jenkins:jenkins /var/jenkins_home
```

**2. Port déjà utilisé:**
```bash
# Vérifier port 8080
netstat -tulpn | grep 8080
# ou
docker ps | grep 8080

# Solution: Changer le port dans docker-compose.yml
ports:
  - "8081:8080"
```

**3. Mémoire insuffisante:**
```bash
# Vérifier logs OOM
docker logs jenkins | grep -i "out of memory"

# Solution: Augmenter la RAM
environment:
  - JAVA_OPTS=-Xmx4g -Xms1g
```

### ❌ Jenkins démarre mais inaccessible

**Diagnostic:**
```bash
# Vérifier si Jenkins écoute
docker exec jenkins netstat -tulpn | grep 8080

# Vérifier les réseaux
docker network inspect proxy
```

**Solutions:**

**1. Vérifier Traefik:**
```bash
docker logs traefik | grep jenkins
```

**2. Tester accès direct:**
```bash
curl http://localhost:8080
```

**3. Vérifier DNS/Hosts:**
```bash
ping jenkins.local
# Doit résoudre vers 127.0.0.1 ou IP du serveur
```

---

## Authentification OIDC

### ❌ "Failed to obtain client secret"

**Symptômes:**
```
[entrypoint] Failed to fetch OIDC client secret
[entrypoint] Keycloak not reachable
```

**Diagnostic:**
```bash
# 1. Vérifier connectivité Keycloak
docker exec jenkins curl http://keycloak:8080/realms/internal/.well-known/openid-configuration

# 2. Vérifier variables d'environnement
docker exec jenkins env | grep KC_
```

**Solutions:**

**1. Keycloak pas démarré:**
```bash
docker-compose up -d keycloak
docker-compose logs -f keycloak

# Attendre que Keycloak soit prêt
docker logs keycloak | grep "Listening on"
```

**2. Realm ou client incorrect:**
```bash
# Vérifier dans Keycloak Admin
http://keycloak.local:8080/admin
Realms → internal → Clients → jenkins
```

**3. Credentials admin invalides:**
```bash
# Vérifier .env
cat .env | grep KC_BOOTSTRAP

# Tester manuellement
curl -X POST "http://keycloak:8080/realms/master/protocol/openid-connect/token" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=<password>" \
  -d "grant_type=password"
```

### ❌ "Login with Keycloak" ne fonctionne pas

**Symptômes:**
- Erreur après redirection
- Page blanche
- "Invalid redirect URI"

**Solutions:**

**1. Vérifier Redirect URIs:**
```
Keycloak → Clients → jenkins → Settings

Valid Redirect URIs:
  http://jenkins.local:8080/*
  http://jenkins.local:8080/securityRealm/finishLogin
```

**2. Vérifier URL de base:**
```yaml
# jenkins.yaml
unclassified:
  location:
    url: "http://jenkins.local:8080"  # Doit correspondre
```

**3. Tester OIDC discovery:**
```bash
curl http://keycloak.local/realms/internal/.well-known/openid-configuration | jq
```

### ❌ Utilisateur connecté mais pas de permissions

**Symptômes:**
```
Access Denied
User jdoe has no permissions
```

**Diagnostic:**
```bash
# Vérifier groupes de l'utilisateur dans Keycloak
Keycloak Admin → Users → jdoe → Groups
```

**Solutions:**

**1. Ajouter l'utilisateur au groupe:**
```
Keycloak → Users → jdoe → Groups → Join
Sélectionner: Jenkins ou IT
```

**2. Vérifier mapper groups:**
```
Clients → jenkins → Client Scopes → jenkins-dedicated → Mappers
Vérifier: groups mapper existe et est configuré
```

**3. Vérifier configuration JCasC:**
```yaml
authorizationStrategy:
  globalMatrix:
    entries:
      - group:
          name: Jenkins  # Doit correspondre au groupe Keycloak
```

---

## Pipelines v0.2.0

### 🆕 Erreurs Communes v0.2.0

#### ❌ "Cannot parse the JSON" Error

**Symptômes:**
```
Error: Cannot parse the JSON
HTTP 400 Bad Request from Keycloak
```

**Cause:**
Caractères spéciaux dans le JSON cassent le parsing shell lors de l'utilisation de `-d '${json}'`

**Solution:**
✅ **FIXÉ dans v0.2.0** - Utilisation de fichiers temporaires au lieu de JSON inline

```groovy
// ❌ ANCIEN (v0.1.0) - Causait des erreurs
sh """
    curl -X POST ... -d '${jsonPayload}'
"""

// ✅ NOUVEAU (v0.2.0) - Fonctionne correctement
def tmpFile = "/tmp/payload_${BUILD_NUMBER}.json"
writeFile file: tmpFile, text: jsonPayload
sh """
    curl -X POST ... -d @${tmpFile}
"""
sh "rm -f ${tmpFile}"
```

---

#### ❌ Password Parameter Returns Encrypted Object

**Symptômes:**
```
Password: {encryptedValue: "...", plainText: "..."}
API rejects password format
```

**Cause:**
Jenkins password parameters retournent un objet au lieu d'une string

**Solution:**
✅ **FIXÉ dans v0.2.0** - Conversion explicite avec `.toString()`

```groovy
// ❌ ANCIEN - Objet encrypted
def pwd = params.PASSWORD

// ✅ NOUVEAU - String utilisable
def pwd = params.PASSWORD.toString()
```

---

#### ❌ Sandbox Security: RejectedAccessException

**Symptômes:**
```
RejectedAccessException: Scripts not permitted to use method net.sf.json.JSONArray join
```

**Cause:**
Jenkins sandbox bloque `.join()` sur les objets JSONArray

**Solution:**
✅ **FIXÉ dans v0.2.0** - Remplacement par `.collect()` et `.each()`

```groovy
// ❌ ANCIEN - Bloqué par sandbox
def uris = redirectUris.join(',')

// ✅ NOUVEAU - Compatible sandbox
def uriList = []
redirectUris.each { uri ->
    uriList.add(uri)
}
```

---

#### ❌ MissingPropertyException: No such property: parameters

**Symptômes:**
```
MissingPropertyException: No such property: parameters
Error in post.always block
```

**Cause:**
Code de cleanup invalide tentant d'accéder à `parameters` dans le bloc post

**Solution:**
✅ **FIXÉ dans v0.2.0** - Suppression du code de cleanup invalide

```groovy
post {
    always {
        script {
            // ❌ ANCIEN - Causait MissingPropertyException
            // parameters.each { key, value -> 
            //     env[key] = null
            // }
            
            // ✅ NOUVEAU - Cleanup correct
            ACCESS_TOKEN = null
        }
    }
}
```

---

### 🔐 Service Account Issues

#### ❌ "Insufficient permissions" Error

**Symptômes:**
```
403 Forbidden
User 'service-account-jenkins-automation' lacks permissions
```

**Diagnostic:**
```bash
# Vérifier les rôles du service account
Keycloak Admin Console
→ Clients → jenkins-automation
→ Service Account Roles
→ Client Roles → realm-management
```

**Solution:**
Assigner les rôles requis:

```
Required Roles (realm-management):
✅ manage-users
✅ view-users
✅ manage-clients
✅ view-clients
✅ query-clients
✅ query-groups
✅ query-users
```

**Test:**
```bash
# Tester avec le pipeline de test
Pipeline: Test-Keycloak-User-Management
```

---

#### ❌ Token Expired

**Symptômes:**
```
401 Unauthorized
Token expired
```

**Cause:**
Les tokens expirent après 5 minutes

**Solution:**
✅ Les pipelines obtiennent un nouveau token à chaque exécution

```groovy
// Token obtenu au début de chaque pipeline
stage('Get Token') {
    steps {
        script {
            ACCESS_TOKEN = keycloakAuth.getAccessToken(...)
        }
    }
}
```

---

### 🧪 Tests d'Intégration

#### ❌ Tests Failing: Resource Already Exists

**Symptômes:**
```
Error: User test-user-12345 already exists
Test failed: CREATE operation
```

**Cause:**
Cleanup incomplet d'une exécution précédente

**Solution:**
```bash
# 1. Nettoyer manuellement
Pipeline: Keycloak-User-Management
ACTION: DELETE_USER
USERNAME: test-user-12345

# 2. Re-exécuter le test
Pipeline: Test-Keycloak-User-Management
```

**Prévention:**
Les tests utilisent `BUILD_NUMBER` pour des noms uniques:
```groovy
def testUsername = "test-user-${BUILD_NUMBER}"
```

---

#### ❌ All Tests Failing

**Symptômes:**
```
42/42 tests failed
Cannot connect to Keycloak
```

**Diagnostic:**
```bash
# 1. Vérifier connectivité Keycloak
docker exec jenkins curl -I http://keycloak:8080

# 2. Vérifier variables d'environnement
docker exec jenkins env | grep KC_
```

**Solution:**
```bash
# Vérifier configuration Jenkins
KC_URL_INTERNAL=keycloak:8080
KC_CLIENT_ID_JENKINS_AUTOMATION=jenkins-automation
KC_SECRET_JENKINS_AUTOMATION=<secret>
```

---

### 📝 DRY_RUN Mode Issues

#### ❌ DRY_RUN Not Working

**Symptômes:**
```
DRY_RUN=true but changes were applied
```

**Cause:**
Certaines actions ne supportent pas DRY_RUN

**Actions supportant DRY_RUN:**
- ✅ DELETE_GROUP
- ✅ DELETE_USER
- ✅ DELETE_CLIENT
- ✅ REVOKE_USER_SESSIONS
- ❌ CREATE operations (pas de sens en DRY_RUN)
- ❌ LIST operations (read-only)

---

### ❌ Pipelines non créés au démarrage

**Symptômes:**
- Aucun job visible dans Jenkins
- Jobs list vide

**Diagnostic:**
```bash
# Vérifier logs d'initialisation
docker logs jenkins | grep "Creating Keycloak Automation Pipeline"
```

**Solutions:**

**1. Scripts init.groovy.d non exécutés:**
```bash
# Relancer manuellement
docker exec jenkins groovy /usr/share/jenkins/ref/init.groovy.d/01-create-pipeline-jobs.groovy
```

**2. Permissions sur les fichiers:**
```bash
docker exec jenkins ls -la /usr/share/jenkins/ref/pipelines/
docker exec -u root jenkins chown -R jenkins:jenkins /usr/share/jenkins/ref/
```

**3. Erreur dans le script Groovy:**
```bash
# Logs d'erreur
docker logs jenkins | grep -A 20 "Failed to create pipeline"
```

### ❌ Pipeline échoue: "Access token invalid"

**Symptômes:**
```
Failed to obtain access token
401 Unauthorized
Token invalid or expired
```

**Solutions:**

**1. Vérifier client automation dans Keycloak:**
```
Clients → jenkins-automation
Service Accounts Enabled: ON
Client Secret: <copier>
```

**2. Vérifier variable d'environnement:**
```bash
docker exec jenkins env | grep KC_SECRET_JENKINS_AUTOMATION
# Doit correspondre au secret Keycloak
```

**3. Vérifier permissions service account:**
```
Clients → jenkins-automation → Service Account Roles
Client Roles → realm-management:
  - manage-users
  - view-users
  - query-groups
```

### ❌ "Failed to load Keycloak library"

**Symptômes:**
```
groovy.lang.MissingMethodException
Cannot load keycloakAuth.groovy
```

**Solutions:**

**1. Vérifier présence des fichiers:**
```bash
docker exec jenkins ls -la /var/jenkins_home/workflow-libs/keycloak-lib/vars/
# Doit contenir: keycloakAuth.groovy, keycloakUser.groovy
```

**2. Permissions:**
```bash
docker exec -u root jenkins chown -R jenkins:jenkins /var/jenkins_home/workflow-libs/
```

**3. Chemin correct dans le pipeline:**
```groovy
// Bon chemin
keycloakAuth = load '/var/jenkins_home/workflow-libs/keycloak-lib/vars/keycloakAuth.groovy'

// Pas: /usr/share/jenkins/...
```

---

## Intégration Keycloak

### ❌ Test Pipeline: "Keycloak not accessible"

**Diagnostic:**
```bash
# Test depuis container Jenkins
docker exec jenkins curl -I http://keycloak:8080
docker exec jenkins curl http://keycloak:8080/realms/internal/.well-known/openid-configuration
```

**Solutions:**

**1. Réseau Docker:**
```bash
# Vérifier que Jenkins est sur le même réseau que Keycloak
docker network inspect proxy | grep -A 5 jenkins
docker network inspect proxy | grep -A 5 keycloak
```

**2. DNS interne:**
```bash
# Tester résolution
docker exec jenkins nslookup keycloak
docker exec jenkins ping -c 2 keycloak
```

### ❌ "User already exists"

**Symptômes:**
```
HTTP 409 Conflict
User 'jdoe' already exists
```

**Solution:**
```groovy
// Vérifier existence avant création
def userExists = false
try {
    def userId = keycloakUser.getUserId(
        accessToken: token,
        realm: 'internal',
        username: 'jdoe'
    )
    userExists = true
} catch (Exception e) {
    userExists = false
}

if (userExists) {
    keycloakUser.updateUser(...)
} else {
    keycloakUser.createUser(...)
}
```

### ❌ "Group not found"

**Symptômes:**
```
Failed to add user to group
Group 'IT' not found in realm 'internal'
```

**Solutions:**

**1. Créer le groupe:**
```
Keycloak Admin → Realms → internal → Groups → Create Group
Name: IT
```

**2. Vérifier nom exact:**
```bash
# Les noms sont case-sensitive
IT ≠ it ≠ It
```

---

## Performance

### 🐌 Jenkins très lent

**Diagnostic:**
```bash
# Vérifier utilisation ressources
docker stats jenkins

# Vérifier logs GC
docker logs jenkins | grep -i "garbage collection"
```

**Solutions:**

**1. Augmenter mémoire:**
```yaml
environment:
  - JAVA_OPTS=-Xmx4g -Xms1g -XX:+UseG1GC
```

**2. Limiter builds concurrents:**
```
Manage Jenkins → Configure System
# of executors: 2
```

**3. Nettoyer anciens builds:**
```
Job Configuration → Discard old builds
Max # of builds to keep: 50
```

### 💾 Disque plein

**Diagnostic:**
```bash
# Espace utilisé
docker exec jenkins df -h /var/jenkins_home

# Taille des workspaces
docker exec jenkins du -sh /var/jenkins_home/workspace/*
```

**Solutions:**

**1. Nettoyer workspaces:**
```bash
docker exec jenkins rm -rf /var/jenkins_home/workspace/*
```

**2. Nettoyer anciens builds:**
```groovy
// Dans pipeline
options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
}
```

**3. Augmenter volume:**
```bash
# Sauvegarder données
docker run --rm -v jenkins_home:/data -v $(pwd):/backup alpine tar czf /backup/jenkins-backup.tar.gz /data

# Recréer volume plus grand
docker volume rm jenkins_home
docker volume create --opt type=none --opt device=/mnt/large-disk/jenkins --opt o=bind jenkins_home
```

---

## Docker

### ❌ "Cannot connect to Docker daemon"

**Symptômes:**
```
Error: Cannot connect to the Docker daemon at unix:///var/run/docker.sock
```

**Solutions:**

**1. Vérifier socket monté:**
```yaml
volumes:
  - /var/run/docker.sock:/var/run/docker.sock
```

**2. Permissions:**
```bash
# Ajouter jenkins au groupe docker (dans container)
docker exec -u root jenkins usermod -aG docker jenkins
docker restart jenkins
```

**3. Tester depuis container:**
```bash
docker exec jenkins docker ps
```

### ❌ Volume permissions

**Symptômes:**
```
Permission denied: /var/jenkins_home/...
```

**Solutions:**
```bash
# Fix permissions
docker exec -u root jenkins chown -R jenkins:jenkins /var/jenkins_home

# Ou recréer volume
docker-compose down
docker volume rm jenkins_home
docker-compose up -d
```

---

## Commandes Utiles

### 🔍 Diagnostic

```bash
# Logs en temps réel
docker-compose logs -f jenkins

# Logs d'erreur uniquement
docker logs jenkins 2>&1 | grep -i error

# Entrer dans le container
docker exec -it jenkins bash

# Vérifier santé
curl -I http://jenkins.local:8080

# Test API
curl -u admin:<token> http://jenkins.local:8080/api/json
```

### 🔄 Redémarrage

```bash
# Redémarrage propre
docker-compose restart jenkins

# Redémarrage forcé
docker-compose down jenkins
docker-compose up -d jenkins

# Rebuild complet
docker-compose down
docker-compose build --no-cache jenkins
docker-compose up -d
```

### 🧹 Nettoyage

```bash
# Nettoyer builds
docker exec jenkins rm -rf /var/jenkins_home/jobs/*/builds/*

# Nettoyer workspaces
docker exec jenkins rm -rf /var/jenkins_home/workspace/*

# Nettoyer logs
docker exec jenkins find /var/jenkins_home -name "*.log" -delete
```

---

## Support

### 📚 Ressources

- [Jenkins Documentation](https://www.jenkins.io/doc/)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [OIDC Plugin](https://plugins.jenkins.io/oic-auth/)
- [Job DSL Plugin](https://plugins.jenkins.io/job-dsl/)

### 🐛 Rapporter un Bug

1. Collecter les logs
2. Décrire le comportement attendu vs réel
3. Indiquer la configuration (OS, Docker version, etc.)
4. Joindre les logs pertinents

---

**⬅️ Retour au [README](./README.md)**
