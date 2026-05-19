package me.eldodebug.flax.management.mods.impl;

import me.eldodebug.flax.management.event.EventTarget;
import me.eldodebug.flax.management.event.impl.EventRender2D;
import me.eldodebug.flax.management.mods.Mod;
import me.eldodebug.flax.management.mods.ModCategory;

public final class MemoryUsageMod extends Mod {

	public MemoryUsageMod() {
		super("memory_usage", "Memory Usage", "Shows Java heap usage", ModCategory.HUD);
	}

	@EventTarget
	public void onRender(EventRender2D event) {
		Runtime runtime = Runtime.getRuntime();
		long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
		long maxMb = runtime.maxMemory() / (1024L * 1024L);
		String text = "Memory: " + usedMb + " / " + maxMb + " MB";
		HudRenderUtil.drawHudLine(event.getContext(), mc, 4, 102, text, 0xFFAB47BC, 0xFFFFFFFF);
	}
}
