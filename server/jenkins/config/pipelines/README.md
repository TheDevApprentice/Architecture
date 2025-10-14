# Jenkins Keycloak Automation Pipelines

Ce dossier contient les pipelines Jenkins pour automatiser la gestion des utilisateurs Keycloak.

## 📋 Table des matières

- [Pipelines disponibles](#pipelines-disponibles)
- [Configuration requise](#configuration-requise)
- [Utilisation](#utilisation)
- [Webhook Integration](#webhook-integration)
- [Sécurité](#sécurité)

## 🚀 Pipelines disponibles

### 1. Keycloak User Management (`keycloak-user-management.jenkinsfile`)

Pipeline interactif pour gérer les utilisateurs Keycloak via l'interface Jenkins.

**Actions supportées:**
- `CREATE_USER` - Créer un nouvel utilisateur
- `UPDATE_USER` - Mettre à jour un utilisateur existant
- `DELETE_USER` - Supprimer un utilisateur
- `RESET_PASSWORD` - Réinitialiser le mot de passe
- `ADD_TO_GROUP` - Ajouter un utilisateur à un groupe
- `LIST_USERS` - Lister tous les utilisateurs d'un realm

**Paramètres:**
- `ACTION` - Action à effectuer
- `REALM` - Realm Keycloak (par défaut: `internal`)
- `USERNAME` - Nom d'utilisateur
- `EMAIL` - Adresse email
- `FIRST_NAME` - Prénom
- `LAST_NAME` - Nom
- `GROUP_NAME` - Nom du groupe (pour ADD_TO_GROUP)
- `EMAIL_VERIFIED` - Email vérifié (boolean)
- `ENABLED` - Compte activé (boolean)
- `TEMPORARY_PASSWORD` - Mot de passe temporaire (boolean)
- `PASSWORD` - Mot de passe (laisser vide pour génération automatique)

### 2. Employee Onboarding Webhook (`employee-onboarding-webhook.jenkinsfile`)

Pipeline automatisé déclenché par webhook pour l'onboarding des employés.

**Fonctionnalités:**
- Création automatique de compte utilisateur
- Attribution automatique aux groupes selon le département
- Génération de mot de passe sécurisé
- Envoi d'email de bienvenue (à configurer)
- Notification HR

**Payload webhook (JSON):**
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

## ⚙️ Configuration requise

### 1. Credentials Jenkins

Créer les credentials suivants dans Jenkins:

```
Manage Jenkins > Credentials > System > Global credentials
```

| ID | Type | Description | Valeur |
|----|------|-------------|--------|
| `keycloak-url` | Secret text | URL Keycloak | `auth.localhost` |
| `keycloak-automation-client-id` | Secret text | Client ID | `jenkins-automation` |
| `keycloak-automation-client-secret` | Secret text | Client Secret | Voir `.env` |

### 2. Shared Library

La bibliothèque partagée `keycloak-lib` doit être configurée:

```
Manage Jenkins > Configure System > Global Pipeline Libraries
```

- **Name:** `keycloak-lib`
- **Default version:** `main` ou `master`
- **Retrieval method:** Modern SCM
- **Source Code Management:** Git
- **Project Repository:** Chemin vers `server/jenkins/shared-library`

### 3. Plugins Jenkins requis

Installer les plugins suivants:
- Pipeline
- Pipeline: Groovy
- Generic Webhook Trigger Plugin
- Credentials Plugin
- Email Extension Plugin (optionnel, pour les notifications)

## 📖 Utilisation

### Utilisation manuelle (User Management Pipeline)

1. Aller dans Jenkins
2. Créer un nouveau Pipeline job
3. Configurer le pipeline avec le fichier `keycloak-user-management.jenkinsfile`
4. Cliquer sur "Build with Parameters"
5. Remplir les paramètres
6. Lancer le build

**Exemple: Créer un utilisateur**
```
ACTION: CREATE_USER
REALM: internal
USERNAME: alice
EMAIL: alice@example.local
FIRST_NAME: Alice
LAST_NAME: Wonderland
GROUP_NAME: Jenkins
ENABLED: true
EMAIL_VERIFIED: false
TEMPORARY_PASSWORD: true
PASSWORD: (laisser vide pour auto-génération)
```

### Utilisation via webhook (Employee Onboarding)

1. Créer un Pipeline job avec `employee-onboarding-webhook.jenkinsfile`
2. Le webhook sera automatiquement configuré avec le token: `employee-onboarding-secret-token`

**URL du webhook:**
```
http://jenkins.localhost/generic-webhook-trigger/invoke?token=employee-onboarding-secret-token
```

**Exemple de requête:**
```bash
curl -X POST http://jenkins.localhost/generic-webhook-trigger/invoke?token=employee-onboarding-secret-token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "bob",
    "email": "bob@example.local",
    "firstName": "Bob",
    "lastName": "Builder",
    "department": "IT",
    "role": "admin",
    "realm": "internal"
  }'
```

## 🔐 Sécurité

### Bonnes pratiques

1. **Secrets Management:**
   - Utiliser Jenkins Credentials pour stocker les secrets
   - Ne jamais hardcoder les credentials dans les pipelines
   - En production, utiliser HashiCorp Vault ou AWS Secrets Manager

2. **Token Webhook:**
   - Changer le token par défaut `employee-onboarding-secret-token`
   - Utiliser un token complexe et unique
   - Restreindre l'accès au webhook par IP si possible

3. **Permissions Keycloak:**
   - Le service account `jenkins-automation` a les permissions minimales requises
   - Limiter les realms accessibles
   - Auditer régulièrement les actions via les logs Keycloak

4. **Mots de passe:**
   - Les mots de passe générés sont sécurisés (16 caractères, aléatoires)
   - Toujours forcer le changement au premier login (`temporary: true`)
   - Ne jamais logger les mots de passe en clair

### Configuration de sécurité Keycloak

Le client `jenkins-automation` dans le realm `internal` a été configuré avec:

- **Service Account Enabled:** `true`
- **Standard Flow:** `false` (pas de login interactif)
- **Direct Access Grants:** `false`
- **Permissions:** 
  - `manage-users`
  - `view-users`
  - `query-users`
  - `query-groups`

## 🧪 Tests

### Test de connexion Keycloak

```groovy
// Dans Jenkins Script Console
@Library('keycloak-lib') _

def token = keycloakAuth.getServiceAccountToken(
    keycloakUrl: 'auth.localhost',
    clientId: 'jenkins-automation',
    clientSecret: 'jwzH52S9i9qlT15ju8wYKNSUWYVC1W2O',
    realm: 'internal'
)

println "Token obtained: ${token ? 'SUCCESS' : 'FAILED'}"
```

### Test de création d'utilisateur

Lancer le pipeline `keycloak-user-management` avec:
- ACTION: CREATE_USER
- USERNAME: test-user
- EMAIL: test@example.local
- Autres champs au choix

Vérifier dans Keycloak Admin Console que l'utilisateur a été créé.

## 📚 Ressources

- [Keycloak Admin REST API](https://www.keycloak.org/docs-api/latest/rest-api/index.html)
- [Jenkins Pipeline Syntax](https://www.jenkins.io/doc/book/pipeline/syntax/)
- [Generic Webhook Trigger Plugin](https://plugins.jenkins.io/generic-webhook-trigger/)

## 🐛 Troubleshooting

### Erreur: "Failed to obtain access token"

**Causes possibles:**
- Credentials incorrects
- Client `jenkins-automation` non configuré dans Keycloak
- Keycloak non accessible depuis Jenkins

**Solution:**
1. Vérifier les credentials dans Jenkins
2. Vérifier que le client existe dans Keycloak Admin Console
3. Tester la connectivité: `curl http://keycloak:8080/realms/internal/.well-known/openid-configuration`

### Erreur: "User not found"

**Causes possibles:**
- Username incorrect
- Utilisateur dans un autre realm

**Solution:**
1. Vérifier le realm spécifié
2. Utiliser l'action LIST_USERS pour voir tous les utilisateurs

### Webhook ne se déclenche pas

**Causes possibles:**
- Token incorrect
- Plugin Generic Webhook Trigger non installé
- Payload JSON mal formaté

**Solution:**
1. Vérifier le token dans l'URL
2. Installer le plugin Generic Webhook Trigger
3. Valider le JSON avec un validateur en ligne

## 📝 Notes

- **Environnement de développement:** Les configurations actuelles sont pour le dev local
- **Production:** Adapter les URLs, secrets, et activer HTTPS
- **Email:** Configurer le plugin Email Extension pour l'envoi d'emails réels
- **Monitoring:** Activer les logs Keycloak events pour auditer les actions
