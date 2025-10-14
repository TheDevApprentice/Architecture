# ⚙️ Configuration Keycloak - Realms, Clients, Groupes

## 📋 Table des Matières

- [Realms](#realms)
- [Clients](#clients)
- [Groupes et Rôles](#groupes-et-rôles)
- [Token Configuration](#token-configuration)
- [Sécurité](#sécurité)

---

## Realms

### Master Realm

**Objectif:** Administration Keycloak

```json
{
  "realm": "master",
  "enabled": true,
  "displayName": "Keycloak Master",
  "loginTheme": "master"
}
```

**Configuration:**
- Utilisateur bootstrap: `${KEYCLOAK_ADMIN}`
- Accès Admin Console uniquement
- Ne pas utiliser pour applications

### Internal Realm

**Objectif:** Applications internes SSO

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
  
  "loginWithEmailAllowed": true,
  "duplicateEmailsAllowed": false,
  "resetPasswordAllowed": true,
  "rememberMe": true
}
```

---

## Clients

### Client: jenkins

```json
{
  "clientId": "jenkins",
  "secret": "${KC_SECRET_JENKINS}",
  "name": "Jenkins",
  "protocol": "openid-connect",
  "publicClient": false,
  "standardFlowEnabled": true,
  "directAccessGrantsEnabled": false,
  "serviceAccountsEnabled": false,
  "redirectUris": [
    "http://${JENKINS_URL}/*",
    "http://${JENKINS_URL}/securityRealm/finishLogin",
    "http://${JENKINS_URL}/OicLogout"
  ],
  "webOrigins": ["http://${JENKINS_URL}"],
  "attributes": {
    "login_theme": "jenkins",
    "pkce.code.challenge.method": "S256",
    "post.logout.redirect.uris": "http://${JENKINS_URL}"
  }
}
```

### Client: jenkins-automation

```json
{
  "clientId": "jenkins-automation",
  "secret": "${KC_SECRET_JENKINS_AUTOMATION}",
  "protocol": "openid-connect",
  "publicClient": false,
  "standardFlowEnabled": false,
  "directAccessGrantsEnabled": false,
  "serviceAccountsEnabled": true,
  "serviceAccountsRoles": {
    "realm-management": [
      "manage-users",
      "view-users",
      "query-groups",
      "query-users"
    ]
  }
}
```

### Client: minio

```json
{
  "clientId": "minio",
  "secret": "${KC_SECRET_MINIO}",
  "protocol": "openid-connect",
  "redirectUris": [
    "http://${MINIO_URL}/*",
    "http://${MINIO_URL}/oauth_callback"
  ],
  "attributes": {
    "login_theme": "minio"
  }
}
```

### Client: grafana

```json
{
  "clientId": "grafana",
  "protocol": "openid-connect",
  "publicClient": true,
  "redirectUris": [
    "http://${GRAFANA_URL}/*",
    "http://${GRAFANA_URL}/login/generic_oauth"
  ]
}
```

---

## Groupes et Rôles

### Realm Roles

```json
{
  "roles": {
    "realm": [
      {
        "name": "config_service",
        "description": "Access to configure internal services"
      },
      {
        "name": "view_service",
        "description": "View-only access to internal services"
      }
    ]
  }
}
```

### Client Roles (Jenkins)

```json
{
  "roles": {
    "client": {
      "jenkins": [
        {
          "name": "admin",
          "description": "Jenkins administrators"
        },
        {
          "name": "user",
          "description": "Jenkins standard users"
        }
      ]
    }
  }
}
```

### Groupes

#### Groupe IT

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
- Administration complète
- Configuration services
- Accès Jenkins admin

#### Groupe Jenkins

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
- Lecture services
- Accès Jenkins standard
- Build/Deploy jobs

---

## Token Configuration

### Token Lifespans

```json
{
  "accessTokenLifespan": 300,                    // 5 minutes
  "accessTokenLifespanForImplicitFlow": 900,     // 15 minutes
  "ssoSessionIdleTimeout": 1800,                 // 30 minutes
  "ssoSessionMaxLifespan": 36000,                // 10 heures
  "offlineSessionIdleTimeout": 2592000,          // 30 jours
  "accessCodeLifespan": 60,                      // 1 minute
  "accessCodeLifespanUserAction": 300,           // 5 minutes
  "accessCodeLifespanLogin": 1800                // 30 minutes
}
```

### Token Claims

**ID Token Claims:**
- `sub` - User ID
- `preferred_username` - Username
- `email` - Email address
- `name` - Full name
- `groups` - User groups
- `locale` - Preferred language

---

## Sécurité

### Brute Force Protection

```json
{
  "bruteForceProtected": true,
  "permanentLockout": false,
  "maxTemporaryLockouts": 3,
  "maxFailureWaitSeconds": 900,           // 15 minutes
  "minimumQuickLoginWaitSeconds": 60,     // 1 minute
  "waitIncrementSeconds": 60,
  "quickLoginCheckMilliSeconds": 1000,
  "maxDeltaTimeSeconds": 43200,           // 12 heures
  "failureFactor": 5                      // 5 tentatives max
}
```

### Password Policy (Recommandé)

```
Admin Console → Realms → internal → Authentication → Policies

- Length: 12 minimum
- Uppercase: 1 minimum
- Lowercase: 1 minimum
- Digits: 1 minimum
- Special Characters: 1 minimum
- Not Username
- Not Email
- Password History: 3
```

---

## SMTP Configuration

```json
{
  "smtpServer": {
    "host": "smtp.company.com",
    "port": "587",
    "from": "noreply@company.com",
    "fromDisplayName": "Company SSO",
    "ssl": "true",
    "starttls": "true",
    "auth": "true",
    "user": "smtp-user",
    "password": "smtp-password"
  }
}
```

---

**⬅️ Retour au [README](./README.md)**
