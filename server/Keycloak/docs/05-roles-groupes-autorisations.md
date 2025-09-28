# 05. Rôles, Groupes, Autorisations

## Rôles
- Créer des rôles `realm` (ex: `admin`, `user`).
- Créer des rôles `client` (ex: `chat:read`, `chat:write`).

## Groupes
- Structurer les utilisateurs (ex: `staff`, `customers`). Les groupes peuvent embarquer des rôles.

## Mappers → tokens
- Ajouter des “client scopes” et “protocol mappers” pour transmettre rôles/groupes en claims.

## Authorization Services (facultatif)
- Politiques (role-based, temps, user-attr), ressources et permissions.
- Évaluation côté Keycloak; le client reçu un RPT/permissions.

## Côté API
- Vérifier `realm_access.roles` et `resource_access[client].roles` dans le JWT.
