# 🚀 Passage du Registry en Production

## 📋 Vue d'ensemble

Ce document explique comment passer le Docker Registry de **développement** (HTTP) à **production** (HTTPS avec TLS).

---

## 🔄 Différences Dev vs Prod

### Mode Développement (Actuel)

```env
# .env (dev)
HOST=localhost
REGISTRY_EXPOSE_PORT=5000          # Port exposé directement
REGISTRY_ENTRYPOINT=web            # HTTP (port 80)
REGISTRY_TLS_ENABLED=false         # Pas de TLS
```

**Accès** :
- Direct : `http://localhost:5000`
- Via Traefik : `http://registry.localhost`

**Sécurité** : HTTP (non chiffré)

---

### Mode Production

```env
# .env.production
HOST=mondomaine.com
REGISTRY_EXPOSE_PORT=              # Pas d'exposition directe (vide)
REGISTRY_ENTRYPOINT=websecure      # HTTPS (port 443)
REGISTRY_TLS_ENABLED=true          # TLS activé
REGISTRY_TLS_CERTRESOLVER=le       # Let's Encrypt
```

**Accès** :
- Via Traefik uniquement : `https://registry.mondomaine.com`

**Sécurité** : HTTPS avec certificat Let's Encrypt (chiffré)

---

## 🎯 Checklist Avant Production

### 1. Infrastructure

- [ ] **Domaine configuré** : `registry.mondomaine.com` pointe vers le serveur
- [ ] **DNS propagé** : Vérifier avec `nslookup registry.mondomaine.com`
- [ ] **Port 443 ouvert** : Firewall autorise le trafic HTTPS
- [ ] **Port 80 ouvert** : Pour le challenge Let's Encrypt
- [ ] **Traefik configuré** : Let's Encrypt activé dans `traefik.yml`

### 2. Sécurité

- [ ] **Credentials forts** : Changer `REGISTRY_USERNAME` et `REGISTRY_PASSWORD`
- [ ] **Backup configuré** : Sauvegarder le volume `registry_data`
- [ ] **Monitoring activé** : Logs et alertes configurés
- [ ] **Firewall** : Limiter l'accès au registry (whitelist IP si possible)

### 3. Configuration

- [ ] **Fichier .env.production** : Créé et configuré
- [ ] **Variables vérifiées** : Tous les `CHANGE_ME_IN_PRODUCTION` remplacés
- [ ] **Registry UI** : Décider si on le garde en production (optionnel)

---

## 🔧 Étapes de Migration

### Étape 1 : Préparer le Fichier .env.production

```bash
# Copier l'exemple
cp .env.production.example .env.production

# Éditer et remplacer les valeurs
nano .env.production
```

**Variables critiques à modifier** :

```env
# Domaine de production
HOST=mondomaine.com

# Credentials forts
REGISTRY_USERNAME=admin
REGISTRY_PASSWORD=VotreMo7DeP@sseF0rt!

# Configuration HTTPS
REGISTRY_EXPOSE_PORT=              # Laisser vide !
REGISTRY_ENTRYPOINT=websecure
REGISTRY_TLS_ENABLED=true
REGISTRY_TLS_CERTRESOLVER=le
```

---

### Étape 2 : Vérifier la Configuration Traefik

Vérifier que Traefik est configuré pour Let's Encrypt :

```yaml
# server/Traefik/traefik.yml
certificatesResolvers:
  le:
    acme:
      tlsChallenge: true
      email: "votre-email@mondomaine.com"
      storage: "/letsencrypt/acme.json"
```

**Important** : L'email sera utilisé par Let's Encrypt pour les notifications.

---

### Étape 3 : Tester en Staging (Optionnel mais Recommandé)

Let's Encrypt a une limite de taux. Il est recommandé de tester d'abord avec le serveur de staging :

```yaml
# Modifier temporairement traefik.yml
certificatesResolvers:
  le:
    acme:
      caServer: "https://acme-staging-v02.api.letsencrypt.org/directory"
      tlsChallenge: true
      email: "votre-email@mondomaine.com"
      storage: "/letsencrypt/acme.json"
```

---

### Étape 4 : Déployer en Production

```bash
# 1. Arrêter les services dev
docker compose -f 01-docker-compose.Infra.dev.yml down

# 2. Charger les variables de production
export $(cat .env.production | xargs)

# 3. Démarrer avec la config production
docker compose -f 01-docker-compose.Infra.dev.yml up -d

# 4. Vérifier les logs
docker logs traefik
docker logs registry

# 5. Attendre la génération du certificat (peut prendre 1-2 minutes)
docker logs traefik | grep -i "certificate"
```

---

### Étape 5 : Vérifier le Certificat

```bash
# Vérifier que le certificat est généré
docker exec traefik ls -la /letsencrypt/

# Tester l'accès HTTPS
curl https://registry.mondomaine.com/v2/

# Vérifier le certificat SSL
openssl s_client -connect registry.mondomaine.com:443 -servername registry.mondomaine.com
```

---

### Étape 6 : Configurer les Clients Docker

**Bonne nouvelle** : Avec HTTPS, **aucune configuration client n'est nécessaire** !

Les clients Docker font confiance aux certificats Let's Encrypt par défaut.

