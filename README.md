Plateforme DevOps Cloud-Ready - Infrastructure as Code
Objectif du projet
Création d'une pierre angulaire réutilisable : une infrastructure complète, containerisée et hautement disponible permettant de déployer rapidement des solutions web robustes sur n'importe quel serveur VPS, avec capacité de mise à l'échelle en cluster sans refactoring.
Architecture & Réalisations
Infrastructure modulaire (IaC)

Architecture Docker Compose multi-fichiers avec séparation des concerns (proxy, sécurité, CI/CD, bases de données, stockage, observabilité)
Configuration centralisée via .env pour déploiements reproductibles
Réseaux Docker segmentés pour isolation et sécurité (proxy, dbnet, cachenet, stornet, keycloaknet)

Sécurité & Authentification

SSO Keycloak avec OIDC/PKCE intégré derrière Traefik en mode proxy-aware
Gestion centralisée des identités, groupes et permissions
Configuration automatisée des mappers/claims pour intégration Jenkins

CI/CD Industrialisé

Jenkins LTS avec Configuration as Code (JCasC)
Installation automatisée des plugins (liste personnalisée + suggested)
Intégrations GitHub/SSH préconfigurées et stratégie d'autorisations basée sur Keycloak

Haute Disponibilité - Bases de données

MariaDB : Cluster Galera (3 nœuds) + ProxySQL + MaxScale pour load-balancing
PostgreSQL : Cluster Patroni (3 nœuds) + Etcd (3 nœuds) + HAProxy
Basculement automatique et résilience garantie

Stockage & Cache

MinIO (S3-compatible) pour objets (images, vidéos, documents)
Redis pour cache et sessions distribuées
Coturn (TURN/STUN) pour communications temps réel

Observabilité complète

Stack Prometheus/Grafana pour métriques et dashboards
Loki/Promtail pour agrégation de logs centralisée
Tempo pour tracing distribué
OpenTelemetry Collector pour instrumentation standardisée
Exporters pour monitoring infrastructure et applicatif

DevX & Productivité

Page d'accueil centralisée pour accès rapide aux services
Private Docker Registry avec UI web
Reverse proxy Traefik avec routing automatique (*.${HOST})

Impact & Valeur
✅ Déploiement en < 10 minutes sur n'importe quel VPS avec Docker
✅ Infrastructure cloud-agnostic (AWS, GCP, Azure, bare metal)
✅ Prêt pour la production avec HA, sécurité et observabilité intégrées
✅ Évolutif sans refactoring : passage de single-node à cluster transparent
✅ Réutilisable : template pour tous les futurs projets web/SaaS