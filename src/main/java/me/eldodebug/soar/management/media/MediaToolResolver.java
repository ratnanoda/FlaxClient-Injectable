package me.eldodebug.soar.management.media;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import me.eldodebug.soar.logger.GlideLogger;
import net.minecraft.client.Minecraft;

/**
 * Resolves media tools without requiring PATH changes. Windows injectable
 * builds carry yt-dlp and ffmpeg inside the client jar; they are extracted to
 * a persistent per-user tools directory on first use.
 */
public final class MediaToolResolver {

    private static final String BUNDLE_VERSION = "youtube-music-1";
    private static final String WINDOWS_RESOURCE_ROOT =
            "assets/minecraft/soar/tools/windows-x64/";

    private MediaToolResolver() {
    }

    public static String resolve(String tool, String environmentKey) {
        String configured = System.getenv(environmentKey);
        if(configured != null && !configured.trim().isEmpty()) {
            return configured.trim();
        }

        boolean windows = System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT).contains("win");
        String executable = windows ? tool + ".exe" : tool;
        List<File> candidates = new ArrayList<File>();

        File persistentTools = resolvePersistentToolsDirectory();
        if(persistentTools != null) {
            File installed = installBundledTool(tool, executable, persistentTools, windows);
            if(installed != null && installed.isFile()) {
                return logResolved(tool, installed.getAbsolutePath());
            }
            candidates.add(new File(persistentTools, executable));
        }

        try {
            File gameDirectory = Minecraft.getMinecraft().mcDataDir;
            candidates.add(new File(gameDirectory, "tools/" + executable));
        } catch(Throwable ignored) {
        }

        String localAppData = System.getenv("LOCALAPPDATA");
        if(localAppData != null && !localAppData.trim().isEmpty()) {
            candidates.add(new File(localAppData, "FlaxClient/tools/" + executable));
        }

        for(File candidate : candidates) {
            if(candidate.isFile()) return logResolved(tool, candidate.getAbsolutePath());
        }

        String fromPath = findOnPath(executable, windows);
        if(fromPath != null) return logResolved(tool, fromPath);

        File downloads = new File(System.getProperty("user.home", "."), "Downloads");
        List<File> matches = new ArrayList<File>();
        findRecursively(downloads, executable, 3, matches);
        if(!matches.isEmpty()) {
            Collections.sort(matches, new Comparator<File>() {
                @Override
                public int compare(File first, File second) {
                    return Long.compare(second.lastModified(), first.lastModified());
                }
            });
            return logResolved(tool, matches.get(0).getAbsolutePath());
        }

