package me.eldodebug.flax.management.mods.impl;

import me.eldodebug.flax.management.mods.Mod;
import me.eldodebug.flax.management.mods.ModCategory;

public final class InternalSettingsMod extends Mod {

	private static InternalSettingsMod instance;

	public InternalSettingsMod() {
		super("internal_settings", "Internal Settings", "Core client settings", ModCategory.OTHER);
		instance = this;
	}

	public static InternalSettingsMod getInstance() {
		return instance;
	}

	@Override
	public void onDisable() {
		setToggled(true);
	}
}
