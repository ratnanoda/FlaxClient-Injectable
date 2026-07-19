# FlaxClient Launcher

A Rust launcher for FlaxClient on Minecraft **1.8.9**.
Built with `eframe/egui` (no Electron).

## Supported platforms (prebuilt binaries)

| Platform | File | Notes |
| --- | --- | --- |
| Windows x86_64 | `FlaxClientLauncher-windows-x86_64.exe` | Windows 10/11 |
| macOS (Intel + Apple Silicon) | `FlaxClientLauncher-macos-universal` | Universal binary, macOS 11+ |
| Linux x86_64 | `FlaxClientLauncher-linux-x86_64` | Built on Ubuntu 22.04 — works on Ubuntu 22.04+, Fedora 36+, Arch Linux, Debian 12+, openSUSE Leap 15.5+ |

Each binary is fully self-contained: it embeds `FlaxClient-Release.jar` and downloads its own Java 8 runtime on first launch. Users only need a single file to run the client.

## Run

- Download the binary for your platform from the GitHub release.
- macOS / Linux: mark it executable once (`chmod +x FlaxClientLauncher-*`), then double-click or run it from a terminal.
- Windows: double-click the `.exe`.

If running from a local clone of this repository:
- Windows: `FlaxClientLauncher.bat` from the repository root
- Linux/macOS: `FlaxClientLauncher.sh` from the repository root

On Linux/macOS, `FlaxClientLauncher.sh` checks the source timestamps and
automatically rebuilds the client jar and release launcher when necessary.
After the first build, unchanged launches start immediately.

## Features

- Microsoft account login (device code flow)
- Offline launch mode
- Launcher data in `%APPDATA%\.flaxclient` on Windows or `~/.local/share/.flaxclient` on Linux/macOS
- Resource packs from `.minecraft/resourcepacks`
- Memory slider tuned for the 1.8.9 build
- Bundled Java 8 runtime auto-downloaded on first launch (Windows / Linux / macOS)
- Embedded `FlaxClient-Release.jar` for single-binary distribution
- Remade widescreen launcher UI

## Requirements (for building)

- Rust toolchain (`cargo`) to build the launcher
- A successful `./gradlew build` on Linux/macOS or `.\gradlew.bat build` on Windows before the launcher release build

## Run (dev)

```bash
cd FlaxClientLauncher
cargo run
```

## Build (release)

```bash
cd ..
./gradlew build
cd FlaxClientLauncher
cargo build --release
```

Output:

- Windows: `target\release\flax-client-launcher.exe`
- Linux: `target/release/flax-client-launcher`
- macOS: `target/release/flax-client-launcher`

## Cross-platform release builds (CI)

All five target distros are produced by GitHub Actions:

- Trigger automatically: push a tag like `v0.2.0` to publish a GitHub Release with the four launcher binaries attached.
- Trigger manually: run the `Release launcher binaries` workflow from the Actions tab.

The workflows live in [`.github/workflows/build.yml`](../.github/workflows/build.yml) (every push to `main`) and [`.github/workflows/release.yml`](../.github/workflows/release.yml) (tags + manual). Both build the FlaxClient jar once, then build the launcher natively on Windows, Linux, and both macOS architectures, embedding the jar into each binary.

## Local cross-compile (optional)

Cross-compiling a GUI app between OSes requires the foreign SDK, so the normal path is the CI workflow above. If you really need to cross-compile from your own machine, build per-target with the appropriate Rust target installed (`rustup target add <triple>`) and the matching system libraries / SDK present. Building each target on its native OS — physical, VM, or CI runner — is by far the simplest route.

## Embedded client build

```bash
./gradlew build
```

This produces `build/libs/FlaxClient-Release.jar`, which is embedded into the launcher binary by `build.rs`.

## CLI test hooks

```bash
cargo run --release -- --prepare-only
cargo run --release -- --launch-test
```

## Data directories

- Launcher data: `%APPDATA%\.flaxclient`
- Game directory: `%APPDATA%\.flaxclient`
- Resource packs link: `%APPDATA%\.flaxclient\resourcepacks` -> `.minecraft\resourcepacks`

On Linux:

- Launcher data: `~/.local/share/.flaxclient`
- Game directory: `~/.local/share/.flaxclient`
- Resource packs link: `~/.local/share/.flaxclient/resourcepacks` -> `~/.minecraft/resourcepacks`

## Notes

- `FlaxClient.json` launches the 1.8.9 client.
- The release binary is intended to be distributed by itself on the same target OS/architecture.
- Please follow the original project's terms before redistributing related assets.
