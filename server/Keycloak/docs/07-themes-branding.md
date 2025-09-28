# 07. Thèmes & Branding

## Objectif
- Donner l’impression que le login “reste dans ton site”.

## Création d’un thème
- Arborescence: `/opt/keycloak/themes/<mon-theme>/` avec `theme.properties`, templates Freemarker, assets.
- Docker: copier le thème via `server/Keycloak/Dockerfile` puis `KC_THEME`/paramétrage realm.

## Personnalisations
- Pages: login, register, OTP, error.
- CSS/JS, logos, i18n.

## Sécurité
- Ne pas exposer d’infos sensibles dans le thème.
