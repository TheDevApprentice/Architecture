# Jenkins Report Templates

Ce dossier contient les templates HTML pour les rapports Jenkins.

## Structure

```
templates/
├── css/
│   └── report.css          # Styles basés sur le thème Keycloak
├── img/
│   └── jenkins-logo.png    # Logo Jenkins (à ajouter manuellement)
└── reportTemplate.html      # Template HTML principal
```

## Configuration

### 1. Ajouter le logo Jenkins

Placez votre logo Jenkins dans `img/jenkins-logo.png`. Le logo doit être:
- Format: PNG avec fond transparent
- Dimensions recommandées: 200x200px ou plus
- Le logo sera affiché dans le panneau de branding à gauche

### 2. Utilisation du template

Le template utilise des placeholders qui sont remplacés automatiquement:

- `{{REPORT_TITLE}}` - Titre du rapport
- `{{REPORT_SUBTITLE}}` - Sous-titre
- `{{REPORT_DATE}}` - Date de génération
- `{{REALM_NAME}}` - Nom du realm Keycloak
- `{{REPORT_TYPE}}` - Type de rapport
- `{{SUMMARY_CARDS}}` - Cartes de résumé
- `{{REPORT_SECTIONS}}` - Sections détaillées

### 3. Design

Le design est inspiré du thème Keycloak avec:
- Panneau de branding à gauche (35%) avec gradient animé bleu
- Contenu du rapport à droite (65%)
- Design responsive (mobile-first)
- Animations subtiles et moderne

### 4. Personnalisation

Pour personnaliser les couleurs, modifiez les variables CSS dans `css/report.css`:

```css
:root {
    --primary-color: #1e40af;
    --accent-color: #3b82f6;
    --gradient-start: #1e40af;
    --gradient-middle: #3b82f6;
    --gradient-end: #60a5fa;
}
```

## Exemples de rapports

Les rapports générés incluent:
- **Security Audit Report** - Audit de sécurité Keycloak
- **Compliance Report** - Rapport de conformité
- Autres rapports personnalisés

Tous les rapports utilisent le même template avec différents contenus.
