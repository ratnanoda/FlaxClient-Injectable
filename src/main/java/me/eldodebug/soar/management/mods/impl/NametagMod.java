package me.eldodebug.soar.management.mods.impl;

import java.util.ArrayList;
import java.util.Arrays;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventTick;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;

public class NametagMod extends Mod {

	private static NametagMod instance;
	private ComboSetting themeSetting = new ComboSetting(TranslateText.STYLE, this, TranslateText.MODERN, new ArrayList<Option>(Arrays.asList(
			new Option(TranslateText.CLASSIC), new Option(TranslateText.MODERN), new Option(TranslateText.MINIMAL), new Option(TranslateText.OUTLINED))));

	public NametagMod() {
		super(TranslateText.NAMETAG, TranslateText.NAMETAG_DESCRIPTION, ModCategory.PLAYER);
		instance = this;
	}

	public static NametagMod getInstance() {
		return instance;
	}

	@EventTarget
	public void onTick(EventTick event) {
		// keep the GUI-exposed Nametags theme in sync with GhostNametagsMod
		if(GhostNametagsMod.getInstance() != null) {
			ComboSetting ghost = GhostNametagsMod.getInstance().getThemeSetting();
			if(ghost != null && themeSetting != null) {
				// if user changed Nametag setting, apply to ghost nametags
				if(!ghost.getOption().getNameKey().equals(themeSetting.getOption().getNameKey())) {
					ghost.setOption(themeSetting.getOption());
				}
				// if ghost changed (rare), reflect back
				else if(!themeSetting.getOption().getNameKey().equals(ghost.getOption().getNameKey())) {
					themeSetting.setOption(ghost.getOption());
				}
			}
		}
	}
}
