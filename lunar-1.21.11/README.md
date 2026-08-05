# FlaxClient for Lunar / Minecraft 1.21.11

This directory is a side-by-side Fabric port. It does not modify or replace the existing Minecraft 1.8.9 injector build.

## Current milestone

- Java 21 and Minecraft 1.21.11 build configuration
- Fabric Loader/Fabric API entrypoint compatible with Lunar's Fabric mod loading path
- Persistent client configuration under `config/flaxclient/client.properties`
- Minimal compatibility HUD
- Right Shift key binding to toggle that HUD
- GitHub Actions build and artifact upload

## Migration status

The original 1.8.9 event, rendering, GUI, input, and profile systems cannot be binary-reused on 1.21.11. They must be ported module by module.

Next candidates for direct migration are the settings/profile framework, mod menu, Break Progress, Health Bar, Ghost Freelook, Ghost Nametags, and YouTube PiP. Modules that automate input or reveal server-side competitive information are intentionally outside this bootstrap and require a separate rules/compatibility review before implementation.

## Build

From the repository root:

```text
gradle -p lunar-1.21.11 build
```

The remapped client mod is written to `lunar-1.21.11/build/libs/`.

## Install in Lunar Client

Select Minecraft 1.21.11, enable the Fabric add-on, open the profile's Mods page, and add the generated non-sources JAR.
