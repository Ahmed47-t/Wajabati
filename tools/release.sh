#!/usr/bin/env bash
# Build, verify and (optionally) publish a release APK for Wajabati.
# Requires: JDK 17, ANDROID_HOME pointing at an SDK with platform 35 + build-tools,
#           and a clean working tree. Run from the repository root.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

VERSION="$(sed -n "s/.*versionName '\([^']*\)'.*/\1/p" app/build.gradle)"
CODE="$(sed -n "s/.*versionCode \([0-9]*\).*/\1/p" app/build.gradle)"
TAG="v${VERSION}"
APK="app/build/outputs/apk/release/app-release.apk"

echo "== Wajabati ${VERSION} (versionCode ${CODE})"

# 1. Guard rails: the version string is repeated in resources, README and ServiceApi.
grep -q "Wajabati ${VERSION}" app/src/main/res/values/strings.xml \
  || { echo "!! about_3 in values/strings.xml does not say 'Wajabati ${VERSION}'"; exit 1; }
grep -q "MealGuard/${VERSION} Android" app/src/main/java/dz/local/mealguard/ServiceApi.java \
  || { echo "!! ServiceApi User-Agent is not MealGuard/${VERSION}"; exit 1; }

# 2. Tests + lint + package.
./gradlew --no-daemon :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease

# 3. Report what was produced.
[ -f "$APK" ] || { echo "!! ${APK} was not produced"; exit 1; }
SHA="$(shasum -a 256 "$APK" 2>/dev/null | cut -d' ' -f1 || sha256sum "$APK" | cut -d' ' -f1)"
SIZE="$(du -h "$APK" | cut -f1)"
echo "== ${APK} (${SIZE})"
echo "== sha256 ${SHA}"

# 4. Optional publish: ./tools/release.sh --publish
if [[ "${1:-}" == "--publish" ]]; then
  ASSET="Wajabati-${VERSION}.apk"
  cp "$APK" "$ASSET"
  gh release create "$TAG" "$ASSET" \
    --title "Wajabati ${VERSION}" \
    --notes-file .github/RELEASE_NOTES_TEMPLATE.md \
    --prerelease
  rm -f "$ASSET"
  echo "== published ${TAG}"
fi
