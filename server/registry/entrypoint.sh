#!/bin/sh
set -e

echo "🔐 Initializing Docker Registry with authentication..."

# Check if username and password are provided
if [ -z "$REGISTRY_USERNAME" ] || [ -z "$REGISTRY_PASSWORD" ]; then
    echo "❌ ERROR: REGISTRY_USERNAME and REGISTRY_PASSWORD must be set"
    exit 1
fi

# Generate htpasswd file
echo "📝 Generating htpasswd file for user: $REGISTRY_USERNAME"
htpasswd -Bbn "$REGISTRY_USERNAME" "$REGISTRY_PASSWORD" > /auth/htpasswd

# Verify htpasswd file was created
if [ ! -f /auth/htpasswd ]; then
    echo "❌ ERROR: Failed to create htpasswd file"
    exit 1
fi

echo "✅ htpasswd file created successfully"
echo "✅ Registry authentication configured"
echo "🚀 Starting Docker Registry..."

# Start the registry
exec registry serve "$@"
