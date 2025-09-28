# 08. Intégration Node/Express (Option B)

## Principe
- Backend “confidential” orchestre OIDC Code Flow (redirections + échange `code`↔`tokens`).
- Sessions avec cookie httpOnly; l’API ne voit jamais le mot de passe.

## Librairie suggérée
- `openid-client` (conforme OIDC). Voir exemple dans la discussion.

## Endpoints typiques
- `GET /login` → redirection vers Keycloak.
- `GET /callback` → échange du `code` contre des tokens; stockage session.
- `GET /logout` → fin de session et redirection vers Keycloak.
- Middleware `requireAuth` pour routes protégées (`/api/*`).

## WebSocket
- Passer le cookie de session (même domaine/proxy) ou un Bearer token court.
- Vérifier le token/session au handshake.

## Refresh token
- Utiliser `client.refresh(refresh_token)` pour régénérer l’`access_token` avant expiration.
