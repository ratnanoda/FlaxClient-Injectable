package me.eldodebug.soar.management.file;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.logger.GlideLogger;
import net.minecraft.client.Minecraft;

public class FileManager {

	private File glideDir, profileDir, cacheDir, screenshotDir, soarDir, customCapeDir, capeCacheDir;
	private boolean migrationSuccess = false;
	private final boolean firstInstallation;
	private boolean beginnerGuidePromptPending;
	
	public FileManager() {
		
		glideDir = new File(Minecraft.getMinecraft().mcDataDir, "glide");
		soarDir = new File(Minecraft.getMinecraft().mcDataDir, "soar");
		profileDir = new File(glideDir, "profile");
		cacheDir = new File(glideDir, "cache");
		screenshotDir = new File(glideDir, "screenshots");
		customCapeDir = new File(cacheDir, "custom-cape");
		capeCacheDir = new File(cacheDir, "cape");

		// Capture this before any migration or directory creation. Existing users,
		// including users of the legacy soar directory, must never be treated as new.
		firstInstallation = !glideDir.exists() && !soarDir.exists();
		beginnerGuidePromptPending = firstInstallation;

		try{

			if(!glideDir.exists()){
				if(soarDir.exists()) {
					migrationSuccess = soarDir.renameTo(glideDir);
					if(!migrationSuccess) createDir(glideDir);
				} else {
					createDir(glideDir);
				}
			}

			if(!profileDir.exists()) createDir(profileDir);
			if(!cacheDir.exists()) createDir(cacheDir);
			if(!screenshotDir.exists()) createDir(screenshotDir);
			if(!customCapeDir.exists()) createDir(customCapeDir);
			if(!capeCacheDir.exists()) createDir(capeCacheDir);

			createVersionFile();

		} catch (Exception e) {
			GlideLogger.error("Something has gone very wrong while trying to create the glide folder which may result in crashes later", e);
		}

	}
	
	private void createVersionFile() {
		
		File versionDir = new File(cacheDir, "version");
		
		createDir(versionDir);
		createFile(new File(versionDir, Glide.getInstance().getVersionIdentifier() + ".tmp"));
	}
	
	public void createDir(File file) {
		file.mkdir();
	}
	
	public void createFile(File file) {
		try {
			file.createNewFile();
		} catch (IOException e) {
			GlideLogger.error("Failed to create file " + file.getName(), e);
		}
	}

	public File getScreenshotDir() {
		return screenshotDir;
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

	public File getCustomCapeDir() {
		return customCapeDir;
	}

	public File getCapeCacheDir() {
		return capeCacheDir;
	}

	public boolean isFirstInstallation() {
		return firstInstallation;
	}

	/**
	 * Returns true exactly once during the first FlaxClient session. The value is
	 * based on whether neither the current nor legacy data directory existed before
	 * FileManager created anything.
	 */
	public synchronized boolean consumeBeginnerGuidePrompt() {
		if(!beginnerGuidePromptPending) return false;
		beginnerGuidePromptPending = false;
		return true;
	}

}
