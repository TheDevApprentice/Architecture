/**
 * Automatic Pipeline Jobs Creation Script
 * 
 * This script runs at Jenkins startup and creates all Keycloak automation pipelines
 * inside the Keycloak folder structure
 * 
 * Prerequisites: 01-create-keycloak-folder-structure.groovy must run first
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

// ============================================================================
// 3. Keycloak Group Management Pipeline
// ============================================================================

def groupManagementJob = keycloakFolder.getItem('keycloak-group-management')
if (groupManagementJob == null) {
    println "[init] 👥 Creating 'keycloak-group-management' pipeline in Keycloak folder..."
    
    groupManagementJob = keycloakFolder.createProject(WorkflowJob.class, 'keycloak-group-management')
    groupManagementJob.description = '''👥 Keycloak Group Management Pipeline

CRUD operations for Keycloak groups:
- CREATE_GROUP
- UPDATE_GROUP
- DELETE_GROUP
- LIST_GROUPS
- GET_GROUP
- ADD_MEMBERS
- REMOVE_MEMBERS
- LIST_MEMBERS
- DETECT_ORPHANS'''
    
    // Load Jenkinsfile content
    def jenkinsfileContent = readJenkinsfile('/usr/share/jenkins/ref/pipelines/keycloak-group-management.jenkinsfile')
    def flowDefinition = new CpsFlowDefinition(jenkinsfileContent, true)
    groupManagementJob.setDefinition(flowDefinition)
    
    groupManagementJob.save()
    println "[init] ✅ 'keycloak-group-management' pipeline created successfully"
} else {
    println "[init] ℹ️  'keycloak-group-management' pipeline already exists"
}

// ============================================================================
// 4. Keycloak RBAC Automation Pipeline
// ============================================================================

def rbacAutomationJob = keycloakFolder.getItem('keycloak-rbac-automation')
if (rbacAutomationJob == null) {
    println "[init] 🔐 Creating 'keycloak-rbac-automation' pipeline in Keycloak folder..."
    
    rbacAutomationJob = keycloakFolder.createProject(WorkflowJob.class, 'keycloak-rbac-automation')
    rbacAutomationJob.description = '''🔐 Keycloak RBAC Automation Pipeline

Automatically assign users to groups based on attributes:
- APPLY_RBAC - Apply rules to single user
- SYNC_USER_GROUPS - Sync user groups from attributes
- SYNC_ALL_USERS - Sync all users in realm
- VALIDATE_RULES - Validate RBAC mapping rules
- DRY_RUN - Preview changes without applying'''
    
    // Load Jenkinsfile content
    def jenkinsfileContent = readJenkinsfile('/usr/share/jenkins/ref/pipelines/keycloak-rbac-automation.jenkinsfile')
    def flowDefinition = new CpsFlowDefinition(jenkinsfileContent, true)
    rbacAutomationJob.setDefinition(flowDefinition)
    
    rbacAutomationJob.save()
    println "[init] ✅ 'keycloak-rbac-automation' pipeline created successfully"
} else {
    println "[init] ℹ️  'keycloak-rbac-automation' pipeline already exists"
}

jenkins.save()

println "=" * 80
println "[init] 🎉 Keycloak pipelines creation completed!"
println "[init] 📁 Location: Keycloak/"
println "[init] 📝 Pipelines created:"
println "[init]    - keycloak-user-management"
println "[init]    - keycloak-group-management"
println "[init]    - keycloak-rbac-automation"
println "[init]    - Test-Keycloak-Integration"
println "=" * 80
