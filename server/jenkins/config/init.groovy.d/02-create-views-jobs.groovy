import jenkins.model.Jenkins
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
println "🚀[init] Creating custom views..."
println "=" * 80
// Create "Integration Tests" view if it doesn't exist
if (jenkins.getView("Integration Tests") == null) {
    def integrationView = new ListView("Integration Tests")
    integrationView.description = "Tests d'intégration"
    
    // Use regex pattern to include jobs
    integrationView.includeRegex = "Test-.*"
    
    // Configure columns
    integrationView.columns.clear()
    integrationView.columns.add(new StatusColumn())
    integrationView.columns.add(new WeatherColumn())
    integrationView.columns.add(new JobColumn())
    integrationView.columns.add(new LastSuccessColumn())
    integrationView.columns.add(new LastFailureColumn())
    integrationView.columns.add(new LastDurationColumn())
    integrationView.columns.add(new BuildButtonColumn())
    
    jenkins.addView(integrationView)
    println "[init] ✅ Created 'Integration Tests' view"
} else {
    println "[init] ℹ️  'Integration Tests' view already exists"
}

// Create "Keycloak Management" view if it doesn't exist
if (jenkins.getView("Keycloak Management") == null) {
    def keycloakView = new ListView("Keycloak Management")
    keycloakView.description = "Pipelines de gestion Keycloak"
    
    // Use regex pattern to include jobs
    keycloakView.includeRegex = "Keycloak-.*"
    
    // Configure columns
    keycloakView.columns.clear()
    keycloakView.columns.add(new StatusColumn())
    keycloakView.columns.add(new WeatherColumn())
    keycloakView.columns.add(new JobColumn())
    keycloakView.columns.add(new LastSuccessColumn())
    keycloakView.columns.add(new LastFailureColumn())
    keycloakView.columns.add(new LastDurationColumn())
    keycloakView.columns.add(new BuildButtonColumn())
    
    jenkins.addView(keycloakView)
    println "[init] ✅ Created 'Keycloak Management' view"
} else {
    println "[init] ℹ️  'Keycloak Management' view already exists"
}
// Save configuration
jenkins.save()

println "[init] Views configuration complete"