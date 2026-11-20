#!/usr/bin/env bash
# Arrange the release APK (and a generated POM) in a Maven layout that GitHub Packages accepts.
# Output: build/gh-packages/…  — used by docs/ci/publish-apk-workflow.yml and tools/release.sh.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../.."

VERSION="$(sed -n "s/.*versionName '\([^']*\)'.*/\1/p" app/build.gradle)"
CODE="$(sed -n "s/.*versionCode \([0-9]*\).*/\1/p" app/build.gradle)"
APK="app/build/outputs/apk/release/app-release.apk"
OUT="build/gh-packages"
COORD="dz/local/mealguard/wajabati-apk/${VERSION}"

[ -f "$APK" ] || { echo "!! $APK not found - run :app:assembleRelease first"; exit 1; }

rm -rf "$OUT"
mkdir -p "$OUT/$COORD"
cp "$APK" "$OUT/$COORD/wajabati-apk-${VERSION}.apk"
cp "$APK" "$OUT/Wajabati-${VERSION}.apk"

cat > "$OUT/$COORD/wajabati-apk-${VERSION}.pom" <<POM
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>dz.local.mealguard</groupId>
  <artifactId>wajabati-apk</artifactId>
  <version>${VERSION}</version>
  <packaging>apk</packaging>
  <name>Wajabati</name>
  <description>Personal Android client for university meal reservations on Webetu. versionCode ${CODE}. Signed with a local debug key; experimental, not a store release.</description>
  <url>https://github.com/${GITHUB_REPOSITORY:-Ahmed47-t/Wajabati}</url>
  <licenses>
    <license>
      <name>Personal use only - no redistribution licence granted</name>
    </license>
  </licenses>
</project>
POM

echo "== staged:"
find "$OUT" -type f | sort
