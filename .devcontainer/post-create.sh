#!/usr/bin/env bash
set -euo pipefail

SDK_ROOT="${ANDROID_HOME:-$HOME/android-sdk}"
CMDTOOLS_VERSION="11076708"
PLATFORM="platforms;android-35"
BUILD_TOOLS="build-tools;35.0.0"
PLATFORM_TOOLS="platform-tools"

mkdir -p "$SDK_ROOT/cmdline-tools"
cd /tmp

if [ ! -x "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "==> Downloading Android command-line tools"
  curl -fsSL -o cmdtools.zip \
    "https://dl.google.com/android/repository/commandlinetools-linux-${CMDTOOLS_VERSION}_latest.zip"
  unzip -q -o cmdtools.zip -d "$SDK_ROOT/cmdline-tools"
  rm -f cmdtools.zip
  if [ -d "$SDK_ROOT/cmdline-tools/cmdline-tools" ]; then
    rm -rf "$SDK_ROOT/cmdline-tools/latest"
    mv "$SDK_ROOT/cmdline-tools/cmdline-tools" "$SDK_ROOT/cmdline-tools/latest"
  fi
fi

export PATH="$SDK_ROOT/cmdline-tools/latest/bin:$SDK_ROOT/platform-tools:$PATH"

echo "==> Accepting SDK licenses"
mkdir -p "$SDK_ROOT/licenses"
printf '24333f8a63b6825ea9c5514f83c2829b004d1fee\n' > "$SDK_ROOT/licenses/android-sdk-license"
printf '84831b9409646a918e30573bab4c9c91346d8abd\n' > "$SDK_ROOT/licenses/android-sdk-preview-license"
yes | sdkmanager --sdk_root="$SDK_ROOT" --licenses >/dev/null || true

echo "==> Installing $PLATFORM, $BUILD_TOOLS, $PLATFORM_TOOLS"
sdkmanager --sdk_root="$SDK_ROOT" "$PLATFORM" "$BUILD_TOOLS" "$PLATFORM_TOOLS"

if [ -f gradlew ]; then
  chmod +x gradlew
fi

echo "==> Android SDK ready at $SDK_ROOT"
sdkmanager --sdk_root="$SDK_ROOT" --list_installed 2>/dev/null | head -20 || true
