#!/usr/bin/env bash
# Builds the debug APK on Ubuntu / Debian / WSL.  Usage: bash build.sh
set -eo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
WORK="$HOME/dreadnought-tools"
export ANDROID_HOME="$WORK/android-sdk"
export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
export PATH="$JAVA_HOME/bin:$PATH"
mkdir -p "$WORK"

sudo apt-get update -y
sudo apt-get install -y openjdk-17-jdk unzip curl

if [ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  curl -L -o "$WORK/cmdtools.zip" https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q -o "$WORK/cmdtools.zip" -d "$WORK/tmp-cmd"
  rm -rf "$ANDROID_HOME/cmdline-tools/latest"
  mv "$WORK/tmp-cmd/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null || true
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platform-tools" "platforms;android-34" "build-tools;34.0.0"

if [ ! -x "$WORK/gradle-8.7/bin/gradle" ]; then
  curl -L -o "$WORK/gradle.zip" https://services.gradle.org/distributions/gradle-8.7-bin.zip
  unzip -q -o "$WORK/gradle.zip" -d "$WORK"
fi

cd "$HERE"
echo "sdk.dir=$ANDROID_HOME" > local.properties
"$WORK/gradle-8.7/bin/gradle" assembleDebug --no-daemon
cp app/build/outputs/apk/debug/app-debug.apk "$HOME/Memorius-debug.apk"
echo "Done: $HOME/Memorius-debug.apk"
