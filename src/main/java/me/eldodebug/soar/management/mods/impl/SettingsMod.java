package me.eldodebug.soar.management.mods.impl;

import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;

/**
 * User-facing container for global client behaviour that does not belong to a
 * single Ghost module.
 */
public class SettingsMod extends Mod {

	private static SettingsMod instance;

	private final BooleanSetting moveFixSetting = new BooleanSetting(TranslateText.NONE, this, true) {
		@Override
		public String getName() {
			return "MoveFix";
		}

		@Override
		public String getNameKey() {
			return "text.movefix";
		}
	};

	public SettingsMod() {
		super(TranslateText.SETTINGS, TranslateText.NONE, ModCategory.OTHER);
		instance = this;
		setToggled(true);
	}

	/** Settings is a container rather than a feature toggle. */
	@Override
	public void toggle() {
		// Keep the module enabled; its individual options are toggled instead.
	}

	public static SettingsMod getInstance() {
		return instance;
	}

	public BooleanSetting getMoveFixSetting() {
		return moveFixSetting;
	}

	public static boolean isMoveFixEnabled() {
		return instance != null && instance.moveFixSetting.isToggled();
	}
}
