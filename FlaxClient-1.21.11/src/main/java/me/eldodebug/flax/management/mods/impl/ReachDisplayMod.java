package me.eldodebug.flax.management.mods.impl;

import me.eldodebug.flax.management.event.EventTarget;
import me.eldodebug.flax.management.event.impl.EventRender2D;
import me.eldodebug.flax.management.mods.Mod;
import me.eldodebug.flax.management.mods.ModCategory;
import net.minecraft.util.hit.HitResult;

import java.text.DecimalFormat;

public final class ReachDisplayMod extends Mod {

	private static final DecimalFormat REACH_FORMAT = new DecimalFormat("0.00");

	public ReachDisplayMod() {
		super("reach_display", "Reach Display", "Shows current crosshair reach distance", ModCategory.HUD);
	}

	@EventTarget
	public void onRender(EventRender2D event) {
		if (mc.player == null || mc.crosshairTarget == null) {
			return;
		}

		HitResult hit = mc.crosshairTarget;
		double distance = hit.getPos().distanceTo(mc.player.getEyePos());
		HudRenderUtil.drawHudLine(
				event.getContext(),
				mc,
				4,
				74,
				"Reach: " + REACH_FORMAT.format(distance) + " blocks",
				0xFFEF5350,
				0xFFFFFFFF);
	}
}
