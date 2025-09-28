# 09. Sécurité & Bonnes pratiques

- **HTTPS partout** en prod (Traefik), `KC_PROXY=edge`, `KC_HOSTNAME` cohérent.
- **Séparer les realms** par environnement/projet.
- **Clients**: Public+PKCE pour SPA, Confidential pour backend; secrets en vault.
- **Limiter les permissions DB**: user dédié pour Keycloak, pas `postgres`.
- **MFA/WebAuthn**: activer selon risques.
- **Durées de tokens**: access courts (5–15 min), refresh raisonnable; rotation.
- **CORS**: `Web Origins` stricts.
- **Désactiver** Direct Access Grants si inutile.
- **Rotation des clés** (Realm Keys) et surveiller `kid` dans les JWT.
- **Logs et audit**: activer l’audit, exporter vers SIEM.
