# 🔐 Sécurité Keycloak

## 📋 Table des Matières

- [Configuration Sécurité](#configuration-sécurité)
- [Brute Force Protection](#brute-force-protection)
- [Token Security](#token-security)
- [Password Policies](#password-policies)
- [HTTPS/TLS](#httpstls)
- [Meilleures Pratiques](#meilleures-pratiques)

---

## Configuration Sécurité

### Brute Force Protection

**Activé par défaut dans realm internal:**

```json
{
  "bruteForceProtected": true,
  "permanentLockout": false,
  "maxTemporaryLockouts": 3,
  "maxFailureWaitSeconds": 900,
  "minimumQuickLoginWaitSeconds": 60,
  "waitIncrementSeconds": 60,
  "quickLoginCheckMilliSeconds": 1000,
  "maxDeltaTimeSeconds": 43200,
  "failureFactor": 5
}
```

**Comportement:**
- **5 échecs** → Blocage 1 minute
- **10 échecs** → Blocage 2 minutes
- **15 échecs** → Blocage 4 minutes
- **Max 3 lockouts temporaires** → Investigation manuelle

### OTP (One-Time Password)

**Configuration:**
```
Realm Settings → Authentication → OTP Policy

Type: Time-based
Algorithm: HmacSHA1
Digits: 6
Period: 30 seconds
Initial counter: 0
```

**Activer pour utilisateurs:**
```
Users → <username> → Required Actions → Configure OTP
```

---

## Token Security

### Token Lifespans

**Configuration recommandée Production:**

```json
{
  "accessTokenLifespan": 300,                    // 5 min
  "accessTokenLifespanForImplicitFlow": 900,     // 15 min
  "ssoSessionIdleTimeout": 1800,                 // 30 min
  "ssoSessionMaxLifespan": 36000,                // 10 heures
  "offlineSessionIdleTimeout": 2592000,          // 30 jours
  "accessCodeLifespan": 60,                      // 1 min
  "accessCodeLifespanLogin": 1800                // 30 min
}
```

### Token Rotation

**Refresh tokens:**
```json
{
  "revokeRefreshToken": true,
  "refreshTokenMaxReuse": 0
}
```

- Refresh token révoqué après usage
- Nouveau refresh token émis à chaque refresh
- Protection contre token replay

### PKCE (Proof Key for Code Exchange)

**Activé pour tous les clients OIDC:**

```json
{
  "attributes": {
    "pkce.code.challenge.method": "S256"
  }
}
```

**Avantages:**
- Protection contre interception authorization code
- Sécurité renforcée pour applications publiques
- Standard recommandé OAuth 2.1

---

## Password Policies

### Policy Recommandée Production

```
Admin Console → Realms → internal → Authentication → Policies

Policies actives:
✅ Length (12)
✅ Uppercase Characters (1)
✅ Lowercase Characters (1)
✅ Digits (1)
✅ Special Characters (1)
✅ Not Username
✅ Not Email
✅ Password History (3)
✅ Expire Password (90 days)
```

### Exemples Passwords Valides

- ✅ `MyP@ssw0rd123`
- ✅ `Secure#Pass2024`
- ✅ `Admin!Pass456`
- ❌ `password` (trop court)
- ❌ `Password123` (pas de caractère spécial)
- ❌ `jdoe123!` (contient username)

---

## HTTPS/TLS

### Production Configuration

**⚠️ CRITIQUE: Activer HTTPS en production**

```yaml
environment:
  - KC_HOSTNAME=keycloak.company.com
  - KC_HOSTNAME_STRICT=true
  - KC_HOSTNAME_STRICT_HTTPS=true
  - KC_HTTP_ENABLED=false
  - KC_HTTPS_CERTIFICATE_FILE=/opt/keycloak/conf/server.crt
  - KC_HTTPS_CERTIFICATE_KEY_FILE=/opt/keycloak/conf/server.key
```

### Certificats SSL

**Option 1: Let's Encrypt (via Traefik)**

```yaml
labels:
  - "traefik.enable=true"
  - "traefik.http.routers.keycloak.rule=Host(`keycloak.company.com`)"
  - "traefik.http.routers.keycloak.tls.certresolver=letsencrypt"
```

**Option 2: Certificats propres**

```bash
# Générer certificat auto-signé (dev only)
openssl req -newkey rsa:2048 -nodes \
  -keyout server.key -x509 -days 365 -out server.crt

# Copier dans container
docker cp server.crt keycloak:/opt/keycloak/conf/
docker cp server.key keycloak:/opt/keycloak/conf/
```

---

## Meilleures Pratiques

### 1️⃣ Admin Bootstrap

```bash
# ✅ Bon: Variable d'environnement
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=<complex-password>

# ❌ Mauvais: Hardcoded dans config
```

**Après premier démarrage:**
1. Créer compte admin personnel
2. Supprimer compte bootstrap
3. Activer OTP pour admins

### 2️⃣ Realm Isolation

```
✅ master realm: Admin uniquement
✅ internal realm: Applications
❌ Ne pas utiliser master pour apps
```

### 3️⃣ Service Accounts

**Permissions minimales:**

```json
{
  "serviceAccountsEnabled": true,
  "serviceAccountsRoles": {
    "realm-management": [
      "manage-users",     // Si besoin
      "view-users",       // Lecture
      "query-users"       // Recherche
    ]
  }
}
```

**❌ Ne pas donner:**
- `realm-admin`
- `manage-realm`
- `manage-clients`

### 4️⃣ Client Secrets

```bash
# Générer secrets sécurisés
openssl rand -base64 32

# Rotation régulière (90 jours)
curl -X POST \
  -H "Authorization: Bearer <token>" \
  "http://keycloak.local/admin/realms/internal/clients/{id}/client-secret"
```

### 5️⃣ Audit Logging

**Activer Events:**

```
Realm Settings → Events → Event Listeners
✅ jboss-logging
✅ email (optionnel)

Save Events: ON
Expiration: 30 days

Event Types to Save:
✅ LOGIN
✅ LOGIN_ERROR
✅ LOGOUT
✅ CODE_TO_TOKEN
✅ REFRESH_TOKEN
```

**Consulter logs:**
```bash
docker logs keycloak | grep "type=LOGIN"
```

### 6️⃣ Database Security

```yaml
# ✅ Connexion chiffrée
KC_DB_URL: jdbc:postgresql://keycloak-db:5432/keycloak?ssl=true&sslmode=require

# ✅ Credentials via secrets
environment:
  - KC_DB_PASSWORD=${KC_DB_PASSWORD}  # From .env

# ✅ Network isolation
networks:
  - keycloaknet  # Pas d'accès externe
```

### 7️⃣ CORS Configuration

**Clients web publics:**

```json
{
  "webOrigins": [
    "https://app.company.com",
    "https://app2.company.com"
  ]
}
```

**❌ Ne jamais utiliser:**
```json
{
  "webOrigins": ["*"]  // Dangereux!
}
```

---

## Monitoring Sécurité

### Alertes à Configurer

| Event | Seuil | Action |
|-------|-------|--------|
| LOGIN_ERROR | > 10/min | Alerte + Investigation |
| Brute force lockout | Tout | Alerte Security Team |
| Token refresh fail | > 50/min | Alerte |
| Admin login | Tout | Log + Notif |
| Client secret regen | Tout | Audit log |

### Logs à Surveiller

```bash
# Échecs de login
docker logs keycloak | grep "LOGIN_ERROR"

# Lockouts
docker logs keycloak | grep "brute force"

# Admin actions
docker logs keycloak | grep "ADMIN_EVENT"
```

---

## Checklist Sécurité

### Pré-Production

- [ ] HTTPS activé
- [ ] Certificats valides
- [ ] Brute force protection ON
- [ ] Password policy configurée
- [ ] Service accounts permissions minimales
- [ ] Client secrets complexes (32+ chars)
- [ ] Audit logging activé
- [ ] Token lifespans ajustés
- [ ] CORS configuré strictement
- [ ] Database chiffrée
- [ ] Admin bootstrap désactivé
- [ ] OTP pour admins

### Post-Déploiement

- [ ] Scan vulnérabilités (Trivy, etc.)
- [ ] Test penetration
- [ ] Rotation secrets (90 jours)
- [ ] Review logs sécurité
- [ ] Mise à jour Keycloak
- [ ] Backup base de données

---

## Incident Response

### Procédure Compromission

**1. Isolation:**
```bash
# Bloquer trafic
docker network disconnect proxy keycloak
```

**2. Investigation:**
```bash
# Examiner logs
docker logs keycloak > incident-$(date +%F).log

# Chercher anomalies
grep -i "LOGIN_ERROR\|ADMIN_EVENT" incident-*.log
```

**3. Remédiation:**
```bash
# Régénérer tous les secrets
for client in jenkins minio grafana; do
  # Via Admin API
done

# Reset passwords admin
# Forcer re-authentication tous users
```

**4. Post-Incident:**
- Rapport d'incident
- Amélioration contrôles
- Formation équipe

---

**⬅️ Retour au [README](./README.md)**
