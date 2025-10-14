# 🚀 Déploiement Jenkins - Guide Complet

## 📋 Table des Matières

- [Prérequis](#prérequis)
- [Configuration Keycloak](#configuration-keycloak)
- [Déploiement Docker](#déploiement-docker)
- [Vérification](#vérification)

---

## Prérequis

### 🔧 Infrastructure

| Composant | Version | Requis |
|-----------|---------|--------|
| Docker | 20.10+ | ✅ |
| Docker Compose | 2.0+ | ✅ |
| Keycloak | 23+ | ✅ |

### 💾 Ressources

| Resource | Minimum | Recommandé |
|----------|---------|------------|
| CPU | 2 cores | 4 cores |
| RAM | 2 GB | 4 GB |
| Disque | 10 GB | 20 GB |

---

## Configuration Keycloak

### 1️⃣ Créer les Groupes

```
Realm: internal → Groups → Create Group
- IT
- Jenkins
```

### 2️⃣ Client OIDC `jenkins`

```
Client ID: jenkins
Access Type: confidential
Valid Redirect URIs: http://${JENKINS_URL}/*
```

**Mapper groups:**
```
Token Claim Name: groups
Add to ID token: ON
```

### 3️⃣ Client Service Account `jenkins-automation`

```
Client ID: jenkins-automation
Service Accounts Enabled: ON
```

**Roles:** manage-users, view-users, query-groups

---

## Déploiement Docker

### 📝 Fichier .env

```bash
KC_BOOTSTRAP_ADMIN_USERNAME=admin
KC_BOOTSTRAP_ADMIN_PASSWORD=<password>
KC_SECRET_JENKINS_AUTOMATION=<secret>
TZ=Europe/Paris
```

### 🐳 Docker Compose

```yaml
services:
  jenkins:
    build: ./server/Jenkins
    ports:
      - "8080:8080"
      - "50000:50000"
    environment:
      - JENKINS_URL=${JENKINS_URL}
      - KC_URL_INTERNAL=${KC_URL_INTERNAL}:8080
      - KC_REALM=internal
      - OIDC_CLIENT_ID=${OIDC_CLIENT_ID}
      - OIDC_CLIENT_SECRET=${KC_SECRET_JENKINS_AUTOMATION}
      - KC_CLIENT_ID_JENKINS_AUTOMATION=${KC_CLIENT_ID_JENKINS_AUTOMATION}
      - KC_SECRET_JENKINS_AUTOMATION=${KC_SECRET_JENKINS_AUTOMATION}
    volumes:
      - jenkins_home:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock
    networks:
      - proxy
      - dbnet
```

### 🚀 Démarrer

```bash
docker-compose build jenkins
docker-compose up -d jenkins
docker-compose logs -f jenkins
```

---

## Vérification

### 1️⃣ Accès Web

```
http://${JENKINS_URL}
```

### 2️⃣ Logs de Démarrage

```bash
docker logs jenkins | grep "entrypoint"
docker logs jenkins | grep "Creating Keycloak"
```

### 3️⃣ Test Pipeline

```
Jenkins → Test-Keycloak-Integration → Build Now
```

**Attendu:** Tous les tests passent ✅

---

## Dépannage

### Erreur "Failed to obtain client secret"

```bash
# Vérifier connectivité (utilisation interne au réseau docker nécessite docker dns résolution)
docker exec jenkins curl http://keycloak:8080/realms/internal/.well-known/openid-configuration

# Vérifier variables
docker exec jenkins env | grep KC_
```

### Pipelines non créés

```bash
docker logs jenkins | grep "init.groovy"
```

---

**⬅️ Retour au [README](./README.md)**
