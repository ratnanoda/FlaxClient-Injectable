package me.eldodebug.soar.management.language;

import me.eldodebug.soar.utils.animation.simple.SimpleAnimation;
import net.minecraft.util.ResourceLocation;

public enum Language {
	JAPANESE("ja-jp", "Japanese (Japan)", new ResourceLocation("soar/flag/ja.png")),
	CHINESE("zh-cn", "Chinese (China)", new ResourceLocation("soar/flag/cn.png")),
	ENGLISHGB("en-gb", "English (United Kingdom)", new ResourceLocation("soar/flag/gb.png")),
	ENGLISH("en-us", "English (United States)", new ResourceLocation("soar/flag/us.png")),
	FRENCH("fr-fr", "French (France)", new ResourceLocation("soar/flag/fr.png")),
	SPANISH("es-es", "Spanish (Spain)", new ResourceLocation("soar/flag/es.png")),
	VIETNAMESE("vi-vn", "Vietnamese (Vietnam)", new ResourceLocation("soar/flag/vn.png")),
	RUSSIAN("ru-ru", "Russian (Russia)", new ResourceLocation("soar/flag/ru.png")),
	PORTUGESE("pt-pt", "Portuguese (Portugal)", new ResourceLocation("soar/flag/pt.png")),
	PERSIAN("fa-ir", "Persian (Iran)", new ResourceLocation("soar/flag/ir.png")),
	LOLCAT("lc-koc", "LOLCAT (Kinduim ov catos)", new ResourceLocation("soar/flag/koc.png"));

	private final SimpleAnimation animation = new SimpleAnimation();
	private final String id;
	private final String nameTranslate;
	private final ResourceLocation flag;

	private Language(String id, String nameTranslate, ResourceLocation flag) {
		this.id = id;
		this.nameTranslate = nameTranslate;
		this.flag = flag;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return nameTranslate;
	}

	public ResourceLocation getFlag() {
		return flag;
	}

	public SimpleAnimation getAnimation() {
		return animation;
	}

	public String getNameTranslate() {
		return nameTranslate;
	}

	public static Language getLanguageById(String id) {
		for(Language lang : Language.values()) {
			if(lang.getId().equals(id)) {
				return lang;
			}
		}

		return Language.ENGLISH;
	}
}
