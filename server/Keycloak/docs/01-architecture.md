# 01. Architecture Keycloak

- **Serveur d’identités**: authentification, sessions, gestion des utilisateurs/groupes/rôles.
- **Protocoles**: OpenID Connect (OIDC)/OAuth2, SAML 2.0.
- **Composants**:
  - Realms (isolation), Clients (apps/API), Users/Groups, Roles (realm/client), Identity Providers, User Federation (LDAP/AD), Flows d’auth.
- **Tokens**: `id_token`, `access_token` (JWT), `refresh_token`. Signature via JWKS (`/.well-known/openid-configuration`).
- **Admin**: console web + API Admin.

## Intégration typique
- Apps Web/SPA: OIDC Authorization Code (+ PKCE).
- APIs: validation de JWT (signature, aud, iss, scopes/roles).
- M2M: Client Credentials (service accounts).
- Passwordless/MFA: TOTP, WebAuthn, conditions de flow.

## Dans ce dépôt
- Accès dev: `http://auth.localhost:8081` (Traefik/ports). 
- DB Postgres via Pgpool (5432). 
- Réseaux Docker: `proxy`, `dbnet`.
