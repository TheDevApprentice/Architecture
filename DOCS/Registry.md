# 🐳 Docker Private Registry

## 📋 Vue d'ensemble

J'ai mis en place un **Docker Registry privé** avec authentification pour stocker et gérer mes images Docker en interne. Le registry est accessible via Traefik et dispose d'une interface web pour faciliter la gestion.

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Client Docker (Dev/CI/CD)                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  docker login localhost:5000                         │   │
│  │  docker push localhost:5000/minio:latest             │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ HTTP (dev) / HTTPS (prod)
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  Traefik (Reverse Proxy)                                     │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  registry.localhost → registry:5000                  │   │
│  │  registry-ui.localhost → registry-ui:80              │   │
│  │  + CORS middleware                                   │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            │
                ┌───────────┴───────────┐
                ▼                       ▼
┌─────────────────────────┐  ┌─────────────────────────┐
│  Docker Registry        │  │  Registry UI            │
│  (Custom Image)         │  │  (Web Interface)        │
│  ┌───────────────────┐  │  │  ┌───────────────────┐  │
│  │ Port: 5000        │  │  │  │ Port: 80          │  │
│  │ Auth: htpasswd    │  │  │  │ Auth: enabled     │  │
│  │ Storage: volume   │  │  │  └───────────────────┘  │
│  └───────────────────┘  │  └─────────────────────────┘
└─────────────────────────┘
```

---

## 🚀 Composants

### 1. Docker Registry (Custom Image)

J'ai créé une image custom basée sur `registry:2` qui génère automatiquement le fichier `htpasswd` au démarrage.

**Fichiers** :
- `server/registry/Dockerfile` : Image custom avec `apache2-utils`
- `server/registry/entrypoint.sh` : Script de génération du `htpasswd`

**Fonctionnalités** :
- ✅ Génération automatique des credentials depuis les variables d'environnement
- ✅ Authentification Basic Auth
- ✅ Suppression d'images activée (`REGISTRY_STORAGE_DELETE_ENABLED=true`)
- ✅ Stockage sur volume Docker persistant

### 2. Registry UI

Interface web pour visualiser et gérer les images du registry.

**Accès** : `http://registry-ui.localhost`

**Fonctionnalités** :
- ✅ Liste des repositories et tags
- ✅ Visualisation des manifests
- ✅ Suppression d'images (si activé)
- ✅ Authentification intégrée

---

## ⚙️ Configuration

### Variables d'Environnement (`.env`)

```env
# Docker Registry
REGISTRY_USERNAME=admin
REGISTRY_PASSWORD=changeme
REGISTRY_URL=registry.${HOST}
REGISTRY_LOADBALENCER_SERVER_PORT=5000
REGISTRY_HTTP_ENABLED=true
REGISTRY_HTTP_ADDR=:5000
REGISTRY_STORAGE_DELETE_ENABLED=true
REGISTRY_STORAGE=filesystem
REGISTRY_STORAGE_FILESYSTEM_ROOTDIRECTORY=/var/lib/registry
```

### Docker Compose

Le registry est configuré dans `01-docker-compose.Infra.dev.yml` :

```yaml
registry:
  build:
    context: ./server/registry
    dockerfile: Dockerfile
  image: registry-custom:latest
  container_name: registry
  ports:
    - "5000:5000"  # Accès direct (dev uniquement)
  environment:
    REGISTRY_USERNAME: ${REGISTRY_USERNAME}
    REGISTRY_PASSWORD: ${REGISTRY_PASSWORD}
  volumes:
    - registry_data:/var/lib/registry
  networks:
    - proxy
```

---

## 📦 Utilisation

### 1. Démarrer le Registry

```powershell
# Démarrer tous les services
docker compose -f 01-docker-compose.Infra.dev.yml up -d

# Vérifier que le registry est démarré
docker ps | Select-String "registry"
```

### 2. Se Connecter au Registry

```powershell
# Login avec les credentials du .env
docker login localhost:5000
# Username: admin
# Password: changeme

# Vérifier la connexion
curl http://localhost:5000/v2/
# Devrait retourner: {}
```

### 3. Build et Push une Image

```powershell
# Build l'image avec le tag du registry
docker build -t localhost:5000/minio:latest ./server/Minio

# Push vers le registry
docker push localhost:5000/minio:latest

# Vérifier que l'image est dans le registry
curl http://localhost:5000/v2/_catalog
# Devrait retourner: {"repositories":["minio"]}
```

### 4. Pull une Image

```powershell
# Pull depuis le registry
docker pull localhost:5000/minio:latest

# Run l'image
docker run -d localhost:5000/minio:latest
```

### 5. Lister les Images du Registry

```powershell
# Lister tous les repositories
curl http://localhost:5000/v2/_catalog

# Lister les tags d'un repository
curl http://localhost:5000/v2/minio/tags/list

# Ou via l'interface web
# http://registry-ui.localhost (login: admin/changeme)
```

