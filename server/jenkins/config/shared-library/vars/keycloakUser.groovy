/**
 * Keycloak User Management Library
 * Provides functions to manage users in Keycloak realms
 */

/**
 * Create a new user in Keycloak
 * @param config Map with user details
 * @return User ID
 */
def createUser(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def username = config.username
    def email = config.email
    def firstName = config.firstName ?: ''
    def lastName = config.lastName ?: ''
    def enabled = config.enabled?: true
    def emailVerified = config.emailVerified?: false
    def password = config.password
    def temporaryPassword = config.temporaryPassword?: true
    def locale = config.locale ?: 'en'
    
    if (!username || !email) {
        error("Username and email are required to create a user")
    }
    
    echo "👤 Creating user '${username}' in realm '${realm}'..."
    
    // Prepare user JSON
    def userJson = groovy.json.JsonOutput.toJson([
        username: username,
        email: email,
        firstName: firstName,
        lastName: lastName,
        enabled: enabled,
        emailVerified: emailVerified,
        attributes: [
            locale: [locale]
        ],
        credentials: password ? [[
            type: 'password',
            value: password,
            temporary: temporaryPassword
        ]] : [],
        requiredActions: temporaryPassword ? ['UPDATE_PASSWORD'] : []
    ])
    
    def createUrl = "http://${keycloakUrl}/admin/realms/${realm}/users"
    
    // Write JSON to temporary file to avoid exposing password in shell command
    def tmpFile = "/tmp/keycloak-user-${username}-${System.currentTimeMillis()}.json"
    writeFile file: tmpFile, text: userJson
    
    // Create user
    def createResponse = sh(
        script: """
            curl -s -w "\\n%{http_code}" -X POST "${createUrl}" \\
                -H "Authorization: Bearer ${accessToken}" \\
                -H "Content-Type: application/json" \\
                -d @${tmpFile}
            rm -f ${tmpFile}
        """,
        returnStdout: true
    ).trim()
    
    def lines = createResponse.split('\n')
    def httpCode = lines[-1]
    
    if (httpCode != '201') {
        error("Failed to create user. HTTP ${httpCode}: ${lines[0..-2].join('\n')}")
    }
    
    echo "✅ User created successfully"
    
    // Get user ID
    def userId = getUserId(keycloakUrl: keycloakUrl, accessToken: accessToken, realm: realm, username: username)
    
    return userId
}

/**
 * Update an existing user
 * @param config Map with user details
 */
def updateUser(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def username = config.username
    
    if (!username) {
        error("Username is required to update a user")
    }
    
    echo "✏️  Updating user '${username}' in realm '${realm}'..."
    
    def userId = getUserId(keycloakUrl: keycloakUrl, accessToken: accessToken, realm: realm, username: username)
    
    // Prepare update JSON (only include non-null values)
    def updateData = [:]
    if (config.email) updateData.email = config.email
    if (config.firstName) updateData.firstName = config.firstName
    if (config.lastName) updateData.lastName = config.lastName
    if (config.enabled != null) updateData.enabled = config.enabled
    if (config.emailVerified != null) updateData.emailVerified = config.emailVerified
    
    // Add locale attribute if provided
    if (config.locale) {
        updateData.attributes = [
            locale: [config.locale]
        ]
    }
    
    def userJson = groovy.json.JsonOutput.toJson(updateData)
    
    def updateUrl = "http://${keycloakUrl}/admin/realms/${realm}/users/${userId}"
    
    def response = sh(
        script: """
            curl -s -w "\\n%{http_code}" -X PUT "${updateUrl}" \\
                -H "Authorization: Bearer ${accessToken}" \\
                -H "Content-Type: application/json" \\
                -d '${userJson}'
        """,
        returnStdout: true
    ).trim()
    
    def lines = response.split('\n')
    def httpCode = lines[-1]
    
    if (httpCode != '204') {
        error("Failed to update user. HTTP ${httpCode}: ${lines[0..-2].join('\n')}")
    }
    
    echo "✅ User updated successfully"
}

/**
 * Delete a user
 * @param config Map with keycloakUrl, accessToken, realm, username
 */
def deleteUser(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def username = config.username
    
    if (!username) {
        error("Username is required to delete a user")
    }
    
    echo "🗑️  Deleting user '${username}' from realm '${realm}'..."
    
    def userId = getUserId(keycloakUrl: keycloakUrl, accessToken: accessToken, realm: realm, username: username)
    
    def deleteUrl = "http://${keycloakUrl}/admin/realms/${realm}/users/${userId}"
    
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
        error("Failed to delete user. HTTP ${httpCode}: ${lines[0..-2].join('\n')}")
    }
    
    echo "✅ User deleted successfully"
}

/**
 * Reset user password
 * @param config Map with password details
 */
