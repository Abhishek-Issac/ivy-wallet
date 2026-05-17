#!/usr/bin/env bash
set -euo pipefail

ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
ANDROID_SDK_ROOT="$ANDROID_HOME"
JAVA_MIN_MAJOR=17
CMDLINE_TOOLS_VERSION="11076708"
SDK_PLATFORM="platforms;android-34"
SDK_BUILD_TOOLS="build-tools;34.0.0"
SDK_PLATFORM_TOOLS="platform-tools"
BUILD_TASK="${1:-assembleDemo}"
GRADLE_MAX_WORKERS="${GRADLE_MAX_WORKERS:-1}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIST_DIR="$REPO_ROOT/dist"
AAPT2_OVERRIDE="$ANDROID_HOME/${SDK_BUILD_TOOLS//;/\/}/aapt2"

if [ -t 1 ]; then
    CYAN='\033[1;36m'; YELLOW='\033[1;33m'; RED='\033[1;31m'; RESET='\033[0m'
else
    CYAN=''; YELLOW=''; RED=''; RESET=''
fi

log() { printf "${CYAN}[ivy-wallet setup]${RESET} %s\n" "$*"; }
warn() { printf "${YELLOW}[ivy-wallet setup] %s${RESET}\n" "$*" >&2; }
fatal() { printf "${RED}[ivy-wallet setup] %s${RESET}\n" "$*" >&2; exit 1; }

SUDO=""
if [ "$(id -u)" -ne 0 ]; then
    if command -v sudo >/dev/null 2>&1; then
        SUDO="sudo"
    else
        fatal "Not running as root and sudo is not installed."
    fi
fi

apt_install() {
    command -v apt-get >/dev/null 2>&1 || fatal "apt-get not found. Install manually: $*"
    log "Installing apt packages: $*"
    $SUDO env DEBIAN_FRONTEND=noninteractive apt-get update -qq
    $SUDO env DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends "$@"
}

ensure_linux() {
    [ "$(uname -s)" = "Linux" ] || fatal "Only Linux is supported by this setup script."
}

ensure_prereqs() {
    local need=()
    command -v curl >/dev/null 2>&1 || need+=("curl")
    command -v unzip >/dev/null 2>&1 || need+=("unzip")
    command -v git >/dev/null 2>&1 || need+=("git")
    if [ "${#need[@]}" -gt 0 ]; then
        apt_install "${need[@]}"
    fi
    apt_install ca-certificates libc6 libstdc++6 zlib1g
}

java_major() {
    if ! command -v java >/dev/null 2>&1; then
        printf '0\n'
        return
    fi
    java -version 2>&1 | sed -n '1s/.*"\([0-9][0-9]*\).*".*/\1/p'
}

ensure_java() {
    local major
    major="$(java_major)"
    if [ -z "$major" ] || [ "$major" -lt "$JAVA_MIN_MAJOR" ]; then
        apt_install openjdk-17-jdk-headless
    fi
    if [ -z "${JAVA_HOME:-}" ] && command -v java >/dev/null 2>&1; then
        JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"
        export JAVA_HOME
    fi
    log "Java: $(java -version 2>&1 | sed -n '1p')"
}

ensure_android_sdk() {
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    if [ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
        local cache_dir="$HOME/.cache/ivy-wallet"
        local zip="$cache_dir/android-cmdline-tools-${CMDLINE_TOOLS_VERSION}.zip"
        mkdir -p "$cache_dir"
        log "Downloading Android command-line tools rev $CMDLINE_TOOLS_VERSION"
        curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" -o "$zip"
        rm -rf "$ANDROID_HOME/cmdline-tools/latest" "$ANDROID_HOME/cmdline-tools/cmdline-tools"
        unzip -q "$zip" -d "$ANDROID_HOME/cmdline-tools"
        mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
    else
        log "Android command-line tools already installed."
    fi

    export ANDROID_HOME ANDROID_SDK_ROOT
    export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

    log "Accepting Android SDK licenses"
    yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses >/dev/null || true

    log "Installing Android SDK packages"
    sdkmanager --sdk_root="$ANDROID_HOME" --install "$SDK_PLATFORM_TOOLS" "$SDK_PLATFORM" "$SDK_BUILD_TOOLS" >/dev/null
}

ensure_aapt2() {
    [ -x "$AAPT2_OVERRIDE" ] || fatal "AAPT2 not found at $AAPT2_OVERRIDE"
    log "AAPT2: $($AAPT2_OVERRIDE version)"
}

write_local_properties() {
    printf 'sdk.dir=%s\n' "$ANDROID_HOME" > "$REPO_ROOT/local.properties"
}

build_apk() {
    cd "$REPO_ROOT"
    chmod +x ./gradlew
    log "Building APK with ./gradlew --no-daemon --max-workers=$GRADLE_MAX_WORKERS $BUILD_TASK"
    ./gradlew \
        --no-daemon \
        --max-workers="$GRADLE_MAX_WORKERS" \
        -Pandroid.aapt2FromMavenOverride="$AAPT2_OVERRIDE" \
        "$BUILD_TASK"
}

stage_apk() {
    local src=""
    local variant="${BUILD_TASK#assemble}"
    variant="${variant,,}"
    if [ -n "$variant" ] && [ -f "$REPO_ROOT/app/build/outputs/apk/$variant/app-$variant.apk" ]; then
        src="$REPO_ROOT/app/build/outputs/apk/$variant/app-$variant.apk"
    else
        src="$(find "$REPO_ROOT/app/build/outputs/apk" -type f -name '*.apk' -printf '%T@ %p\n' | sort -nr | awk 'NR == 1 {print $2}')"
    fi
    [ -n "$src" ] && [ -f "$src" ] || fatal "Build completed but no APK was found under app/build/outputs/apk"
    mkdir -p "$DIST_DIR"
    local ts
    ts="$(date +%Y%m%d-%H%M%S)"
    cp "$src" "$DIST_DIR/IvyWallet-${variant:-latest}-${ts}.apk"
    cp "$src" "$DIST_DIR/IvyWallet-latest.apk"
    log "Build complete: $DIST_DIR/IvyWallet-latest.apk"
}

main() {
    ensure_linux
    ensure_prereqs
    ensure_java
    ensure_android_sdk
    ensure_aapt2
    write_local_properties
    build_apk
    stage_apk
}

main "$@"
