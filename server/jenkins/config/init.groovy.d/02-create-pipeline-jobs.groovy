/**
 * Automatic Pipeline Jobs Creation Script
 * 
 * This script runs at Jenkins startup and creates all Keycloak automation pipelines
 * inside the Keycloak folder structure
 * 
 * Prerequisites: 03-create-keycloak-folder-structure.groovy must run first
 */

import jenkins.model.Jenkins
import com.cloudbees.hudson.plugins.folder.Folder
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition

def jenkins = Jenkins.instance

println "=" * 80
println "[init] 🚀 Starting Keycloak pipelines creation..."
println "=" * 80

// Get Keycloak folder (must exist from 03-create-keycloak-folder-structure.groovy)
def keycloakFolder = jenkins.getItem('Keycloak')
if (keycloakFolder == null) {
    println "[init] ❌ ERROR: Keycloak folder not found! Run 03-create-keycloak-folder-structure.groovy first"
    return
}

println "[init] ✅ Found Keycloak folder"

// Function to read Jenkinsfile content
def readJenkinsfile(String path) {
    def file = new File(path)
    if (file.exists()) {
        return file.text
    } else {
        throw new FileNotFoundException("Jenkinsfile not found: ${path}")
    }
}

// ============================================================================
// 1. Keycloak User Management Pipeline
// ============================================================================

def userManagementJob = keycloakFolder.getItem('keycloak-user-management')
if (userManagementJob == null) {
    println "[init] 📝 Creating 'keycloak-user-management' pipeline in Keycloak folder..."
    
    userManagementJob = keycloakFolder.createProject(WorkflowJob.class, 'keycloak-user-management')
    userManagementJob.description = '''👤 Keycloak User Management Pipeline

CRUD operations for Keycloak users:
- CREATE_USER
- UPDATE_USER
- DELETE_USER
- RESET_PASSWORD
- ADD_TO_GROUP
- LIST_USERS'''
    
    // Load Jenkinsfile content
    def jenkinsfileContent = readJenkinsfile('/usr/share/jenkins/ref/pipelines/keycloak-user-management.jenkinsfile')
    def flowDefinition = new CpsFlowDefinition(jenkinsfileContent, true)
    userManagementJob.setDefinition(flowDefinition)
    
    userManagementJob.save()
    println "[init] ✅ 'keycloak-user-management' pipeline created successfully"
} else {
    println "[init] ℹ️  'keycloak-user-management' pipeline already exists"
}

// ============================================================================
// 2. Test Keycloak Integration Pipeline  
// ============================================================================

def testIntegrationJob = keycloakFolder.getItem('Test-Keycloak-Integration')
if (testIntegrationJob == null) {
    println "[init] 🧪 Creating 'Test-Keycloak-Integration' pipeline in Keycloak folder..."
    
    testIntegrationJob = keycloakFolder.createProject(WorkflowJob.class, 'Test-Keycloak-Integration')
    testIntegrationJob.description = '''🧪 Keycloak Integration Test Pipeline

Test suite for Keycloak API integration:
- Connection test
- Authentication test
- User operations test
- Group operations test'''
    
    // Load Jenkinsfile content
    def jenkinsfileContent = readJenkinsfile('/usr/share/jenkins/ref/pipelines/test-keycloak-integration.jenkinsfile')
    def flowDefinition = new CpsFlowDefinition(jenkinsfileContent, true)
    testIntegrationJob.setDefinition(flowDefinition)
    
    testIntegrationJob.save()
    println "[init] ✅ 'Test-Keycloak-Integration' pipeline created successfully"
} else {
    println "[init] ℹ️  'Test-Keycloak-Integration' pipeline already exists"
}

jenkins.save()

println "[init] 🎉 Keycloak pipelines creation completed!"
println "[init] 📁 Location: Keycloak/"
println "[init] 📝 Pipelines created: keycloak-user-management, Test-Keycloak-Integration"
