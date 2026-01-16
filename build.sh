#!/bin/bash

# Build script for Android Ad SDK
# This script checks prerequisites and builds the SDK

set -e  # Exit on error

echo "=========================================="
echo "Android Ad SDK - Build Script"
echo "=========================================="
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check Java
echo "Checking prerequisites..."
echo ""

echo -n "Java: "
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo -e "${GREEN}✓ Found (version $JAVA_VERSION)${NC}"
else
    echo -e "${RED}✗ Not found${NC}"
    echo ""
    echo "Java is required to build the SDK."
    echo ""
    echo "Install Java:"
    echo "  macOS:   brew install openjdk@17"
    echo "  Linux:   sudo apt install openjdk-17-jdk"
    echo "  Windows: Download from https://www.oracle.com/java/technologies/downloads/"
    echo ""
    echo "After installing, run this script again."
    exit 1
fi

# Check if gradlew exists and is executable
echo -n "Gradle Wrapper: "
if [ -f "./gradlew" ] && [ -x "./gradlew" ]; then
    echo -e "${GREEN}✓ Found${NC}"
else
    echo -e "${YELLOW}! Making gradlew executable${NC}"
    chmod +x ./gradlew
fi

echo ""
echo "=========================================="
echo "Building SDK..."
echo "=========================================="
echo ""

# Clean build (optional)
if [ "$1" == "clean" ]; then
    echo "Cleaning previous build..."
    ./gradlew clean
    echo ""
fi

# Build the AAR
echo "Building release AAR..."
./gradlew :adsdk:assembleRelease

BUILD_STATUS=$?

echo ""
echo "=========================================="

if [ $BUILD_STATUS -eq 0 ]; then
    echo -e "${GREEN}✓ Build Successful!${NC}"
    echo "=========================================="
    echo ""

    AAR_PATH="adsdk/build/outputs/aar/adsdk.aar"

    if [ -f "$AAR_PATH" ]; then
        AAR_SIZE=$(ls -lh "$AAR_PATH" | awk '{print $5}')
        echo "AAR file created:"
        echo "  Location: $AAR_PATH"
        echo "  Size: $AAR_SIZE"
        echo ""

        # Offer to copy to Desktop
        echo "Copy to Desktop? (y/n)"
        read -r response
        if [[ "$response" =~ ^[Yy]$ ]]; then
            cp "$AAR_PATH" ~/Desktop/adsdk.aar
            echo -e "${GREEN}✓ Copied to ~/Desktop/adsdk.aar${NC}"
        fi

        echo ""
        echo "Next steps:"
        echo "  1. Copy adsdk.aar to your app's libs/ folder"
        echo "  2. Add to build.gradle: implementation files('libs/adsdk.aar')"
        echo "  3. See BUILD_GUIDE.md for detailed integration instructions"
    fi
else
    echo -e "${RED}✗ Build Failed${NC}"
    echo "=========================================="
    echo ""
    echo "Common issues:"
    echo "  1. JAVA_HOME not set - Run: export JAVA_HOME=\$(/usr/libexec/java_home)"
    echo "  2. SDK location missing - Create local.properties with sdk.dir"
    echo "  3. Network issues - Check internet connection for first build"
    echo ""
    echo "See BUILD_GUIDE.md for troubleshooting help"
    exit 1
fi

echo ""
