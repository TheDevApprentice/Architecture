# 🎨 Thèmes Keycloak - Personnalisation

## 📋 Table des Matières

- [Vue d'ensemble](#vue-densemble)
- [Thèmes Disponibles](#thèmes-disponibles)
- [Structure](#structure)
- [Personnalisation](#personnalisation)

---

## Vue d'ensemble

Les thèmes Keycloak personnalisent l'interface utilisateur pour chaque service intégré, offrant:

- **Branding cohérent** - Logo et couleurs par service
- **Expérience utilisateur** - Interface adaptée
- **Multilingue** - Support EN/FR
- **Responsive** - Mobile-friendly

---

## Thèmes Disponibles

### 1️⃣ Thème `internal` (Défaut)

**Usage:** Realm internal par défaut

**Caractéristiques:**
- Design moderne et épuré
- Couleurs neutres
- Logo générique
- Multi-langue (EN/FR)

### 2️⃣ Thème `jenkins`

**Usage:** Client Jenkins

**Configuration:**
```json
{
  "clientId": "jenkins",
  "attributes": {
    "login_theme": "jenkins"
  }
}
```

**Caractéristiques:**
- Logo Jenkins
- Couleurs CI/CD
- Messages adaptés

### 3️⃣ Thème `minio`

**Usage:** Client MinIO

**Configuration:**
```json
{
  "clientId": "minio",
  "attributes": {
    "login_theme": "minio"
  }
}
```

**Caractéristiques:**
- Logo MinIO
- Couleurs storage
- Interface S3

### 4️⃣ Thème `master`

**Usage:** Master realm (admin)

**Caractéristiques:**
- Interface admin
- Sécurité renforcée
- Couleurs sobres

---

## Structure

### Arborescence

```
themes/
├── internal/
│   └── login/
│       ├── theme.properties
│       ├── messages/
│       │   ├── messages_en.properties
│       │   └── messages_fr.properties
│       ├── resources/
│       │   ├── css/
│       │   ├── img/
│       │   └── js/
│       └── *.ftl                    # Templates FreeMarker
│
├── jenkins/
│   └── login/
│       └── (même structure)
│
├── minio/
│   └── login/
│       └── (même structure)
│
└── master/
    └── login/
        └── (même structure)
```

### Fichiers Principaux

#### theme.properties
```properties
parent=keycloak
import=common/keycloak
styles=css/custom.css
scripts=js/custom.js
```

#### messages_fr.properties
```properties
loginTitle=Connexion
usernameOrEmail=Nom d'utilisateur ou email
password=Mot de passe
doLogIn=Se connecter
rememberMe=Se souvenir de moi
```

---

## Personnalisation

### Ajouter un Nouveau Thème

**1. Créer la structure:**
```bash
mkdir -p themes/monservice/login
cd themes/monservice/login
```

**2. Copier depuis un thème existant:**
```bash
cp -r themes/internal/login/* themes/monservice/login/
```

**3. Personnaliser:**
- Modifier `theme.properties`
- Ajouter logos dans `resources/img/`
- Customiser `resources/css/custom.css`
- Adapter messages `messages/messages_*.properties`

**4. Configurer client:**
```json
{
  "clientId": "monservice",
  "attributes": {
    "login_theme": "monservice"
  }
}
```

### Personnaliser CSS

**resources/css/custom.css:**
```css
/* Couleurs principales */
:root {
  --primary-color: #007bff;
  --secondary-color: #6c757d;
  --background-color: #f8f9fa;
}

/* Logo */
.kc-logo {
  background-image: url('../img/logo.png');
  height: 60px;
  width: 200px;
}

/* Boutons */
#kc-login {
  background-color: var(--primary-color);
  border-color: var(--primary-color);
}

/* Card */
#kc-form-wrapper {
  background: white;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  border-radius: 8px;
  padding: 2rem;
}
```

### Ajouter Logo

**1. Ajouter image:**
```
themes/monservice/login/resources/img/logo.png
```

**2. Référencer dans CSS:**
```css
.kc-logo {
  background-image: url('../img/logo.png');
}
```

---

## Cache

### Désactiver Cache (Dev)

**Configuration Dockerfile:**
```dockerfile
CMD ["start", "--spi-theme-cache-themes=false", "--spi-theme-cache-templates=false"]
```

### Clear Cache (Production)

```bash
docker-compose restart keycloak
```

---

**⬅️ Retour au [README](./README.md)**
