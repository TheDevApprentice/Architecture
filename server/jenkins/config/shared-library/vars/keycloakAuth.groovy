/**
 * Keycloak Authentication Library
 * Handles authentication and token management for Keycloak API
 */

/**
 * Get service account access token using client credentials
 * @param config Map with keycloakUrl, clientId, clientSecret
 * @return Access token string
 */
def getServiceAccountToken(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def clientId = env.KC_CLIENT_ID_JENKINS_AUTOMATION
    def clientSecret = env.KC_SECRET_JENKINS_AUTOMATION
    def realm = env.KC_REALM
    
    echo "🔐 Requesting service account token from Keycloak..."
    
    def tokenUrl = "http://${keycloakUrl}/realms/${realm}/protocol/openid-connect/token"
    
    def response = sh(
        script: """
            curl -s -X POST "${tokenUrl}" \\
                -H "Content-Type: application/x-www-form-urlencoded" \\
                -d "grant_type=client_credentials" \\
                -d "client_id=${clientId}" \\
                -d "client_secret=${clientSecret}"
        """,
        returnStdout: true
    ).trim()
    
    def jsonResponse = readJSON(text: response)
    
    if (!jsonResponse.access_token) {
        error("Failed to obtain access token: ${response}")
    }
    
    return jsonResponse.access_token
}

/**
 * Get admin access token using username/password
 * @param config Map with keycloakUrl, username, password
 * @return Access token string
 */
def getAdminToken(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def username = config.username
    def password = config.password
    def realm = env.KC_REALM
    
    if (!username || !password) {
        error("Username and password are required for admin token")
    }
    
    echo "🔐 Requesting admin token from Keycloak..."
    
    def tokenUrl = "http://${keycloakUrl}/realms/${realm}/protocol/openid-connect/token"
    
    def response = sh(
        script: """
            curl -s -X POST "${tokenUrl}" \\
                -H "Content-Type: application/x-www-form-urlencoded" \\
                -d "grant_type=password" \\
                -d "client_id=admin-cli" \\
                -d "username=${username}" \\
                -d "password=${password}"
        """,
        returnStdout: true
    ).trim()
    
    def jsonResponse = readJSON(text: response)
    
    if (!jsonResponse.access_token) {
        error("Failed to obtain admin token: ${response}")
    }
    
    return jsonResponse.access_token
}

/**
 * Validate an access token using token introspection endpoint
 * Requires client authentication (client_id and client_secret)
 * @param config Map with accessToken
 * @return true if valid, false otherwise
 */
def validateToken(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def clientId = env.KC_CLIENT_ID_JENKINS_AUTOMATION
    def clientSecret = env.KC_SECRET_JENKINS_AUTOMATION
    
    if (!accessToken) {
        return false
    }
    
    def introspectUrl = "http://${keycloakUrl}/realms/${realm}/protocol/openid-connect/token/introspect"
    
    try {
        def response = sh(
            script: """
                curl -s -X POST "${introspectUrl}" \\
                    -H "Content-Type: application/x-www-form-urlencoded" \\
                    -d "client_id=${clientId}" \\
                    -d "client_secret=${clientSecret}" \\
                    -d "token=${accessToken}"
            """,
            returnStdout: true
        ).trim()
        
        def jsonResponse = readJSON(text: response)
        return jsonResponse.active == true
    } catch (Exception e) {
        echo "⚠️  Token validation failed: ${e.message}"
        return false
    }
}

// Return this script for use with load()
return this
