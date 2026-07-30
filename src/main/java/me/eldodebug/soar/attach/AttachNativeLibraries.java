package me.eldodebug.soar.attach;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Function;

import org.lwjgl.system.Configuration;

/**
 * Extracts the embedded LWJGL 3 natives before any LWJGL 3 class initializes.
 * Minecraft 1.8.9 already has LWJGL 2 natives, so the two sets deliberately
 * live in separate directories.
 */
final class AttachNativeLibraries {

    private static final String RESOURCE_ROOT = "/windows/x64/org/lwjgl/";
    private static final String[] LIBRARIES = {
            "lwjgl.dll",
            "nanovg/lwjgl_nanovg.dll",
            "stb/lwjgl_stb.dll"
    };
    private static volatile boolean prepared;

    private AttachNativeLibraries() {
    }

    static synchronized void prepare() throws IOException {
        if (prepared) {
            return;
        }

        Path directory = new File(
                new File(System.getProperty("java.io.tmpdir"), "FlaxClient"),
                "natives-" + processId()).toPath();
        Files.createDirectories(directory);

        for (String resourceName : LIBRARIES) {
            String fileName = resourceName.substring(resourceName.lastIndexOf('/') + 1);
            Path destination = directory.resolve(fileName);
            try (InputStream input = AttachNativeLibraries.class.getResourceAsStream(
                    RESOURCE_ROOT + resourceName)) {
                if (input == null) {
                    throw new IOException(
                            "Embedded native library is missing: " + resourceName);
                }
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        String nativePath = directory.toAbsolutePath().toString();
        System.setProperty("org.lwjgl.librarypath", nativePath);
        // Minecraft's LWJGL 2 launcher may have initialized a similarly named
        // property before the attach. Update LWJGL 3's live configuration too.
        Configuration.LIBRARY_PATH.set(nativePath);
        Configuration.LIBRARY_NAME.set(
                directory.resolve("lwjgl.dll").toAbsolutePath().toString());
        Configuration.BUNDLED_LIBRARY_NAME_MAPPER.set(
                new Function<String, String>() {
                    @Override
                    public String apply(String libraryName) {
                        String mappedName = System.mapLibraryName(libraryName);
                        Path extracted = directory.resolve(mappedName);
                        return Files.isRegularFile(extracted)
                                ? extracted.toAbsolutePath().toString()
                                : mappedName;
                    }
                });
        String javaLibraryPath = System.getProperty("java.library.path", "");
        System.setProperty(
                "java.library.path",
                nativePath + File.pathSeparator + javaLibraryPath);
        prepared = true;
    }

    private static String processId() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        int separator = runtimeName.indexOf('@');
        String value = separator < 0 ? runtimeName : runtimeName.substring(0, separator);
        return value.matches("[0-9]+") ? value : "current";
    }
}