---

## 🔐 Authentification

### Fonctionnement

L'authentification est gérée via **Basic Auth** avec un fichier `htpasswd` généré automatiquement au démarrage du container.

**Processus** :
1. Le container démarre
2. L'entrypoint lit `REGISTRY_USERNAME` et `REGISTRY_PASSWORD`
3. Génère `/auth/htpasswd` avec `htpasswd -Bbn`
4. Le registry utilise ce fichier pour l'authentification

### Changer les Credentials

```powershell
# 1. Modifier dans .env
REGISTRY_USERNAME=newuser
REGISTRY_PASSWORD=newpassword

# 2. Redémarrer le registry
docker compose -f 01-docker-compose.Infra.dev.yml restart registry

# 3. Se reconnecter
docker logout localhost:5000
docker login localhost:5000
```

---

## 🌐 Accès via Traefik

### En Développement (HTTP)

**URLs** :
- Registry API : `http://registry.localhost`
- Registry UI : `http://registry-ui.localhost`
- Accès direct : `http://localhost:5000`

**Configuration** :
```yaml
labels:
  - "traefik.http.routers.registry.rule=Host(`registry.${HOST}`)"
  - "traefik.http.routers.registry.entrypoints=web"
```

### En Production (HTTPS)

Pour passer en HTTPS avec Let's Encrypt, décommenter dans `docker-compose.yml` :

```yaml
labels:
  # Décommenter ces lignes :
  - "traefik.http.routers.registry.entrypoints=websecure"
  - "traefik.http.routers.registry.tls=true"
  - "traefik.http.routers.registry.tls.certresolver=le"
```

**Avantages HTTPS** :
- ✅ Communication chiffrée
- ✅ Pas besoin de configurer "insecure-registries" sur les clients
- ✅ Certificat auto-renouvelé par Let's Encrypt

---

## 🔧 Configuration Client Docker

### Pour HTTP (Dev)

Si j'utilise le registry en HTTP (sans HTTPS), je dois configurer chaque client Docker :

**Windows (Docker Desktop)** :
1. Ouvrir **Docker Desktop**
2. **Settings** → **Docker Engine**
3. Ajouter :
```json
{
  "insecure-registries": [
    "registry.mondomaine.com:5000",
    "localhost:5000"
  ]
}
```
4. **Apply & Restart**

**Linux** :
```bash
# Éditer /etc/docker/daemon.json
sudo nano /etc/docker/daemon.json

# Ajouter :
{
  "insecure-registries": ["registry.mondomaine.com:5000"]
}

# Redémarrer Docker
sudo systemctl restart docker
```

### Pour HTTPS (Prod)

Avec HTTPS, **aucune configuration client n'est nécessaire** ! Docker fait confiance aux certificats Let's Encrypt par défaut.

---

## 🔄 Intégration CI/CD

### Jenkins Pipeline Exemple

```groovy
pipeline {
    agent any
    
    environment {
        REGISTRY = 'localhost:5000'
        IMAGE_NAME = 'minio'
        IMAGE_TAG = "${BUILD_NUMBER}"
    }
    
    stages {
        stage('Build') {
            steps {
                script {
                    sh "docker build -t ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG} ./server/Minio"
                    sh "docker tag ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG} ${REGISTRY}/${IMAGE_NAME}:latest"
                }
            }
        }
        
        stage('Push to Registry') {
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-registry-creds',
                        usernameVariable: 'REGISTRY_USER',
                        passwordVariable: 'REGISTRY_PASS'
                    )]) {
                        sh "echo ${REGISTRY_PASS} | docker login ${REGISTRY} -u ${REGISTRY_USER} --password-stdin"
                        sh "docker push ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
                        sh "docker push ${REGISTRY}/${IMAGE_NAME}:latest"
                    }
                }
            }
        }
        
        stage('Deploy') {
            steps {
                script {
                    sh "docker pull ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
                    sh "docker stop ${IMAGE_NAME} || true"
                    sh "docker rm ${IMAGE_NAME} || true"
                    sh "docker run -d --name ${IMAGE_NAME} ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
                }
            }
        }
    }
    
    post {
        always {
            sh "docker logout ${REGISTRY}"
        }
    }
}
```

### Credentials Jenkins

Pour utiliser le registry dans Jenkins, je dois créer des credentials :

1. **Jenkins** → **Manage Jenkins** → **Credentials**
2. **Add Credentials** → **Username with password**
   - ID : `docker-registry-creds`
   - Username : `admin`
   - Password : `changeme`

---

## 🗑️ Gestion des Images

### Supprimer une Image

```powershell
# 1. Supprimer le tag via l'API
curl -X DELETE http://localhost:5000/v2/minio/manifests/<digest>

# 2. Lancer le garbage collector
docker exec registry bin/registry garbage-collect /etc/docker/registry/config.yml

# Ou via Registry UI (si activé)
```

