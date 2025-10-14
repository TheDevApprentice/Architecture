# Jenkins Configuration

Ce dossier contient toute la configuration Jenkins qui sera copiée dans l'image Docker.

## 📁 Structure

```
config/
├── jenkins.yaml              # Configuration JCasC (Jenkins Configuration as Code)
├── plugins.txt               # Liste des plugins à installer
├── entrypoint.sh            # Script de démarrage personnalisé
├── pipelines/               # Pipelines Jenkinsfile
│   ├── keycloak-user-management.jenkinsfile
│   ├── employee-onboarding-webhook.jenkinsfile
│   ├── test-keycloak-integration.jenkinsfile
│   └── README.md
└── shared-library/          # Bibliothèque partagée
    └── vars/
        ├── keycloakAuth.groovy
        └── keycloakUser.groovy
```

## 🔧 Utilisation dans le Dockerfile

Les fichiers sont copiés dans l'image Docker lors du build:

```dockerfile
# Configuration JCasC
COPY ./config/jenkins.yaml /usr/share/jenkins/ref/casc_configs/jenkins.yaml

# Pipelines
COPY ./config/pipelines/ /usr/share/jenkins/ref/pipelines/

# Shared Library
COPY ./config/shared-library/ /var/jenkins_home/shared-library/
```

## 📝 Fichiers

### jenkins.yaml
Configuration Jenkins as Code (JCasC):
- Authentification OIDC avec Keycloak
- Matrice d'autorisation (groupes IT et Jenkins)
- Configuration de l'URL Jenkins

### plugins.txt
Liste des plugins Jenkins à installer automatiquement au build de l'image.

### entrypoint.sh
Script de démarrage qui peut:
- Récupérer dynamiquement le secret OIDC depuis Keycloak (optionnel)
- Initialiser des configurations au démarrage
- Exécuter Jenkins

### pipelines/
Contient les Jenkinsfiles prêts à l'emploi:
- **keycloak-user-management**: Gestion interactive des utilisateurs
- **employee-onboarding-webhook**: Onboarding automatisé via webhook
- **test-keycloak-integration**: Tests d'intégration

### shared-library/
Bibliothèque partagée Groovy avec fonctions réutilisables:
- **keycloakAuth**: Fonctions d'authentification Keycloak
- **keycloakUser**: Fonctions de gestion des utilisateurs

## 🚀 Déploiement

1. **Build l'image:**
   ```bash
   docker compose -f 16-docker-compose.Infra.dev.cicd.yml build
   ```

2. **Démarrer Jenkins:**
   ```bash
   docker compose -f 16-docker-compose.Infra.dev.cicd.yml up -d
   ```

3. **Vérifier que les fichiers sont présents:**
   ```bash
   # Pipelines
   docker exec jenkins ls -la /usr/share/jenkins/ref/pipelines/
   
   # Shared Library
   docker exec jenkins ls -la /var/jenkins_home/shared-library/vars/
   ```

## 📚 Configuration de la Shared Library dans Jenkins

Une fois Jenkins démarré, configurer la bibliothèque partagée:

1. **Manage Jenkins** > **Configure System**
2. **Global Pipeline Libraries** > **Add**
3. Configuration:
   - **Name:** `keycloak-lib`
   - **Default version:** `main`
   - **Load implicitly:** ☑️
   - **Retrieval method:** Modern SCM
   - **Source Code Management:** Git
   - **Project Repository:** `file:///var/jenkins_home/shared-library`

## 🔄 Mise à jour

Pour mettre à jour la configuration:

1. Modifier les fichiers dans `config/`
2. Rebuild l'image Docker
3. Redémarrer le container

```bash
docker compose -f 16-docker-compose.Infra.dev.cicd.yml down
docker compose -f 16-docker-compose.Infra.dev.cicd.yml up -d --build
```

## ⚠️ Notes

- Les fichiers dans `/usr/share/jenkins/ref/` sont copiés dans `/var/jenkins_home/` au premier démarrage uniquement
- Pour forcer la mise à jour, supprimer le volume `jenkins_data` ou les fichiers spécifiques dans le container
- La shared library est accessible directement depuis `/var/jenkins_home/shared-library/`
