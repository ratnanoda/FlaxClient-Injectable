<p align="center">
  <img src="/.github/assets/GlideClientLogo.png" alt="Glide Client. An updated version of Soar client" title="GlideClient">
</p>

## More features, fixes and quality of life.
Download now on our website! https://glideclient.com/download
### Join our discord! https://glideclient.com/discord
<br>


## License 
Glide uses [GPL v3](https://github.com/GlideClient/client/blob/main/LICENSE)

## Issues
Please either contact us on discord or make a github issues if there is any issues you need to bring to our attention

## building (assuming intelij idea)
```
gradlew setupDecompWorkspace
```
```
gradlew genIntellijRuns
```
```
gradlew build
```

## Platform support
windows (working well) <br>
linux (working but some issues noticed that can be fixed by using the fps limit mod) <br>
mac os (working with some limitations)

## Launch with FlaxClientLauncher
1. On Windows, run `FlaxClientLauncher.bat` in the repository root.
2. On Linux/macOS, run `FlaxClientLauncher.sh` in the repository root after building the Rust launcher.
3. The launcher is fixed to **Minecraft 1.8.9**.
4. Click `Launch FlaxClient` to start with your Microsoft account or offline profile.

## Single-binary distribution
- The launcher embeds `FlaxClient-Release.jar` at build time and downloads its own Java 8 runtime on first launch.
- End users only need the single launcher executable for their OS — no JDK, no extra files.
- Prebuilt binaries are attached to GitHub Releases:
  - Windows (10/11, x86_64): `FlaxClientLauncher-windows-x86_64.exe`
  - macOS (Intel + Apple Silicon, universal): `FlaxClientLauncher-macos-universal`
  - Linux x86_64 (Ubuntu / Fedora / Arch Linux / Debian / openSUSE): `FlaxClientLauncher-linux-x86_64`
- On macOS/Linux, run `chmod +x FlaxClientLauncher-*` once before the first launch.

## Building all platform binaries
- GitHub Actions builds all four binaries natively for each OS:
  - `.github/workflows/build.yml` runs on every push to `main` and uploads artifacts.
  - `.github/workflows/release.yml` runs on tags (`v*`) and publishes a GitHub Release with the binaries attached.
- To produce a release locally, push a tag:
  ```
  git tag v0.2.0
  git push origin v0.2.0
  ```
- Or run the `Release launcher binaries` workflow manually from the Actions tab.




