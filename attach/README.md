# FlaxClient attach runtime

This directory contains the Windows x64 attach runtime that bridges an already-running
Minecraft 1.8.9 JVM to the FlaxClient Java code.

The DLL establishes the native-to-JVM bridge, adds an embedded client jar to
the running Java class loader, installs schema-preserving JVMTI hooks, and
starts the FlaxClient managers on Minecraft's main thread.

The packaged build supports the existing Lunar Client 1.8.9 path and adds a
Dawn/Feather-style 1.8.9 path. Lunar uses the MCP/readable runtime jar. Dawn is
selected when the loaded `net.minecraft.client.Minecraft` class exposes SRG
members such as `field_71439_g`, and receives the ForgeGradle-reobfuscated SRG
jar instead. Both jars are embedded into the same DLL and selected at runtime.
This compatibility path does not bypass or disable launcher or anti-cheat
protections.

## Build

Build from the repository root on Windows x64 with a JDK 8 `JAVA_HOME`, Visual
Studio C++ build tools, and CMake available:

```powershell
.\.tooling\gradle-4.10.3\gradle-4.10.3\bin\gradle.bat clean attachPackage
```

If you normally use the Gradle wrapper and it works in your environment, the
equivalent command is:

```powershell
.\gradlew.bat clean attachPackage
```

The build creates both runtime variants, embeds them into `FlaxClient.dll`, and
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
