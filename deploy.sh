#!/bin/bash
# Automated script for Clojars deployment
# Execute: ./deploy.sh

set -e  # Stop on error

echo "🪲 Kafka Metamorphosis - Deploy to Clojars"
echo "============================================"

# Check if environment variables are configured
if [ -z "$CLOJARS_USERNAME" ] || [ -z "$CLOJARS_PASSWORD" ]; then
    echo "❌ Error: Environment variables not configured"
    echo "Execute first: source deploy-setup.sh"
    exit 1
fi

echo "✅ Clojars credentials found for: $CLOJARS_USERNAME"

# 1. Check project syntax
echo ""
echo "🔍 1. Checking project syntax..."
lein check

# 2. Run tests
echo ""
echo "🧪 2. Running tests..."
lein test

# 3. Clean previous builds
echo ""
echo "🧹 3. Cleaning previous builds..."
lein clean

# 4. Compile project
echo ""
echo "🔨 4. Compiling project..."
lein compile

# 5. Confirm deployment
echo ""
echo "🚀 5. Ready for deployment!"
echo "Current version: $(grep defproject project.clj | cut -d'"' -f4)"
echo ""
read -p "Confirm deployment to Clojars? (y/N): " confirm

if [[ $confirm =~ ^[Yy]$ ]]; then
    echo ""
    echo "📤 Deploying to Clojars..."
    lein deploy clojars
    
    echo ""
    echo "🎉 Deployment completed successfully!"
    echo "📋 Library available at: https://clojars.org/org.clojars.caioclavico/kafka-metamorphosis"
    echo "📦 To use:"
    echo "   [org.clojars.caioclavico/kafka-metamorphosis \"$(grep defproject project.clj | cut -d'"' -f4)\"]"
else
    echo "❌ Deployment cancelled by user"
    exit 1
fi
