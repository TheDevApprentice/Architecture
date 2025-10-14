# 🛠️ Dépannage Traefik

## 📋 Table des Matières

- [Démarrage](#démarrage)
- [Routage](#routage)
- [SSL/TLS](#ssltls)
- [Performance](#performance)

---

## Démarrage

### Container ne démarre pas

```bash
# Vérifier logs
docker logs traefik

# Erreurs communes:
# - Port déjà utilisé (80, 443, 8080)
# - Docker socket non accessible
# - Config YAML invalide
```

**Solutions:**

```bash
# 1. Vérifier ports
netstat -tulpn | grep -E '80|443|8080'

# 2. Tester Docker socket
docker ps

# 3. Valider config
docker run --rm -v $(pwd)/traefik.yml:/traefik.yml traefik:v2.11 \
  --configFile=/traefik.yml --dry-run
```

### Erreur "permission denied" Docker socket

```bash
# Vérifier permissions
ls -la /var/run/docker.sock

# Solution: Ajouter user au groupe docker
sudo usermod -aG docker $USER
```

---

## Routage

### Service non accessible

**1. Vérifier service running**

```bash
docker ps | grep myservice
```

**2. Vérifier labels**

```bash
docker inspect myservice | grep -A 20 Labels
```

**3. Vérifier réseau proxy**

```bash
docker inspect myservice | grep -A 10 Networks
# Doit contenir "proxy"
```

**4. Vérifier routes Traefik**

```bash
# Via dashboard
curl http://localhost:8080/api/http/routers | jq

# Via logs
docker logs traefik | grep myservice
```

### "404 Not Found"

**Causes:**
- Host header incorrect
- Règle de routage non matchée
- Service non sur réseau proxy

**Diagnostic:**

```bash
# Test avec Host header
curl -H "Host: myservice.local" http://localhost

# Vérifier règle
docker inspect myservice | grep "traefik.http.routers"
```

### "502 Bad Gateway"

**Causes:**
- Service backend down
- Port incorrect
- Network isolation

**Solutions:**

```bash
# 1. Service running?
docker ps | grep myservice

# 2. Port correct?
docker inspect myservice | grep -i port

# 3. Connectivité?
docker exec traefik ping myservice
```

---

## SSL/TLS

### Certificat non généré

**Diagnostic:**

```bash
# Logs ACME
docker logs traefik | grep -i acme

# Vérifier acme.json
docker exec traefik cat /letsencrypt/acme.json
```

**Causes communes:**

**1. Email manquant**

```yaml
certificatesResolvers:
  le:
    acme:
      email: "admin@company.com"  # ⚠️ Requis
```

**2. Challenge échoue**

```bash
# Port 443 accessible?
telnet your-domain.com 443

# Firewall bloque?
sudo iptables -L | grep 443
```

**3. Rate limit Let's Encrypt**

```bash
# Vérifier staging
certificatesResolvers:
  le:
    acme:
      caServer: "https://acme-staging-v02.api.letsencrypt.org/directory"
```

**4. DNS non propagé**

```bash
# Vérifier DNS
nslookup your-domain.com
dig your-domain.com
```

### "Certificate has expired"

```bash
# Vérifier expiration
docker logs traefik | grep -i "certificate.*expir"

# Forcer renouvellement
docker exec traefik rm /letsencrypt/acme.json
docker-compose restart traefik
```

### "Invalid certificate"

```bash
# Vérifier certificat
openssl s_client -connect your-domain.com:443 -showcerts

# Test SSL
curl -vI https://your-domain.com
```

---

## Performance

### Traefik lent

**Diagnostic:**

```bash
# CPU/RAM
docker stats traefik

# Logs errors
docker logs traefik | grep -i error
```

**Solutions:**

**1. Augmenter ressources**

```yaml
deploy:
  resources:
    limits:
      cpus: '2'
      memory: 1G
```

**2. Activer cache**

```yaml
http:
  middlewares:
    cache:
      plugin:
        souin:
          ttl: 300
```

**3. Désactiver logs access**

```yaml
accessLog: {}  # Désactiver
```

### Trop de logs

```yaml
log:
  level: WARN  # Au lieu de DEBUG
```

---

## Dashboard

### Dashboard inaccessible

**Development:**

```bash
# Vérifier config
docker exec traefik cat /etc/traefik/traefik.yml | grep -A 2 api

# Doit avoir:
# dashboard: true
# insecure: true
```

**Port correct:**

```bash
curl http://localhost:8080
```

---

## Commandes Utiles

```bash
# Restart Traefik
docker-compose restart traefik

# Reload config (si file provider)
docker kill -s SIGHUP traefik

# Vérifier routes temps réel
watch -n 1 'curl -s http://localhost:8080/api/http/routers | jq'

# Debug complet
docker logs --tail 1000 -f traefik

# Reset complet (DEV!)
docker-compose down traefik
docker volume rm letsencrypt
docker-compose up -d traefik
```

---

## Logs Importants

```bash
# Démarrage
docker logs traefik | grep "Configuration loaded"

# Routes découvertes
docker logs traefik | grep "Creating router"

# Certificats
docker logs traefik | grep -i "certificate"

# Erreurs
docker logs traefik | grep -E "ERROR|FATAL"
```

---

**⬅️ Retour au [README](./README.md)**
