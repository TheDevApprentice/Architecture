/**
 * Keycloak Folder Structure Creation Script
 * 
 * Creates the Keycloak folder with Management and Tests views
 * This script runs at Jenkins startup (before pipeline creation)
 */

import jenkins.model.Jenkins
import com.cloudbees.hudson.plugins.folder.Folder
import hudson.model.ListView
import hudson.views.StatusColumn
import hudson.views.WeatherColumn
import hudson.views.JobColumn
import hudson.views.LastSuccessColumn
import hudson.views.LastFailureColumn
import hudson.views.LastDurationColumn
import hudson.views.BuildButtonColumn

def jenkins = Jenkins.instance

println "=" * 80
println "[init] 🚀 Starting Keycloak folder structure creation..."
println "=" * 80

// ============================================================================
// 1. Create Keycloak Folder
// ============================================================================

def keycloakFolder = jenkins.getItem('Keycloak')
if (keycloakFolder == null) {
    println "[init] 📁 Creating 'Keycloak' folder..."
    println "=" * 80
    keycloakFolder = jenkins.createProject(Folder.class, 'Keycloak')
    keycloakFolder.description = '''🔐 Keycloak Management & Automation Pipelines
    
Base modulaire pour automatiser la gestion de Keycloak:
- User Management
- Group Management
- Client Management
- Security & Audit
- Maintenance
- Integration Tests'''
    
    keycloakFolder.save()
    println "[init] ✅ 'Keycloak' folder created successfully"
    println "=" * 80
} else {
    println "[init] ℹ️  'Keycloak' folder already exists"
    println "=" * 80
}

// ============================================================================
// 2. Create "Management" View in Keycloak folder
// ============================================================================

def managementView = keycloakFolder.getView("Management")
if (managementView == null) {
    println "[init] 📊 Creating 'Management' view in Keycloak folder..."
    println "=" * 80
    managementView = new ListView("Management", keycloakFolder)
    managementView.description = "📊 Keycloak Management Pipelines (User, Group, Client, Security & Audit, Maintenance)"
    
    // Include regex pattern for management pipelines
    managementView.includeRegex = ".*([Mm]anagement|[Aa]udit|[Cc]leanup|[Ss]ession|[Cc]ompliance|[Rr]bac|[Ss]ervice-account).*"
    
    // Configure columns
    managementView.columns.clear()
    managementView.columns.add(new StatusColumn())
    managementView.columns.add(new WeatherColumn())
    managementView.columns.add(new JobColumn())
    managementView.columns.add(new LastSuccessColumn())
    managementView.columns.add(new LastFailureColumn())
    managementView.columns.add(new LastDurationColumn())
    managementView.columns.add(new BuildButtonColumn())
    
    keycloakFolder.addView(managementView)
    keycloakFolder.save()
    
    println "[init] ✅ 'Management' view created successfully"
    println "=" * 80
} else {
    println "[init] ℹ️  'Management' view already exists"
    println "=" * 80
}

// ============================================================================
// 3. Create "Tests" View in Keycloak folder
// ============================================================================

def testsView = keycloakFolder.getView("Tests")
if (testsView == null) {
    println "[init] 🧪 Creating 'Tests' view in Keycloak folder..."
    println "=" * 80
    testsView = new ListView("Tests", keycloakFolder)
    testsView.description = "🧪 Keycloak Integration Tests"
    
    // Include regex pattern for test pipelines
    testsView.includeRegex = ".*[Tt]est.*"
    
    // Configure columns
    testsView.columns.clear()
    testsView.columns.add(new StatusColumn())
    testsView.columns.add(new WeatherColumn())
    testsView.columns.add(new JobColumn())
    testsView.columns.add(new LastSuccessColumn())
    testsView.columns.add(new LastFailureColumn())
    testsView.columns.add(new LastDurationColumn())
    testsView.columns.add(new BuildButtonColumn())
    
    keycloakFolder.addView(testsView)
    keycloakFolder.save()
    
    println "[init] ✅ 'Tests' view created successfully"
    println "=" * 80
} else {
    println "[init] ℹ️  'Tests' view already exists"
    println "=" * 80
}

// ============================================================================
// 4. Set default view to Management
// ============================================================================

if (keycloakFolder.primaryView?.name != "Management") {
    keycloakFolder.primaryView = managementView
    keycloakFolder.save()
    println "[init] ✅ Set 'Management' as default view"
    println "=" * 80
}

jenkins.save()

println "=" * 80
println "[init] 🎉 Keycloak folder structure setup completed!"
println "[init] 📁 Folder: Keycloak"
println "[init] 📊 Views: Management, Tests"
println "=" * 80