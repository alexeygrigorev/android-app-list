#!/usr/bin/env bash
# Builds build/app-list.apk with the plain SDK tools (no Gradle).
# Requires: JDK 17+, Android SDK with build-tools and a platform installed.
set -euo pipefail
cd "$(dirname "$0")"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
BUILD_TOOLS_VERSION="${BUILD_TOOLS_VERSION:-$(ls "$SDK/build-tools" | sort -V | tail -1)}"
PLATFORM="${PLATFORM:-android-35}"
BT="$SDK/build-tools/$BUILD_TOOLS_VERSION"
JAR="$SDK/platforms/$PLATFORM/android.jar"
KEYSTORE="${KEYSTORE:-$HOME/.android/debug.keystore}"

# On Windows (Git Bash) the tools are .exe/.bat and need Windows paths.
if [[ "${OS:-}" == "Windows_NT" ]]; then
  EXE=.exe; BAT=.bat
  winpath() { cygpath -w "$1"; }
  run_bat() { local tool="$1"; shift; cmd //c "$(winpath "$BT/$tool.bat")" "$@"; }
else
  EXE=; BAT=
  winpath() { echo "$1"; }
  run_bat() { local tool="$1"; shift; "$BT/$tool" "$@"; }
fi

[[ -f "$JAR" ]] || { echo "Missing $JAR - install the platform (e.g. sdkmanager \"platforms;$PLATFORM\")" >&2; exit 1; }

rm -rf build
mkdir -p build/classes build/dex

"$BT/aapt2$EXE" link -o build/base.apk --manifest AndroidManifest.xml -I "$JAR"

javac --release 17 -classpath "$JAR" -d build/classes $(find src -name '*.java')

CLASSES=()
while IFS= read -r c; do CLASSES+=("$(winpath "$c")"); done < <(find build/classes -name '*.class')
run_bat d8 --lib "$(winpath "$JAR")" --min-api 26 --output "$(winpath build/dex)" "${CLASSES[@]}"

cp build/base.apk build/unsigned.apk
(cd build/dex && jar uf ../unsigned.apk classes.dex)

"$BT/zipalign$EXE" -f 4 build/unsigned.apk build/aligned.apk

if [[ ! -f "$KEYSTORE" ]]; then
  mkdir -p "$(dirname "$KEYSTORE")"
  keytool -genkeypair -keystore "$KEYSTORE" -storepass android -alias androiddebugkey \
    -keypass android -keyalg RSA -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
fi

run_bat apksigner sign --ks "$(winpath "$KEYSTORE")" --ks-pass pass:android \
  --out "$(winpath build/app-list.apk)" "$(winpath build/aligned.apk)"

echo "Built build/app-list.apk"
