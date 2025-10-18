# 🔍 Jenkins OIDC Authentication Error Analysis

## 📋 Executive Summary

**Error Type:** `IllegalStateException: cannot call getRootUrlFromRequest from outside a request handling thread`

**Severity:** Medium - Intermittent UI errors, but Jenkins remains functional

**Status:** Known bug in `oic-auth-plugin` (Issue #506)

**Impact:** Users occasionally see "Oops! Un problème est survenu lors du traitement de la requête" when navigating the Jenkins UI

---

## 🐛 Error Details

### Error Message
```
java.lang.IllegalStateException: cannot call getRootUrlFromRequest from outside a request handling thread
	at jenkins.model.Jenkins.getRootUrlFromRequest(Jenkins.java:2510)
	at PluginClassLoader for oic-auth//org.jenkinsci.plugins.oic.OicSecurityRealm.getRootUrl(OicSecurityRealm.java:1104)
	at PluginClassLoader for oic-auth//org.jenkinsci.plugins.oic.OicSecurityRealm.ensureRootUrl(OicSecurityRealm.java:1111)
	at PluginClassLoader for oic-auth//org.jenkinsci.plugins.oic.OicSecurityRealm.buildOAuthRedirectUrl(OicSecurityRealm.java:1120)
	at PluginClassLoader for oic-auth//org.jenkinsci.plugins.oic.OicSecurityRealm.buildOidcClient(OicSecurityRealm.java:611)
	at PluginClassLoader for oic-auth//org.jenkinsci.plugins.oic.OicSecurityRealm.refreshExpiredToken(OicSecurityRealm.java:1290)
	at PluginClassLoader for oic-auth//org.jenkinsci.plugins.oic.OicSecurityRealm.handleTokenExpiration(OicSecurityRealm.java:1223)
```

### When It Occurs
- **Trigger:** Background token refresh attempts
- **Frequency:** Intermittent (when OIDC tokens expire)
- **Context:** Outside HTTP request handling thread (background thread)
- **User Impact:** Page refresh shows error, but subsequent refresh usually works

---

## 🔬 Root Cause Analysis

### 1. **Configuration Issue**

**Current Configuration** (`jenkins.yaml` line 17):
```yaml
rootURLFromRequest: true
```

**Problem:** When `rootURLFromRequest: true`, the plugin attempts to dynamically determine the Jenkins root URL from the incoming HTTP request. However, when token refresh happens in a **background thread** (not during an active HTTP request), there is no request context available.

### 2. **Call Stack Analysis**

```
Background Thread (Token Refresh)
    ↓
OicSecurityRealm.handleTokenExpiration()
    ↓
OicSecurityRealm.refreshExpiredToken()
    ↓
OicSecurityRealm.buildOidcClient()
    ↓
OicSecurityRealm.buildOAuthRedirectUrl()
    ↓
OicSecurityRealm.ensureRootUrl()
    ↓
OicSecurityRealm.getRootUrl()
    ↓
Jenkins.getRootUrlFromRequest() ❌ FAILS - No request context!
```

### 3. **Why Background Token Refresh?**

The OIDC plugin attempts to refresh expired tokens automatically to maintain user sessions. This happens:
- When a user's access token expires (typically 5-15 minutes)
- In a background filter/thread to avoid interrupting user actions
- **Without** an active HTTP request context

### 4. **Configuration Conflict**

We have **two** URL configurations:

1. **Dynamic URL** (from request):
   ```yaml
   rootURLFromRequest: true  # Line 17
   ```

2. **Static URL** (configured):
   ```yaml
   unclassified:
     location:
       url: "http://${JENKINS_URL}"  # Line 66
   ```

The plugin prioritizes `rootURLFromRequest: true`, which causes the error when no request is available.

---

## 🌍 Environment Analysis

### Current Setup

**From `.env`:**
```bash
JENKINS_URL=jenkins.localhost
KC_URL=auth.localhost
KC_URL_INTERNAL=keycloak:8080
```

**From `docker-compose`:**
```yaml
environment:
  - JENKINS_URL=${JENKINS_URL}
  - KC_URL=${KC_URL}
  - KC_URL_INTERNAL=${KC_URL_INTERNAL}
```

**From `jenkins.yaml`:**
```yaml
jenkins:
  securityRealm:
    oic:
      rootURLFromRequest: true  # ❌ PROBLEM
      postLogoutRedirectUrl: "http://${JENKINS_URL}"
      
unclassified:
  location:
    url: "http://${JENKINS_URL}"  # ✅ CORRECT
```

---

## 🎯 Why This Happens in The Setup

1. **Development Environment:** Using `localhost` with Traefik reverse proxy
2. **Token Expiration:** OIDC tokens expire regularly (Keycloak default: 5-15 min)
3. **Background Refresh:** Plugin tries to refresh tokens automatically
4. **No Request Context:** Background thread has no HTTP request to extract URL from
5. **Fallback Fails:** Plugin doesn't fall back to static URL configuration

---

## ✅ Solution

### Disable Dynamic Root URL (RECOMMENDED)

**Change `jenkins.yaml` line 17:**

```yaml
jenkins:
  securityRealm:
    oic:
      rootURLFromRequest: false  # ✅ Use static URL instead
```

**Why this works:**
- Forces plugin to use the static URL from `unclassified.location.url`
- Static URL is always available, even in background threads
- No dependency on HTTP request context

**Pros:**
- ✅ Simple fix (one line change)
- ✅ Reliable in all contexts
- ✅ Works with reverse proxies
- ✅ No code changes needed

**Cons:**
- ⚠️ Must ensure `JENKINS_URL` environment variable is correct
- ⚠️ URL must be accessible from both users and Jenkins container

---

## 🔗 Related Issues & References

### Official Bug Report
- **GitHub Issue:** [jenkinsci/oic-auth-plugin#506](https://github.com/jenkinsci/oic-auth-plugin/issues/506)
- **Status:** Confirmed bug (as of Jan 2025)
- **Affected Versions:** Jenkins 2.479.1+, oic-auth-plugin (latest)

### Similar Issues
- **Issue #477:** "Failed to refresh expired token"
- **Issue #647:** "Differences between Jenkins Jetty session and OpenID Provider session"

### Jenkins Documentation
- [Jenkins Location Configuration](https://www.jenkins.io/doc/book/managing/system-configuration/#jenkins-location)
- [OIDC Auth Plugin Documentation](https://plugins.jenkins.io/oic-auth/)

---

## 🎓 Lessons Learned

1. **Background Threads:** Be aware of context availability in background operations
2. **Plugin Configuration:** Understand difference between dynamic and static configurations
3. **Error Patterns:** Intermittent errors often indicate race conditions or context issues
4. **Reverse Proxies:** Static URLs are more reliable with proxies than dynamic detection

---