def resetPassword(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def username = config.username
    def password = config.password
    def temporary = config.temporary != null ? config.temporary : true
    
    if (!username || !password) {
        error("Username and password are required")
    }
    
    echo "🔑 Resetting password for user '${username}'..."
    
    def userId = getUserId(keycloakUrl: keycloakUrl, accessToken: accessToken, realm: realm, username: username)
    
    def credentialJson = groovy.json.JsonOutput.toJson([
        type: 'password',
        value: password,
        temporary: temporary
    ])
    
    def resetUrl = "http://${keycloakUrl}/admin/realms/${realm}/users/${userId}/reset-password"
    
    // Write JSON to temporary file to avoid exposing password in shell command
    def tmpFile = "/tmp/keycloak-password-${username}-${System.currentTimeMillis()}.json"
    writeFile file: tmpFile, text: credentialJson
    
    def response = sh(
        script: """
            curl -s -w "\\n%{http_code}" -X PUT "${resetUrl}" \\
                -H "Authorization: Bearer ${accessToken}" \\
                -H "Content-Type: application/json" \\
                -d @${tmpFile}
            rm -f ${tmpFile}
        """,
        returnStdout: true
    ).trim()
    
    def lines = response.split('\n')
    def httpCode = lines[-1]
    
    if (httpCode != '204') {
        error("Failed to reset password. HTTP ${httpCode}: ${lines[0..-2].join('\n')}")
    }
    
    echo "✅ Password reset successfully"
}

/**
 * Add user to a group
 * @param config Map with group details
 */
def addUserToGroup(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def username = config.username
    def groupName = config.groupName
    
    if (!username || !groupName) {
        error("Username and groupName are required")
    }
    
    echo "👥 Adding user '${username}' to group '${groupName}'..."
    
    def userId = getUserId(keycloakUrl: keycloakUrl, accessToken: accessToken, realm: realm, username: username)
    def groupId = getGroupId(keycloakUrl: keycloakUrl, accessToken: accessToken, realm: realm, groupName: groupName)
    
    def addUrl = "http://${keycloakUrl}/admin/realms/${realm}/users/${userId}/groups/${groupId}"
    
    def response = sh(
        script: """
            curl -s -w "\\n%{http_code}" -X PUT "${addUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def lines = response.split('\n')
    def httpCode = lines[-1]
    
    if (httpCode != '204') {
        error("Failed to add user to group. HTTP ${httpCode}: ${lines[0..-2].join('\n')}")
    }
    
    echo "✅ User added to group successfully"
}

/**
 * List all users in a realm
 * @param config Map with keycloakUrl, accessToken, realm
 * @return List of user objects
 */
def listUsers(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def max = config.max ?: 100
    
    echo "📋 Listing users in realm '${realm}'..."
    
    def listUrl = "http://${keycloakUrl}/admin/realms/${realm}/users?max=${max}"
    
    def response = sh(
        script: """
            curl -s -X GET "${listUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def users = readJSON(text: response)
    
    echo "Found ${users.size()} users"
    
    return users
}

/**
 * Get user ID by username
 * @param config Map with keycloakUrl, accessToken, realm, username
 * @return User ID
 */
def getUserId(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def username = config.username
    
    def searchUrl = "http://${keycloakUrl}/admin/realms/${realm}/users?username=${username}&exact=true"
    
    def response = sh(
        script: """
            curl -s -X GET "${searchUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def users = readJSON(text: response)
    
    if (users.size() == 0) {
        error("User '${username}' not found in realm '${realm}'")
    }
    
    return users[0].id
}

/**
 * Get group ID by group name
 * @param config Map with keycloakUrl, accessToken, realm, groupName
 * @return Group ID
 */
def getGroupId(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def groupName = config.groupName
    
    def groupsUrl = "http://${keycloakUrl}/admin/realms/${realm}/groups"
    
    def response = sh(
        script: """
            curl -s -X GET "${groupsUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def groups = readJSON(text: response)
    def group = groups.find { it.name == groupName }
    
    if (!group) {
        error("Group '${groupName}' not found in realm '${realm}'")
    }
    
    return group.id
}

/**
 * Generate a secure random password that complies with Keycloak password policy
 * Policy: length(12) and digits(1) and lowerCase(1) and upperCase(1) and specialChars(1)
 * @param length Password length (default: 16, minimum: 12)
 * @return Generated password
 */
def generatePassword(int length = 16) {
    // Ensure minimum length of 12
    if (length < 12) {
        length = 12
    }
    
    // Character sets
    def uppercase = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
    def lowercase = 'abcdefghijklmnopqrstuvwxyz'
    def digits = '0123456789'
    def special = '!@#$%^&*'
    
    // Generate password with guaranteed character types
    def password = sh(
        script: """
            # Generate one character from each required set
            UPPER=\$(echo '${uppercase}' | fold -w1 | shuf | head -n1)
            LOWER=\$(echo '${lowercase}' | fold -w1 | shuf | head -n1)
            DIGIT=\$(echo '${digits}' | fold -w1 | shuf | head -n1)
            SPECIAL=\$(echo '${special}' | fold -w1 | shuf | head -n1)
            
            # Generate remaining random characters
            REMAINING=\$(< /dev/urandom tr -dc '${uppercase}${lowercase}${digits}${special}' | head -c\$((${length} - 4)))
            
            # Combine and shuffle all characters
            echo "\${UPPER}\${LOWER}\${DIGIT}\${SPECIAL}\${REMAINING}" | fold -w1 | shuf | tr -d '\\n'
        """,
        returnStdout: true
    ).trim()
    
    return password
}

// Return this script for use with load()
return this
