package me.eldodebug.flax.management.mods.impl;

import me.eldodebug.flax.management.event.EventTarget;
import me.eldodebug.flax.management.event.impl.EventRender2D;
import me.eldodebug.flax.management.mods.Mod;
import me.eldodebug.flax.management.mods.ModCategory;

public final class FPSDisplayMod extends Mod {

	public FPSDisplayMod() {
		super("fps_display", "FPS Display", "Shows current FPS", ModCategory.HUD);
	}

	@EventTarget
	public void onRender(EventRender2D event) {
		int fps = mc.getCurrentFps();
		HudRenderUtil.drawHudLine(event.getContext(), mc, 4, 4, "FPS: " + fps, 0xFF4DD0E1, 0xFFFFFFFF);
	}
}
