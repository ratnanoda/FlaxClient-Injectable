package me.eldodebug.flax.management.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.eldodebug.flax.FlaxClient;
import me.eldodebug.flax.core.Glide;
import me.eldodebug.flax.management.file.FileManager;
import me.eldodebug.flax.management.mods.Mod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ProfileManager {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final File profileFile;

	public ProfileManager(FileManager fileManager) {
		profileFile = new File(fileManager.getProfileDir(), "Default-1.21.11.json");
	}

	public void loadActiveProfile() {
		if (!profileFile.exists()) {
			if (migrateLegacyProfile()) {
				saveActiveProfile();
			}
			return;
		}

		try (FileReader reader = new FileReader(profileFile, StandardCharsets.UTF_8)) {
			JsonObject root = GSON.fromJson(reader, JsonObject.class);
			if (root == null || !root.has("modules")) {
				return;
			}
			JsonObject modules = root.getAsJsonObject("modules");
			for (Mod mod : Glide.getInstance().getModManager().getMods()) {
				if (modules.has(mod.getId())) {
					mod.setToggled(modules.get(mod.getId()).getAsBoolean());
				}
			}
		} catch (Exception error) {
			FlaxClient.LOGGER.error("Failed to load profile {}", profileFile.getName(), error);
		}
	}

	private boolean migrateLegacyProfile() {
		File legacyProfile = new File(profileFile.getParentFile(), "Default.json");
		if (!legacyProfile.exists()) {
			return false;
		}

		try (FileReader reader = new FileReader(legacyProfile, StandardCharsets.UTF_8)) {
			JsonObject root = GSON.fromJson(reader, JsonObject.class);
			if (root == null || !root.has("Mods")) {
				return false;
			}

			JsonObject legacyMods = root.getAsJsonObject("Mods");
			Map<String, Boolean> legacyToggles = new HashMap<>();
			for (Map.Entry<String, JsonElement> entry : legacyMods.entrySet()) {
				if (!entry.getValue().isJsonObject()) {
					continue;
				}
				JsonObject module = entry.getValue().getAsJsonObject();
				if (!module.has("Toggle")) {
					continue;
				}
				legacyToggles.put(
						normalizeLegacyKey(entry.getKey()),
						module.get("Toggle").getAsBoolean());
			}

			boolean migratedAny = false;
			for (Mod mod : Glide.getInstance().getModManager().getMods()) {
				String lookup = legacyLookupKey(mod.getId());
				Boolean toggled = legacyToggles.get(lookup);
				if (toggled == null) {
					continue;
				}
				mod.setToggled(toggled);
				migratedAny = true;
			}

			if (migratedAny) {
				FlaxClient.LOGGER.info("Migrated legacy module toggles from {}", legacyProfile.getName());
			}
			return migratedAny;
		} catch (Exception error) {
			FlaxClient.LOGGER.error("Failed to migrate legacy profile {}", legacyProfile.getName(), error);
			return false;
		}
	}

	private static String legacyLookupKey(String modId) {
		return switch (modId) {
			case "internal_settings" -> "none";
			case "server_ip_display" -> "serverip";
			default -> normalizeAlphaNum(modId);
		};
	}

	private static String normalizeLegacyKey(String rawKey) {
		String key = rawKey.toLowerCase(Locale.ROOT);
		if (key.startsWith("text.")) {
			key = key.substring("text.".length());
		}
		if (key.endsWith(".name")) {
			key = key.substring(0, key.length() - ".name".length());
		}
		return normalizeAlphaNum(key);
	}

	private static String normalizeAlphaNum(String value) {
		StringBuilder normalized = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char current = value.charAt(i);
			if (current >= 'a' && current <= 'z') {
				normalized.append(current);
			} else if (current >= '0' && current <= '9') {
				normalized.append(current);
			}
		}
		return normalized.toString();
	}

	public void saveActiveProfile() {
		JsonObject root = new JsonObject();
		JsonObject modules = new JsonObject();
		for (Mod mod : Glide.getInstance().getModManager().getMods()) {
			modules.addProperty(mod.getId(), mod.isToggled());
		}
		root.add("modules", modules);
		root.addProperty("version", "1.21.11");

		try (FileWriter writer = new FileWriter(profileFile, StandardCharsets.UTF_8)) {
			GSON.toJson(root, writer);
		} catch (Exception error) {
			FlaxClient.LOGGER.error("Failed to save profile {}", profileFile.getName(), error);
		}
	}
}
