# FlaxClient attach runtime

This directory contains the Windows x64 attach runtime that bridges an already-running
Minecraft 1.8.9 JVM to the FlaxClient Java code.

The DLL establishes the native-to-JVM bridge, adds an embedded client jar to
the running Java class loader, installs schema-preserving JVMTI hooks, and
starts the FlaxClient managers on Minecraft's main thread.

The packaged build preserves the existing Lunar Client 1.8.9 and legacy
Dawn/Feather 1.8.9 paths, and also supports the Java 25/Fabric-based Dawn Client
Minecraft 26.2 runtime. The DLL detects the loaded Minecraft member layout and
selects one of three embedded jars at runtime. The 26.2 bridge is isolated from
the legacy jar so Fabric owns its ASM classes and the original 1.8.9 managers
remain unchanged. This compatibility path does not bypass or disable launcher
or anti-cheat protections.

On Minecraft 26.2, the port exposes the same module list that is registered by
the 1.8.9 client: Aim Assist, Auto Clicker, Bed ESP, Break Progress, ESP, Fast
Place, Ghost Freelook, Ghost Nametags, Healthbar, Jump Reset, Safe Walk and
YouTube PiP. Gameplay modules use the 26.2 player/input APIs, ESP uses the
client-side glow state, and the information modules render through 26.2's GUI
render-state extraction. Hold V for Ghost Freelook. The YouTube decoder and
textured in-game PiP surface are still being migrated; its menu entry is kept
stable so saved settings remain compatible.

Press Right Shift to open the lightweight module menu, use Up/Down to select a
module, and Enter or Space to toggle it. Settings are stored under
`%USERPROFILE%\.flaxclient\dawn-26.2.properties`.

## Build

Build from the repository root on Windows x64 with a JDK 8 `JAVA_HOME`, Visual
Studio C++ build tools, and CMake available. Minecraft 26.2 itself still runs
on Java 25; Java 8 is needed only by the legacy ForgeGradle build:

```powershell
.\.tooling\gradle-4.10.3\gradle-4.10.3\bin\gradle.bat clean attachPackage
```

If you normally use the Gradle wrapper and it works in your environment, the
equivalent command is:

```powershell
.\gradlew.bat clean attachPackage
```

The build creates all three runtime variants, embeds them into `FlaxClient.dll`, and
then embeds that DLL into the injector executable. The final distributable is:

```text
build\attach\FlaxClient.exe
```

No sidecar DLL or jar is required for the packaged build. At runtime the EXE
materializes its embedded DLL under the FlaxClient runtime directory before
loading it into the selected JVM. Native attach diagnostics are written to
`%TEMP%\FlaxClient\attach.log`.

Run the EXE after the Minecraft 1.8.9 main menu has appeared. The injector, DLL,
and target JVM must all be x64. The injector automatically selects a visible
Minecraft Java process. If multiple supported Minecraft instances are running,
pass `--pid <process-id>`.
