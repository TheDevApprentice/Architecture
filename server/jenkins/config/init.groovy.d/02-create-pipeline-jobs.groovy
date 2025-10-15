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

// ============================================================================
// 5. Keycloak Client Management Pipeline
// ============================================================================

def clientManagementJob = keycloakFolder.getItem('keycloak-client-management')
if (clientManagementJob == null) {
    println "[init] 🔐 Creating 'keycloak-client-management' pipeline in Keycloak folder..."
    
    clientManagementJob = keycloakFolder.createProject(WorkflowJob.class, 'keycloak-client-management')
    clientManagementJob.description = '''🔐 Keycloak Client Management Pipeline

CRUD operations for Keycloak clients (OIDC/SAML):
- CREATE_CLIENT
- CREATE_FROM_TEMPLATE (web-app, spa, backend-service, mobile-app)
- UPDATE_CLIENT
- DELETE_CLIENT
- LIST_CLIENTS
- GET_CLIENT
- GET_CLIENT_SECRET
- REGENERATE_SECRET
- ENABLE_CLIENT
- DISABLE_CLIENT'''
    
    // Load Jenkinsfile content
    def jenkinsfileContent = readJenkinsfile('/usr/share/jenkins/ref/pipelines/keycloak-client-management.jenkinsfile')
    def flowDefinition = new CpsFlowDefinition(jenkinsfileContent, true)
    clientManagementJob.setDefinition(flowDefinition)
    
    clientManagementJob.save()
    println "[init] ✅ 'keycloak-client-management' pipeline created successfully"
} else {
    println "[init] ℹ️  'keycloak-client-management' pipeline already exists"
}

// ============================================================================
// 6. Keycloak Service Account Management Pipeline
// ============================================================================

def serviceAccountJob = keycloakFolder.getItem('keycloak-service-account-management')
if (serviceAccountJob == null) {
    println "[init] 🤖 Creating 'keycloak-service-account-management' pipeline in Keycloak folder..."
    
    serviceAccountJob = keycloakFolder.createProject(WorkflowJob.class, 'keycloak-service-account-management')
    serviceAccountJob.description = '''🤖 Keycloak Service Account Management Pipeline

Management of service accounts (M2M) with secret rotation:
- CREATE_SERVICE_ACCOUNT
- LIST_SERVICE_ACCOUNTS
- GET_SERVICE_ACCOUNT
- DELETE_SERVICE_ACCOUNT
- ROTATE_SECRET (with health check)
- GET_SA_TOKEN
- ENABLE_SA
- DISABLE_SA'''
    
    // Load Jenkinsfile content
    def jenkinsfileContent = readJenkinsfile('/usr/share/jenkins/ref/pipelines/keycloak-service-account-management.jenkinsfile')
    def flowDefinition = new CpsFlowDefinition(jenkinsfileContent, true)
    serviceAccountJob.setDefinition(flowDefinition)
    
    serviceAccountJob.save()
    println "[init] ✅ 'keycloak-service-account-management' pipeline created successfully"
} else {
    println "[init] ℹ️  'keycloak-service-account-management' pipeline already exists"
}

// ============================================================================
// 7. Keycloak Security Audit Pipeline
// ============================================================================

def securityAuditJob = keycloakFolder.getItem('keycloak-security-audit')
if (securityAuditJob == null) {
    println "[init] 🔒 Creating 'keycloak-security-audit' pipeline in Keycloak folder..."
    
    securityAuditJob = keycloakFolder.createProject(WorkflowJob.class, 'keycloak-security-audit')
    securityAuditJob.description = '''🔒 Keycloak Security Audit Pipeline

Automated security audit with detailed reporting:
- Unverified emails detection
- Inactive users (>90 days)
- Disabled users
- Orphan groups
- Service accounts audit
- Multiple report formats (HTML, JSON, CSV)
- Scheduled daily execution'''
    
    // Load Jenkinsfile content
    def jenkinsfileContent = readJenkinsfile('/usr/share/jenkins/ref/pipelines/keycloak-security-audit.jenkinsfile')
    def flowDefinition = new CpsFlowDefinition(jenkinsfileContent, true)
    securityAuditJob.setDefinition(flowDefinition)
    
    securityAuditJob.save()
    println "[init] ✅ 'keycloak-security-audit' pipeline created successfully"
} else {
    println "[init] ℹ️  'keycloak-security-audit' pipeline already exists"
}

