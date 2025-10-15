/**
 * Keycloak Client Management Library
 * Provides functions for managing Keycloak clients (OIDC/SAML applications)
 */

/**
 * Create a new client in Keycloak
 * @param config Map with client details
 * @return Client ID
 */
def createClient(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def clientId = config.clientId
    def protocol = config.protocol ?: 'openid-connect' // openid-connect or saml
    def publicClient = config.publicClient ?: false
    def redirectUris = config.redirectUris ?: []
    def webOrigins = config.webOrigins ?: []
    def description = config.description ?: ''
    def enabled = config.enabled != null ? config.enabled : true
    def serviceAccountsEnabled = config.serviceAccountsEnabled ?: false
    def directAccessGrantsEnabled = config.directAccessGrantsEnabled ?: true
    def standardFlowEnabled = config.standardFlowEnabled ?: true
    def implicitFlowEnabled = config.implicitFlowEnabled ?: false
    
    if (!clientId) {
        error("Client ID is required to create a client")
    }
    
    echo "🔧 Creating client '${clientId}' in realm '${realm}'..."
    echo "   Protocol: ${protocol}"
    echo "   Public: ${publicClient}"
    
    def clientJson = groovy.json.JsonOutput.toJson([
        clientId: clientId,
        protocol: protocol,
        publicClient: publicClient,
        redirectUris: redirectUris,
        webOrigins: webOrigins,
        description: description,
        enabled: enabled,
        serviceAccountsEnabled: serviceAccountsEnabled,
        directAccessGrantsEnabled: directAccessGrantsEnabled,
        standardFlowEnabled: standardFlowEnabled,
        implicitFlowEnabled: implicitFlowEnabled,
        attributes: [
            'pkce.code.challenge.method': 'S256'
        ]
    ])
    
    def createUrl = "http://${keycloakUrl}/admin/realms/${realm}/clients"
    
    def response = sh(
        script: """
            curl -s -w "\\n%{http_code}" -X POST "${createUrl}" \\
                -H "Authorization: Bearer ${accessToken}" \\
                -H "Content-Type: application/json" \\
                -d '${clientJson}'
        """,
        returnStdout: true
    ).trim()
    
    def lines = response.split('\n')
    def httpCode = lines[-1]
    
    if (httpCode != '201') {
        error("Failed to create client. HTTP ${httpCode}: ${lines[0..-2].join('\n')}")
    }
    
    echo "✅ Client '${clientId}' created successfully"
    
    // Get the internal client UUID
    def clientUuid = getClientUuid(
        accessToken: accessToken,
        clientId: clientId
    )
    
    return clientUuid
}

/**
 * Create client from template
 * @param config Map with accessToken, clientId, and template name
 * @return Client UUID
 */
def createClientFromTemplate(Map config) {
    def template = config.template
    def clientId = config.clientId
    def redirectUris = config.redirectUris ?: []
    
    echo "📋 Creating client from template: ${template}"
    
    def templates = [
        'web-app': [
            publicClient: false,
            standardFlowEnabled: true,
            directAccessGrantsEnabled: false,
            implicitFlowEnabled: false,
            serviceAccountsEnabled: false
        ],
        'spa': [
            publicClient: true,
            standardFlowEnabled: true,
            directAccessGrantsEnabled: false,
            implicitFlowEnabled: false,
            serviceAccountsEnabled: false
        ],
        'backend-service': [
            publicClient: false,
            standardFlowEnabled: false,
            directAccessGrantsEnabled: false,
            implicitFlowEnabled: false,
            serviceAccountsEnabled: true
        ],
        'mobile-app': [
            publicClient: true,
            standardFlowEnabled: true,
            directAccessGrantsEnabled: false,
            implicitFlowEnabled: false,
            serviceAccountsEnabled: false
        ]
    ]
    
    if (!templates.containsKey(template)) {
        error("Unknown template: ${template}. Available: ${templates.keySet().join(', ')}")
    }
    
    def templateConfig = templates[template]
    
    return createClient(
        accessToken: config.accessToken,
        clientId: clientId,
        redirectUris: redirectUris,
        webOrigins: redirectUris.collect { it.replaceAll('/.*$', '') }, // Extract origins from URIs
        publicClient: templateConfig.publicClient,
        standardFlowEnabled: templateConfig.standardFlowEnabled,
        directAccessGrantsEnabled: templateConfig.directAccessGrantsEnabled,
        implicitFlowEnabled: templateConfig.implicitFlowEnabled,
        serviceAccountsEnabled: templateConfig.serviceAccountsEnabled
    )
}

