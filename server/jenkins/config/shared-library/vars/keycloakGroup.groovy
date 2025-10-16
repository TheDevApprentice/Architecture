/**
 * Keycloak Group Management Library
 * Provides functions for managing Keycloak groups, members, and role assignments
 */

/**
 * Create a new group in Keycloak
 * @param config Map with group details
 * @return Group ID
 */
def createGroup(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def groupName = config.groupName
    def parentGroupName = config.parentGroupName ?: null
    def attributes = config.attributes ?: [:]
    
    if (!groupName) {
        error("Group name is required to create a group")
    }
    
    echo "👥 Creating group '${groupName}' in realm '${realm}'..."
    
    def groupJson = groovy.json.JsonOutput.toJson([
        name: groupName,
        attributes: attributes
    ])
    
    def createUrl = "http://${keycloakUrl}/admin/realms/${realm}/groups"
    
    // If parent group specified, get parent ID and use sub-groups endpoint
    if (parentGroupName) {
        def parentId = getGroupId(
            accessToken: accessToken,
            groupName: parentGroupName
        )
        
        if (!parentId) {
            error("Parent group '${parentGroupName}' not found")
        }
        
        createUrl = "http://${keycloakUrl}/admin/realms/${realm}/groups/${parentId}/children"
        echo "📁 Creating as child of group '${parentGroupName}'"
    }
    
    def response = sh(
        script: """
            curl -s -i -X POST "${createUrl}" \\
                -H "Authorization: Bearer ${accessToken}" \\
                -H "Content-Type: application/json" \\
                -d '${groupJson}'
        """,
        returnStdout: true
    ).trim()
    
    // Extract group ID from Location header
    def locationLine = response.split('\n').find { it.startsWith('Location:') || it.startsWith('location:') }
    def groupId = null
    
    if (locationLine) {
        def location = locationLine.split(':', 2)[1].trim()
        groupId = location.tokenize('/')[-1]
    }
    
    if (!response.contains('201')) {
        error("Failed to create group. Response: ${response}")
    }
    
    echo "✅ Group '${groupName}' created successfully (ID: ${groupId})"
    return groupId
}

/**
 * Update an existing group
 * @param config Map with group details
 */
def updateGroup(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def groupName = config.groupName
    def newName = config.newName ?: groupName
    def attributes = config.attributes ?: null
    
    if (!groupName) {
        error("Group name is required to update a group")
    }
    
    echo "✏️  Updating group '${groupName}' in realm '${realm}'..."
    
    def groupId = getGroupId(
        accessToken: accessToken,
        groupName: groupName
    )
    
    if (!groupId) {
        error("Group '${groupName}' not found")
    }
    
    def groupData = [name: newName]
    if (attributes != null) {
        groupData.attributes = attributes
    }
    
    def groupJson = groovy.json.JsonOutput.toJson(groupData)
    
    def updateUrl = "http://${keycloakUrl}/admin/realms/${realm}/groups/${groupId}"
    
    def response = sh(
        script: """
            curl -s -w "\\n%{http_code}" -X PUT "${updateUrl}" \\
                -H "Authorization: Bearer ${accessToken}" \\
                -H "Content-Type: application/json" \\
                -d '${groupJson}'
        """,
        returnStdout: true
    ).trim()
    
    def lines = response.split('\n')
    def httpCode = lines[-1]
    
    if (httpCode != '204') {
        error("Failed to update group. HTTP ${httpCode}: ${lines[0..-2].join('\n')}")
    }
    
    echo "✅ Group updated successfully"
}

/**
 * Delete a group
 * @param config Map with group details
 */
def deleteGroup(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def groupName = config.groupName
    
    if (!groupName) {
        error("Group name is required to delete a group")
    }
    
    echo "🗑️  Deleting group '${groupName}' from realm '${realm}'..."
    
    def groupId = getGroupId(
        accessToken: accessToken,
        groupName: groupName
    )
    
    if (!groupId) {
        error("Group '${groupName}' not found")
    }
    
    def deleteUrl = "http://${keycloakUrl}/admin/realms/${realm}/groups/${groupId}"
    
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
        error("Failed to delete group. HTTP ${httpCode}: ${lines[0..-2].join('\n')}")
    }
    
    echo "✅ Group '${groupName}' deleted successfully"
}

