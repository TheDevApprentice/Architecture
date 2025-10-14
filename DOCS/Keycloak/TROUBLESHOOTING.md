# 🛠️ Dépannage Keycloak

## 📋 Table des Matières

- [Démarrage](#démarrage)
- [Base de Données](#base-de-données)
- [Import Realms](#import-realms)
- [Authentification](#authentification)

---

## Démarrage

### Container ne démarre pas

```bash
# Vérifier logs
docker logs keycloak
docker logs keycloak-db

# Vérifier DB ready
docker exec keycloak-db pg_isready -U keycloak

# Restart
docker-compose restart keycloak-db keycloak
```

### Variables manquantes

```bash
# Vérifier env
docker exec keycloak env | grep KC_

# Requis:
# KC_DB_URL, KC_DB_USERNAME, KC_DB_PASSWORD
# KEYCLOAK_ADMIN, KEYCLOAK_ADMIN_PASSWORD
```

---

## Base de Données

### Erreur connexion DB

```bash
# Test connexion
docker exec keycloak-db psql -U keycloak -d keycloak -c "SELECT 1"

# Vérifier network
docker network inspect keycloaknet | grep keycloak
```

---

## Import Realms

### JSON invalide

```bash
# Valider
docker exec keycloak cat /opt/keycloak/data/import/02-internal.json | jq

# Variables non remplacées?
docker exec keycloak env | grep KC_SECRET
```

---

## Authentification

### Invalid redirect_uri

```
Admin Console → Clients → jenkins
Valid Redirect URIs: http://jenkins.local/*
Web Origins: http://jenkins.local
```

### User not found

```bash
# Vérifier realm
curl "http://keycloak.local/admin/realms/internal/users?username=jdoe"
```

### Password trop faible

```
Realm Settings → Authentication → Policies
Vérifier: Length, Uppercase, Lowercase, Digits, Special
```

---

## Thèmes

### Thème non appliqué

```bash
# Thème existe?
docker exec keycloak ls /opt/keycloak/themes/jenkins

# Clear cache
docker-compose restart keycloak
```

---

## Performance

### Keycloak lent

```bash
# Augmenter RAM
environment:
  - JAVA_OPTS_APPEND=-Xms512m -Xmx2g

# Nettoyer sessions
Admin Console → Sessions → Revoke All
```

---

## Commandes Utiles

```bash
# Logs temps réel
docker-compose logs -f keycloak

# Restart propre
docker-compose restart keycloak

# Reset complet (DEV!)
docker-compose down
docker volume rm keycloak_db
docker-compose up -d
```

---

**⬅️ Retour au [README](./README.md)**
