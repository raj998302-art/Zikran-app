#!/bin/bash
echo ""
echo "ZIKRAN APK Builder v5.0 (Gradle 8.9 wrapper)"
echo "============================================"
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
export PATH=$JAVA_HOME/bin:$PATH
echo "Java: $(java -version 2>&1 | awk -F '"' '/version/ {print $2}')"
cd "$(dirname "$0")"
chmod +x gradlew
./gradlew assembleRelease --no-daemon 2>&1 | tail -20
APK=$(find . -name "app-release.apk" 2>/dev/null | head -1)
if [ -n "$APK" ]; then
  echo ""
  echo "SUCCESS! Signed APK: $APK ($(du -k "$APK" | cut -f1)KB)"
  cp "$APK" ~/storage/downloads/ZIKRAN.apk 2>/dev/null && echo "Copied to Downloads -> ZIKRAN.apk"
else
  echo "Build failed. Try: ./gradlew assembleRelease --stacktrace"
fi
