package me.eldodebug.flax.management.mods.impl;

import me.eldodebug.flax.management.event.EventTarget;
import me.eldodebug.flax.management.event.impl.EventRender2D;
import me.eldodebug.flax.management.mods.Mod;
import me.eldodebug.flax.management.mods.ModCategory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class ClockMod extends Mod {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

	public ClockMod() {
		super("clock", "Clock", "Shows local system time", ModCategory.HUD);
	}

	@EventTarget
	public void onRender(EventRender2D event) {
		String now = LocalTime.now().format(FORMATTER);
		HudRenderUtil.drawHudLine(event.getContext(), mc, 4, 88, "Time: " + now, 0xFF26A69A, 0xFFFFFFFF);
	}
}
