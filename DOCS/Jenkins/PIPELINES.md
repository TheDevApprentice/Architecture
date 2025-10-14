# 🔄 Jenkins Pipelines - Documentation Complète

## 📋 Table des Matières

- [Vue d'ensemble](#vue-densemble)
- [Pipeline 1: Keycloak User Management](#pipeline-1-keycloak-user-management)
- [Pipeline 2: Employee Onboarding Webhook](#pipeline-2-employee-onboarding-webhook)
- [Pipeline 3: Test Keycloak Integration](#pipeline-3-test-keycloak-integration)
- [Utilisation des Pipelines](#utilisation-des-pipelines)
- [Exemples d'Utilisation](#exemples-dutilisation)

---

## Vue d'ensemble

Cette configuration Jenkins inclut **3 pipelines préconfigurés** pour automatiser la gestion des utilisateurs Keycloak. Ces pipelines sont créés automatiquement au démarrage de Jenkins via les scripts `init.groovy.d`.

### Pipelines Disponibles

| Pipeline | Type | Trigger | Objectif |
|----------|------|---------|----------|
| **Keycloak-User-Management** | Paramétré | Manuel | Gestion interactive des utilisateurs |
| **Employee-Onboarding-Webhook** | Automatique | Webhook | Onboarding automatisé d'employés |
| **Test-Keycloak-Integration** | Test | Manuel | Suite de tests d'intégration |

---

## Pipeline 1: Keycloak User Management

### 📄 Description

Pipeline interactif permettant de gérer manuellement les utilisateurs Keycloak via l'interface Jenkins. Il offre une interface conviviale pour toutes les opérations CRUD sur les utilisateurs.

### 📍 Emplacement

```
Fichier: server/Jenkins/config/pipelines/keycloak-user-management.jenkinsfile
Job: Keycloak-User-Management
```

### 🎯 Actions Disponibles

1. **CREATE_USER** - Créer un nouveau compte utilisateur
2. **UPDATE_USER** - Mettre à jour un utilisateur existant
3. **DELETE_USER** - Supprimer un utilisateur
4. **RESET_PASSWORD** - Réinitialiser le mot de passe
5. **ADD_TO_GROUP** - Ajouter un utilisateur à un groupe
6. **LIST_USERS** - Lister tous les utilisateurs du realm

### ⚙️ Paramètres

| Paramètre | Type | Obligatoire | Description | Valeur par défaut |
|-----------|------|-------------|-------------|-------------------|
| **ACTION** | Choice | ✅ | Action à effectuer | `CREATE_USER` |
| **REALM** | String | ✅ | Realm Keycloak | `internal` |
| **USERNAME** | String | ✅ (sauf LIST) | Nom d'utilisateur | - |
| **EMAIL** | String | ✅ (CREATE) | Adresse email | - |
| **FIRST_NAME** | String | ❌ | Prénom | - |
| **LAST_NAME** | String | ❌ | Nom de famille | - |
| **GROUP_NAME** | String | ❌ | Groupe à assigner | - |
| **LOCALE** | Choice | ❌ | Langue préférée | `en` |
| **EMAIL_VERIFIED** | Boolean | ❌ | Email vérifié | `false` |
| **ENABLED** | Boolean | ❌ | Compte activé | `true` |
| **TEMPORARY_PASSWORD** | Boolean | ❌ | Mot de passe temporaire | `true` |
| **PASSWORD** | Password | ❌ | Mot de passe (auto-généré si vide) | `changeMe123!` |

### 🔄 Flux d'Exécution

```
┌─────────────────────────────────────────┐
│  1. Load Keycloak Library               │
│     - keycloakAuth.groovy               │
│     - keycloakUser.groovy               │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│  2. Validate Parameters                 │
│     - Check required fields             │
│     - Validate action-specific params   │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│  3. Get Access Token                    │
│     - Service account authentication    │
│     - Keycloak token endpoint           │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│  4. Execute Action                      │
│     - Switch based on ACTION param      │
│     - Call appropriate library function │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│  5. Test Connection (CREATE only)       │
│     - Optional connectivity test        │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│  6. Cleanup                             │
│     - Clear sensitive data              │
│     - Null ACCESS_TOKEN                 │
└─────────────────────────────────────────┘
```

### 📝 Exemple d'Utilisation

#### Créer un Utilisateur

1. Accéder à Jenkins → **Keycloak-User-Management**
2. Cliquer sur **"Build with Parameters"**
3. Remplir les paramètres:
   - ACTION: `CREATE_USER`
   - REALM: `internal`
   - USERNAME: `jdoe`
   - EMAIL: `john.doe@company.com`
   - FIRST_NAME: `John`
   - LAST_NAME: `Doe`
   - GROUP_NAME: `Jenkins`
   - PASSWORD: (laisser vide pour auto-génération)
4. Cliquer sur **"Build"**

**Résultat:**
```
✅ User created successfully with ID: abc-123-def
📧 Username: jdoe
📧 Email: john.doe@company.com
🔑 Generated password: Xy9@mK4pQw2#L8vN
⚠️  IMPORTANT: Save this password securely!
👥 Adding user to group: Jenkins
✅ User added to group 'Jenkins'
```

#### Réinitialiser un Mot de Passe

1. ACTION: `RESET_PASSWORD`
2. USERNAME: `jdoe`
3. TEMPORARY_PASSWORD: `true`
4. PASSWORD: (laisser vide pour auto-génération)

**Résultat:**
```
✅ Password reset successfully
🔑 New password: Np5&qT2xRw9@Km7L
```

### 🔒 Variables d'Environnement Utilisées

```groovy
environment {
    KC_URL_INTERNAL = "${KC_URL_INTERNAL}"
    KC_CLIENT_ID = "${KC_CLIENT_ID_JENKINS_AUTOMATION}"
    KC_CLIENT_SECRET = "${KC_SECRET_JENKINS_AUTOMATION}"
}
```

### ⚠️ Gestion des Erreurs

- **Username requis**: Erreur si ACTION nécessite username et il est vide
- **Email requis**: Erreur si CREATE_USER sans email
- **Token invalide**: Erreur si l'authentification échoue
- **Utilisateur inexistant**: Erreur si UPDATE/DELETE d'un utilisateur qui n'existe pas

---

## Pipeline 2: Employee Onboarding Webhook

### 📄 Description

Pipeline automatisé conçu pour être déclenché par un webhook externe (système RH, API, etc.). Il automatise complètement le processus d'onboarding des nouveaux employés en créant leur compte Keycloak, en les assignant aux bons groupes, et en envoyant un email de bienvenue.

### 📍 Emplacement

```
Fichier: server/Jenkins/config/pipelines/employee-onboarding-webhook.jenkinsfile
Job: Employee-Onboarding-Webhook
```

### 🎯 Fonctionnalités

- ✅ **Création automatique de compte** avec validation des champs
- 🔍 **Détection de doublon** - Met à jour si l'utilisateur existe déjà
- 🎯 **Attribution automatique aux groupes** basée sur le département
- 🔑 **Génération de mot de passe sécurisé** (16 caractères)
- 📧 **Email de bienvenue** avec credentials (template HTML)
- 📬 **Notification RH** après succès

### 🌐 Configuration du Webhook

#### URL du Webhook

```
POST http://jenkins.local:8080/generic-webhook-trigger/invoke?token=employee-onboarding-secret-token
```

#### Token de Sécurité

```groovy
token: 'employee-onboarding-secret-token'
```

⚠️ **Important:** Changer ce token en production!

### 📦 Payload JSON

```json
{
  "username": "jdoe",
  "email": "john.doe@company.com",
  "firstName": "John",
  "lastName": "Doe",
  "department": "IT",
  "role": "developer",
  "realm": "internal"
}
```

#### Champs du Payload

| Champ | Type | Obligatoire | Description |
|-------|------|-------------|-------------|
| `username` | string | ✅ | Identifiant unique de l'utilisateur |
| `email` | string | ✅ | Adresse email professionnelle |
| `firstName` | string | ❌ | Prénom de l'employé |
| `lastName` | string | ❌ | Nom de famille de l'employé |
| `department` | string | ❌ | Département (pour attribution groupe) |
| `role` | string | ❌ | Rôle dans l'entreprise |
| `realm` | string | ❌ | Realm Keycloak (défaut: `internal`) |

### 🎯 Mapping Département → Groupe

Le pipeline utilise une logique de mapping pour assigner automatiquement les utilisateurs aux bons groupes Keycloak:

```groovy
def groupMapping = [
    'IT': 'IT',                    // Département IT → Groupe IT
    'Engineering': 'IT',           // Engineering → Groupe IT
    'DevOps': 'IT',                // DevOps → Groupe IT
    'Development': 'Jenkins',      // Development → Groupe Jenkins
    'QA': 'Jenkins',               // QA → Groupe Jenkins
    'Support': 'Jenkins'           // Support → Groupe Jenkins
]

env.targetGroup = groupMapping[env.department] ?: 'Jenkins'  // Défaut: Jenkins
```

### 🔄 Flux d'Exécution

```
┌────────────────────────────────────────────┐
│  TRIGGER: Webhook POST Request             │
└────────────┬───────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────┐
│  1. Load Keycloak Library                   │
└────────────┬────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────┐
│  2. Parse Webhook Payload                   │
│     - Extract username, email, etc.         │
│     - Validate required fields              │
└────────────┬────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────┐
│  3. Determine Group Assignment              │
│     - Map department to Keycloak group      │
└────────────┬────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────┐
│  4. Get Keycloak Access Token               │
└────────────┬────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────┐
│  5. Check if User Exists                    │
│     - Query Keycloak by username            │
└────────────┬────────────────────────────────┘
             │
        ┌────┴────┐
        │         │
  User Exists   User Not Found
        │         │
        ▼         ▼
   ┌────────┐  ┌────────────┐
   │ UPDATE │  │   CREATE   │
   │  USER  │  │    USER    │
   └────┬───┘  └─────┬──────┘
        │            │
        └─────┬──────┘
              │
              ▼
┌─────────────────────────────────────────────┐
│  6. Assign to Group                         │
│     - Add user to determined group          │
└────────────┬────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────┐
│  7. Send Welcome Email (if new user)        │
│     - HTML email with credentials           │
└────────────┬────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────┐
│  8. Notify HR                               │
│     - Send completion notification          │
└─────────────────────────────────────────────┘
```

### 📧 Template Email de Bienvenue

```html
<html>
<body>
    <h2>Welcome to the Company!</h2>
    <p>Hello John Doe,</p>
    <p>Your account has been created. Here are your login credentials:</p>
    <ul>
        <li><strong>Username:</strong> jdoe</li>
        <li><strong>Temporary Password:</strong> Xy9@mK4pQw2#L8vN</li>
        <li><strong>Login URL:</strong> http://keycloak:8080</li>
    </ul>
    <p><strong>Important:</strong> You will be required to change your password on first login.</p>
    <p>You have been assigned to the <strong>IT</strong> group.</p>
    <p>If you have any questions, please contact IT support.</p>
    <br>
    <p>Best regards,<br>IT Team</p>
</body>
</html>
```

### 📝 Exemple d'Utilisation

#### Appel Webhook via cURL

```bash
curl -X POST \
  "http://jenkins.local:8080/generic-webhook-trigger/invoke?token=employee-onboarding-secret-token" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jdoe",
    "email": "john.doe@company.com",
    "firstName": "John",
    "lastName": "Doe",
    "department": "IT",
    "role": "DevOps Engineer",
    "realm": "internal"
  }'
```

#### Intégration avec Système RH (Python)

```python
import requests

def onboard_employee(employee_data):
    webhook_url = "http://jenkins.local:8080/generic-webhook-trigger/invoke"
    params = {"token": "employee-onboarding-secret-token"}
    
    response = requests.post(
        webhook_url,
        params=params,
        json=employee_data
    )
    
    return response.status_code == 200

# Utilisation
employee = {
    "username": "jdoe",
    "email": "john.doe@company.com",
    "firstName": "John",
    "lastName": "Doe",
    "department": "IT",
    "role": "DevOps Engineer"
}

if onboard_employee(employee):
    print("✅ Onboarding initiated successfully")
else:
    print("❌ Onboarding failed")
```

### 🔒 Sécurité

- **Token requis**: Toutes les requêtes doivent inclure le token
- **Validation des champs**: Username et email obligatoires
- **Mot de passe sécurisé**: 16 caractères avec majuscules, minuscules, chiffres, caractères spéciaux
- **Temporary password**: Changement obligatoire à la première connexion

---

## Pipeline 3: Test Keycloak Integration

### 📄 Description

Suite de tests complète pour valider l'intégration entre Jenkins et Keycloak. Ce pipeline doit être exécuté après chaque modification de configuration pour garantir que tout fonctionne correctement.

### 📍 Emplacement

```
Fichier: server/Jenkins/config/pipelines/test-keycloak-integration.jenkinsfile
Job: Test-Keycloak-Integration
```

### 🎯 Tests Exécutés

#### Test 1: Keycloak Connectivity ✅
- Vérifie que Keycloak est accessible
- Teste le endpoint `.well-known/openid-configuration`
- Valide le code HTTP 200

#### Test 2: Service Account Authentication 🔐
- Obtient un access token via client credentials
- Valide le token avec l'endpoint introspection
- Vérifie que le token est actif

#### Test 3: List Users 📋
- Liste les utilisateurs du realm
- Vérifie que l'API retourne des données
- Affiche les 5 premiers utilisateurs

#### Test 4: Create Test User 👤
- Crée un utilisateur avec username unique (test-jenkins-{BUILD_NUMBER})
- Assigne un mot de passe temporaire
- Valide la création avec un ID retourné

#### Test 5: Update Test User ✏️
- Met à jour le prénom/nom de l'utilisateur test
- Marque l'email comme vérifié
- Valide que les modifications sont appliquées

#### Test 6: Reset Password 🔑
- Génère un nouveau mot de passe sécurisé
- Réinitialise le mot de passe de l'utilisateur test
- Configure en mode non-temporaire

#### Test 7: Add to Group 👥
- Ajoute l'utilisateur test au groupe "Jenkins"
- Valide l'attribution (ou note si le groupe n'existe pas)

#### Test 8: Delete Test User 🗑️
- Supprime l'utilisateur test créé
- Nettoie les données de test
- Valide la suppression

### ⚙️ Paramètres

| Paramètre | Type | Valeurs | Description |
|-----------|------|---------|-------------|
| **REALM** | Choice | `internal` | Realm Keycloak à tester |

### 🔄 Variables d'Environnement

```groovy
environment {
    KC_URL_INTERNAL = "${KC_URL_INTERNAL}"
    KC_CLIENT_ID = "${KC_CLIENT_ID_JENKINS_AUTOMATION}"
    KC_CLIENT_SECRET = "${KC_SECRET_JENKINS_AUTOMATION}"
    TEST_USERNAME = "test-jenkins-${BUILD_NUMBER}"
    TEST_EMAIL = "test-jenkins-${BUILD_NUMBER}@example.local"
}
```

### 📊 Sortie de Console Exemple

```
================================================================================
TEST 1: Checking Keycloak connectivity...
================================================================================
✅ Keycloak is accessible

================================================================================
TEST 2: Testing service account authentication...
================================================================================
✅ Successfully obtained access token
✅ Token is valid

================================================================================
TEST 3: Testing user listing...
================================================================================
✅ Successfully retrieved users
   Total users: 15

   Sample users:
  - admin (admin@company.local)
  - jdoe (john.doe@company.com)
  - asmith (alice.smith@company.com)

================================================================================
TEST 4: Testing user creation...
================================================================================
✅ User created successfully
   User ID: 123e4567-e89b-12d3-a456-426614174000
   Username: test-jenkins-42
   Email: test-jenkins-42@example.local

================================================================================
TEST 5: Testing user update...
================================================================================
✅ User updated successfully

================================================================================
TEST 6: Testing password reset...
================================================================================
✅ Password reset successfully
   New password: Np5&qT2xRw9@Km7L

================================================================================
TEST 7: Testing group assignment...
================================================================================
✅ User added to group successfully

================================================================================
TEST 8: Testing user deletion (cleanup)...
================================================================================
✅ Test user deleted successfully

================================================================================
🎉 ALL TESTS PASSED!
================================================================================

✅ Keycloak connectivity
✅ Service account authentication
✅ Token validation
✅ List users
✅ Create user
✅ Update user
✅ Reset password
✅ Add to group (if group exists)
✅ Delete user

The Keycloak integration is working correctly!
You can now use the automation pipelines safely.
================================================================================
```

### 🧹 Nettoyage Automatique

Le pipeline inclut un bloc `post.always` qui nettoie automatiquement l'utilisateur test même en cas d'échec:

```groovy
post {
    always {
        script {
            // Clean up sensitive data
            env.ACCESS_TOKEN = null
            
            // Try to clean up test user if it still exists
            try {
                if (env.TEST_USER_ID) {
                    echo "🧹 Cleaning up test user (if exists)..."
                    keycloakUser.deleteUser(...)
                }
            } catch (Exception e) {
                echo "Note: Test user cleanup skipped (may already be deleted)"
            }
        }
    }
}
```

### 📝 Quand Exécuter ce Pipeline?

- ✅ **Après le premier déploiement** - Valider la configuration initiale
- ✅ **Après modification de jenkins.yaml** - Vérifier l'authentification OIDC
- ✅ **Après changement de realm** - Tester le nouveau realm
- ✅ **En cas de problème** - Diagnostiquer les erreurs d'intégration
- ✅ **Avant la production** - Garantir que tout fonctionne

---

## Utilisation des Pipelines

### Accès via Interface Web

1. **Accéder à Jenkins:**
   ```
   http://jenkins.local:8080
   ```

2. **Se connecter:**
   - Cliquer sur "Login with Keycloak"
   - Utiliser les credentials Keycloak

3. **Lancer un Pipeline:**
   - Cliquer sur le nom du pipeline
   - Pour pipelines paramétrés: "Build with Parameters"
   - Pour tests: "Build Now"

### Organisation par Vues

Les pipelines sont automatiquement organisés dans des vues:

#### Vue "Keycloak Management"
- Keycloak-User-Management
- Employee-Onboarding-Webhook

#### Vue "Integration Tests"
- Test-Keycloak-Integration

### Permissions Requises

| Action | Groupe IT | Groupe Jenkins |
|--------|-----------|----------------|
| Voir les pipelines | ✅ | ✅ |
| Lancer un build | ✅ | ✅ |
| Configurer un pipeline | ✅ | ❌ |
| Supprimer un pipeline | ✅ | ❌ |

---

## Exemples d'Utilisation

### Scénario 1: Onboarding Manuel

**Contexte:** Un nouveau développeur rejoint l'équipe IT

**Étapes:**
1. Accéder à Jenkins → Keycloak-User-Management
2. Build with Parameters:
   - ACTION: `CREATE_USER`
   - USERNAME: `jsmith`
   - EMAIL: `james.smith@company.com`
   - FIRST_NAME: `James`
   - LAST_NAME: `Smith`
   - GROUP_NAME: `IT`
   - EMAIL_VERIFIED: `true`
3. Build → L'utilisateur est créé avec un mot de passe généré

### Scénario 2: Onboarding Automatisé via RH

**Contexte:** Le système RH déclenche automatiquement la création de compte

**Flux:**
```
Système RH → Webhook → Jenkins → Keycloak
```

**Webhook POST:**
```bash
curl -X POST \
  "http://jenkins.local:8080/generic-webhook-trigger/invoke?token=employee-onboarding-secret-token" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jsmith",
    "email": "james.smith@company.com",
    "firstName": "James",
    "lastName": "Smith",
    "department": "IT",
    "role": "Developer"
  }'
```

**Résultat:**
- Compte créé automatiquement
- Assigné au groupe IT
- Email de bienvenue envoyé
- RH notifié

### Scénario 3: Diagnostic de Problème

**Contexte:** L'authentification Keycloak ne fonctionne pas

**Étapes:**
1. Lancer Test-Keycloak-Integration
2. Vérifier quelle étape échoue:
   - Test 1 failed → Problème réseau/connectivité
   - Test 2 failed → Credentials invalides
   - Test 4 failed → Permissions insuffisantes
3. Corriger la configuration
4. Relancer le test

---

## 🔧 Personnalisation

### Modifier le Mapping Département → Groupe

Éditer `employee-onboarding-webhook.jenkinsfile`:

```groovy
def groupMapping = [
    'IT': 'IT',
    'Engineering': 'IT',
    'HR': 'HR',              // Ajouter nouveau mapping
    'Finance': 'Finance',    // Ajouter nouveau mapping
    'Sales': 'Sales'         // Ajouter nouveau mapping
]
```

### Changer le Token Webhook

Éditer `employee-onboarding-webhook.jenkinsfile`:

```groovy
token: 'your-secure-token-here'
```

### Modifier le Template Email

Éditer la section "Send Welcome Email":

```groovy
def emailBody = """
<html>
<body>
    <!-- Votre template HTML personnalisé -->
</body>
</html>
"""
```

---

## 📚 Ressources

- [Keycloak Admin REST API](https://www.keycloak.org/docs-api/latest/rest-api/index.html)
- [Generic Webhook Trigger Plugin](https://plugins.jenkins.io/generic-webhook-trigger/)
- [Jenkins Pipeline Syntax](https://www.jenkins.io/doc/book/pipeline/syntax/)
- [SHARED_LIBRARY.md](./SHARED_LIBRARY.md) - Documentation de la bibliothèque Keycloak

---

**⬅️ Retour au [README](./README.md)**
