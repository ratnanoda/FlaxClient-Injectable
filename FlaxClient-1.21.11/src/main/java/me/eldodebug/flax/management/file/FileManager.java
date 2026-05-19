package me.eldodebug.flax.management.file;

import me.eldodebug.flax.FlaxClient;
import net.minecraft.client.MinecraftClient;

import java.io.File;

public final class FileManager {

	private final File glideDir;
	private final File profileDir;
	private final File cacheDir;

	public FileManager() {
		File gameDir = MinecraftClient.getInstance().runDirectory;
		File soarDir = new File(gameDir, "soar");
		glideDir = new File(gameDir, "glide");

		if (!glideDir.exists() && soarDir.exists()) {
			if (!soarDir.renameTo(glideDir)) {
				glideDir.mkdirs();
			}
		} else if (!glideDir.exists()) {
			glideDir.mkdirs();
		}

		profileDir = new File(glideDir, "profile");
		cacheDir = new File(glideDir, "cache");
		profileDir.mkdirs();
		cacheDir.mkdirs();

		FlaxClient.LOGGER.info("Using Flax data directory: {}", glideDir.getAbsolutePath());
	}

	public File getGlideDir() {
		return glideDir;
	}

	public File getProfileDir() {
		return profileDir;
	}

	public File getCacheDir() {
		return cacheDir;
	}
}
