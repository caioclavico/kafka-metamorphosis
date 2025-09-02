#!/bin/bash
# Script to configure Clojars deployment
# Execute: source deploy-setup.sh

echo "🚀 Setting up environment for Clojars deployment..."

# Request user credentials
read -p "Enter your Clojars username (caioclavico): " username
username=${username:-caioclavico}

read -s -p "Enter your Clojars deploy token: " token
echo

# Export environment variables
export CLOJARS_USERNAME="$username"
export CLOJARS_PASSWORD="$token"

echo "✅ Environment variables configured:"
echo "   CLOJARS_USERNAME: $username"
echo "   CLOJARS_PASSWORD: ****"
echo ""
echo "📋 Available commands:"
echo "   lein check          # Check project"
echo "   lein test           # Run tests"
echo "   lein deploy clojars # Deploy to Clojars"
echo ""
echo "🎯 To deploy:"
echo "   1. lein check"
echo "   2. lein test"
echo "   3. lein deploy clojars"
