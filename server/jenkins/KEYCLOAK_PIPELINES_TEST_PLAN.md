# 🧪 Plan de Test - Pipelines de Management Keycloak

Ce document détaille le plan de test complet pour valider toutes les actions des 4 pipelines de management Keycloak.

---

## 📋 Table des Matières

1. [Keycloak User Management](#1-keycloak-user-management)
2. [Keycloak Group Management](#2-keycloak-group-management)
3. [Keycloak Client Management](#3-keycloak-client-management)
4. [Keycloak Session Management](#4-keycloak-session-management)

---

## 1. 👤 Keycloak User Management

**Pipeline:** `Keycloak/Keycloak-User-Management`

### Test 1.1: LIST_USERS - Lister les utilisateurs

**Paramètres:**
```yaml
ACTION: LIST_USERS
REALM: internal
```

**Résultat attendu:**
- ✅ Liste de tous les utilisateurs du realm
- ✅ Affichage username, email, status enabled

---

### Test 1.2: CREATE_USER - Créer un utilisateur

**Paramètres:**
```yaml
ACTION: CREATE_USER
REALM: internal
USERNAME: test-user-jenkins
EMAIL: test-user-jenkins@example.local
FIRST_NAME: Test
LAST_NAME: Jenkins
GROUP_NAME: 
LOCALE: fr
EMAIL_VERIFIED: false
ENABLED: true
TEMPORARY_PASSWORD: true
PASSWORD: TestPassword123!
```

**Résultat attendu:**
- ✅ User créé avec succès
- ✅ ID utilisateur retourné
- ✅ Email et username configurés
- ✅ Mot de passe temporaire défini

---

### Test 1.3: UPDATE_USER - Mettre à jour un utilisateur

**Paramètres:**
```yaml
ACTION: UPDATE_USER
REALM: internal
USERNAME: test-user-jenkins
EMAIL: test-user-jenkins-updated@example.local
FIRST_NAME: Test Updated
LAST_NAME: Jenkins Updated
GROUP_NAME: 
LOCALE: en
EMAIL_VERIFIED: true
ENABLED: true
```

**Résultat attendu:**
- ✅ User mis à jour
- ✅ Email vérifié passé à true
- ✅ Nom et prénom modifiés

---

### Test 1.4: RESET_PASSWORD - Réinitialiser le mot de passe

**Paramètres:**
```yaml
ACTION: RESET_PASSWORD
REALM: internal
USERNAME: test-user-jenkins
PASSWORD: NewPassword456!
TEMPORARY_PASSWORD: true
```

**Résultat attendu:**
- ✅ Mot de passe réinitialisé
- ✅ Mot de passe **TOUJOURS temporaire** (sécurité)
- ⚠️  Message: "User must change password on next login"
- 🔐 L'utilisateur devra changer son mot de passe à la prochaine connexion

---

### Test 1.5: ADD_TO_GROUP - Ajouter à un groupe

**Paramètres:**
```yaml
ACTION: ADD_TO_GROUP
REALM: internal
USERNAME: test-user-jenkins
GROUP_NAME: Jenkins
```

**Résultat attendu:**
- ✅ User ajouté au groupe Jenkins
- ⚠️  Si le groupe n'existe pas, afficher un avertissement

---

### Test 1.6: DELETE_USER - Supprimer un utilisateur

**Paramètres:**
```yaml
ACTION: DELETE_USER
REALM: internal
USERNAME: test-user-jenkins
```

**Résultat attendu:**
- ✅ User supprimé avec succès

---

## 2. 👥 Keycloak Group Management

**Pipeline:** `Keycloak/Keycloak-Group-Management`

### Test 2.1: LIST_GROUPS - Lister les groupes

**Paramètres:**
```yaml
ACTION: LIST_GROUPS
REALM: internal
```

**Résultat attendu:**
- ✅ Liste de tous les groupes
- ✅ Affichage ID, name, path, subgroups

---

### Test 2.2: CREATE_GROUP - Créer un groupe

**Paramètres:**
```yaml
ACTION: CREATE_GROUP
REALM: internal
GROUP_NAME: test-group-jenkins
PARENT_GROUP: 
NEW_GROUP_NAME: 
USERNAMES: 
ATTRIBUTES: {"department": "IT", "location": "Paris"}
DRY_RUN: false
```

**Note sur les ATTRIBUTES:**
- Format accepté : `{"key": "value"}` ou `{"key": ["value1", "value2"]}`
- Les valeurs simples sont automatiquement converties en arrays par le pipeline
- Format interne Keycloak : `{"key": ["value"]}`

**Résultat attendu:**
- ✅ Groupe créé avec succès
- ✅ ID du groupe retourné
- ✅ Attributs configurés (department: ["IT"], location: ["Paris"])

---

### Test 2.3: CREATE_GROUP - Créer un sous-groupe

**Paramètres:**
```yaml
ACTION: CREATE_GROUP
REALM: internal
GROUP_NAME: test-subgroup-jenkins
PARENT_GROUP: test-group-jenkins
NEW_GROUP_NAME: 
USERNAMES: 
ATTRIBUTES: {}
DRY_RUN: false

ACTION: CREATE_GROUP
REALM: internal
GROUP_NAME: test-subgroup1-jenkins
PARENT_GROUP: test-group-jenkins
NEW_GROUP_NAME: 
USERNAMES: 
ATTRIBUTES: {}
DRY_RUN: false
```

**Résultat attendu:**
- ✅ Sous-groupe créé
- ✅ Hiérarchie respectée (parent → enfant)

---

### Test 2.4: GET_GROUP - Obtenir les détails d'un groupe

**Paramètres:**
```yaml
ACTION: GET_GROUP
REALM: internal
GROUP_NAME: test-group-jenkins

ACTION: GET_GROUP
REALM: internal
GROUP_NAME: test-subgroup-jenkins

ACTION: GET_GROUP
REALM: internal
GROUP_NAME: test-subgroup1-jenkins
```

**Résultat attendu:**
- ✅ Détails complets du groupe
- ✅ Attributs affichés
- ✅ Nombre de membres
- ✅ Liste des sous-groupes

---

### Test 2.5: ADD_MEMBERS - Ajouter des membres au groupe

**Pré-requis:** Créer d'abord des utilisateurs test

**Créer les utilisateurs:**
```yaml
# User 1
ACTION: CREATE_USER (dans User Management)
USERNAME: member1-test
EMAIL: member1@test.local

# User 2
ACTION: CREATE_USER (dans User Management)
USERNAME: member2-test
EMAIL: member2@test.local

# User 3
ACTION: CREATE_USER (dans User Management)
USERNAME: member3-test
EMAIL: member3@test.local
```

**Paramètres ADD_MEMBERS:**
```yaml
ACTION: ADD_MEMBERS
REALM: internal
GROUP_NAME: test-group-jenkins
USERNAMES: 
member1-test
member2-test
ATTRIBUTES: {}
DRY_RUN: false

ACTION: ADD_MEMBERS
REALM: internal
GROUP_NAME: test-subgroup-jenkins
USERNAMES: 
member3-test
ATTRIBUTES: {}
DRY_RUN: false
```

**Résultat attendu:**
- ✅ 2 membres ajoutés avec succès
- ✅ Confirmation pour chaque utilisateur

---

### Test 2.6: LIST_MEMBERS - Lister les membres du groupe

**Paramètres:**
```yaml
ACTION: LIST_MEMBERS
REALM: internal
GROUP_NAME: test-group-jenkins

ACTION: LIST_MEMBERS
REALM: internal
GROUP_NAME: test-subgroup-jenkins
```

**Résultat attendu:**
- ✅ Liste des 2 membres ajoutés
- ✅ Affichage username, email, enabled

---

### Test 2.7: UPDATE_GROUP - Mettre à jour un groupe

**Paramètres:**
```yaml
ACTION: UPDATE_GROUP
REALM: internal
GROUP_NAME: test-group-jenkins
NEW_GROUP_NAME: test-group-jenkins-renamed
ATTRIBUTES: {"department": "Engineering", "location": "Lyon", "updated": "true"}
DRY_RUN: false
```

**Note:** Les valeurs simples sont automatiquement converties en arrays

**Résultat attendu:**
- ✅ Groupe renommé
- ✅ Attributs mis à jour (department: ["Engineering"], location: ["Lyon"], updated: ["true"])

---

### Test 2.8: REMOVE_MEMBERS - Retirer des membres

**Paramètres:**
```yaml
ACTION: REMOVE_MEMBERS
REALM: internal
GROUP_NAME: test-group-jenkins-renamed
USERNAMES: 
member1-test
ATTRIBUTES: {}
DRY_RUN: false
```

**Résultat attendu:**
- ✅ member1-test retiré du groupe
- ✅ member2-test reste dans le groupe

---

### Test 2.9: DETECT_ORPHANS - Détecter les groupes orphelins

**Paramètres:**
```yaml
ACTION: DETECT_ORPHANS
REALM: internal
```

**Résultat attendu:**
- ✅ Liste des groupes sans membres
- ✅ test-subgroup1-jenkins devrait apparaître (sans membres)

---

### Test 2.10: DELETE_GROUP - Supprimer un groupe (DRY RUN)

**Paramètres:**
```yaml
ACTION: DELETE_GROUP
REALM: internal
GROUP_NAME: test-subgroup-jenkins
DRY_RUN: true
```

**Résultat attendu:**
- ✅ Simulation de suppression
- ✅ Aucune suppression réelle

---

### Test 2.11: DELETE_GROUP - Supprimer un sous-groupe

**Paramètres:**
```yaml
ACTION: DELETE_GROUP
REALM: internal
GROUP_NAME: test-subgroup-jenkins
DRY_RUN: false
```

**Résultat attendu:**
- ⚠️  Demande de confirmation
- ✅ Sous-groupe supprimé après confirmation

---

### Test 2.12: DELETE_GROUP - Supprimer le groupe principal

**Paramètres:**
```yaml
ACTION: DELETE_GROUP
REALM: internal
GROUP_NAME: test-group-jenkins-renamed
DRY_RUN: false
```

**Résultat attendu:**
- ⚠️  Avertissement: groupe contient 1 membre (member2-test)
- ⚠️  Demande de confirmation
- ✅ Groupe supprimé après confirmation

---

### Test 2.13: Nettoyage des utilisateurs test

**Supprimer dans User Management:**
```yaml
ACTION: DELETE_USER
USERNAME: member1-test

ACTION: DELETE_USER
USERNAME: member2-test
```

---

## 3. 🔐 Keycloak Client Management

**Pipeline:** `Keycloak/Keycloak-Client-Management`

### Test 3.1: LIST_CLIENTS - Lister les clients

**Paramètres:**
```yaml
ACTION: LIST_CLIENTS
REALM: internal
CLIENT_ID: 
TEMPLATE: custom
PROTOCOL: openid-connect
PUBLIC_CLIENT: false
REDIRECT_URIS: 
WEB_ORIGINS: 
DESCRIPTION: 
SERVICE_ACCOUNTS_ENABLED: false
DRY_RUN: false
```

**Résultat attendu:**
- ✅ Liste de tous les clients
- ✅ Affichage type (Public/Confidential), protocol, status

---

### Test 3.2: CREATE_CLIENT - Créer un client confidentiel

**Paramètres:**
```yaml
ACTION: CREATE_CLIENT
REALM: internal
CLIENT_ID: test-backend-service
TEMPLATE: custom
PROTOCOL: openid-connect
PUBLIC_CLIENT: false
REDIRECT_URIS: 
http://localhost:8080/*
https://api.test.com/*
WEB_ORIGINS: 
http://localhost:8080
https://api.test.com
DESCRIPTION: Test backend service client
SERVICE_ACCOUNTS_ENABLED: true
DRY_RUN: false
```

**Résultat attendu:**
- ✅ Client créé avec UUID
- ✅ Secret généré automatiquement
- ✅ Service accounts activé
- 🔑 **IMPORTANT:** Noter le client secret affiché

---

### Test 3.3: CREATE_FROM_TEMPLATE - Créer un client SPA

**Paramètres:**
```yaml
ACTION: CREATE_FROM_TEMPLATE
REALM: internal
CLIENT_ID: test-spa-app
TEMPLATE: spa
PROTOCOL: openid-connect
PUBLIC_CLIENT: true
REDIRECT_URIS: 
http://localhost:3000/*
https://app.test.com/*
WEB_ORIGINS: 
DESCRIPTION: Test SPA application
SERVICE_ACCOUNTS_ENABLED: false
DRY_RUN: false
```

**Résultat attendu:**
- ✅ Client SPA créé (public)
- ✅ Configuration PKCE activée
- ✅ Pas de secret (client public)

---

### Test 3.4: GET_CLIENT - Obtenir les détails d'un client

**Paramètres:**
```yaml
ACTION: GET_CLIENT
REALM: internal
CLIENT_ID: test-backend-service
```

**Résultat attendu:**
- ✅ Détails complets du client
- ✅ UUID, protocol, type, flows activés
- ✅ Redirect URIs et Web Origins

---

### Test 3.5: GET_CLIENT_SECRET - Obtenir le secret

**Paramètres:**
```yaml
ACTION: GET_CLIENT_SECRET
REALM: internal
CLIENT_ID: test-backend-service
```

**Résultat attendu:**
- ✅ Secret affiché (masqué partiellement)
- ⚠️  Avertissement sécurité

---

### Test 3.6: UPDATE_CLIENT - Mettre à jour un client

**Paramètres:**
```yaml
ACTION: UPDATE_CLIENT
REALM: internal
CLIENT_ID: test-backend-service
REDIRECT_URIS: 
http://localhost:8080/*
https://api.test.com/*
https://api-staging.test.com/*
WEB_ORIGINS: 
http://localhost:8080
https://api.test.com
https://api-staging.test.com
DESCRIPTION: Updated backend service with staging environment
DRY_RUN: false
```

**Résultat attendu:**
- ✅ Client mis à jour
- ✅ Nouveau redirect URI ajouté

---

### Test 3.7: REGENERATE_SECRET - Régénérer le secret (DRY RUN)

**Paramètres:**
```yaml
ACTION: REGENERATE_SECRET
REALM: internal
CLIENT_ID: test-backend-service
DRY_RUN: true
```

**Résultat attendu:**
- ✅ Simulation de régénération
- ✅ Aucun changement réel

---

### Test 3.8: REGENERATE_SECRET - Régénérer le secret

**Paramètres:**
```yaml
ACTION: REGENERATE_SECRET
REALM: internal
CLIENT_ID: test-backend-service
DRY_RUN: false
```

**Résultat attendu:**
- ⚠️  Demande de confirmation
- ✅ Nouveau secret généré
- ⚠️  Ancien secret invalidé
- 🔑 **IMPORTANT:** Noter le nouveau secret

---

### Test 3.9: DISABLE_CLIENT - Désactiver un client

**Paramètres:**
```yaml
ACTION: DISABLE_CLIENT
REALM: internal
CLIENT_ID: test-spa-app
```

**Résultat attendu:**
- ✅ Client désactivé
- ✅ Le client ne peut plus être utilisé pour l'authentification

---

### Test 3.10: ENABLE_CLIENT - Réactiver un client

**Paramètres:**
```yaml
ACTION: ENABLE_CLIENT
REALM: internal
CLIENT_ID: test-spa-app
```

**Résultat attendu:**
- ✅ Client réactivé
- ✅ Le client peut à nouveau être utilisé

---

### Test 3.11: DELETE_CLIENT - Supprimer un client (DRY RUN)

**Paramètres:**
```yaml
ACTION: DELETE_CLIENT
REALM: internal
CLIENT_ID: test-spa-app
DRY_RUN: true
```

**Résultat attendu:**
- ✅ Simulation de suppression
- ✅ Affichage des détails du client
- ✅ Aucune suppression réelle

---

### Test 3.12: DELETE_CLIENT - Supprimer le client SPA

**Paramètres:**
```yaml
ACTION: DELETE_CLIENT
REALM: internal
CLIENT_ID: test-spa-app
DRY_RUN: false
```

**Résultat attendu:**
- ⚠️  Demande de confirmation
- ✅ Client supprimé après confirmation

---

### Test 3.13: DELETE_CLIENT - Supprimer le client backend

**Paramètres:**
```yaml
ACTION: DELETE_CLIENT
REALM: internal
CLIENT_ID: test-backend-service
DRY_RUN: false
```

**Résultat attendu:**
- ⚠️  Demande de confirmation
- ✅ Client supprimé après confirmation

---

## 4. 🔒 Keycloak Session Management

**Pipeline:** `Keycloak/Keycloak-Session-Management`

### Test 4.1: SESSION_STATISTICS - Statistiques des sessions

**Paramètres:**
```yaml
ACTION: SESSION_STATISTICS
REALM: internal
USERNAME: 
ANOMALY_SESSION_AGE_DAYS: 7
EMERGENCY_MODE: false
```

**Résultat attendu:**
- ✅ Nombre total de sessions actives
- ✅ Nombre d'utilisateurs uniques
- ✅ Nombre de clients uniques
- ✅ Âge moyen des sessions
- ✅ Sessions par utilisateur

---

### Test 4.2: LIST_ACTIVE_SESSIONS - Lister toutes les sessions

**Paramètres:**
```yaml
ACTION: LIST_ACTIVE_SESSIONS
REALM: internal
USERNAME: 
ANOMALY_SESSION_AGE_DAYS: 7
EMERGENCY_MODE: false
```

**Résultat attendu:**
- ✅ Liste de toutes les sessions actives
- ✅ Groupées par utilisateur
- ✅ Détails: client, âge de la session, IP

---

### Test 4.3: Créer un utilisateur pour tester les sessions

**Pré-requis:** Créer un utilisateur dans User Management

```yaml
ACTION: CREATE_USER
REALM: internal
USERNAME: session-test-user
EMAIL: session-test@example.local
PASSWORD: SessionTest123!
ENABLED: true
```

**Ensuite:** Se connecter à Keycloak avec cet utilisateur pour créer une session

---

### Test 4.4: LIST_USER_SESSIONS - Sessions d'un utilisateur spécifique

**Paramètres:**
```yaml
ACTION: LIST_USER_SESSIONS
REALM: internal
USERNAME: session-test-user
ANOMALY_SESSION_AGE_DAYS: 7
EMERGENCY_MODE: false
```

**Résultat attendu:**
- ✅ Sessions de l'utilisateur session-test-user
- ✅ Détails: session ID, IP, heure de début, dernière activité
- ✅ Liste des clients utilisés

---

### Test 4.5: DETECT_ANOMALIES - Détecter les anomalies

**Paramètres:**
```yaml
ACTION: DETECT_ANOMALIES
REALM: internal
USERNAME: 
ANOMALY_SESSION_AGE_DAYS: 1
EMERGENCY_MODE: false
```

**Résultat attendu:**
- ✅ Détection des sessions longues (>1 jour)
- ✅ Détection des utilisateurs avec plusieurs IPs
- ✅ Liste des anomalies trouvées

---

### Test 4.6: REVOKE_USER_SESSIONS - Révoquer les sessions d'un utilisateur

**Paramètres:**
```yaml
ACTION: REVOKE_USER_SESSIONS
REALM: internal
USERNAME: session-test-user
ANOMALY_SESSION_AGE_DAYS: 7
EMERGENCY_MODE: false
```

**Résultat attendu:**
- ⚠️  Affichage du nombre de sessions à révoquer
- ⚠️  Demande de confirmation (timeout 5 min)
- ✅ Toutes les sessions de l'utilisateur révoquées
- ✅ L'utilisateur est déconnecté

---

### Test 4.7: Vérifier que les sessions sont révoquées

**Paramètres:**
```yaml
ACTION: LIST_USER_SESSIONS
REALM: internal
USERNAME: session-test-user
```

**Résultat attendu:**
- ✅ Aucune session active pour cet utilisateur

---

### Test 4.8: REVOKE_ALL_SESSIONS - Mode EMERGENCY (ATTENTION!)

**⚠️  TEST CRITIQUE - À faire en dernier**

**Paramètres:**
```yaml
ACTION: REVOKE_ALL_SESSIONS
REALM: internal
USERNAME: 
ANOMALY_SESSION_AGE_DAYS: 7
EMERGENCY_MODE: false
```

**Résultat attendu:**
- 🚨 Avertissement sérieux
- ⚠️  Double confirmation requise (timeout 5 min + 2 min)
- ✅ TOUTES les sessions du realm révoquées
- ✅ TOUS les utilisateurs déconnectés
- 📧 Notification à envoyer aux ops

---

### Test 4.9: Vérifier l'état après REVOKE_ALL

**Paramètres:**
```yaml
ACTION: SESSION_STATISTICS
REALM: internal
```

**Résultat attendu:**
- ✅ Total sessions: 0
- ✅ Tous les utilisateurs déconnectés

---

### Test 4.10: Nettoyage - Supprimer l'utilisateur test

**Dans User Management:**
```yaml
ACTION: DELETE_USER
REALM: internal
USERNAME: session-test-user
```

---

## 📊 Récapitulatif des Tests

### Nombre de tests par pipeline

| Pipeline | Nombre de tests | Durée estimée |
|----------|-----------------|---------------|
| User Management | 6 tests | ~15 min |
| Group Management | 13 tests | ~30 min |
| Client Management | 13 tests | ~30 min |
| Session Management | 10 tests | ~25 min |
| **TOTAL** | **42 tests** | **~100 min** |

---

## ✅ Checklist de Validation

### Pré-requis
- [ ] Jenkins opérationnel
- [ ] Keycloak accessible (http://keycloak:8080)
- [ ] Service account `jenkins-automation` configuré avec les bons rôles
- [ ] Toutes les pipelines chargées dans Jenkins

### User Management
- [ ] Test 1.1: LIST_USERS
- [ ] Test 1.2: CREATE_USER
- [ ] Test 1.3: UPDATE_USER
- [ ] Test 1.4: RESET_PASSWORD
- [ ] Test 1.5: ADD_TO_GROUP
- [ ] Test 1.6: DELETE_USER

### Group Management
- [ ] Test 2.1: LIST_GROUPS
- [ ] Test 2.2: CREATE_GROUP
- [ ] Test 2.3: CREATE_GROUP (subgroup)
- [ ] Test 2.4: GET_GROUP
- [ ] Test 2.5: ADD_MEMBERS
- [ ] Test 2.6: LIST_MEMBERS
- [ ] Test 2.7: UPDATE_GROUP
- [ ] Test 2.8: REMOVE_MEMBERS
- [ ] Test 2.9: DETECT_ORPHANS
- [ ] Test 2.10: DELETE_GROUP (DRY RUN)
- [ ] Test 2.11: DELETE_GROUP (subgroup)
- [ ] Test 2.12: DELETE_GROUP (main group)
- [ ] Test 2.13: Cleanup users

### Client Management
- [ ] Test 3.1: LIST_CLIENTS
- [ ] Test 3.2: CREATE_CLIENT
- [ ] Test 3.3: CREATE_FROM_TEMPLATE
- [ ] Test 3.4: GET_CLIENT
- [ ] Test 3.5: GET_CLIENT_SECRET
- [ ] Test 3.6: UPDATE_CLIENT
- [ ] Test 3.7: REGENERATE_SECRET (DRY RUN)
- [ ] Test 3.8: REGENERATE_SECRET
- [ ] Test 3.9: DISABLE_CLIENT
- [ ] Test 3.10: ENABLE_CLIENT
- [ ] Test 3.11: DELETE_CLIENT (DRY RUN)
- [ ] Test 3.12: DELETE_CLIENT (SPA)
- [ ] Test 3.13: DELETE_CLIENT (backend)

### Session Management
- [ ] Test 4.1: SESSION_STATISTICS
- [ ] Test 4.2: LIST_ACTIVE_SESSIONS
- [ ] Test 4.3: Create test user
- [ ] Test 4.4: LIST_USER_SESSIONS
- [ ] Test 4.5: DETECT_ANOMALIES
- [ ] Test 4.6: REVOKE_USER_SESSIONS
- [ ] Test 4.7: Verify revocation
- [ ] Test 4.8: REVOKE_ALL_SESSIONS ⚠️
- [ ] Test 4.9: Verify all revoked
- [ ] Test 4.10: Cleanup test user

---

## 🐛 Troubleshooting

### Erreurs Communes

#### 1. "User not found"
- Vérifier que l'utilisateur existe avec LIST_USERS
- Vérifier l'orthographe exacte du username

#### 2. "Group not found"
- Vérifier que le groupe existe avec LIST_GROUPS
- Respecter la casse (case-sensitive)

#### 3. "Client not found"
- Vérifier avec LIST_CLIENTS
- Utiliser le clientId, pas l'UUID

#### 4. "Insufficient permissions"
- Vérifier les rôles du service account jenkins-automation
- Doit avoir: manage-users, manage-clients, view-clients, etc.

#### 5. "Token expired"
- Le token expire après 5 minutes
- Relancer le pipeline

---

## 📝 Notes de Test

### À documenter pour chaque test:
- ✅ Test réussi / ❌ Test échoué
- Temps d'exécution
- Messages d'erreur éventuels
- Comportement observé vs comportement attendu
- Screenshots des résultats importants

### Exemple de documentation:

```
Test 2.5: ADD_MEMBERS
Status: ✅ Réussi
Durée: 12s
Notes: 2 membres ajoutés avec succès. 
       Confirmation affichée pour chaque utilisateur.
       member1-test: ✅
       member2-test: ✅
```

---

## 🎯 Ordre de Test Recommandé

1. **User Management** (créer des ressources de base)
2. **Group Management** (utilise les users créés)
3. **Client Management** (indépendant)
4. **Session Management** (en dernier, surtout REVOKE_ALL)

---

**Date de création:** 2025-10-17  
**Version:** 1.0  
**Auteur:** Keycloak Automation Team
