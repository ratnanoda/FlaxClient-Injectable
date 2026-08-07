# FlaxClient attach runtime

This directory contains the Windows x64 DLL that bridges an already-running
Lunar Client 26.1.2 JVM to the FlaxClient Java code. The legacy 1.8.9 bridge is
kept in the common runtime for backwards-compatible development builds.

The DLL establishes the native-to-JVM bridge, adds the embedded client jar to
Lunar's Java 25 class loader, installs schema-preserving JVMTI hooks, and starts
the version adapter on Minecraft's main thread.

This target supports Lunar Client 26.1.2 x64. It does not bypass or disable
launcher or anti-cheat protections.

## Build

Build the single-file DLL from the repository root:

```powershell
.\.tooling\gradle-4.10.3\gradle-4.10.3\bin\gradle.bat attachPackage
```

The reobfuscated client jar is embedded into
`build/attach/FlaxClient.dll`; no sidecar jar is required. Native attach
diagnostics are written to `%TEMP%\FlaxClient\attach.log`.

Inject only after the Lunar Client 26.1.2 main menu has appeared. The DLL and
the target JVM must both be x64. Loading the DLL more than once is ignored by
the Java bootstrap.

Run `FlaxInjector.exe` from the same directory as `FlaxClient.dll`. It
automatically selects a visible Minecraft Java process. If multiple Minecraft
instances are running, pass `--pid <process-id>`.
