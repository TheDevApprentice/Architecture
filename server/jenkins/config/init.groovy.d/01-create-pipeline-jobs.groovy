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
// 1. Keycloak User Management Pipeline
pipelineJob('Keycloak-User-Management') {
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

// 2. Employee Onboarding Webhook Pipeline
pipelineJob('Employee-Onboarding-Webhook') {
    description('Automated employee onboarding via webhook trigger')
    
    definition {
        cps {
            script(readFileFromWorkspace('/usr/share/jenkins/ref/pipelines/employee-onboarding-webhook.jenkinsfile'))
            sandbox(true)
        }
    }
    
    // Note: Generic webhook trigger configuration needs to be added manually
    // or configured via Jenkins Configuration as Code (JCasC)
}

// 3. Test Keycloak Integration Pipeline
pipelineJob('Test-Keycloak-Integration') {
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
            script(readFileFromWorkspace('/usr/share/jenkins/ref/pipelines/test-keycloak-integration.jenkinsfile'))
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
