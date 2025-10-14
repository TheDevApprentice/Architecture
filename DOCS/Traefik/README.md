# 🌐 Traefik - Documentation Complète

## 📋 Sommaire

1. [Vue d'ensemble](#-vue-densemble)
2. [Architecture](#-architecture)
3. [Configuration](#-configuration)
4. [Routing](#-routing)
5. [SSL/TLS](#-ssltls)
6. [Déploiement](#-déploiement)
7. [Monitoring](#-monitoring)
8. [Dépannage](#-dépannage)

---

## 🎯 Vue d'ensemble

Traefik est le **reverse proxy et load balancer** de l'infrastructure, servant de point d'entrée unique pour tous les services HTTP/HTTPS.

### Fonctionnalités Clés

- **🔄 Reverse Proxy** - Routage intelligent des requêtes
- **🔐 SSL/TLS Automatique** - Let's Encrypt intégré
- **🐳 Docker Integration** - Auto-découverte via labels
- **📊 Monitoring** - Métriques Prometheus
- **⚡ Load Balancing** - Distribution de charge
- **🛡️ Middlewares** - Auth, rate limit, compression

### 🏗️ Architecture

```
┌────────────────────────────────────────────┐
│              Internet                       │
└─────────────┬──────────────────────────────┘
              │
       HTTP (80) / HTTPS (443)
              │
              ▼
┌────────────────────────────────────────────┐
│           Traefik Container                │
├────────────────────────────────────────────┤
│                                            │
│  ┌──────────────────────────────────────┐ │
│  │  EntryPoints                         │ │
│  │  ├─> web (:80)      → HTTP          │ │
│  │  ├─> websecure (:443) → HTTPS       │ │
│  │  └─> metrics (:9100) → Prometheus   │ │
│  └──────────────────────────────────────┘ │
│                                            │
│  ┌──────────────────────────────────────┐ │
│  │  Router Engine                       │ │
│  │  - Parse labels                      │ │
│  │  - Match rules                       │ │
│  │  - Apply middlewares                 │ │
│  └──────────────────────────────────────┘ │
│                                            │
│  ┌──────────────────────────────────────┐ │
│  │  Providers                           │ │
│  │  └─> Docker (socket mounted)        │ │
│  └──────────────────────────────────────┘ │
│                                            │
│  ┌──────────────────────────────────────┐ │
│  │  Certificate Resolver (Production)   │ │
│  │  └─> Let's Encrypt ACME             │ │
│  └──────────────────────────────────────┘ │
│                                            │
└─────────────────┬──────────────────────────┘
                  │
            Docker Network (proxy)
                  │
     ┌────────────┼────────────┐
     │            │            │
     ▼            ▼            ▼
┌─────────┐  ┌─────────┐  ┌─────────┐
│Keycloak │  │ Jenkins │  │  MinIO  │
└─────────┘  └─────────┘  └─────────┘
```

### 🔑 Rôle dans l'Infrastructure

| Fonction | Description |
|----------|-------------|
| **Gateway** | Point d'entrée unique pour toutes les requêtes |
| **Routing** | Dirige vers le bon service selon hostname/path |
| **SSL Termination** | Gère les certificats SSL/TLS |
| **Service Discovery** | Détecte automatiquement les services Docker |
| **Load Balancing** | Distribue la charge entre instances |

---

## 📂 Structure de la Documentation

| Fichier | Description |
|---------|-------------|
| **[CONFIGURATION.md](./CONFIGURATION.md)** | Configuration dev/prod, entrypoints, providers |
| **[DEPLOYMENT.md](./DEPLOYMENT.md)** | Guide de déploiement et Docker Compose |
| **[ROUTING.md](./ROUTING.md)** | Routage, labels Docker, middlewares |
| **[SECURITY.md](./SECURITY.md)** | SSL/TLS, Let's Encrypt, sécurité |
| **[TROUBLESHOOTING.md](./TROUBLESHOOTING.md)** | Solutions aux problèmes courants |

---

## ⚙️ Configuration

### Deux Modes de Déploiement

#### 🛠️ Mode Development (`traefik.dev.yml`)

```yaml
api:
  dashboard: true        # ✅ Dashboard activé
  insecure: true         # ✅ Accès sans auth (dev)

log:
  level: DEBUG           # Logs verbeux

entryPoints:
  web:
    address: ":80"       # HTTP uniquement
  metrics:
    address: ":9100"     # Prometheus metrics
```

**Caractéristiques:**
- Dashboard accessible: `http://localhost:8080`
- HTTP seulement (pas de HTTPS)
- Logs DEBUG pour développement
- Pas de Let's Encrypt
- Pas de redirection HTTP→HTTPS

#### 🚀 Mode Production (`traefik.yml`)

```yaml
api:
  dashboard: false       # ❌ Dashboard désactivé
  insecure: false        # ❌ Pas d'accès non sécurisé

entryPoints:
  web:
    address: ":80"
    http:
      redirections:
        entryPoint:
          to: websecure  # ✅ Redirection HTTP→HTTPS
          scheme: https
  websecure:
    address: ":443"      # ✅ HTTPS

certificatesResolvers:
  le:
    acme:
      tlsChallenge: true
      email: "admin@company.com"
      storage: "/letsencrypt/acme.json"
```

**Caractéristiques:**
- HTTPS automatique avec Let's Encrypt
- Redirection HTTP→HTTPS forcée
- Dashboard désactivé
- Certificats persistés dans volume

---

## 🔀 Routing

### Configuration via Labels Docker

**Exemple: Keycloak**

```yaml
services:
  keycloak:
    image: keycloak:latest
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.keycloak.rule=Host(`keycloak.local`)"
      - "traefik.http.routers.keycloak.entrypoints=web"
      - "traefik.http.services.keycloak.loadbalancer.server.port=8080"
    networks:
      - proxy
```

**Décomposition:**
- `traefik.enable=true` - Active Traefik pour ce service
- `rule=Host(...)` - Règle de routage (hostname)
- `entrypoints=web` - Utilise entrypoint HTTP
- `server.port=8080` - Port backend du service

### Routing HTTPS (Production)

```yaml
labels:
  - "traefik.enable=true"
  - "traefik.http.routers.keycloak.rule=Host(`keycloak.company.com`)"
  - "traefik.http.routers.keycloak.entrypoints=websecure"
  - "traefik.http.routers.keycloak.tls.certresolver=le"
  - "traefik.http.services.keycloak.loadbalancer.server.port=8080"
```

**Voir [ROUTING.md](./ROUTING.md) pour tous les exemples**

---

## 🔐 SSL/TLS

### Let's Encrypt Automatique (Production)

```yaml
certificatesResolvers:
  le:
    acme:
      tlsChallenge: true                    # Challenge TLS-ALPN-01
      email: "admin@company.com"            # Email Let's Encrypt
      storage: "/letsencrypt/acme.json"     # Stockage certificats
```

**Fonctionnement:**
1. Service demande certificat via labels
2. Traefik contacte Let's Encrypt
3. Challenge TLS-ALPN-01 effectué
4. Certificat obtenu et stocké
5. Renouvellement automatique (90 jours)

### Volume Persistence

```yaml
volumes:
  - letsencrypt:/letsencrypt
```

**Important:** Volume persiste les certificats entre redémarrages

**Voir [SECURITY.md](./SECURITY.md) pour configuration SSL complète**

---

## 📊 Monitoring

### Prometheus Metrics

**Endpoint:** `http://traefik:9100/metrics`

```yaml
metrics:
  prometheus:
    entryPoint: metrics
```

**Métriques disponibles:**
- `traefik_entrypoint_requests_total` - Requêtes totales
- `traefik_entrypoint_request_duration_seconds` - Latence
- `traefik_service_requests_total` - Requêtes par service
- `traefik_backend_connections_count` - Connexions actives

### Dashboard (Development)

**URL:** `http://localhost:8080`

**Fonctionnalités:**
- Vue temps réel des routes
- Services découverts
- Middlewares actifs
- Certificats SSL
- Métriques HTTP

---

## 🚀 Déploiement

### Docker Compose

```yaml
services:
  traefik:
    build:
      context: ./server/Traefik
      args:
        NODE_ENV: ${NODE_ENV:-dev}
    container_name: traefik
    ports:
      - "80:80"
      - "443:443"
      - "8080:8080"    # Dashboard (dev only)
      - "9100:9100"    # Metrics
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - letsencrypt:/letsencrypt
    networks:
      - proxy
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.api.rule=Host(`traefik.local`)"
      - "traefik.http.routers.api.service=api@internal"

networks:
  proxy:
    name: proxy
    driver: bridge

volumes:
  letsencrypt:
```

### Démarrage

```bash
# Development
NODE_ENV=dev docker-compose up -d traefik

# Production
NODE_ENV=production docker-compose up -d traefik
```

**Voir [DEPLOYMENT.md](./DEPLOYMENT.md) pour guide complet**

---

## 🔄 Workflow Typique

### Requête HTTP → Service

```
1. Client → http://keycloak.local
   └─> DNS résout vers IP serveur

2. Serveur:80 → Traefik entrypoint "web"
   └─> Parse Host header: keycloak.local

3. Traefik → Match router rule
   └─> Router "keycloak" matched

4. Traefik → Forward to service
   └─> Container keycloak:8080

5. Service → Response
   └─> Traefik → Client
```

### Requête HTTPS → Service (Production)

```
1. Client → https://keycloak.company.com
   
2. Traefik:443 → TLS handshake
   └─> Présente certificat Let's Encrypt
   
3. TLS établi → Decrypt request
   
4. Match router → Forward to backend
   
5. Backend response → Encrypt → Client
```

---

## 🛠️ Dépannage Rapide

### Service non accessible

```bash
# Vérifier Traefik running
docker ps | grep traefik

# Vérifier logs
docker logs traefik

# Vérifier routes (dashboard)
curl http://localhost:8080/api/http/routers
```

### Certificat SSL non généré

```bash
# Vérifier logs ACME
docker logs traefik | grep -i acme

# Vérifier fichier acme.json
docker exec traefik ls -la /letsencrypt/acme.json

# Vérifier email Let's Encrypt
docker exec traefik cat /etc/traefik/traefik.yml | grep email
```

**Voir [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) pour plus de solutions**

---

## 📚 Ressources

### Documentation Officielle
- [Traefik Documentation](https://doc.traefik.io/traefik/)
- [Docker Provider](https://doc.traefik.io/traefik/providers/docker/)
- [Let's Encrypt](https://doc.traefik.io/traefik/https/acme/)
- [Middlewares](https://doc.traefik.io/traefik/middlewares/overview/)

### Liens Internes
- [Architecture Globale](../ARCHITECTURE.md)
- [Documentation Keycloak](../Keycloak/README.md)
- [Documentation Jenkins](../Jenkins/README.md)

---

## 🤝 Contribution

### Structure des Fichiers

```
server/Traefik/
├── Dockerfile              # Image multi-stage (dev/prod)
├── traefik.dev.yml         # Config development
└── traefik.yml             # Config production
```

### Ajouter un Service

```yaml
# Dans docker-compose.yml
services:
  monservice:
    image: monservice:latest
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.monservice.rule=Host(`monservice.local`)"
      - "traefik.http.services.monservice.loadbalancer.server.port=8080"
    networks:
      - proxy
```

---

## 📝 Changelog

### Version 2.11

- ✅ Docker provider auto-discovery
- ✅ Let's Encrypt ACME
- ✅ Prometheus metrics
- ✅ TLS Challenge support
- ✅ HTTP/HTTPS entrypoints
- ✅ Dashboard API
- ✅ Multi-stage Dockerfile (dev/prod)

---

**📖 Pour commencer, consultez [DEPLOYMENT.md](./DEPLOYMENT.md)**
