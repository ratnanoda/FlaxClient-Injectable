#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
LAUNCHER_DIR="$ROOT_DIR/FlaxClientLauncher"
LAUNCHER_BIN="$LAUNCHER_DIR/target/release/flax-client-launcher"
CLIENT_JAR="$ROOT_DIR/build/libs/FlaxClient-Release.jar"
DIST_DIR="$ROOT_DIR/dist"
DIST_BIN="$DIST_DIR/FlaxClientLauncher-Releases-1.0-linux-x86_64"

package_only=0
if [[ "${1:-}" == "--package-only" ]]; then
    package_only=1
    shift
fi

log() {
    printf '[FlaxClient] %s\n' "$*"
}

client_needs_build=0
if [[ ! -f "$CLIENT_JAR" ]]; then
    client_needs_build=1
elif find "$ROOT_DIR/src/main/java" "$ROOT_DIR/src/main/resources" "$ROOT_DIR/src/main/native" \
        "$ROOT_DIR/build.gradle" "$ROOT_DIR/gradle.properties" \
        -type f -newer "$CLIENT_JAR" -print -quit | grep -q .; then
    client_needs_build=1
fi

if (( client_needs_build )); then
    log "Building FlaxClient Releases 1.0..."
    "$ROOT_DIR/gradlew" -p "$ROOT_DIR" build -x bumpBuildNumber
fi

launcher_needs_build=0
if [[ ! -x "$LAUNCHER_BIN" ]]; then
    launcher_needs_build=1
elif [[ "$CLIENT_JAR" -nt "$LAUNCHER_BIN" ]]; then
    launcher_needs_build=1
elif find "$LAUNCHER_DIR/src" "$LAUNCHER_DIR/assets" "$LAUNCHER_DIR/build.rs" "$LAUNCHER_DIR/Cargo.toml" \
        "$LAUNCHER_DIR/Cargo.lock" "$LAUNCHER_DIR/FlaxClient.json" "$LAUNCHER_DIR/flaxicon.png" \
        -type f -newer "$LAUNCHER_BIN" -print -quit | grep -q .; then
    launcher_needs_build=1
fi

if (( launcher_needs_build )); then
    if ! command -v cargo >/dev/null 2>&1; then
        log "Rust/Cargo is required to build the launcher."
        log "Install Rust from https://rustup.rs and run this script again."
        exit 1
    fi

    log "Embedding the latest client and building the launcher..."
    cargo build --manifest-path "$LAUNCHER_DIR/Cargo.toml" --release
fi

if [[ ! -x "$LAUNCHER_BIN" ]]; then
    log "Launcher build did not produce: $LAUNCHER_BIN"
    exit 1
fi

mkdir -p "$DIST_DIR"
if [[ ! -x "$DIST_BIN" || "$LAUNCHER_BIN" -nt "$DIST_BIN" ]]; then
    cp "$LAUNCHER_BIN" "$DIST_BIN"
    chmod +x "$DIST_BIN"
fi

if (( package_only )); then
    log "Single-file launcher ready: $DIST_BIN"
    exit 0
fi

log "Starting the single-file FlaxClient Launcher..."
cd "$DIST_DIR"
exec "$DIST_BIN" "$@"
