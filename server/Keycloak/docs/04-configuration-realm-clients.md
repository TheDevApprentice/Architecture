# 04. Config: Realm & Clients

## Realm
- Créer un realm dédié (ne pas utiliser `master`).
- Configurer internationalisation, thèmes, politiques de mot de passe, tokens (durées).

## Client OIDC
- Type: **Confidential** (backend) ou **Public+PKCE** (SPA).
- URIs de redirection (login/logout), Web Origins.
- Scopes: `openid` + scopes personnalisés.
- Mappers: injecter claims (rôles, email, groupes) dans les tokens.

## Rôles client vs rôles realm
- Realm roles: globaux.
- Client roles: spécifiques à une application.

## Sécurité
- Désactiver Direct Access Grants si non utilisé.
- Exiger PKCE pour les publics.
- Configurer CORS via “Web Origins”.