/**
 * Update an existing client
 * @param config Map with client details
 */
def updateClient(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def clientId = config.clientId
    
    if (!clientId) {
        error("Client ID is required to update a client")
    }
    
    echo "✏️  Updating client '${clientId}' in realm '${realm}'..."
    
    def clientUuid = getClientUuid(
        accessToken: accessToken,
        clientId: clientId
    )
    
    if (!clientUuid) {
        error("Client '${clientId}' not found")
    }
    
    // Build update data with only provided fields
    def updateData = [:]
    if (config.redirectUris != null) updateData.redirectUris = config.redirectUris
    if (config.webOrigins != null) updateData.webOrigins = config.webOrigins
    if (config.description != null) updateData.description = config.description
    if (config.enabled != null) updateData.enabled = config.enabled
    if (config.publicClient != null) updateData.publicClient = config.publicClient
    
    def clientJson = groovy.json.JsonOutput.toJson(updateData)
    
    def updateUrl = "http://${keycloakUrl}/admin/realms/${realm}/clients/${clientUuid}"
    
    def response = sh(
        script: """
            curl -s -w "\\n%{http_code}" -X PUT "${updateUrl}" \\
                -H "Authorization: Bearer ${accessToken}" \\
                -H "Content-Type: application/json" \\
                -d '${clientJson}'
        """,
        returnStdout: true
    ).trim()
    
    def lines = response.split('\n')
    def httpCode = lines[-1]
    
    if (httpCode != '204') {
        error("Failed to update client. HTTP ${httpCode}: ${lines[0..-2].join('\n')}")
    }
    
    echo "✅ Client '${clientId}' updated successfully"
}

/**
 * Delete a client
 * @param config Map with accessToken and clientId
 */