/**
 * List all groups in a realm
 * @param config Map with accessToken
 * @return List of groups
 */
def listGroups(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    
    echo "📋 Listing groups in realm '${realm}'..."
    
    def listUrl = "http://${keycloakUrl}/admin/realms/${realm}/groups"
    
    def response = sh(
        script: """
            curl -s -X GET "${listUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def groups = readJSON(text: response)
    
    echo "✅ Found ${groups.size()} groups"
    return groups
}

/**
 * Get detailed information about a group
 * @param config Map with accessToken and groupName
 * @return Group details
 */
def getGroup(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def groupName = config.groupName
    
    if (!groupName) {
        error("Group name is required")
    }
    
    def groupId = getGroupId(
        accessToken: accessToken,
        groupName: groupName
    )
    
    if (!groupId) {
        error("Group '${groupName}' not found")
    }
    
    def getUrl = "http://${keycloakUrl}/admin/realms/${realm}/groups/${groupId}"
    
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
 * Add users to a group
 * @param config Map with accessToken, groupName, and usernames
 */
def addMembers(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def groupName = config.groupName
    def usernames = config.usernames instanceof List ? config.usernames : [config.usernames]
    
    if (!groupName) {
        error("Group name is required")
    }
    
    echo "👥 Adding ${usernames.size()} user(s) to group '${groupName}'..."
    
    def groupId = getGroupId(
        accessToken: accessToken,
        groupName: groupName
    )
    
    if (!groupId) {
        error("Group '${groupName}' not found")
    }
    
    def successCount = 0
    def failedUsers = []
    
    usernames.each { username ->
        try {
            // Get user ID
            def userId = getUserId(
                accessToken: accessToken,
                username: username
            )
            
            if (!userId) {
                echo "⚠️  User '${username}' not found, skipping"
                failedUsers << username
                return
            }
            
            // Add user to group
            def addUrl = "http://${keycloakUrl}/admin/realms/${realm}/users/${userId}/groups/${groupId}"
            
            sh(
                script: """
                    curl -s -o /dev/null -w "%{http_code}" -X PUT "${addUrl}" \\
                        -H "Authorization: Bearer ${accessToken}"
                """,
                returnStdout: false
            )
            
            echo "  ✅ Added '${username}' to group"
            successCount++
            
        } catch (Exception e) {
            echo "  ❌ Failed to add '${username}': ${e.message}"
            failedUsers << username
        }
    }
    
    echo "✅ Successfully added ${successCount}/${usernames.size()} users to group '${groupName}'"
    
    if (failedUsers.size() > 0) {
        echo "⚠️  Failed users: ${failedUsers.join(', ')}"
    }
}

/**
 * Remove users from a group
 * @param config Map with accessToken, groupName, and usernames
 */
def removeMembers(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def groupName = config.groupName
    def usernames = config.usernames instanceof List ? config.usernames : [config.usernames]
    
    if (!groupName) {
        error("Group name is required")
    }
    
    echo "👥 Removing ${usernames.size()} user(s) from group '${groupName}'..."
    
    def groupId = getGroupId(
        accessToken: accessToken,
        groupName: groupName
    )
    
    if (!groupId) {
        error("Group '${groupName}' not found")
    }
    
    def successCount = 0
    def failedUsers = []
    
    usernames.each { username ->
        try {
            // Get user ID
            def userId = getUserId(
                accessToken: accessToken,
                username: username
            )
            
            if (!userId) {
                echo "⚠️  User '${username}' not found, skipping"
                failedUsers << username
                return
            }
            
            // Remove user from group
            def removeUrl = "http://${keycloakUrl}/admin/realms/${realm}/users/${userId}/groups/${groupId}"
            
            sh(
                script: """
                    curl -s -o /dev/null -w "%{http_code}" -X DELETE "${removeUrl}" \\
                        -H "Authorization: Bearer ${accessToken}"
                """,
                returnStdout: false
            )
            
            echo "  ✅ Removed '${username}' from group"
            successCount++
            
        } catch (Exception e) {
            echo "  ❌ Failed to remove '${username}': ${e.message}"
            failedUsers << username
        }
    }
    
    echo "✅ Successfully removed ${successCount}/${usernames.size()} users from group '${groupName}'"
    
    if (failedUsers.size() > 0) {
        echo "⚠️  Failed users: ${failedUsers.join(', ')}"
    }
}

/**
 * List members of a group
 * @param config Map with accessToken and groupName
 * @return List of group members
 */
def listMembers(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def groupName = config.groupName
    
    if (!groupName) {
        error("Group name is required")
    }
    
    echo "👥 Listing members of group '${groupName}'..."
    
    def groupId = getGroupId(
        accessToken: accessToken,
        groupName: groupName
    )
    
    if (!groupId) {
        error("Group '${groupName}' not found")
    }
    
    def membersUrl = "http://${keycloakUrl}/admin/realms/${realm}/groups/${groupId}/members"
    
    def response = sh(
        script: """
            curl -s -X GET "${membersUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def members = readJSON(text: response)
    
    echo "✅ Found ${members.size()} members in group '${groupName}'"
    return members
}

/**
 * Get group ID by name (searches recursively through group hierarchy)
 * @param config Map with accessToken and groupName
 * @return Group ID or null if not found
 */
def getGroupId(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def groupName = config.groupName
    
    // First try simple search for top-level groups
    def searchUrl = "http://${keycloakUrl}/admin/realms/${realm}/groups?search=${URLEncoder.encode(groupName, 'UTF-8')}"
    
    def response = sh(
        script: """
            curl -s -X GET "${searchUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def groups = readJSON(text: response)
    
    // Recursively search through group hierarchy
    def foundId = searchGroupRecursive(groups, groupName)
    
    return foundId
}

/**
 * Recursively search for a group by name in the group hierarchy
 * @param groups List of groups to search
 * @param targetName Name of the group to find
 * @return Group ID or null if not found
 */
private def searchGroupRecursive(groups, targetName) {
    for (group in groups) {
        // Check if this group matches
        if (group.name == targetName) {
            return group.id
        }
        
        // Check subgroups recursively
        if (group.subGroups && group.subGroups.size() > 0) {
            def foundInSubgroups = searchGroupRecursive(group.subGroups, targetName)
            if (foundInSubgroups) {
                return foundInSubgroups
            }
        }
    }
    
    return null
}

/**
 * Get user ID by username
 * @param config Map with accessToken and username
 * @return User ID or null if not found
 */
def getUserId(Map config) {
    def keycloakUrl = env.KC_URL_INTERNAL
    def accessToken = config.accessToken
    def realm = env.KC_REALM
    def username = config.username
    
    def searchUrl = "http://${keycloakUrl}/admin/realms/${realm}/users?username=${URLEncoder.encode(username, 'UTF-8')}&exact=true"
    
    def response = sh(
        script: """
            curl -s -X GET "${searchUrl}" \\
                -H "Authorization: Bearer ${accessToken}"
        """,
        returnStdout: true
    ).trim()
    
    def users = readJSON(text: response)
    
    return users?.size() > 0 ? users[0].id : null
}

/**
 * Check if a group exists
 * @param config Map with accessToken and groupName
 * @return true if group exists, false otherwise
 */
def groupExists(Map config) {
    def groupId = getGroupId(config)
    return groupId != null
}

/**
 * Detect orphan groups (groups with no members)
 * @param config Map with accessToken
 * @return List of orphan group names
 */
def detectOrphanGroups(Map config) {
    def accessToken = config.accessToken
    
    echo "🔍 Detecting orphan groups (groups with no members)..."
    
    def groups = listGroups(
        accessToken: accessToken
    )
    
    def orphanGroups = []
    
    groups.each { group ->
        def members = listMembers(
            accessToken: accessToken,
            groupName: group.name
        )
        
        if (members.size() == 0) {
            orphanGroups << group.name
        }
    }
    
    echo "✅ Found ${orphanGroups.size()} orphan groups"
    return orphanGroups
}

return this
