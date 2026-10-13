#!/usr/bin/env bash
set -e

echo "=============================================="
echo "    KothaPad: Cross-Account Workspace Setup    "
echo "=============================================="

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

# 1. Restore debug.keystore if missing
if [ ! -f "debug.keystore" ]; then
    if [ -f "debug.keystore.base64" ]; then
        echo "[1/4] Restoring debug.keystore from debug.keystore.base64..."
        base64 -d debug.keystore.base64 > debug.keystore
    else
        echo "[1/4] Warning: debug.keystore.base64 not found."
    fi
else
    echo "[1/4] debug.keystore already exists."
fi

# 2. Ensure .env.example and .env exist
if [ ! -f ".env.example" ]; then
    echo "[2/4] Creating .env.example..."
    echo "GEMINI_API_KEY=your_api_key_here" > .env.example
fi

if [ ! -f ".env" ]; then
    echo "[2/4] Restoring .env from .env.example..."
    cp .env.example .env
else
    echo "[2/4] .env already exists."
fi

# 3. Build APK with Gradle
echo "[3/4] Compiling Android app with Gradle..."
if [ -x "./gradlew" ]; then
    ./gradlew assembleDebug
else
    gradle assembleDebug
fi

# 4. Confirm root APK output
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    cp -f "app/build/outputs/apk/debug/app-debug.apk" "KothaPad.apk"
    echo "[4/4] Compiled successfully! KothaPad.apk is available in the root folder."
else
    echo "[4/4] Warning: Output APK not found."
fi

echo "=============================================="
echo " Setup complete! Ready for local development. "
echo "=============================================="