def deleteClient(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def clientId = config.clientId
    
    if (!clientId) {
        error("Client ID is required to delete a client")
    }
    
    echo "🗑️  Deleting client '${clientId}' from realm '${realm}'..."
    
    def clientUuid = getClientUuid(
        accessToken: accessToken,
        clientId: clientId
    )
    
    if (!clientUuid) {
        error("Client '${clientId}' not found")
    }
    
    def deleteUrl = "http://${keycloakUrl}/admin/realms/${realm}/clients/${clientUuid}"
    
    def response = sh(
        script: """
            curl -s -w "\\n%{http_code}" -X DELETE "${deleteUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def lines = response.split('\n')
    def httpCode = lines[-1]
    
    if (httpCode != '204') {
        error("Failed to delete client. HTTP ${httpCode}: ${lines[0..-2].join('\n')}")
    }
    
    echo "✅ Client '${clientId}' deleted successfully"
}

/**
 * List all clients in a realm
 * @param config Map with accessToken
 * @return List of clients
 */
def listClients(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    
    echo "📋 Listing clients in realm '${realm}'..."
    
    def listUrl = "http://${keycloakUrl}/admin/realms/${realm}/clients"
    
    def response = sh(
        script: """
            curl -s -X GET "${listUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def clients = readJSON(text: response)

    // Ensure we always have a list of maps
    if (clients instanceof Map) {
        clients = clients.collect { entry ->
            entry.value
        }
    }

    // Filter out system clients (starting with realm- or account-)
    def userClients = clients.findAll { client ->
        def clientId = client?.clientId
        clientId &&
            !clientId.startsWith('realm-') &&
            !clientId.startsWith('account') &&
            !clientId.startsWith('admin-') &&
            !clientId.startsWith('broker') &&
            !clientId.startsWith('security-')
    }
    
    echo "✅ Found ${userClients.size()} user clients (${clients.size()} total)"
    return userClients
}

/**
 * Get detailed information about a client
 * @param config Map with accessToken and clientId
 * @return Client details
 */
def getClient(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def clientId = config.clientId
    
    if (!clientId) {
        error("Client ID is required")
    }
    
    def clientUuid = getClientUuid(
        accessToken: accessToken,
        clientId: clientId
    )
    
    if (!clientUuid) {
        error("Client '${clientId}' not found")
    }
    
    def getUrl = "http://${keycloakUrl}/admin/realms/${realm}/clients/${clientUuid}"
    
    def response = sh(
        script: """
            curl -s -X GET "${getUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    return readJSON(text: response)
}

/**
 * Get client secret (for confidential clients)
 * @param config Map with accessToken and clientId
 * @return Client secret
 */
def getClientSecret(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def clientId = config.clientId
    
    if (!clientId) {
        error("Client ID is required")
    }
    
    echo "🔐 Retrieving secret for client '${clientId}'..."
    
    def clientUuid = getClientUuid(
        accessToken: accessToken,
        clientId: clientId
    )
    
    if (!clientUuid) {
        error("Client '${clientId}' not found")
    }
    
    def secretUrl = "http://${keycloakUrl}/admin/realms/${realm}/clients/${clientUuid}/client-secret"
    
    def response = sh(
        script: """
            curl -s -X GET "${secretUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def secretData = readJSON(text: response)
    
    echo "✅ Secret retrieved (will be masked in output)"
    return secretData.value
}

/**
 * Regenerate client secret
 * @param config Map with accessToken and clientId
 * @return New client secret
 */
def regenerateSecret(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def clientId = config.clientId
    
    if (!clientId) {
        error("Client ID is required")
    }
    
    echo "🔄 Regenerating secret for client '${clientId}'..."
    
    def clientUuid = getClientUuid(
        accessToken: accessToken,
        clientId: clientId
    )
    
    if (!clientUuid) {
        error("Client '${clientId}' not found")
    }
    
    def regenerateUrl = "http://${keycloakUrl}/admin/realms/${realm}/clients/${clientUuid}/client-secret"
    
    def response = sh(
        script: """
            curl -s -X POST "${regenerateUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def secretData = readJSON(text: response)
    
    echo "✅ New secret generated (will be masked in output)"
    return secretData.value
}

/**
 * Enable or disable a client
 * @param config Map with accessToken, clientId, and enabled (boolean)
 */
def setClientEnabled(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def clientId = config.clientId
    def enabled = config.enabled
    
    if (!clientId) {
        error("Client ID is required")
    }
    
    def action = enabled ? 'Enabling' : 'Disabling'
    echo "${action} client '${clientId}'..."
    
    updateClient(
        accessToken: accessToken,
        clientId: clientId,
        enabled: enabled
    )
    
    echo "✅ Client '${clientId}' ${enabled ? 'enabled' : 'disabled'}"
}

/**
 * Get client UUID by client ID
 * @param config Map with accessToken and clientId
 * @return Client UUID or null if not found
 */
def getClientUuid(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def clientId = config.clientId
    
    def searchUrl = "http://${keycloakUrl}/admin/realms/${realm}/clients?clientId=${URLEncoder.encode(clientId, 'UTF-8')}"
    
    def response = sh(
        script: """
            curl -s -X GET "${searchUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def clients = readJSON(text: response)
    
    // Should return exactly one client
    return clients?.size() > 0 ? clients[0].id : null
}

/**
 * Check if a client exists
 * @param config Map with accessToken and clientId
 * @return true if client exists, false otherwise
 */
def clientExists(Map config) {
    def clientUuid = getClientUuid(config)
    return clientUuid != null
}

/**
 * Get service account user for a client
 * @param config Map with accessToken and clientId
 * @return Service account user details
 */
def getServiceAccountUser(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def clientId = config.clientId
    
    if (!clientId) {
        error("Client ID is required")
    }
    
    echo "👤 Getting service account user for client '${clientId}'..."
    
    def clientUuid = getClientUuid(
        accessToken: accessToken,
        clientId: clientId
    )
    
    if (!clientUuid) {
        error("Client '${clientId}' not found")
    }
    
    def saUrl = "http://${keycloakUrl}/admin/realms/${realm}/clients/${clientUuid}/service-account-user"
    
    def response = sh(
        script: """
            curl -s -X GET "${saUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    return readJSON(text: response)
}

return this
