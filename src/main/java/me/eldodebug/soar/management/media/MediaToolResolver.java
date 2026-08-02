package me.eldodebug.soar.management.media;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import me.eldodebug.soar.attach.DejectBridge;
import me.eldodebug.soar.logger.GlideLogger;
import net.minecraft.client.Minecraft;

/** Finds packaged media tools without requiring the player to edit PATH. */
public final class MediaToolResolver {

    private MediaToolResolver() {
    }

    public static String resolve(String tool, String environmentKey) {
        String configured = System.getenv(environmentKey);
        if(configured != null && !configured.trim().isEmpty()) {
            File configuredFile = new File(configured.trim());
            return configuredFile.isFile()
                    ? logResolved(tool, configuredFile.getAbsolutePath())
                    : configured.trim();
        }

        boolean windows = System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT).contains("win");
        String executable = windows ? tool + ".exe" : tool;
        List<File> candidates = new ArrayList<File>();

        addRuntimeCandidates(candidates, DejectBridge.getModuleDirectory(), executable);
        addRuntimeCandidates(candidates, System.getProperty("user.dir"), executable);

        try {
            File gameDirectory = Minecraft.getMinecraft().mcDataDir;
            candidates.add(new File(gameDirectory, "tools/" + executable));
            candidates.add(new File(gameDirectory, executable));
        } catch(Throwable ignored) {
        }

        String localAppData = System.getenv("LOCALAPPDATA");
        if(localAppData != null && !localAppData.trim().isEmpty()) {
            candidates.add(new File(localAppData, "FlaxClient/tools/" + executable));
        }

        String appData = System.getenv("APPDATA");
        if(appData != null && !appData.trim().isEmpty()) {
            candidates.add(new File(appData, "FlaxClient/tools/" + executable));
        }

        for(File candidate : candidates) {
            if(candidate.isFile()) {
                return logResolved(tool, candidate.getAbsolutePath());
            }
        }

        String fromPath = findOnPath(executable, windows);
        if(fromPath != null) {
            return logResolved(tool, fromPath);
        }

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

    private static void addRuntimeCandidates(List<File> candidates, String base, String executable) {
        if(base == null || base.trim().isEmpty()) {
            return;
        }
        File directory = new File(base.trim());
        candidates.add(new File(directory, "tools/" + executable));
        candidates.add(new File(directory, "bin/" + executable));
        candidates.add(new File(directory, executable));
    }

    public static boolean canRun(String command, String argument) {
        Process process = null;
        try {
            process = new ProcessBuilder(command, argument)
                    .redirectErrorStream(true).start();
            drain(process.getInputStream());
            return process.waitFor() == 0;
        } catch(Exception ignored) {
            return false;
        } finally {
            if(process != null && process.isAlive()) {
                try {
                    process.destroyForcibly();
                } catch(Exception ignored) {
                }
            }
        }
    }

    public static String containingDirectory(String command) {
        if(command == null) {
            return "";
        }
        File file = new File(command);
        if(file.isFile() && file.getParentFile() != null) {
            return file.getParentFile().getAbsolutePath();
        }
        return command;
    }

    private static String findOnPath(String executable, boolean windows) {
        try {
            Process process = new ProcessBuilder(windows ? "where.exe" : "which", executable)
                    .redirectErrorStream(true).start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
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
        if(directory == null || depth < 0 || !directory.isDirectory()) {
            return;
        }
        File[] children = directory.listFiles();
        if(children == null) {
            return;
        }
        for(File child : children) {
            if(child.isFile() && child.getName().equalsIgnoreCase(name)) {
                results.add(child);
            } else if(child.isDirectory() && depth > 0) {
                findRecursively(child, name, depth - 1, results);
            }
        }
    }

    private static void drain(InputStream input) {
        try {
            byte[] buffer = new byte[2048];
            while(input.read(buffer) >= 0) {
            }
        } catch(Exception ignored) {
        }
    }

    private static String logResolved(String tool, String path) {
        GlideLogger.info("Resolved " + tool + " at " + path);
        return path;
    }
}