### Lister les Images et Tailles

```powershell
# Lister tous les repositories
curl http://localhost:5000/v2/_catalog | ConvertFrom-Json

# Lister les tags d'un repository
curl http://localhost:5000/v2/minio/tags/list | ConvertFrom-Json

# Obtenir le manifest d'une image
curl http://localhost:5000/v2/minio/manifests/latest
```

---

## 📊 Monitoring

### Vérifier l'État du Registry

```powershell
# Vérifier que le registry répond
curl http://localhost:5000/v2/

# Vérifier les logs
docker logs registry

# Vérifier l'espace disque utilisé
docker exec registry du -sh /var/lib/registry
```

### Logs

```powershell
# Logs en temps réel
docker logs -f registry

# Dernières 50 lignes
docker logs --tail 50 registry

# Logs avec timestamps
docker logs -t registry
```

---

## 🔒 Sécurité

### Bonnes Pratiques

✅ **Credentials forts** : Utiliser des mots de passe complexes en production
✅ **HTTPS en production** : Toujours utiliser HTTPS pour la production
✅ **Backup régulier** : Sauvegarder le volume `registry_data`
✅ **Limitation d'accès** : Utiliser un firewall pour limiter l'accès au registry
✅ **Rotation des credentials** : Changer régulièrement les credentials

### Fichiers Sensibles

Les fichiers suivants contiennent des informations sensibles et sont dans `.gitignore` :

- `.env` : Credentials du registry
- `server/registry/htpasswd` : Fichier de mots de passe (généré automatiquement)

---

## 🐛 Troubleshooting

### Problème : "no basic auth credentials"

**Cause** : Pas connecté au registry

**Solution** :
```powershell
docker login localhost:5000
```

### Problème : "denied: connecting to registry"

**Cause** : Mauvais credentials

**Solution** :
```powershell
# Vérifier les credentials dans .env
cat .env | Select-String "REGISTRY"

# Se reconnecter
docker logout localhost:5000
docker login localhost:5000
```

### Problème : "dial tcp: lookup registry.localhost: no such host"

**Cause** : DNS ne résout pas `registry.localhost`

**Solution** :
```powershell
# Utiliser localhost:5000 directement
docker login localhost:5000

# Ou ajouter dans C:\Windows\System32\drivers\etc\hosts
127.0.0.1 registry.localhost
```

### Problème : "Get https://registry.localhost/v2/: dial tcp :443"

**Cause** : Docker essaie d'utiliser HTTPS

**Solution** :
```json
// Ajouter dans Docker Desktop → Settings → Docker Engine
{
  "insecure-registries": ["registry.localhost", "localhost:5000"]
}
```

### Problème : Registry UI ne se connecte pas

**Cause** : CORS ou authentification mal configurée

**Solution** :
```powershell
# Vérifier les logs
docker logs registry-ui
docker logs registry

# Vérifier la config CORS dans docker-compose
# Redémarrer les services
docker compose -f 01-docker-compose.Infra.dev.yml restart registry registry-ui
```

---

## 📚 Commandes Utiles

```powershell
# Démarrer le registry
docker compose -f 01-docker-compose.Infra.dev.yml up -d registry registry-ui

# Arrêter le registry
docker compose -f 01-docker-compose.Infra.dev.yml stop registry registry-ui

# Redémarrer le registry
docker compose -f 01-docker-compose.Infra.dev.yml restart registry

# Voir les logs
docker logs -f registry

# Rebuild l'image custom
docker compose -f 01-docker-compose.Infra.dev.yml build registry

# Lister les images du registry
curl http://localhost:5000/v2/_catalog

# Lister les tags d'une image
curl http://localhost:5000/v2/minio/tags/list

# Supprimer toutes les images locales du registry
docker images localhost:5000/* -q | ForEach-Object { docker rmi $_ }

# Backup du volume registry
docker run --rm -v base_registry_data:/data -v ${PWD}:/backup alpine tar czf /backup/registry-backup.tar.gz -C /data .

# Restore du volume registry
docker run --rm -v base_registry_data:/data -v ${PWD}:/backup alpine tar xzf /backup/registry-backup.tar.gz -C /data
```

---

## 🎯 Résumé

J'ai mis en place un **Docker Registry privé** avec :

✅ **Image custom** avec génération automatique des credentials
✅ **Authentification Basic Auth** configurable via `.env`
✅ **Interface web** pour gérer les images
✅ **Routing Traefik** avec CORS
✅ **Support HTTP (dev)** et **HTTPS (prod)**
✅ **Intégration CI/CD** prête pour Jenkins
✅ **Stockage persistant** sur volume Docker

Le registry est **opérationnel** et prêt à être utilisé pour stocker mes images Docker en interne ! 🚀
