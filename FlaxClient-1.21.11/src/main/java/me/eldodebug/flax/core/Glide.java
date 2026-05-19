package me.eldodebug.flax.core;

import me.eldodebug.flax.FlaxClient;
import me.eldodebug.flax.gui.modmenu.FlaxModMenuScreen;
import me.eldodebug.flax.management.event.EventManager;
import me.eldodebug.flax.management.file.FileManager;
import me.eldodebug.flax.management.mods.ModManager;
import me.eldodebug.flax.management.profile.ProfileManager;
import net.minecraft.client.MinecraftClient;

public final class Glide {

	private static final Glide INSTANCE = new Glide();

	private final MinecraftClient mc = MinecraftClient.getInstance();
	private EventManager eventManager;
	private ModManager modManager;
	private FileManager fileManager;
	private ProfileManager profileManager;
	private boolean started;

	private Glide() {
	}

	public static Glide getInstance() {
		return INSTANCE;
	}

	public void start() {
		if (started) {
			return;
		}
		started = true;

		fileManager = new FileManager();
		eventManager = new EventManager();
		modManager = new ModManager();
		modManager.init();
		profileManager = new ProfileManager(fileManager);
		profileManager.loadActiveProfile();
		GlideHandler.register();

		FlaxClient.LOGGER.info("FlaxClient core started with {} modules.", modManager.getMods().size());
	}

	public void stop() {
		if (!started) {
			return;
		}
		if (profileManager != null) {
			profileManager.saveActiveProfile();
		}
		modManager.disableAll();
		started = false;
	}

	public void openModMenu() {
		mc.setScreen(new FlaxModMenuScreen());
	}

	public MinecraftClient mc() {
		return mc;
	}

	public EventManager getEventManager() {
		return eventManager;
	}

	public ModManager getModManager() {
		return modManager;
	}

	public FileManager getFileManager() {
		return fileManager;
	}

	public ProfileManager getProfileManager() {
		return profileManager;
	}
}