// ============================================================================
// 8. Keycloak Session Management Pipeline
// ============================================================================

def sessionManagementJob = keycloakFolder.getItem('keycloak-session-management')
if (sessionManagementJob == null) {
    println "[init] 🔐 Creating 'keycloak-session-management' pipeline in Keycloak folder..."
    
    sessionManagementJob = keycloakFolder.createProject(WorkflowJob.class, 'keycloak-session-management')
    sessionManagementJob.description = '''🔐 Keycloak Session Management Pipeline

Management of active user sessions:
- LIST_ACTIVE_SESSIONS
- LIST_USER_SESSIONS
- REVOKE_USER_SESSIONS
- REVOKE_ALL_SESSIONS (Emergency)
- SESSION_STATISTICS
- DETECT_ANOMALIES (long-running, multiple IPs)'''
    
    // Load Jenkinsfile content
    def jenkinsfileContent = readJenkinsfile('/usr/share/jenkins/ref/pipelines/keycloak-session-management.jenkinsfile')
    def flowDefinition = new CpsFlowDefinition(jenkinsfileContent, true)
    sessionManagementJob.setDefinition(flowDefinition)
    
    sessionManagementJob.save()
    println "[init] ✅ 'keycloak-session-management' pipeline created successfully"
} else {
    println "[init] ℹ️  'keycloak-session-management' pipeline already exists"
}

// ============================================================================
// 9. Keycloak Compliance Reporting Pipeline
// ============================================================================

def complianceReportJob = keycloakFolder.getItem('keycloak-compliance-report')
if (complianceReportJob == null) {
    println "[init] 📊 Creating 'keycloak-compliance-report' pipeline in Keycloak folder..."
    
    complianceReportJob = keycloakFolder.createProject(WorkflowJob.class, 'keycloak-compliance-report')
    complianceReportJob.description = '''📊 Keycloak Compliance Reporting Pipeline

Generate compliance reports:
- FULL_COMPLIANCE
- ACCESS_REVIEW
- PRIVILEGED_ACCOUNTS
- PASSWORD_POLICY
- CLIENT_SECRETS_AUDIT
- MFA_ADOPTION
- Multiple formats (HTML, PDF, CSV, JSON)'''
    
    // Load Jenkinsfile content
    def jenkinsfileContent = readJenkinsfile('/usr/share/jenkins/ref/pipelines/keycloak-compliance-report.jenkinsfile')
    def flowDefinition = new CpsFlowDefinition(jenkinsfileContent, true)
    complianceReportJob.setDefinition(flowDefinition)
    
    complianceReportJob.save()
    println "[init] ✅ 'keycloak-compliance-report' pipeline created successfully"
} else {
    println "[init] ℹ️  'keycloak-compliance-report' pipeline already exists"
}

jenkins.save()

println "=" * 80
println "[init] 🎉 Keycloak pipelines creation completed!"
println "[init] 📁 Location: Keycloak/"
println "[init] 📝 Pipelines created:"
println "[init]    - keycloak-user-management"
println "[init]    - keycloak-group-management"
println "[init]    - keycloak-rbac-automation"
println "[init]    - keycloak-client-management"
println "[init]    - keycloak-service-account-management"
println "[init]    - keycloak-security-audit"
println "[init]    - keycloak-session-management"
println "[init]    - keycloak-compliance-report"
println "[init]    - Test-Keycloak-Integration"
println "=" * 80
