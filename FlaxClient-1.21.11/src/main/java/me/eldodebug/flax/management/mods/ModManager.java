package me.eldodebug.flax.management.mods;

import me.eldodebug.flax.management.mods.impl.FPSDisplayMod;
import me.eldodebug.flax.management.mods.impl.FullbrightMod;
import me.eldodebug.flax.management.mods.impl.InternalSettingsMod;
import me.eldodebug.flax.management.mods.impl.AutoClickerMod;
import me.eldodebug.flax.management.mods.impl.CPSDisplayMod;
import me.eldodebug.flax.management.mods.impl.ClockMod;
import me.eldodebug.flax.management.mods.impl.CoordsMod;
import me.eldodebug.flax.management.mods.impl.MemoryUsageMod;
import me.eldodebug.flax.management.mods.impl.PingDisplayMod;
import me.eldodebug.flax.management.mods.impl.ReachDisplayMod;
import me.eldodebug.flax.management.mods.impl.SpeedometerMod;
import me.eldodebug.flax.management.mods.impl.ToggleSneakMod;
import me.eldodebug.flax.management.mods.impl.ToggleSprintMod;
import me.eldodebug.flax.management.mods.impl.ZoomMod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class ModManager {

	private final List<Mod> mods = new ArrayList<>();
	private final Map<String, Mod> modsById = new HashMap<>();

	public void init() {
		Map<String, Supplier<Mod>> factories = new HashMap<>();
		factories.put("fullbright", FullbrightMod::new);
		factories.put("toggle_sprint", ToggleSprintMod::new);
		factories.put("toggle_sneak", ToggleSneakMod::new);
		factories.put("fps_display", FPSDisplayMod::new);
		factories.put("cps_display", CPSDisplayMod::new);
		factories.put("coords", CoordsMod::new);
		factories.put("speedometer", SpeedometerMod::new);
		factories.put("clock", ClockMod::new);
		factories.put("ping_display", PingDisplayMod::new);
		factories.put("reach_display", ReachDisplayMod::new);
		factories.put("memory_usage", MemoryUsageMod::new);
		factories.put("auto_clicker", AutoClickerMod::new);
		factories.put("zoom", ZoomMod::new);
		factories.put("internal_settings", InternalSettingsMod::new);

		for (ModDefinition definition : ModCatalog.definitions()) {
			Supplier<Mod> factory = factories.get(definition.id());
			Mod mod = factory != null ? factory.get() : new StubMod(definition);
			mods.add(mod);
			modsById.put(definition.id(), mod);
		}

		modsById.get("internal_settings").setToggled(true);
	}

	public List<Mod> getMods() {
		return mods;
	}

	public Mod getMod(String id) {
		return modsById.get(id);
	}

	public void disableAll() {
		for (Mod mod : mods) {
			if (!"internal_settings".equals(mod.getId())) {
				mod.setToggled(false);
			}
		}
		InternalSettingsMod.getInstance().setToggled(true);
	}
}
