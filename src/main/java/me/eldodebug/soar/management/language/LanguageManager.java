package me.eldodebug.soar.management.language;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import me.eldodebug.soar.logger.GlideLogger;

public class LanguageManager {

	private final HashMap<String, String> translateMap = new HashMap<String, String>();
	private Language currentLanguage;

	public LanguageManager() {
		setCurrentLanguage(Language.ENGLISH);
	}

	private void loadMap(HashMap<String, String> map, String language) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				LanguageManager.class.getClassLoader().getResourceAsStream("assets/minecraft/soar/language/" + language + ".properties"),
				StandardCharsets.UTF_8))) {

			String line;
			while((line = reader.readLine()) != null) {
				line = line.trim();
				if(line.isEmpty() || line.startsWith("#")) {
					continue;
				}

				int separatorIndex = line.indexOf('=');
				if(separatorIndex <= 0) {
					continue;
				}

				String key = line.substring(0, separatorIndex).trim();
				String value = line.substring(separatorIndex + 1);
				map.put(key, value);
			}
		} catch(Exception e) {
			GlideLogger.error("Failed to load translate", e);
		}
	}

	public Language getCurrentLanguage() {
		return currentLanguage;
	}

	public void setCurrentLanguage(Language currentLanguage) {
		this.currentLanguage = currentLanguage;
		translateMap.clear();

		// Base fallback language is English (US).
		loadMap(translateMap, Language.ENGLISH.getId());
		loadMap(translateMap, currentLanguage.getId());

		for(TranslateText text : TranslateText.values()) {
			text.setText(translateMap.getOrDefault(text.getKey(), text.getKey()));
		}
	}
}