        GlideLogger.warn("Unable to locate media tool " + executable);
        return tool;
    }

    private static File resolvePersistentToolsDirectory() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if(localAppData != null && !localAppData.trim().isEmpty()) {
            return new File(localAppData, "FlaxClient/tools");
        }
        try {
            return new File(Minecraft.getMinecraft().mcDataDir, "tools");
        } catch(Throwable ignored) {
            return new File(System.getProperty("user.home", "."), ".flaxclient/tools");
        }
    }

    private static File installBundledTool(String tool, String executable,
            File toolsDirectory, boolean windows) {
        if(!windows || !("yt-dlp".equals(tool) || "ffmpeg".equals(tool))) return null;
        if(!toolsDirectory.exists() && !toolsDirectory.mkdirs()) return null;

        File destination = new File(toolsDirectory, executable);
        File marker = new File(toolsDirectory, "." + tool + "-bundle-version");
        if(destination.isFile() && destination.length() > 0L
                && BUNDLE_VERSION.equals(readFirstLine(marker))) {
            return destination;
        }

        try {
            if("yt-dlp".equals(tool)) {
                installDirectResource(WINDOWS_RESOURCE_ROOT + "yt-dlp.exe", destination);
            } else {
                installZippedExecutable(WINDOWS_RESOURCE_ROOT + "ffmpeg.zip",
                        "ffmpeg.exe", destination);
            }
            writeMarker(marker, BUNDLE_VERSION);
            GlideLogger.info("Installed bundled " + tool + " to " + destination.getAbsolutePath());
            return destination;
        } catch(Exception e) {
            GlideLogger.error("Unable to install bundled media tool " + tool, e);
            return destination.isFile() ? destination : null;
        }
    }

    private static void installDirectResource(String resourcePath, File destination)
            throws Exception {
        InputStream resource = MediaToolResolver.class.getClassLoader()
                .getResourceAsStream(resourcePath);
        if(resource == null) throw new IllegalStateException("Missing resource " + resourcePath);
        File temporary = new File(destination.getParentFile(), destination.getName() + ".part");
        try(InputStream input = new BufferedInputStream(resource);
                FileOutputStream output = new FileOutputStream(temporary)) {
            copy(input, output);
        }
        replaceFile(temporary, destination);
    }

    private static void installZippedExecutable(String resourcePath, String expectedName,
            File destination) throws Exception {
        InputStream resource = MediaToolResolver.class.getClassLoader()
                .getResourceAsStream(resourcePath);
        if(resource == null) throw new IllegalStateException("Missing resource " + resourcePath);
        File temporary = new File(destination.getParentFile(), destination.getName() + ".part");
        boolean found = false;
        try(ZipInputStream zip = new ZipInputStream(new BufferedInputStream(resource))) {
            ZipEntry entry;
            while((entry = zip.getNextEntry()) != null) {
                if(entry.isDirectory()) continue;
                String fileName = new File(entry.getName()).getName();
                if(!expectedName.equalsIgnoreCase(fileName)) continue;
                try(FileOutputStream output = new FileOutputStream(temporary)) {
                    copy(zip, output);
                }
                found = true;
                break;
            }
        }
        if(!found) {
            temporary.delete();
            throw new IllegalStateException(expectedName + " was not found in " + resourcePath);
        }
        replaceFile(temporary, destination);
    }

    private static void replaceFile(File temporary, File destination) throws Exception {
        if(destination.exists() && !destination.delete()) {
            throw new IllegalStateException("Unable to replace " + destination);
        }
        if(!temporary.renameTo(destination)) {
            try(FileInputStream input = new FileInputStream(temporary);
                    FileOutputStream output = new FileOutputStream(destination)) {
                copy(input, output);
            }
            temporary.delete();
        }
        destination.setExecutable(true, false);
    }

    private static void copy(InputStream input, FileOutputStream output) throws Exception {
        byte[] buffer = new byte[64 * 1024];
        int read;
        while((read = input.read(buffer)) != -1) {
            if(read > 0) output.write(buffer, 0, read);
        }
        output.flush();
    }

    private static String readFirstLine(File file) {
        if(!file.isFile()) return null;
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), "UTF-8"))) {
            return reader.readLine();
        } catch(Exception ignored) {
            return null;
        }
    }

    private static void writeMarker(File file, String value) {
        try(PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(file), "UTF-8"))) {
            writer.println(value);
        } catch(Exception ignored) {
        }
    }

    private static String findOnPath(String executable, boolean windows) {
        try {
            Process process = new ProcessBuilder(windows ? "where.exe" : "which", executable)
                    .redirectErrorStream(true).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String result = reader.readLine();
            if(process.waitFor() == 0 && result != null && !result.trim().isEmpty()) {
                return result.trim();
            }
        } catch(Exception ignored) {
        }
        return null;
    }

    private static void findRecursively(File directory, String name, int depth,
            List<File> results) {
        if(directory == null || depth < 0 || !directory.isDirectory()) return;
        File[] children = directory.listFiles();
        if(children == null) return;
        for(File child : children) {
            if(child.isFile() && child.getName().equalsIgnoreCase(name)) {
                results.add(child);
            } else if(child.isDirectory() && depth > 0) {
                findRecursively(child, name, depth - 1, results);
            }
        }
    }

    private static String logResolved(String tool, String path) {
        GlideLogger.info("Resolved " + tool + " at " + path);
        return path;
    }
}
