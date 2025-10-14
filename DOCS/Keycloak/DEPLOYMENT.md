# 🚀 Déploiement Keycloak - Guide Complet

## 📋 Table des Matières

- [Prérequis](#prérequis)
- [Configuration](#configuration)
- [Déploiement](#déploiement)
- [Vérification](#vérification)
- [Post-Installation](#post-installation)

---

## Prérequis

### Infrastructure

| Composant | Version | Requis |
|-----------|---------|--------|
| Docker | 20.10+ | ✅ |
| Docker Compose | 2.0+ | ✅ |
| PostgreSQL | 16+ | ✅ (intégré) |

### Ressources

| Resource | Minimum | Recommandé |
|----------|---------|------------|
| CPU | 2 cores | 4 cores |
| RAM | 2 GB | 4 GB |
| Disque | 5 GB | 10 GB |

---

## Configuration

### Fichier .env

```bash
# Database
KC_DB_PASSWORD=<secure-password>

# Keycloak Admin
KC_BOOTSTRAP_ADMIN_USERNAME=admin
KC_BOOTSTRAP_ADMIN_PASSWORD=<admin-password>

# Hostname
KC_HOSTNAME=keycloak.local

# Client Secrets (générer avec: openssl rand -base64 32)
KC_SECRET_JENKINS=<jenkins-client-secret>
KC_SECRET_JENKINS_AUTOMATION=<automation-secret>
KC_SECRET_MINIO=<minio-secret>
KC_SECRET_GRAFANA=<grafana-secret>

# Service URLs (pour realm import)
JENKINS_URL=jenkins.local
MINIO_URL=minio.local
GRAFANA_URL=grafana.local
```

---

## Déploiement

### Docker Compose

```yaml
services:
  keycloak-db:
    build: ./server/Keycloak/db
    container_name: keycloak-db
    environment:
      - POSTGRES_DB=keycloak
      - POSTGRES_USER=keycloak
      - POSTGRES_PASSWORD=${KC_DB_PASSWORD}
    volumes:
      - keycloak_db:/var/lib/postgresql/data
    networks:
      - keycloaknet
    healthcheck:
      test: ["CMD", "pg_isready", "-U", "keycloak"]
      interval: 10s
      timeout: 5s
      retries: 5
  
  keycloak:
    build: ./server/Keycloak
    container_name: keycloak
    environment:
      # Database
      - KC_DB=postgres
      - KC_DB_URL=jdbc:postgresql://keycloak-db:5432/keycloak
      - KC_DB_USERNAME=keycloak
      - KC_DB_PASSWORD=${KC_DB_PASSWORD}
      
      # Admin Bootstrap
      - KEYCLOAK_ADMIN=${KC_BOOTSTRAP_ADMIN_USERNAME}
      - KEYCLOAK_ADMIN_PASSWORD=${KC_BOOTSTRAP_ADMIN_PASSWORD}
      
      # Hostname
      - KC_HOSTNAME=${KC_HOSTNAME}
      - KC_HTTP_ENABLED=true
      - KC_HOSTNAME_STRICT=false
      - KC_HOSTNAME_STRICT_HTTPS=false
      
      # Import realm variables
      - HOST=${KC_HOSTNAME}
      - KC_CLIENT_ID_JENKINS=jenkins
      - KC_SECRET_JENKINS=${KC_SECRET_JENKINS}
      - KC_CLIENT_ID_JENKINS_AUTOMATION=jenkins-automation
      - KC_SECRET_JENKINS_AUTOMATION=${KC_SECRET_JENKINS_AUTOMATION}
      - JENKINS_URL=${JENKINS_URL}
      - MINIO_URL=${MINIO_URL}
      - GRAFANA_URL=${GRAFANA_URL}
    ports:
      - "8080:8080"
    networks:
      - proxy
      - keycloaknet
    depends_on:
      keycloak-db:
        condition: service_healthy
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.keycloak.rule=Host(`${KC_HOSTNAME}`)"
      - "traefik.http.services.keycloak.loadbalancer.server.port=8080"

volumes:
  keycloak_db:

networks:
  proxy:
    external: true
  keycloaknet:
    driver: bridge
```

### Démarrer les Services

```bash
# Build les images
docker-compose build keycloak-db keycloak

# Démarrer
docker-compose up -d keycloak-db keycloak

# Suivre les logs
docker-compose logs -f keycloak
```

---

## Vérification

### 1. Vérifier le Démarrage

```bash
# Logs Keycloak
docker logs keycloak

# Chercher messages clés:
# - "Keycloak running"
# - "Import: Processing realm file"
# - "Import: Realm imported successfully"
```

### 2. Tester l'Accès

```bash
# Health check
curl http://keycloak.local/health

# Realm discovery
curl http://keycloak.local/realms/internal/.well-known/openid-configuration
```

### 3. Accès Admin Console

```
URL: http://keycloak.local/admin
Username: admin
Password: <KC_BOOTSTRAP_ADMIN_PASSWORD>
```

### 4. Vérifier Import Realms

**Admin Console → Realms:**
- ✅ master realm
- ✅ internal realm

**Internal Realm → Clients:**
- ✅ jenkins
- ✅ jenkins-automation
- ✅ minio
- ✅ grafana

**Internal Realm → Groups:**
- ✅ IT
- ✅ Jenkins

---

## Post-Installation

### Créer Premier Utilisateur

**Via Admin Console:**
```
Realms → internal
Users → Add User
  Username: jdoe
  Email: john.doe@company.com
  Email Verified: ON
  
Credentials tab:
  Set Password
  Temporary: OFF
  
Groups tab:
  Join Groups → IT ou Jenkins
```

**Via API:**
```bash
# Get admin token
TOKEN=$(curl -X POST "http://keycloak.local/realms/master/protocol/openid-connect/token" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=<password>" \
  -d "grant_type=password" | jq -r .access_token)

# Create user
curl -X POST "http://keycloak.local/admin/realms/internal/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username":"jdoe",
    "email":"john.doe@company.com",
    "enabled":true,
    "emailVerified":true,
    "credentials":[{"type":"password","value":"SecurePass123!","temporary":false}]
  }'
```

### Configurer SMTP (Optionnel)

```
Realms → internal → Realm Settings → Email

Host: smtp.company.com
Port: 587
From: noreply@company.com
From Display Name: Company SSO
Enable SSL: ON
Enable StartTLS: ON
Enable Authentication: ON
Username: smtp-user
Password: <smtp-password>

Test Connection → Send test email
```

### Vérifier Intégrations

**Tester Jenkins SSO:**
```
1. Accéder à Jenkins
2. Cliquer "Log in"
3. Redirection vers Keycloak (thème jenkins)
4. Login avec utilisateur créé
5. Redirection vers Jenkins (authentifié)
```

---

## Dépannage

### Problème: Database not ready

```bash
# Vérifier DB
docker exec keycloak-db pg_isready -U keycloak

# Restart
docker-compose restart keycloak-db
docker-compose restart keycloak
```

### Problème: Import realm failed

```bash
# Vérifier JSON syntax
docker exec keycloak cat /opt/keycloak/data/import/02-internal.json | jq

# Vérifier variables d'environnement
docker exec keycloak env | grep KC_
```

### Problème: Cannot access Admin Console

```bash
# Vérifier hostname
docker exec keycloak env | grep KC_HOSTNAME

# Vérifier /etc/hosts
ping keycloak.local
```

---

**⬅️ Retour au [README](./README.md)**
