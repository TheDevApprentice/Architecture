# 11. Troubleshooting

- **Impossible de se connecter**: vérifier `KC_HOSTNAME`, proxy headers, CORS, horloge système.
- **Erreur JWT côté API**: vérifier `iss`, `aud`, l’URL de JWKS, et la rotation des clés.
- **DB introuvable**: Heidi/pgadmin doivent sélectionner `keycloak` (sinon `postgres`).
- **502/404 via Traefik**: labels, réseau `proxy`, port interne 8080.
- **Flows/MFA bloquants**: revoir le flow d’auth dans le realm; tester avec un flow simple.
- **IdP externe**: mismatch redirect URIs, clock skew, scopes, certificats.
- **Thème non pris en compte**: monter le dossier `themes`, vérifier `Login Theme` du realm.
