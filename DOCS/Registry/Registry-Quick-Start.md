# 🚀 Docker Registry - Quick Start

## 📋 TL;DR

**Développement** : HTTP sur `localhost:5000`
**Production** : HTTPS sur `registry.mondomaine.com` via Traefik

---

## ⚡ Démarrage Rapide

### Développement

```bash
# 1. Démarrer
docker compose -f 01-docker-compose.Infra.dev.yml up -d registry registry-ui

# 2. Login
docker login localhost:5000
# Username: admin
# Password: changeme

# 3. Push une image
docker build -t localhost:5000/myapp:latest .
docker push localhost:5000/myapp:latest

# 4. Interface web
# http://registry-ui.localhost
```

### Production

```bash
# 1. Créer .env.production
cp .env.production.example .env.production
nano .env.production  # Modifier HOST et credentials

# 2. Charger les variables
export $(cat .env.production | xargs)

# 3. Démarrer
docker compose -f 01-docker-compose.Infra.dev.yml up -d

# 4. Login (HTTPS automatique)
docker login registry.mondomaine.com
```

---

## 🔧 Configuration

### Variables Clés (.env)

| Variable | Dev | Production |
|----------|-----|------------|
| `HOST` | `localhost` | `mondomaine.com` |
| `REGISTRY_EXPOSE_PORT` | `5000` | `` (vide) |
| `REGISTRY_ENTRYPOINT` | `web` | `websecure` |
| `REGISTRY_TLS_ENABLED` | `false` | `true` |
| `REGISTRY_USERNAME` | `admin` | `admin` |
| `REGISTRY_PASSWORD` | `changeme` | **Mot de passe fort** |

---

## 📚 Documentation Complète

- **[Registry.md](./Registry.md)** : Documentation complète du registry
- **[Registry-Production.md](./Registry-Production.md)** : Guide de migration en production

---

## 🎯 Architecture

```
Dev:  Client → localhost:5000 (HTTP direct)
      Client → registry.localhost (HTTP via Traefik)

Prod: Client → registry.mondomaine.com (HTTPS via Traefik + Let's Encrypt)
```

---

## ✅ Checklist Production

- [ ] Domaine configuré (`registry.mondomaine.com`)
- [ ] DNS propagé
- [ ] Ports 80 et 443 ouverts
- [ ] `.env.production` créé et configuré
- [ ] Credentials forts définis
- [ ] Backup configuré
- [ ] Certificat Let's Encrypt généré
- [ ] Tests de push/pull réussis

---

## 🐛 Problèmes Courants

### "no basic auth credentials"
```bash
docker login localhost:5000
```

### "denied: connecting to registry"
```bash
# Vérifier credentials dans .env
cat .env | grep REGISTRY
```

### "dial tcp: lookup registry.localhost: no such host"
```bash
# Utiliser localhost:5000 directement
docker login localhost:5000
```

### Certificat non généré (production)
```bash
# Vérifier DNS
nslookup registry.mondomaine.com

# Vérifier logs Traefik
docker logs traefik | grep -i acme
```

---

## 📞 Support

Voir la documentation complète dans `DOCS/Registry.md` pour plus de détails.