```bash
# Sur n'importe quel serveur
docker login registry.mondomaine.com
docker push registry.mondomaine.com/minio:latest
```

---

## 🔒 Sécurité en Production

### 1. Credentials Forts

```bash
# Générer un mot de passe fort
openssl rand -base64 32

# Mettre à jour dans .env.production
REGISTRY_PASSWORD=<mot_de_passe_généré>
```

### 2. Limitation d'Accès (Optionnel)

Ajouter un middleware Traefik pour whitelist les IPs :

```yaml
# Dans docker-compose
labels:
  - "traefik.http.routers.registry.middlewares=registry-cors,registry-ipwhitelist"
  - "traefik.http.middlewares.registry-ipwhitelist.ipwhitelist.sourcerange=1.2.3.4/32,5.6.7.8/32"
```

### 3. Backup Automatique

```bash
# Script de backup (à exécuter via cron)
#!/bin/bash
BACKUP_DIR="/backups/registry"
DATE=$(date +%Y%m%d_%H%M%S)

docker run --rm \
  -v base_registry_data:/data \
  -v ${BACKUP_DIR}:/backup \
  alpine tar czf /backup/registry-${DATE}.tar.gz -C /data .

# Garder seulement les 7 derniers backups
find ${BACKUP_DIR} -name "registry-*.tar.gz" -mtime +7 -delete
```

### 4. Monitoring

```bash
# Vérifier l'espace disque
docker exec registry du -sh /var/lib/registry

# Vérifier les logs d'erreur
docker logs registry | grep -i error

# Vérifier l'expiration du certificat
docker exec traefik cat /letsencrypt/acme.json | jq '.le.Certificates[0].certificate' | openssl x509 -noout -dates
```

---

## 🔄 Rollback vers Dev

Si besoin de revenir en développement :

```bash
# 1. Arrêter les services
docker compose -f 01-docker-compose.Infra.dev.yml down

# 2. Recharger les variables dev
export $(cat .env | xargs)

# 3. Redémarrer
docker compose -f 01-docker-compose.Infra.dev.yml up -d
```

---

## 📊 Comparaison des Configurations

| Aspect | Développement | Production |
|--------|---------------|------------|
| **Domaine** | `localhost` | `mondomaine.com` |
| **Protocole** | HTTP | HTTPS |
| **Port exposé** | `5000` | Aucun (via Traefik uniquement) |
| **Certificat** | Aucun | Let's Encrypt |
| **Entrypoint** | `web` (80) | `websecure` (443) |
| **TLS** | Désactivé | Activé |
| **Config client** | Insecure registry requis | Aucune config |
| **Sécurité** | Basique | Renforcée |

---

## 🐛 Troubleshooting Production

### Problème : Certificat non généré

**Symptômes** :
```
Error: certificate signed by unknown authority
```

**Solutions** :
1. Vérifier que le domaine pointe vers le serveur
2. Vérifier que les ports 80 et 443 sont ouverts
3. Vérifier les logs Traefik : `docker logs traefik | grep -i acme`
4. Vérifier l'email dans `traefik.yml`

### Problème : "Rate limit exceeded"

**Cause** : Trop de tentatives de génération de certificat

**Solution** :
1. Utiliser le serveur de staging pour tester
2. Attendre 1 semaine (limite Let's Encrypt)
3. Vérifier que le domaine est correct avant de réessayer

### Problème : Registry inaccessible

**Vérifications** :
```bash
# DNS résout correctement ?
nslookup registry.mondomaine.com

# Port 443 ouvert ?
telnet registry.mondomaine.com 443

# Traefik route correctement ?
docker logs traefik | grep registry

# Registry répond ?
docker exec registry wget -O- http://localhost:5000/v2/
```

---

## ✅ Checklist Post-Déploiement

- [ ] **Certificat généré** : Vérifier dans `/letsencrypt/acme.json`
- [ ] **HTTPS fonctionne** : `curl https://registry.mondomaine.com/v2/`
- [ ] **Login fonctionne** : `docker login registry.mondomaine.com`
- [ ] **Push fonctionne** : `docker push registry.mondomaine.com/test:latest`
- [ ] **Pull fonctionne** : `docker pull registry.mondomaine.com/test:latest`
- [ ] **Certificat valide** : Vérifier dans le navigateur
- [ ] **Auto-renewal configuré** : Traefik renouvelle automatiquement
- [ ] **Backup configuré** : Script de backup en place
- [ ] **Monitoring actif** : Logs et alertes configurés

---

## 🎯 Résumé

Pour passer en production, il suffit de :

1. ✅ Configurer le domaine DNS
2. ✅ Créer `.env.production` avec les bonnes valeurs
3. ✅ Démarrer avec les variables de production
4. ✅ Attendre la génération du certificat Let's Encrypt
5. ✅ Tester l'accès HTTPS

**Le registry est prêt pour la production avec un simple changement de variables d'environnement !** 🚀

---

## 📚 Ressources

- [Docker Registry Documentation](https://docs.docker.com/registry/)
- [Traefik Let's Encrypt](https://doc.traefik.io/traefik/https/acme/)
- [Let's Encrypt Rate Limits](https://letsencrypt.org/docs/rate-limits/)
