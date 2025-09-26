Une belle base avec le cluster MariaDB Galera et le setup en réseau. Pour intégrer un système d'auth centralisé comme Keycloak/Authentik dans l'architecture

Recommandations :
🔧 Où placer le système d'auth dans l'archi actuelle :
Dans le network: proxy - À côté de Traefik

Un service keycloak ou authentik
Exposé sur un sous-domaine dédié : auth.menuwebservice.com
Traefik route les requêtes /auth/* vers ce service

🔄 Modifications à apporter :
1. Niveau Traefik (proxy)

Ajouter un middleware d'authentification forward-auth
Traefik vérifie les tokens JWT avant de router vers les apps
Configuration des headers d'authentification

2. Niveau Applications (Server #1 & #2)

Remplacer les systèmes auth custom par des vérifications de tokens JWT
Tes apps deviennent des "resource servers"
Plus besoin de gérer les sessions/cookies d'auth

3. Base de données

Keycloak/Authentik aura sa propre DB (peut utiliser ton cluster MariaDB)
Les apps gardent leurs données métier
Sync des utilisateurs via API si besoin

🎯 Avantages de cette approche :

Scalabilité : Un seul point d'auth pour tous les services
Sécurité : Tokens JWT avec expiration, refresh tokens
UX : SSO entre toutes les apps
Maintenance : Plus de logique d'auth dupliquée dans chaque app

🚀 Ordre de migration recommandé :

Déployer Keycloak/Authentik à côté de Traefik
Configurer un realm/domain de test
Migrer une app à la fois vers le nouveau système
Configurer Traefik pour l'auth forward sur les autres apps


# TODO

Architecture : 

- proxy sur le port 80 et 443 qui expose les services docker tel que traefik, minio, redis, turn.
- Sous domaine spécifique pour le système d'auth centralisé : auth.menuwebservice.com
- Sous domaine spécifique pour le système de mailing : mail.menuwebservice.com

- menuwebservice.com
    - proxy (docker traefik)
    - redis (docker redis)
    - minio (docker minio)
    - turn (docker coturn)
    - mail (docker mail) route vers sous-domaine mail.menuwebservice.com 
    - authentik/keycloak (docker authentik/keycloak) route vers sous-domaine auth.menuwebservice.com
    - proxySql (docker proxysql)
    - mariadb nodes (docker mariadb)
        - mariadb 1
        - mariadb 2
        - mariadb 3
    - mariadb galera (docker mariadb)
    - maxscale (docker maxscale)
    - mariadb_backup (docker mariadb)

- auth.menuwebservice.com
- mail.menuwebservice.com

Scripting : 
- Script .sh de création de sous domaine auth.menuwebservice.com (le sous domaine doit être déclarer au DNS hostinger mais le script sers à s'assurer de créer un certificat let's encrypt pour le sous domaine si il n'y en a pas et s'execute de maniére périodique afin de s'assurer que le certificat est encore bon et le renouvellever si besoin)

- Script .sh de création de sous domaine mail.menuwebservice.com (le sous domaine doit être déclarer au DNS hostinger mais le script sers à s'assurer de créer un certificat let's encrypt pour le sous domaine si il n'y en a pas et s'execute de maniére périodique afin de s'assurer que le certificat est encore bon et le renouvellever si besoin)