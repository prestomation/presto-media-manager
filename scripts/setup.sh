#!/usr/bin/env bash
# Installs the Android SDK command-line tools + the platform/build-tools this
# project needs, so the repo can build and run JVM (Roborazzi/Robolectric) tests
# in a headless container (local dev container or Claude Code on the web).
set -euo pipefail

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
CMDLINE_TOOLS_VERSION="11076708"  # cmdline-tools 12.0
PLATFORM="android-35"
BUILD_TOOLS="35.0.0"

export ANDROID_SDK_ROOT
export ANDROID_HOME="$ANDROID_SDK_ROOT"

mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"

if [ ! -d "$ANDROID_SDK_ROOT/cmdline-tools/latest" ]; then
  echo "Downloading Android command-line tools..."
  tmpzip="$(mktemp --suffix=.zip)"
  curl -fsSL -o "$tmpzip" \
    "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
  rm -rf "$ANDROID_SDK_ROOT/cmdline-tools/tmp"
  unzip -q "$tmpzip" -d "$ANDROID_SDK_ROOT/cmdline-tools/tmp"
  mv "$ANDROID_SDK_ROOT/cmdline-tools/tmp/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"
  rm -rf "$ANDROID_SDK_ROOT/cmdline-tools/tmp" "$tmpzip"
fi

SDKMANAGER="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"

yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true
"$SDKMANAGER" \
  "platform-tools" \
  "platforms;${PLATFORM}" \
  "build-tools;${BUILD_TOOLS}"

# Persist for later shells / the gradle build.
{
  echo "sdk.dir=$ANDROID_SDK_ROOT"
} > "$(git rev-parse --show-toplevel 2>/dev/null || echo .)/local.properties"

echo "Android SDK ready at $ANDROID_SDK_ROOT"
