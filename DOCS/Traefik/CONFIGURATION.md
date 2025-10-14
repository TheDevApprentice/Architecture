# ⚙️ Configuration Traefik

## 📋 Table des Matières

- [Fichiers de Configuration](#fichiers-de-configuration)
- [EntryPoints](#entrypoints)
- [Providers](#providers)
- [API & Dashboard](#api--dashboard)
- [Logging](#logging)
- [Metrics](#metrics)

---

## Fichiers de Configuration

### traefik.dev.yml (Development)

```yaml
api:
  dashboard: true        # Dashboard activé
  insecure: true         # Accessible sans authentification

log:
  level: DEBUG           # Logs verbeux pour debug

providers:
  docker:
    exposedByDefault: false    # Sécurité: opt-in explicite

entryPoints:
  web:
    address: ":80"             # HTTP uniquement en dev
  metrics:
    address: ":9100"           # Prometheus metrics

metrics:
  prometheus:
    entryPoint: metrics
```

**Usage:**
- Développement local
- Tests et debugging
- Dashboard accessible sur `:8080`

### traefik.yml (Production)

```yaml
api:
  dashboard: false       # Dashboard désactivé
  insecure: false        # Pas d'accès non sécurisé

log:
  level: DEBUG

providers:
  docker:
    exposedByDefault: false

entryPoints:
  web:
    address: ":80"
    http:
      redirections:
        entryPoint:
          to: websecure          # Redirection HTTP→HTTPS
          scheme: https
  websecure:
    address: ":443"              # HTTPS
  metrics:
    address: ":9100"

metrics:
  prometheus:
    entryPoint: metrics
    
certificatesResolvers:
  le:
    acme:
      tlsChallenge: true
      email: "admin@company.com"
      storage: "/letsencrypt/acme.json"
```

**Usage:**
- Production
- HTTPS obligatoire
- Let's Encrypt automatique

---

## EntryPoints

### web (HTTP)

```yaml
entryPoints:
  web:
    address: ":80"
```

**Development:** Endpoint principal  
**Production:** Redirige vers HTTPS

### websecure (HTTPS)

```yaml
entryPoints:
  websecure:
    address: ":443"
```

**Production uniquement**  
Gère les connexions TLS/SSL

### metrics (Prometheus)

```yaml
entryPoints:
  metrics:
    address: ":9100"
```

Expose métriques pour Prometheus

---

## Providers

### Docker Provider

```yaml
providers:
  docker:
    exposedByDefault: false    # ⚠️ Important: opt-in
```

**Configuration:**
- Monitore Docker socket
- Découverte automatique via labels
- `exposedByDefault: false` - Sécurité

**Montage requis:**
```yaml
volumes:
  - /var/run/docker.sock:/var/run/docker.sock:ro
```

---

## API & Dashboard

### Development

```yaml
api:
  dashboard: true
  insecure: true
```

**Accès:** `http://localhost:8080`

**Features:**
- Vue temps réel des routers
- Services découverts
- Middlewares
- Certificats

### Production

```yaml
api:
  dashboard: false
  insecure: false
```

**Dashboard désactivé** pour sécurité

---

## Logging

### Niveaux

```yaml
log:
  level: DEBUG    # DEBUG, INFO, WARN, ERROR, FATAL, PANIC
```

**Recommandations:**
- Development: `DEBUG`
- Production: `INFO` ou `WARN`

### Logs Disponibles

```bash
# Logs Traefik
docker logs traefik

# Filtrer erreurs
docker logs traefik | grep ERROR

# Logs ACME
docker logs traefik | grep -i acme
```

---

## Metrics

### Prometheus

```yaml
metrics:
  prometheus:
    entryPoint: metrics
```

**Endpoint:** `http://traefik:9100/metrics`

**Métriques:**
```
traefik_entrypoint_requests_total
traefik_entrypoint_request_duration_seconds_sum
traefik_service_requests_total
traefik_backend_connections_count
```

---

**⬅️ Retour au [README](./README.md)**
