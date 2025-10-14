# 🔐 Sécurité Traefik

## 📋 Table des Matières

- [SSL/TLS](#ssltls)
- [Let's Encrypt](#lets-encrypt)
- [Meilleures Pratiques](#meilleures-pratiques)
- [Hardening](#hardening)

---

## SSL/TLS

### Configuration Production

```yaml
entryPoints:
  web:
    address: ":80"
    http:
      redirections:
        entryPoint:
          to: websecure
          scheme: https
  websecure:
    address: ":443"

certificatesResolvers:
  le:
    acme:
      tlsChallenge: true
      email: "admin@company.com"
      storage: "/letsencrypt/acme.json"
```

### Labels Service

```yaml
labels:
  - "traefik.http.routers.myservice.entrypoints=websecure"
  - "traefik.http.routers.myservice.tls.certresolver=le"
```

---

## Let's Encrypt

### Challenge Types

#### TLS Challenge (Recommandé)

```yaml
certificatesResolvers:
  le:
    acme:
      tlsChallenge: true
```

**Avantages:**
- Simple à configurer
- Pas besoin d'ouvrir port 80
- Fonctionne via port 443

#### HTTP Challenge

```yaml
certificatesResolvers:
  le:
    acme:
      httpChallenge:
        entryPoint: web
```

**Usage:** Si TLS Challenge ne fonctionne pas

#### DNS Challenge

```yaml
certificatesResolvers:
  le:
    acme:
      dnsChallenge:
        provider: cloudflare
```

**Usage:** Wildcards `*.company.com`

### Volume Persistence

```yaml
volumes:
  - letsencrypt:/letsencrypt
```

**Important:**
- Persiste certificats
- Évite rate limiting Let's Encrypt
- Backup recommandé

### Rate Limits

**Let's Encrypt:**
- 50 certificats / semaine / domaine
- 5 échecs / heure / hostname

**Solution:** Tester en staging

```yaml
certificatesResolvers:
  le:
    acme:
      caServer: "https://acme-staging-v02.api.letsencrypt.org/directory"
```

---

## Meilleures Pratiques

### 1. Désactiver Dashboard en Production

```yaml
api:
  dashboard: false
  insecure: false
```

### 2. Exposer Services Explicitement

```yaml
providers:
  docker:
    exposedByDefault: false
```

**Toujours** utiliser `traefik.enable=true`

### 3. Docker Socket en Read-Only

```yaml
volumes:
  - /var/run/docker.sock:/var/run/docker.sock:ro
```

⚠️ **Socket Docker = root access**

### 4. Security Headers

```yaml
labels:
  - "traefik.http.middlewares.security.headers.sslredirect=true"
  - "traefik.http.middlewares.security.headers.stsSeconds=31536000"
  - "traefik.http.middlewares.security.headers.stsIncludeSubdomains=true"
  - "traefik.http.middlewares.security.headers.stsPreload=true"
  - "traefik.http.middlewares.security.headers.contentTypeNosniff=true"
  - "traefik.http.middlewares.security.headers.browserXssFilter=true"
  - "traefik.http.middlewares.security.headers.frameDeny=true"
```

### 5. Rate Limiting

```yaml
labels:
  - "traefik.http.middlewares.ratelimit.ratelimit.average=100"
  - "traefik.http.middlewares.ratelimit.ratelimit.burst=50"
```

### 6. IP Whitelist

```yaml
labels:
  - "traefik.http.middlewares.whitelist.ipwhitelist.sourcerange=192.168.1.0/24,10.0.0.0/8"
```

---

## Hardening

### TLS Configuration

```yaml
entryPoints:
  websecure:
    address: ":443"
    http:
      tls:
        minVersion: VersionTLS12
        cipherSuites:
          - TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
          - TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
          - TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305
```

### Disable HTTP/1.0

```yaml
entryPoints:
  web:
    http:
      http1:
        maxVersion: "1.1"
```

### Logging Sensitivity

```yaml
log:
  level: INFO        # Production: INFO ou WARN
  
# Ne pas logger secrets
accessLog:
  filters:
    statusCodes:
      - "200"
      - "300-302"
    retryAttempts: true
```

---

## Monitoring Sécurité

### Logs à Surveiller

```bash
# Erreurs TLS
docker logs traefik | grep -i tls

# Certificats expirés
docker logs traefik | grep -i "certificate.*expir"

# Échecs authentification
docker logs traefik | grep -i "401\|403"
```

### Health Checks

```bash
# Vérifier certificats
curl -vI https://service.company.com 2>&1 | grep -i "expire"

# Test SSL Labs
# https://www.ssllabs.com/ssltest/
```

---

## Checklist Sécurité

### Pré-Production

- [ ] Dashboard désactivé
- [ ] HTTPS obligatoire
- [ ] Certificats Let's Encrypt configurés
- [ ] Docker socket en read-only
- [ ] `exposedByDefault: false`
- [ ] Security headers activés
- [ ] Rate limiting configuré
- [ ] TLS minimum v1.2
- [ ] Logs sécurité activés

### Post-Déploiement

- [ ] Test SSL Labs (A+ rating)
- [ ] Vérifier certificats valides
- [ ] Tester redirection HTTP→HTTPS
- [ ] Audit logs sécurité
- [ ] Backup acme.json

---

**⬅️ Retour au [README](./README.md)**
