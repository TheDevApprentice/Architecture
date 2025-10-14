# Jenkins Init Scripts - Automatic Job Creation

## 📋 Vue d'ensemble

Les scripts dans ce dossier (`init.groovy.d/`) sont exécutés **automatiquement** au démarrage de Jenkins, **avant** que l'interface web ne soit disponible.

Cette approche permet de créer tous les pipelines Jenkins au démarrage sans intervention manuelle.

## 🎯 Architecture

```
/usr/share/jenkins/ref/
├── init.groovy.d/
│   └── 01-create-pipeline-jobs.groovy    ← Crée automatiquement les jobs
├── pipelines/
│   ├── keycloak-user-management.jenkinsfile
│   ├── employee-onboarding-webhook.jenkinsfile
│   └── test-keycloak-integration.jenkinsfile
└── casc_configs/
    └── jenkins.yaml                       ← Config JCasC (auth, permissions)
```

## 🔧 Comment ça fonctionne

### 1. Au démarrage de Jenkins

```
1. Jenkins démarre
2. Plugins sont chargés
3. JCasC configure l'authentification (jenkins.yaml)
4. Scripts dans init.groovy.d/ sont exécutés (ordre alphabétique)
5. 01-create-pipeline-jobs.groovy utilise Job DSL API
6. Les 3 pipelines sont créés automatiquement
7. Jenkins UI devient disponible avec les jobs déjà créés
```

### 2. API Job DSL (depuis v1.47+)

Le script utilise l'API Job DSL directement sans avoir besoin d'un "seed job":

```groovy
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.plugin.JenkinsJobManagement

def jobManagement = new JenkinsJobManagement(System.out, [:], workspace)
new DslScriptLoader(jobManagement).runScript(jobDslScript)
```

**Référence:** [PR #837](https://github.com/jenkinsci/job-dsl-plugin/pull/837)

## 📦 Pipelines créés automatiquement

### 1. **Keycloak-User-Management**
- Description: Gestion interactive des utilisateurs Keycloak
- Type: Pipeline paramétré
- Paramètres: ACTION, REALM, USERNAME, EMAIL, etc.
- Jenkinsfile: `/usr/share/jenkins/ref/pipelines/keycloak-user-management.jenkinsfile`

### 2. **Employee-Onboarding-Webhook**
- Description: Onboarding automatique via webhook
- Type: Pipeline
- Trigger: Generic Webhook (à configurer manuellement)
- Jenkinsfile: `/usr/share/jenkins/ref/pipelines/employee-onboarding-webhook.jenkinsfile`

### 3. **Test-Keycloak-Integration**
- Description: Tests d'intégration Keycloak API
- Type: Pipeline paramétré
- Paramètres: REALM
- Jenkinsfile: `/usr/share/jenkins/ref/pipelines/test-keycloak-integration.jenkinsfile`

## 🔄 Workflow de développement

### Modifier un pipeline existant

1. Éditer le Jenkinsfile dans `config/pipelines/`
2. Les changements sont pris en compte immédiatement
3. Pas besoin de rebuild l'image Docker

```bash
# Les Jenkinsfiles sont lus à chaque exécution du job
# Changements instantanés!
```

### Ajouter un nouveau pipeline

1. **Créer le Jenkinsfile:**
```bash
touch config/pipelines/mon-nouveau-pipeline.jenkinsfile
```

2. **Ajouter dans `01-create-pipeline-jobs.groovy`:**
```groovy
pipelineJob('Mon-Nouveau-Pipeline') {
    description('Description du pipeline')
    
    parameters {
        string {
            name('PARAM1')
            defaultValue('valeur')
            description('Description')
            trim(true)
        }
    }
    
    definition {
        cps {
            script(readFileFromWorkspace('/usr/share/jenkins/ref/pipelines/mon-nouveau-pipeline.jenkinsfile'))
            sandbox(true)
        }
    }
}
```

3. **Rebuild et redémarrer:**
```bash
docker compose -f 16-docker-compose.Infra.dev.cicd.yml up -d --build
```

## 🔍 Debugging

### Vérifier les logs de création

```bash
docker logs jenkins 2>&1 | grep "Creating Keycloak Automation Pipeline Jobs"
```

### Sortie attendue

```
================================================================================
🚀 Creating Keycloak Automation Pipeline Jobs...
================================================================================

📝 Processing Job DSL script...

✅ Successfully created 3 job(s):
   - Keycloak-User-Management
   - Employee-Onboarding-Webhook
   - Test-Keycloak-Integration

================================================================================
🎉 Pipeline jobs creation completed successfully!
================================================================================
```

### En cas d'erreur

```bash
# Logs complets
docker logs jenkins

# Erreurs spécifiques
docker logs jenkins 2>&1 | grep "ERROR\|Failed"

# Vérifier que les fichiers sont présents
docker exec jenkins ls -la /usr/share/jenkins/ref/init.groovy.d/
docker exec jenkins ls -la /usr/share/jenkins/ref/pipelines/
```

## ⚠️ Notes importantes

### Exécution unique
- Les scripts init.groovy.d s'exécutent à **chaque** démarrage
- Si le job existe déjà, Job DSL le met à jour (ne le duplique pas)
- Pour recréer complètement: supprimer le volume et redémarrer

### Ordre d'exécution
- Les scripts sont exécutés par ordre alphabétique
- Préfixe numérique (01-, 02-, etc.) pour contrôler l'ordre

### Limitations
- Les webhooks Generic Trigger ne peuvent pas être configurés via Job DSL
- Solution: Configurer manuellement ou via Jenkins Configuration as Code

## 🚀 Déploiement

```bash
# Build et démarrage
docker compose -f 16-docker-compose.Infra.dev.cicd.yml up -d --build

# Attendre que Jenkins soit prêt (~30-60 secondes)
# Les jobs seront créés automatiquement

# Vérifier
docker logs jenkins | grep "Pipeline jobs creation"
```

## 📚 Références

- [Job DSL Plugin](https://plugins.jenkins.io/job-dsl/)
- [Job DSL API Reference](https://jenkinsci.github.io/job-dsl-plugin/)
- [PR #837 - Direct API access](https://github.com/jenkinsci/job-dsl-plugin/pull/837)
- [DigitalOcean Tutorial](https://www.digitalocean.com/community/tutorials/how-to-automate-jenkins-job-configuration-using-job-dsl)

## 🎓 Avantages de cette approche

✅ **Automatisation complète** - Aucune configuration manuelle
✅ **Infrastructure as Code** - Tout est versionné
✅ **Reproductible** - Même setup à chaque démarrage
✅ **Maintenable** - Code Groovy lisible et documenté
✅ **Flexible** - Facile d'ajouter de nouveaux pipelines
