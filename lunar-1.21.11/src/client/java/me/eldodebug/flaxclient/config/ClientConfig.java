package me.eldodebug.flaxclient.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class ClientConfig {
    private static final String HUD_ENABLED_KEY = "hud.enabled";
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("flaxclient")
            .resolve("client.properties");

    private boolean hudEnabled;

    private ClientConfig(boolean hudEnabled) {
        this.hudEnabled = hudEnabled;
    }

    public static ClientConfig load() {
        Properties properties = new Properties();

        if (Files.isRegularFile(CONFIG_PATH)) {
            try (InputStream input = Files.newInputStream(CONFIG_PATH)) {
                properties.load(input);
            } catch (IOException ignored) {
                // Fall back to defaults. The next successful save repairs the file.
            }
        }

        return new ClientConfig(Boolean.parseBoolean(properties.getProperty(HUD_ENABLED_KEY, "true")));
    }

    public boolean hudEnabled() {
        return hudEnabled;
    }

    public void toggleHud() {
        hudEnabled = !hudEnabled;
    }

    public void save() {
        Properties properties = new Properties();
        properties.setProperty(HUD_ENABLED_KEY, Boolean.toString(hudEnabled));

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Path temporary = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "FlaxClient 1.21.11 settings");
            }

            try {
                Files.move(temporary, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailure) {
                Files.move(temporary, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            // Configuration persistence must not crash the client.
        }
    }
}
