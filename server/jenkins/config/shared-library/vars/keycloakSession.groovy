import java.net.URLEncoder

/**
 * Keycloak Session Management Library
 * Provides core functions for session operations
 */

/**
 * List all active sessions in realm
 * @param config Map with accessToken
 * @return List of active sessions
 */
def listActiveSessions(Map config) {
    def keycloakUrl = config.keycloakUrl ?: env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = config.realm ?: env.KC_REALM
    def max = config.max ?: 100
    
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
    
    if (max && allSessions.size() > max) {
        return allSessions.take(max)
    }
    
    return allSessions
}

/**
 * Get sessions for a specific user
 * @param config Map with accessToken and userId
 * @return List of user sessions
 */
def listUserSessions(Map config) {
    def keycloakUrl = config.keycloakUrl ?: env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = config.realm ?: env.KC_REALM
    def userId = config.userId
    
    if (!userId) {
        error('User ID is required to list user sessions')
    }
    
    echo "📊 Retrieving sessions for user ID '${userId}'..."
    
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
    
    echo "✅ Found ${sessions.size()} sessions for user"
    return sessions
}

/**
 * Find long-running sessions
 * @param config Map with accessToken and thresholdDays
 * @return List of long-running sessions
 */
def findLongRunningSessions(Map config) {
    // Load keycloakSession helper
    def keycloakSession = load '/var/jenkins_home/workflow-libs/keycloak-lib/vars/keycloakSession.groovy'
    
    def sessions = config.sessions ?: keycloakSession.listActiveSessions(config)
    def thresholdDays = config.thresholdDays ?: 7
    def thresholdMs = thresholdDays * 24L * 60L * 60L * 1000L
    def now = System.currentTimeMillis()
    
    return sessions.findAll { session ->
        if (session.start) {
            def start = session.start as Long
            (now - start) > thresholdMs
        } else {
            false
        }
    }.collect { session ->
        def start = session.start as Long
        [
            id: session.id,
            username: session.username,
            ageInDays: ((now - start) / (24L * 60L * 60L * 1000L)).intValue()
        ]
    }
}

/**
 * Revoke all sessions for a user
 * @param config Map with accessToken and userId
 */
def revokeUserSessions(Map config) {
    def keycloakUrl = config.keycloakUrl ?: env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = config.realm ?: env.KC_REALM
    def userId = config.userId
    
    if (!userId) {
        error('User ID is required to revoke sessions')
    }
    
    echo "🔒 Revoking all sessions for user ID '${userId}'..."
    
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
    
    echo "✅ All sessions revoked for user"
}

/**
 * Revoke all sessions in realm (EMERGENCY)
 * @param config Map with accessToken
 */
def revokeAllSessions(Map config) {
    def keycloakUrl = config.keycloakUrl ?: env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = config.realm ?: env.KC_REALM
    
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

return this
