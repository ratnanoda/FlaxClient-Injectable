# FlaxClient attach runtime

This directory contains the Windows x64 DLL that bridges an already-running
official Minecraft 1.8.9 or Lunar Client 1.8.9 JVM to the FlaxClient Java code.

The DLL establishes the native-to-JVM bridge, adds the embedded client jar to
the running Java 8/17 class loader, installs schema-preserving JVMTI hooks, and
starts the FlaxClient managers on Minecraft's main thread.

This target supports the official x64 Minecraft 1.8.9 client and Lunar Client
1.8.9. It does not bypass or disable launcher or anti-cheat protections.

## Build

Build the single-file DLL from the repository root:

```powershell
.\.tooling\gradle-4.10.3\gradle-4.10.3\bin\gradle.bat attachPackage
```

The reobfuscated client jar is embedded into
`build/attach/FlaxClient.dll`; no sidecar jar is required. Native attach
diagnostics are written to `%TEMP%\FlaxClient\attach.log`.

Inject only after the Minecraft 1.8.9 main menu has appeared. The DLL and the
target JVM must both be x64. Loading the DLL more than once is ignored by the
Java bootstrap.

Run `FlaxInjector.exe` from the same directory as `FlaxClient.dll`. It
automatically selects a visible Minecraft Java process. If multiple Minecraft
instances are running, pass `--pid <process-id>`.
