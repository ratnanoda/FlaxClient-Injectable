package me.eldodebug.soar.management.language;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import me.eldodebug.soar.logger.GlideLogger;

public class LanguageManager {

	private final HashMap<String, String> translateMap = new HashMap<String, String>();
	private final HashMap<String, String> baseTranslateMap = new HashMap<String, String>();
	private Language currentLanguage;

	public LanguageManager() {
		setCurrentLanguage(Language.ENGLISH);
	}

	private void loadMap(HashMap<String, String> map, String language) {
		String resourcePath = "assets/minecraft/soar/language/" + language + ".properties";
		InputStream stream = LanguageManager.class.getClassLoader().getResourceAsStream(resourcePath);
		if(stream == null) {
			GlideLogger.error("Language resource was not found: " + resourcePath);
			return;
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {

			String line;
			while((line = reader.readLine()) != null) {
				// UTF-8 property files created by some Windows editors begin with a
				// BOM. String.trim() does not remove it, which made the first key
				// impossible to resolve and left the raw translation key on screen.
				if(!line.isEmpty() && line.charAt(0) == '\uFEFF') {
					line = line.substring(1);
				}
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
		baseTranslateMap.clear();

		// Base fallback language is English (US).
		loadMap(baseTranslateMap, Language.ENGLISH.getId());
		translateMap.putAll(baseTranslateMap);
		loadMap(translateMap, currentLanguage.getId());

		for(TranslateText text : TranslateText.values()) {
			text.setText(translateMap.getOrDefault(text.getKey(), text.getKey()));
		}
	}

	/**
	 * Returns the stable English label used for locale-independent ordering.
	 */
	public String getBaseTranslation(String key) {
		return baseTranslateMap.getOrDefault(key, key);
	}
}
