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

# 4. Stage the Maven layout for GitHub Packages (Packages box on the repository page).
bash docs/ci/stage-apk-package.sh >/dev/null
echo "== staged for packages: build/gh-packages/dz/local/mealguard/wajabati-apk/${VERSION}/"

# 5. Optional publish: ./tools/release.sh --publish
if [[ "${1:-}" == "--publish" ]]; then
  ASSET="Wajabati-${VERSION}.apk"
  cp "$APK" "$ASSET"
  gh release create "$TAG" "$ASSET" \
    --title "Wajabati ${VERSION}" \
    --notes-file .github/RELEASE_NOTES_TEMPLATE.md \
    --prerelease
  rm -f "$ASSET"
  echo "== published ${TAG}"

  # 5b. Also publish as a package, if the token has the packages:write scope.
  #     GITHUB_ACTOR / GITHUB_TOKEN must be set for this step.
  if [[ -n "${GITHUB_TOKEN:-}" && -n "${GITHUB_ACTOR:-}" ]]; then
    for f in build/gh-packages/dz/local/mealguard/wajabati-apk/"${VERSION}"/*; do
      name="$(basename "$f")"
      curl --fail-with-body -sS -X PUT -u "${GITHUB_ACTOR}:${GITHUB_TOKEN}" \
        -H "Content-Type: application/octet-stream" --data-binary @"$f" \
        "https://maven.pkg.github.com/Ahmed47-t/Wajabati/dz/local/mealguard/wajabati-apk/${VERSION}/${name}" \
        && echo "== package: ${name}" \
        || echo "-- package upload skipped for ${name} (token needs packages:write)"
    done
  else
    echo "-- GITHUB_TOKEN/GITHUB_ACTOR not set: package upload skipped (release only)"
  fi
fi
