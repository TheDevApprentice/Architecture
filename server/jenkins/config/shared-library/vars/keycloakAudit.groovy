/**
 * Keycloak Audit & Reporting Library
 * Provides functions for security audits, session management, and compliance reporting
 */

/**
 * Get all active sessions in realm
 * @param config Map with accessToken
 * @return List of active sessions
 */
def getActiveSessions(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    
    echo "📊 Retrieving active sessions in realm '${realm}'..."
    
    // Get all clients first
    def clientsUrl = "http://${keycloakUrl}/admin/realms/${realm}/clients"
    
    def clientsResponse = sh(
        script: """
            curl -s -X GET "${clientsUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def clients = readJSON(text: clientsResponse)
    
    def allSessions = []
    def sessionCount = 0
    
    // Get sessions for each client
    clients.each { client ->
        try {
            def sessionsUrl = "http://${keycloakUrl}/admin/realms/${realm}/clients/${client.id}/user-sessions"
            
            def sessionsResponse = sh(
                script: """
                    curl -s -X GET "${sessionsUrl}" \\
                        -H "Authorization: Bearer ${accessToken}"
                """,
                returnStdout: true
            ).trim()
            
            def clientSessions = readJSON(text: sessionsResponse)
            
            if (clientSessions && clientSessions.size() > 0) {
                clientSessions.each { session ->
                    session.clientId = client.clientId
                    allSessions << session
                    sessionCount++
                }
            }
        } catch (Exception e) {
            // Skip clients without sessions
        }
    }
    
    echo "✅ Found ${sessionCount} active sessions across ${clients.size()} clients"
    return allSessions
}

/**
 * Get sessions for a specific user
 * @param config Map with accessToken and username
 * @return List of user sessions
 */
def getUserSessions(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def username = config.username
    
    echo "📊 Retrieving sessions for user '${username}'..."
    
    // First get user ID
    def searchUrl = "http://${keycloakUrl}/admin/realms/${realm}/users?username=${URLEncoder.encode(username, 'UTF-8')}&exact=true"
    
    def userResponse = sh(
        script: """
            curl -s -X GET "${searchUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def users = readJSON(text: userResponse)
    
    if (!users || users.size() == 0) {
        error("User '${username}' not found")
    }
    
    def userId = users[0].id
    
    // Get user sessions
    def sessionsUrl = "http://${keycloakUrl}/admin/realms/${realm}/users/${userId}/sessions"
    
    def sessionsResponse = sh(
        script: """
            curl -s -X GET "${sessionsUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def sessions = readJSON(text: sessionsResponse)
    
    echo "✅ Found ${sessions.size()} sessions for user '${username}'"
    return sessions
}

/**
 * Revoke all sessions for a user
 * @param config Map with accessToken and username
 */
def revokeUserSessions(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def username = config.username
    
    echo "🔒 Revoking all sessions for user '${username}'..."
    
    // First get user ID
    def searchUrl = "http://${keycloakUrl}/admin/realms/${realm}/users?username=${URLEncoder.encode(username, 'UTF-8')}&exact=true"
    
    def userResponse = sh(
        script: """
            curl -s -X GET "${searchUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def users = readJSON(text: userResponse)
    
    if (!users || users.size() == 0) {
        error("User '${username}' not found")
    }
    
    def userId = users[0].id
    
    // Logout user (revokes all sessions)
    def logoutUrl = "http://${keycloakUrl}/admin/realms/${realm}/users/${userId}/logout"
    
    def response = sh(
        script: """
            curl -s -w "\\n%{http_code}" -X POST "${logoutUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def lines = response.split('\n')
    def httpCode = lines[-1]
    
    if (httpCode != '204') {
        error("Failed to revoke sessions. HTTP ${httpCode}")
    }
    
    echo "✅ All sessions revoked for user '${username}'"
}

/**
 * Revoke all sessions in realm (EMERGENCY)
 * @param config Map with accessToken
 */
def revokeAllSessions(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    
    echo "⚠️  EMERGENCY: Revoking ALL sessions in realm '${realm}'..."
    
    def logoutUrl = "http://${keycloakUrl}/admin/realms/${realm}/logout-all"
    
    def response = sh(
        script: """
            curl -s -w "\\n%{http_code}" -X POST "${logoutUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def lines = response.split('\n')
    def httpCode = lines[-1]
    
    if (httpCode != '204') {
        error("Failed to revoke all sessions. HTTP ${httpCode}")
    }
    
    echo "✅ ALL sessions revoked in realm '${realm}'"
}

/**
 * Detect users with unverified emails
 * @param config Map with accessToken
 * @return List of users with unverified emails
 */
def detectUnverifiedEmails(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    
    echo "🔍 Detecting users with unverified emails..."
    
    def usersUrl = "http://${keycloakUrl}/admin/realms/${realm}/users"
    
    def response = sh(
        script: """
            curl -s -X GET "${usersUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def users = readJSON(text: response)
    
    def unverifiedUsers = users.findAll { !it.emailVerified && it.email }
    
    echo "⚠️  Found ${unverifiedUsers.size()} users with unverified emails"
    return unverifiedUsers
}

/**
 * Detect inactive users (no recent login)
 * @param config Map with accessToken and inactiveDays
 * @return List of inactive users
 */
def detectInactiveUsers(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def inactiveDays = config.inactiveDays ?: 90
    
    echo "🔍 Detecting users inactive for >${inactiveDays} days..."
    
    def usersUrl = "http://${keycloakUrl}/admin/realms/${realm}/users"
    
    def response = sh(
        script: """
            curl -s -X GET "${usersUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def users = readJSON(text: response)
    
    def now = new Date().time
    def thresholdMs = inactiveDays * 24 * 60 * 60 * 1000
    
    def inactiveUsers = []
    
    users.each { user ->
        // Check if user has never logged in or last login > threshold
        if (!user.attributes?.lastLogin) {
            // User never logged in - check creation date
            if (user.createdTimestamp) {
                def age = now - user.createdTimestamp
                if (age > thresholdMs) {
                    inactiveUsers << user
                }
            }
        } else {
            def lastLogin = user.attributes.lastLogin[0] as Long
            def inactiveTime = now - lastLogin
            if (inactiveTime > thresholdMs) {
                inactiveUsers << user
            }
        }
    }
    
    echo "⚠️  Found ${inactiveUsers.size()} inactive users"
    return inactiveUsers
}

/**
 * Detect disabled users
 * @param config Map with accessToken
 * @return List of disabled users
 */
def detectDisabledUsers(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    
    echo "🔍 Detecting disabled users..."
    
    def usersUrl = "http://${keycloakUrl}/admin/realms/${realm}/users"
    
    def response = sh(
        script: """
            curl -s -X GET "${usersUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def users = readJSON(text: response)
    
    def disabledUsers = users.findAll { !it.enabled }
    
    echo "⚠️  Found ${disabledUsers.size()} disabled users"
    return disabledUsers
}

/**
 * Generate security audit report using template
 * @param config Map with accessToken and audit results
 * @return HTML report
 */
def generateSecurityReport(Map config) {
    def auditResults = config.auditResults
    def timestamp = new Date().format("yyyy-MM-dd HH:mm:ss")
    def realmName = config.realm ?: env.KC_REALM ?: 'internal'
    
    // Read the template
    def templatePath = '/var/jenkins_home/workflow-libs/keycloak-lib/templates/reports/reportTemplate.html'
    def template = readFile(templatePath)
    def cssContent = readFile('/var/jenkins_home/workflow-libs/keycloak-lib/templates/reports/css/report.css')
    
    // Generate summary cards
    def summaryCards = generateSummaryCards(auditResults)
    
    // Generate report sections
    def sections = generateReportSections(auditResults)
    
    // Replace placeholders
    def html = template
        .replace('{{REPORT_TITLE}}', 'Keycloak Security Audit Report')
        .replace('{{REPORT_SUBTITLE}}', 'Comprehensive security analysis and compliance check')
        .replace('{{REPORT_DATE}}', timestamp)
        .replace('{{REALM_NAME}}', realmName)
        .replace('{{REPORT_TYPE}}', 'Security Audit')
        .replace('{{SUMMARY_CARDS}}', summaryCards)
        .replace('{{REPORT_SECTIONS}}', sections)
        .replace('<link rel="stylesheet" href="css/report.css">', "<style>${cssContent}</style>")
    
    return html
}

/**
 * Generate summary cards HTML
 */
def generateSummaryCards(auditResults) {
    def unverifiedClass = auditResults.unverifiedEmails > 20 ? 'danger' : (auditResults.unverifiedEmails > 5 ? 'warning' : 'success')
    def inactiveClass = auditResults.inactiveUsers > 50 ? 'danger' : (auditResults.inactiveUsers > 10 ? 'warning' : 'success')
    
    return """
<div class="summary-card ${unverifiedClass}">
    <div class="summary-card-label">Unverified Emails</div>
    <div class="summary-card-value">${auditResults.unverifiedEmails}</div>
</div>
<div class="summary-card ${inactiveClass}">
    <div class="summary-card-label">Inactive Users (>90d)</div>
    <div class="summary-card-value">${auditResults.inactiveUsers}</div>
</div>
<div class="summary-card">
    <div class="summary-card-label">Disabled Users</div>
    <div class="summary-card-value">${auditResults.disabledUsers}</div>
</div>
<div class="summary-card">
    <div class="summary-card-label">Orphan Groups</div>
    <div class="summary-card-value">${auditResults.orphanGroups}</div>
</div>
<div class="summary-card">
    <div class="summary-card-label">Service Accounts</div>
    <div class="summary-card-value">${auditResults.serviceAccounts ?: 0}</div>
</div>
"""
}

/**
 * Generate report sections HTML
 */
def generateReportSections(auditResults) {
    def sections = ""
    
    // Unverified Emails Section
    sections += """
<div class="report-section">
    <h2 class="section-title">📧 Users with Unverified Emails</h2>
    ${auditResults.unverifiedEmailsList?.size() > 0 ? generateUserTable(auditResults.unverifiedEmailsList) : '<div class="empty-state"><div class="empty-state-icon">✅</div><div class="empty-state-text">No users with unverified emails</div></div>'}
</div>
"""
    
    // Inactive Users Section
    sections += """
<div class="report-section">
    <h2 class="section-title">⏰ Inactive Users (>90 days)</h2>
    ${auditResults.inactiveUsersList?.size() > 0 ? generateUserTable(auditResults.inactiveUsersList) : '<div class="empty-state"><div class="empty-state-icon">✅</div><div class="empty-state-text">No inactive users detected</div></div>'}
</div>
"""
    
    // Disabled Users Section
    sections += """
<div class="report-section">
    <h2 class="section-title">🚫 Disabled Users</h2>
    ${auditResults.disabledUsersList?.size() > 0 ? generateUserTable(auditResults.disabledUsersList) : '<div class="empty-state"><div class="empty-state-icon">✅</div><div class="empty-state-text">No disabled users</div></div>'}
</div>
"""
    
    // Recommendations Section
    sections += generateRecommendations(auditResults)
    
    return sections
}

/**
 * Generate recommendations based on audit results
 */
def generateRecommendations(auditResults) {
    def recommendations = []
    
    if (auditResults.unverifiedEmails > 0) {
        recommendations << "Send email verification reminders to users with unverified emails"
    }
    if (auditResults.inactiveUsers > 10) {
        recommendations << "Review and consider disabling inactive user accounts"
    }
    if (auditResults.orphanGroups > 0) {
        recommendations << "Clean up orphan groups with no members"
    }
    
    if (recommendations.size() == 0) {
        recommendations << "All security checks passed. Continue monitoring regularly."
    }
    
    def recList = recommendations.collect { "<li>${it}</li>" }.join('\n')
    
    return """
<div class="report-section">
    <h2 class="section-title">💡 Recommendations</h2>
    <ul style="margin: 16px 0; padding-left: 24px; line-height: 1.8;">
        ${recList}
    </ul>
</div>
"""
}

/**
 * Helper to generate user table HTML
 */
def generateUserTable(users) {
    if (users.size() == 0) return '<div class="empty-state"><div class="empty-state-text">No users found</div></div>'
    
    def rows = users.collect { user ->
        def statusBadge = user.enabled ? '<span class="badge success">Enabled</span>' : '<span class="badge danger">Disabled</span>'
        def emailVerified = user.emailVerified ? '✅' : '❌'
        "<tr><td>${user.username ?: 'N/A'}</td><td>${user.email ?: 'N/A'}</td><td>${user.firstName ?: ''} ${user.lastName ?: ''}</td><td>${emailVerified}</td><td>${statusBadge}</td></tr>"
    }.join('\n')
    
    return """
<table class="data-table">
    <thead>
        <tr>
            <th>Username</th>
            <th>Email</th>
            <th>Name</th>
            <th>Email Verified</th>
            <th>Status</th>
        </tr>
    </thead>
    <tbody>
        ${rows}
    </tbody>
</table>
"""
}

/**
 * Get session statistics
 * @param config Map with accessToken
 * @return Session statistics
 */
def getSessionStatistics(Map config) {
    def sessions = getActiveSessions(config)
    
    def stats = [
        totalSessions: sessions.size(),
        uniqueUsers: sessions.collect { it.username }.unique().size(),
        uniqueClients: sessions.collect { it.clientId }.unique().size(),
        averageSessionAge: 0
    ]
    
    if (sessions.size() > 0) {
        def now = new Date().time
        def totalAge = sessions.sum { session ->
            session.start ? (now - session.start) : 0
        }
        stats.averageSessionAge = (totalAge / sessions.size() / 1000 / 60).intValue() // minutes
    }
    
    return stats
}

return this
