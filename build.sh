#!/bin/bash

# Build script for GalaxyRealmsAPI plugin
# Everything stays in this folder!

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Building GalaxyRealmsAPI plugin..."
echo "Working directory: $SCRIPT_DIR"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed!"
    echo "Install Maven: sudo apt-get install maven"
    exit 1
fi

# Clean and build
mvn clean package

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build successful!"
    
    # Copy JAR to plugin folder (parent directory)
    if [ -f "target/GalaxyRealmsAPI-1.0.0.jar" ]; then
        cp target/GalaxyRealmsAPI-1.0.0.jar ../GalaxyRealmsAPI.jar
        echo "✅ JAR copied to: ../GalaxyRealmsAPI.jar"
        echo ""
        echo "Plugin is ready! Restart your server to load it."
    else
        echo "⚠️  JAR file not found in target/"
    fi
else
    echo ""
    echo "❌ Build failed! Check the errors above."
    exit 1
fi
