# 03. Flows OIDC/OAuth2

## Authorization Code + PKCE (SPA)
- Redirection navigateur vers Keycloak → login → retour avec `code` → échange `code` contre `tokens` côté front (PKCE).
- Avantages: sécurité (pas de secret côté front), MFA, SSO, UX.

## Authorization Code (backend confidentiel)
- Redirection via serveur → `/callback` échange `code`↔`tokens` (avec `client_secret`).
- Tokens stockés côté serveur en session/cookies httpOnly.

## Client Credentials (M2M)
- Pas d’utilisateur final; `client_id`/`client_secret` ↔ `access_token`.

## Refresh tokens
- Renouveler l’`access_token` court. Gérer l’expiration/rotation.

## Validation des JWT côté API
- Vérifier `iss`, `aud`, `exp`, signature JWKS, et rôles/scopes.

## Endpoints standards
- Découverte: `/.well-known/openid-configuration`
- Authorization, Token, UserInfo, JWKS, Logout.
