# 🔀 Routage Traefik

## 📋 Table des Matières

- [Labels Docker](#labels-docker)
- [Règles de Routage](#règles-de-routage)
- [Middlewares](#middlewares)
- [Load Balancing](#load-balancing)

---

## Labels Docker

### Configuration Basique

```yaml
labels:
  - "traefik.enable=true"
  - "traefik.http.routers.myservice.rule=Host(`myservice.local`)"
  - "traefik.http.routers.myservice.entrypoints=web"
  - "traefik.http.services.myservice.loadbalancer.server.port=8080"
```

**Décomposition:**
1. `traefik.enable=true` - Active Traefik
2. `routers.myservice.rule` - Règle de matching
3. `routers.myservice.entrypoints` - EntryPoint utilisé
4. `services.myservice.loadbalancer.server.port` - Port backend

### HTTPS (Production)

```yaml
labels:
  - "traefik.enable=true"
  - "traefik.http.routers.myservice.rule=Host(`myservice.company.com`)"
  - "traefik.http.routers.myservice.entrypoints=websecure"
  - "traefik.http.routers.myservice.tls.certresolver=le"
  - "traefik.http.services.myservice.loadbalancer.server.port=8080"
```

**Ajouts:**
- `entrypoints=websecure` - HTTPS
- `tls.certresolver=le` - Utilise Let's Encrypt

---

## Règles de Routage

### Host-based

```yaml
# Hostname simple
rule: Host(`keycloak.local`)

# Multiples hosts
rule: Host(`keycloak.local`) || Host(`auth.local`)
```

### Path-based

```yaml
# Path prefix
rule: Host(`api.local`) && PathPrefix(`/v1`)

# Path exact
rule: Host(`api.local`) && Path(`/health`)
```

### Header-based

```yaml
# Custom header
rule: Host(`api.local`) && Headers(`X-API-Key`, `secret123`)
```

### Combinaisons

```yaml
# Host + Path
rule: Host(`api.local`) && (PathPrefix(`/v1`) || PathPrefix(`/v2`))

# Host + Method
rule: Host(`api.local`) && Method(`GET`, `POST`)
```

---

## Middlewares

### Redirect HTTPS

```yaml
labels:
  # Middleware definition
  - "traefik.http.middlewares.redirect-https.redirectscheme.scheme=https"
  - "traefik.http.middlewares.redirect-https.redirectscheme.permanent=true"
  
  # Apply middleware
  - "traefik.http.routers.myservice.middlewares=redirect-https"
```

### Basic Auth

```yaml
labels:
  # Create user: admin / password
  # htpasswd -nb admin password
  - "traefik.http.middlewares.auth.basicauth.users=admin:$$apr1$$..."
  - "traefik.http.routers.myservice.middlewares=auth"
```

### Rate Limiting

```yaml
labels:
  - "traefik.http.middlewares.ratelimit.ratelimit.average=100"
  - "traefik.http.middlewares.ratelimit.ratelimit.burst=50"
  - "traefik.http.routers.myservice.middlewares=ratelimit"
```

### Headers

```yaml
labels:
  # Add security headers
  - "traefik.http.middlewares.security.headers.sslredirect=true"
  - "traefik.http.middlewares.security.headers.stsSeconds=31536000"
  - "traefik.http.middlewares.security.headers.contentTypeNosniff=true"
  - "traefik.http.middlewares.security.headers.browserXssFilter=true"
  - "traefik.http.routers.myservice.middlewares=security"
```

### Multiple Middlewares

```yaml
labels:
  - "traefik.http.routers.myservice.middlewares=auth,ratelimit,security"
```

---

## Load Balancing

### Multiple Instances

```yaml
services:
  app:
    image: myapp:latest
    deploy:
      replicas: 3
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.app.rule=Host(`app.local`)"
      - "traefik.http.services.app.loadbalancer.server.port=8080"
    networks:
      - proxy
```

**Traefik distribue automatiquement** entre les 3 instances

### Health Checks

```yaml
labels:
  - "traefik.http.services.app.loadbalancer.healthcheck.path=/health"
  - "traefik.http.services.app.loadbalancer.healthcheck.interval=10s"
  - "traefik.http.services.app.loadbalancer.healthcheck.timeout=3s"
```

---

## Exemples Complets

### Keycloak

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

### Jenkins

```yaml
services:
  jenkins:
    image: jenkins:lts
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.jenkins.rule=Host(`jenkins.local`)"
      - "traefik.http.routers.jenkins.entrypoints=web"
      - "traefik.http.services.jenkins.loadbalancer.server.port=8080"
    networks:
      - proxy
```

### MinIO (avec auth)

```yaml
services:
  minio:
    image: minio/minio
    labels:
      - "traefik.enable=true"
      # Console
      - "traefik.http.routers.minio-console.rule=Host(`minio.local`)"
      - "traefik.http.routers.minio-console.service=minio-console"
      - "traefik.http.services.minio-console.loadbalancer.server.port=9001"
      # API
      - "traefik.http.routers.minio-api.rule=Host(`s3.local`)"
      - "traefik.http.routers.minio-api.service=minio-api"
      - "traefik.http.services.minio-api.loadbalancer.server.port=9000"
    networks:
      - proxy
```

---

**⬅️ Retour au [README](./README.md)**
