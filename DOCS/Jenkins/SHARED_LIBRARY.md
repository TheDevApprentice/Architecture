# 📚 Bibliothèque Partagée Keycloak - API Reference

## 📋 Table des Matières

- [Vue d'ensemble](#vue-densemble)
- [Module keycloakAuth.groovy](#module-keycloakauthgroovy)
- [Module keycloakUser.groovy](#module-keycloakusergroovy)
- [Utilisation dans les Pipelines](#utilisation-dans-les-pipelines)
- [Exemples](#exemples)

---

## Vue d'ensemble

La **bibliothèque partagée Keycloak** encapsule les interactions avec l'API REST Admin de Keycloak.

### 📦 Modules

| Module | Fonctions | Objectif |
|--------|-----------|----------|
| **keycloakAuth.groovy** | 3 | Authentification et tokens |
| **keycloakUser.groovy** | 9 | Gestion utilisateurs |

### 📍 Emplacement

```
/var/jenkins_home/workflow-libs/keycloak-lib/vars/
├── keycloakAuth.groovy
└── keycloakUser.groovy
```

### 🔄 Chargement

```groovy
def keycloakAuth = load '/var/jenkins_home/workflow-libs/keycloak-lib/vars/keycloakAuth.groovy'
def keycloakUser = load '/var/jenkins_home/workflow-libs/keycloak-lib/vars/keycloakUser.groovy'
```

---

## Module keycloakAuth.groovy

### 🔐 getServiceAccountToken()

Obtient un access token via client credentials.

**Paramètres:** Map config (utilise env vars)

**Retour:** String (JWT token)

**Exemple:**
```groovy
def token = keycloakAuth.getServiceAccountToken(
    keycloakUrl: env.KC_URL_INTERNAL,
    clientId: env.KC_CLIENT_ID,
    clientSecret: env.KC_CLIENT_SECRET
)
```

### 🔐 getAdminToken()

Obtient un token via username/password.

**Paramètres:**
- `config.username` - Admin username
- `config.password` - Admin password

**Retour:** String (JWT token)

### ✅ validateToken()

Valide un access token.

**Paramètres:**
- `config.accessToken` - Token à valider

**Retour:** Boolean (true si valide)

---

## Module keycloakUser.groovy

### 👤 createUser()

Crée un nouvel utilisateur.

**Paramètres:**
- `accessToken` ✅ - Token valide
- `username` ✅ - Nom d'utilisateur
- `email` ✅ - Email
- `firstName` ❌ - Prénom
- `lastName` ❌ - Nom
- `enabled` ❌ - Activé (défaut: true)
- `emailVerified` ❌ - Email vérifié (défaut: false)
- `password` ❌ - Mot de passe
- `temporaryPassword` ❌ - Temporaire (défaut: true)
- `locale` ❌ - Langue (défaut: 'en')

**Retour:** String (User ID)

**Exemple:**
```groovy
def userId = keycloakUser.createUser(
    accessToken: token,
    realm: 'internal',
    username: 'jdoe',
    email: 'jdoe@company.com',
    firstName: 'John',
    lastName: 'Doe',
    password: 'SecurePass123!',
    temporaryPassword: true
)
```

### ✏️ updateUser()

Met à jour un utilisateur.

**Paramètres:**
- `accessToken` ✅
- `username` ✅
- `email`, `firstName`, `lastName`, `enabled`, `emailVerified`, `locale` ❌

**Exemple:**
```groovy
keycloakUser.updateUser(
    accessToken: token,
    realm: 'internal',
    username: 'jdoe',
    email: 'newemail@company.com',
    emailVerified: true
)
```

### 🗑️ deleteUser()

Supprime un utilisateur.

**Paramètres:**
- `accessToken` ✅
- `username` ✅

**Exemple:**
```groovy
keycloakUser.deleteUser(
    accessToken: token,
    realm: 'internal',
    username: 'jdoe'
)
```

### 🔑 resetPassword()

Réinitialise le mot de passe.

**Paramètres:**
- `accessToken` ✅
- `username` ✅
- `password` ✅
- `temporary` ❌ - Défaut: true

**Exemple:**
```groovy
def newPass = keycloakUser.generatePassword()
keycloakUser.resetPassword(
    accessToken: token,
    realm: 'internal',
    username: 'jdoe',
    password: newPass,
    temporary: true
)
```

### 👥 addUserToGroup()

Ajoute un utilisateur à un groupe.

**Paramètres:**
- `accessToken` ✅
- `username` ✅
- `groupName` ✅

**Exemple:**
```groovy
keycloakUser.addUserToGroup(
    accessToken: token,
    realm: 'internal',
    username: 'jdoe',
    groupName: 'IT'
)
```

### 📋 listUsers()

Liste les utilisateurs.

**Paramètres:**
- `accessToken` ✅
- `max` ❌ - Défaut: 100

**Retour:** List<Map> (objets utilisateur)

**Exemple:**
```groovy
def users = keycloakUser.listUsers(
    accessToken: token,
    realm: 'internal',
    max: 50
)

users.each { user ->
    echo "${user.username} - ${user.email}"
}
```

### 🔍 getUserId()

Obtient l'ID d'un utilisateur.

**Paramètres:**
- `accessToken` ✅
- `username` ✅

**Retour:** String (UUID)

### 🔍 getGroupId()

Obtient l'ID d'un groupe.

**Paramètres:**
- `accessToken` ✅
- `groupName` ✅

**Retour:** String (UUID)

### 🎲 generatePassword()

Génère un mot de passe sécurisé.

**Paramètres:**
- `length` - Défaut: 16 (min: 12)

**Retour:** String

**Politique:** Au moins 1 majuscule, 1 minuscule, 1 chiffre, 1 caractère spécial

**Exemple:**
```groovy
def pass = keycloakUser.generatePassword(16)
echo "Generated: ${pass}"
// Output: Xy9@mK4pQw2#L8vN
```

---

## Utilisation dans les Pipelines

### Pipeline Complet

```groovy
def keycloakAuth
def keycloakUser

pipeline {
    agent any
    
    environment {
        KC_URL_INTERNAL = "${KC_URL_INTERNAL}"
        KC_CLIENT_ID = "${KC_CLIENT_ID_JENKINS_AUTOMATION}"
        KC_CLIENT_SECRET = "${KC_SECRET_JENKINS_AUTOMATION}"
    }
    
    stages {
        stage('Load Library') {
            steps {
                script {
                    keycloakAuth = load '/var/jenkins_home/workflow-libs/keycloak-lib/vars/keycloakAuth.groovy'
                    keycloakUser = load '/var/jenkins_home/workflow-libs/keycloak-lib/vars/keycloakUser.groovy'
                }
            }
        }
        
        stage('Authenticate') {
            steps {
                script {
                    env.ACCESS_TOKEN = keycloakAuth.getServiceAccountToken(...)
                }
            }
        }
        
        stage('Create User') {
            steps {
                script {
                    def password = keycloakUser.generatePassword(16)
                    def userId = keycloakUser.createUser(
                        accessToken: env.ACCESS_TOKEN,
                        realm: 'internal',
                        username: 'newuser',
                        email: 'newuser@company.com',
                        password: password,
                        temporaryPassword: true
                    )
                    echo "User created: ${userId}"
                }
            }
        }
    }
    
    post {
        always {
            script {
                env.ACCESS_TOKEN = null
            }
        }
    }
}
```

---

## Exemples

### Bulk User Creation

```groovy
def users = [
    [username: 'user1', email: 'user1@company.com', group: 'IT'],
    [username: 'user2', email: 'user2@company.com', group: 'Jenkins']
]

users.each { userData ->
    try {
        def password = keycloakUser.generatePassword()
        def userId = keycloakUser.createUser(
            accessToken: token,
            realm: 'internal',
            username: userData.username,
            email: userData.email,
            password: password
        )
        
        keycloakUser.addUserToGroup(
            accessToken: token,
            realm: 'internal',
            username: userData.username,
            groupName: userData.group
        )
        
        echo "✅ ${userData.username}: ${password}"
    } catch (Exception e) {
        echo "❌ Failed: ${e.message}"
    }
}
```

### Check User Exists

```groovy
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

### Password Rotation

```groovy
def usersToRotate = ['user1', 'user2', 'user3']

usersToRotate.each { username ->
    def newPass = keycloakUser.generatePassword(20)
    keycloakUser.resetPassword(
        accessToken: token,
        realm: 'internal',
        username: username,
        password: newPass,
        temporary: false
    )
    echo "✅ ${username}: ${newPass}"
}
```

### Generate User Report

```groovy
def users = keycloakUser.listUsers(
    accessToken: token,
    realm: 'internal',
    max: 1000
)

def csv = "Username,Email,Enabled,Email Verified\n"
users.each { user ->
    csv += "${user.username},${user.email},${user.enabled},${user.emailVerified}\n"
}

writeFile file: 'user-report.csv', text: csv
archiveArtifacts artifacts: 'user-report.csv'
```

---

## Gestion des Erreurs

### Try-Catch Pattern

```groovy
try {
    def userId = keycloakUser.createUser(...)
    echo "✅ User created"
} catch (Exception e) {
    echo "❌ Failed: ${e.message}"
}
```

### Token Validation

```groovy
def token = keycloakAuth.getServiceAccountToken(...)

if (!keycloakAuth.validateToken(accessToken: token)) {
    error("Token invalide")
}
```

---

## Variables d'Environnement

Toutes les fonctions utilisent ces variables:

```groovy
env.KC_URL_INTERNAL           // keycloak:8080
env.KC_REALM                  // internal
env.KC_CLIENT_ID_JENKINS_AUTOMATION
env.KC_SECRET_JENKINS_AUTOMATION
```

---

## Endpoints API Utilisés

| Fonction | Méthode | Endpoint |
|----------|---------|----------|
| getServiceAccountToken | POST | `/realms/{realm}/protocol/openid-connect/token` |
| validateToken | POST | `/realms/{realm}/protocol/openid-connect/token/introspect` |
| createUser | POST | `/admin/realms/{realm}/users` |
| updateUser | PUT | `/admin/realms/{realm}/users/{userId}` |
| deleteUser | DELETE | `/admin/realms/{realm}/users/{userId}` |
| resetPassword | PUT | `/admin/realms/{realm}/users/{userId}/reset-password` |
| addUserToGroup | PUT | `/admin/realms/{realm}/users/{userId}/groups/{groupId}` |
| listUsers | GET | `/admin/realms/{realm}/users` |

---

**⬅️ Retour au [README](./README.md)**
