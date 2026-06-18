#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
LAUNCHER_DIR="$ROOT_DIR/FlaxClientLauncher"
LAUNCHER_BIN="$LAUNCHER_DIR/target/release/flax-client-launcher"

if [[ ! -x "$LAUNCHER_BIN" ]]; then
    echo "[FlaxClient] flax-client-launcher was not found or is not executable:"
    echo "$LAUNCHER_BIN"
    echo
    echo "Build the launcher first with:"
    echo "  ./gradlew build"
    echo "  cd FlaxClientLauncher && cargo build --release"
    exit 1
fi

export FLAX_SKIP_LOCAL_BUILD=1
cd "$LAUNCHER_DIR"
exec "$LAUNCHER_BIN" "$@"
