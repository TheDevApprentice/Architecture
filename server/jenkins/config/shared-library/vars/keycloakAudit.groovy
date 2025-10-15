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
 * Generate security audit report
 * @param config Map with accessToken and audit results
 * @return HTML report
 */
def generateSecurityReport(Map config) {
    def auditResults = config.auditResults
    def timestamp = new Date().format("yyyy-MM-dd HH:mm:ss")
    
    def html = """
<!DOCTYPE html>
<html>
<head>
    <title>Keycloak Security Audit Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; }
        h1 { color: #333; border-bottom: 3px solid #e74c3c; padding-bottom: 10px; }
        h2 { color: #555; margin-top: 30px; border-left: 4px solid #3498db; padding-left: 10px; }
        .summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin: 20px 0; }
        .card { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px; }
        .card.warning { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
        .card.critical { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }
        .card h3 { margin: 0; font-size: 14px; opacity: 0.9; }
        .card .number { font-size: 36px; font-weight: bold; margin: 10px 0; }
        table { width: 100%; border-collapse: collapse; margin: 20px 0; }
        th { background: #34495e; color: white; padding: 12px; text-align: left; }
        td { padding: 10px; border-bottom: 1px solid #ddd; }
        tr:hover { background: #f8f9fa; }
        .status-ok { color: #27ae60; font-weight: bold; }
        .status-warning { color: #f39c12; font-weight: bold; }
        .status-critical { color: #e74c3c; font-weight: bold; }
        .footer { margin-top: 30px; text-align: center; color: #7f8c8d; font-size: 12px; }
    </style>
</head>
<body>
    <div class="container">
        <h1>🔒 Keycloak Security Audit Report</h1>
        <p><strong>Generated:</strong> ${timestamp}</p>
        <p><strong>Realm:</strong> ${env.KC_REALM}</p>
        
        <div class="summary">
            <div class="card ${auditResults.unverifiedEmails > 20 ? 'critical' : 'warning'}">
                <h3>Unverified Emails</h3>
                <div class="number">${auditResults.unverifiedEmails}</div>
            </div>
            <div class="card ${auditResults.inactiveUsers > 50 ? 'critical' : 'warning'}">
                <h3>Inactive Users (>90d)</h3>
                <div class="number">${auditResults.inactiveUsers}</div>
            </div>
            <div class="card">
                <h3>Disabled Users</h3>
                <div class="number">${auditResults.disabledUsers}</div>
            </div>
            <div class="card">
                <h3>Orphan Groups</h3>
                <div class="number">${auditResults.orphanGroups}</div>
            </div>
        </div>
        
        <h2>📊 Detailed Findings</h2>
        
        <h3>Users with Unverified Emails</h3>
        ${auditResults.unverifiedEmailsList.size() > 0 ? generateUserTable(auditResults.unverifiedEmailsList) : '<p class="status-ok">✅ No issues found</p>'}
        
        <h3>Inactive Users</h3>
        ${auditResults.inactiveUsersList.size() > 0 ? generateUserTable(auditResults.inactiveUsersList) : '<p class="status-ok">✅ No issues found</p>'}
        
        <h3>Disabled Users</h3>
        ${auditResults.disabledUsersList.size() > 0 ? generateUserTable(auditResults.disabledUsersList) : '<p class="status-ok">✅ No issues found</p>'}
        
        <div class="footer">
            <p>Generated by Jenkins Keycloak Automation</p>
        </div>
    </div>
</body>
</html>
"""
    
    return html
}

/**
 * Helper to generate user table HTML
 */
def generateUserTable(users) {
    if (users.size() == 0) return '<p>No users found</p>'
    
    def rows = users.collect { user ->
        "<tr><td>${user.username}</td><td>${user.email ?: 'N/A'}</td><td>${user.firstName ?: ''} ${user.lastName ?: ''}</td><td>${user.enabled ? '✅' : '❌'}</td></tr>"
    }.join('\n')
    
    return """
<table>
    <thead>
        <tr>
            <th>Username</th>
            <th>Email</th>
            <th>Name</th>
            <th>Enabled</th>
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
