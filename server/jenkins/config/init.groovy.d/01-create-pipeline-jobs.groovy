/**
 * Automatic Pipeline Jobs Creation Script
 * 
 * This script runs at Jenkins startup and creates all Keycloak automation pipelines
 * using Job DSL API directly (available since Job DSL 1.47+)
 * 
 * Reference: https://github.com/jenkinsci/job-dsl-plugin/pull/837
 */

import jenkins.model.Jenkins
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.plugin.JenkinsJobManagement

println "=" * 80
println "🚀 Creating Keycloak Automation Pipeline Jobs..."
println "=" * 80

// Job DSL script content
def jobDslScript = '''
// Ensure Keycloak folder exists
folder('Keycloak') {
    description('🔐 Keycloak Management & Automation Pipelines')
}

// Pipelines

// 1. Keycloak User Management Pipeline
pipelineJob('Keycloak/Keycloak-User-Management') {
    description('Interactive pipeline for Keycloak user management (create, update, delete users)')
    
    parameters {
        choice {
            name('ACTION')
            choices([
                'CREATE_USER', 
                'UPDATE_USER', 
                'DELETE_USER', 
                'RESET_PASSWORD', 
                'ADD_TO_GROUP', 
                'LIST_USERS'
            ])
            description('Action to perform')
        }
        string {
            name('REALM')
            defaultValue('internal')
            description('Keycloak realm')
            trim(true)
        }
        string {
            name('USERNAME')
            defaultValue('')
            description('Username (required for all actions except LIST_USERS)')
            trim(true)
        }
        string {
            name('EMAIL')
            defaultValue('')
            description('Email address (required for CREATE_USER)')
            trim(true)
        }
        string {
            name('FIRST_NAME')
            defaultValue('')
            description('First name (optional)')
            trim(true)
        }
        string {
            name('LAST_NAME')
            defaultValue('')
            description('Last name (optional)')
            trim(true)
        }
        string {
            name('GROUP_NAME')
            defaultValue('')
            description('Group name (for CREATE_USER or ADD_TO_GROUP)')
            trim(true)
        }
        booleanParam {
            name('ENABLED')
            defaultValue(true)
            description('Enable user account')
        }
        booleanParam {
            name('EMAIL_VERIFIED')
            defaultValue(false)
            description('Mark email as verified')
        }
        booleanParam {
            name('TEMPORARY_PASSWORD')
            defaultValue(true)
            description('Force password change on first login')
        }
        string {
            name('PASSWORD')
            defaultValue('')
            description('Password (leave empty for auto-generation)')
            trim(true)
        }
    }
    
    definition {
        cps {
            script(readFileFromWorkspace('/usr/share/jenkins/ref/pipelines/keycloak-user-management.jenkinsfile'))
            sandbox(true)
        }
    }
}

// 2. Keycloak Group Management Pipeline
pipelineJob('Keycloak/Keycloak-Group-Management') {
    description('CRUD operations for Keycloak groups, members, and role assignments')
    
    parameters {
        choice {
            name('ACTION')
            choices([
                'CREATE_GROUP',
                'UPDATE_GROUP',
                'DELETE_GROUP',
                'LIST_GROUPS',
                'GET_GROUP',
                'ADD_MEMBERS',
                'REMOVE_MEMBERS',
                'LIST_MEMBERS',
                'DETECT_ORPHANS'
            ])
            description('Action to perform')
        }
        string {
            name('REALM')
            defaultValue('internal')
            description('Keycloak realm (e.g., internal, master)')
            trim(true)
        }
        string {
            name('GROUP_NAME')
            defaultValue('')
            description('Group name (required for most actions)')
            trim(true)
        }
        string {
            name('NEW_GROUP_NAME')
            defaultValue('')
            description('New group name (for UPDATE_GROUP action)')
            trim(true)
        }
        string {
            name('PARENT_GROUP')
            defaultValue('')
            description('Parent group name (for hierarchical groups)')
            trim(true)
        }
        text {
            name('USERNAMES')
            defaultValue('')
            description('List of usernames (one per line) for ADD_MEMBERS/REMOVE_MEMBERS')
        }
        string {
            name('ATTRIBUTES')
            defaultValue('{}')
            description('Custom attributes as JSON (e.g., {"department": "IT", "location": "Paris"})')
            trim(true)
        }
        booleanParam {
            name('DRY_RUN')
            defaultValue(false)
            description('Preview changes without executing (for destructive actions)')
        }
    }
    
    definition {
        cps {
            script(readFileFromWorkspace('/usr/share/jenkins/ref/pipelines/keycloak-group-management.jenkinsfile'))
            sandbox(true)
        }
    }
}

// 3. Keycloak Client Management Pipeline
pipelineJob('Keycloak/Keycloak-Client-Management') {
    description('CRUD operations for Keycloak clients (OIDC/SAML applications)')
    
    parameters {
        choice {
            name('ACTION')
            choices([
                'CREATE_CLIENT',
                'CREATE_FROM_TEMPLATE',
                'UPDATE_CLIENT',
                'DELETE_CLIENT',
                'LIST_CLIENTS',
                'GET_CLIENT',
                'GET_CLIENT_SECRET',
                'REGENERATE_SECRET',
                'ENABLE_CLIENT',
                'DISABLE_CLIENT'
            ])
            description('Action to perform')
        }
        string {
            name('REALM')
            defaultValue('internal')
            description('Keycloak realm')
            trim(true)
        }
        string {
            name('CLIENT_ID')
            defaultValue('')
            description('Client ID (e.g., my-web-app)')
            trim(true)
        }
        choice {
            name('TEMPLATE')
            choices([
                'custom',
                'web-app',
                'spa',
                'backend-service',
                'mobile-app'
            ])
            description('Client template (for CREATE_FROM_TEMPLATE)')
        }
        choice {
            name('PROTOCOL')
            choices(['openid-connect', 'saml'])
            description('Protocol (for CREATE_CLIENT)')
        }
        booleanParam {
            name('PUBLIC_CLIENT')
            defaultValue(false)
            description('Public client (no secret required, e.g., SPA)')
        }
        text {
            name('REDIRECT_URIS')
            defaultValue('')
            description('Redirect URIs (one per line) Example: http://localhost:3000/* https://myapp.com/*')
        }
        text {
            name('WEB_ORIGINS')
            defaultValue('')
            description('Web origins for CORS (one per line) Example: http://localhost:3000 https://myapp.com')
        }
        string {
            name('DESCRIPTION')
            defaultValue('')
            description('Client description')
            trim(true)
        }
        booleanParam {
            name('SERVICE_ACCOUNTS_ENABLED')
            defaultValue(false)
            description('Enable service accounts (for backend services)')
        }
        booleanParam {
            name('DRY_RUN')
            defaultValue(false)
            description('Preview changes without executing')
        }
    }
    
    definition {
        cps {
            script(readFileFromWorkspace('/usr/share/jenkins/ref/pipelines/keycloak-client-management.jenkinsfile'))
            sandbox(true)
        }
    }
}

// 4. Keycloak Security Audit Pipeline
pipelineJob('Keycloak/Keycloak-Security-Audit') {
    description('Automated security audit with detailed reporting')
    
    parameters {
        string {
            name('REALM')
            defaultValue('internal')
            description('Keycloak realm to audit')
            trim(true)
        }
        string {
            name('INACTIVE_DAYS_THRESHOLD')
            defaultValue('90')
            description('Days of inactivity to flag users')
            trim(true)
        }
        string {
            name('UNVERIFIED_EMAIL_THRESHOLD')
            defaultValue('20')
            description('Alert threshold for unverified emails')
            trim(true)
        }
        string {
            name('INACTIVE_USERS_THRESHOLD')
            defaultValue('50')
            description('Alert threshold for inactive users')
            trim(true)
        }
        choice {
            name('REPORT_FORMAT')
            choices(['HTML', 'JSON', 'CSV', 'ALL'])
            description('Output report format')
        }
        booleanParam {
            name('SEND_EMAIL_ALERTS')
            defaultValue(false)
            description('Send email alerts if critical issues found')
        }
    }
    
    definition {
        cps {
            script(readFileFromWorkspace('/usr/share/jenkins/ref/pipelines/keycloak-security-audit.jenkinsfile'))
            sandbox(true)
        }
    }
}

// 5. Keycloak Session Management Pipeline
pipelineJob('Keycloak/Keycloak-Session-Management') {
    description('Management of active user sessions with emergency revocation capabilities')
    
    parameters {
        choice {
            name('ACTION')
            choices([
                'LIST_ACTIVE_SESSIONS',
                'LIST_USER_SESSIONS',
                'REVOKE_USER_SESSIONS',
                'REVOKE_ALL_SESSIONS',
                'SESSION_STATISTICS',
                'DETECT_ANOMALIES'
            ])
            description('Action to perform')
        }
        string {
            name('REALM')
            defaultValue('internal')
            description('Keycloak realm')
            trim(true)
        }
        string {
            name('USERNAME')
            defaultValue('')
            description('Username (required for user-specific actions)')
            trim(true)
        }
        string {
            name('ANOMALY_SESSION_AGE_DAYS')
            defaultValue('7')
            description('Flag sessions older than X days as anomalies')
            trim(true)
        }
        booleanParam {
            name('EMERGENCY_MODE')
            defaultValue(false)
            description('⚠️  Skip approval gates (USE WITH CAUTION)')
        }
    }
    
    definition {
        cps {
            script(readFileFromWorkspace('/usr/share/jenkins/ref/pipelines/keycloak-session-management.jenkinsfile'))
            sandbox(true)
        }
    }
}

// 6. Keycloak Compliance Report Pipeline
pipelineJob('Keycloak/Keycloak-Compliance-Report') {
    description('Generate compliance reports (GDPR, access review, privileged accounts, etc.)')
    
    parameters {
        choice {
            name('REPORT_TYPE')
            choices([
                'FULL_COMPLIANCE',
                'ACCESS_REVIEW',
                'PRIVILEGED_ACCOUNTS',
                'PASSWORD_POLICY',
                'CLIENT_SECRETS_AUDIT',
                'MFA_ADOPTION'
            ])
            description('Type of compliance report')
        }
        string {
            name('REALM')
            defaultValue('internal')
            description('Keycloak realm')
            trim(true)
        }
        choice {
            name('OUTPUT_FORMAT')
            choices(['HTML', 'PDF', 'CSV', 'JSON', 'ALL'])
            description('Report output format')
        }
        booleanParam {
            name('SEND_EMAIL')
            defaultValue(false)
            description('Send report via email')
        }
        string {
            name('EMAIL_RECIPIENTS')
            defaultValue('')
            description('Email recipients (comma-separated)')
            trim(true)
        }
    }
    
    definition {
        cps {
            script(readFileFromWorkspace('/usr/share/jenkins/ref/pipelines/keycloak-compliance-report.jenkinsfile'))
            sandbox(true)
        }
    }
}

// Tests

// 2. Test Keycloak Integration Pipeline
pipelineJob('Keycloak/Test-Keycloak-User-Management') {
    description('Test suite for Keycloak API integration')
    
    parameters {
        choice {
            name('REALM')
            choices([
                'internal'
            ])
            description('Keycloak realm to test')
        }
    }
    
    definition {
        cps {
            script(readFileFromWorkspace('/usr/share/jenkins/ref/pipelines/test-keycloak-user-management.jenkinsfile'))
            sandbox(true)
        }
    }
}

// 2a. Test Keycloak Group Management
pipelineJob('Keycloak/Test-Keycloak-Group-Management') {
    description('Integration tests for group management operations')
    
    parameters {
        choice {
            name('REALM')
            choices(['internal'])
            description('Keycloak realm to test')
        }
    }
    
    definition {
        cps {
            script(readFileFromWorkspace('/usr/share/jenkins/ref/pipelines/test-keycloak-group-management.jenkinsfile'))
            sandbox(true)
        }
    }
}

// 2b. Test Keycloak Client Management
pipelineJob('Keycloak/Test-Keycloak-Client-Management') {
    description('Integration tests for client management operations')
    
    parameters {
        choice {
            name('REALM')
            choices(['internal'])
            description('Keycloak realm to test')
        }
    }
    
    definition {
        cps {
            script(readFileFromWorkspace('/usr/share/jenkins/ref/pipelines/test-keycloak-client-management.jenkinsfile'))
            sandbox(true)
        }
    }
}

// 2c. Test Keycloak Session Management
pipelineJob('Keycloak/Test-Keycloak-Session-Management') {
    description('Integration tests for session management')
    
    parameters {
        choice {
            name('REALM')
            choices(['internal'])
            description('Keycloak realm to test')
        }
    }
    
    definition {
        cps {
            script(readFileFromWorkspace('/usr/share/jenkins/ref/pipelines/test-keycloak-session-management.jenkinsfile'))
            sandbox(true)
        }
    }
}

'''

try {
    // Create job management context
    def workspace = new File('/usr/share/jenkins/ref')
    def jobManagement = new JenkinsJobManagement(System.out, [:], workspace)
    
    // Execute Job DSL script
    println "\n📝 Processing Job DSL script..."
    def result = new DslScriptLoader(jobManagement).runScript(jobDslScript)
    
    // Display results
    println "\n✅ Successfully created ${result.jobs.size()} job(s):"
    result.jobs.each { job ->
        println "   - ${job.jobName}"
    }
    
    println "\n" + "=" * 80
    println "🎉 Pipeline jobs creation completed successfully!"
    println "=" * 80
    
} catch (Exception e) {
    println "\n❌ Failed to create pipeline jobs:"
    println "Error: ${e.message}"
    e.printStackTrace()
    
    println "\n" + "=" * 80
    println "⚠️  Pipeline jobs creation failed! Check logs above for details."
    println "=" * 80
}
