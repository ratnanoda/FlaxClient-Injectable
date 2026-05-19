package me.eldodebug.flax.management.mods.impl;

import me.eldodebug.flax.management.event.EventTarget;
import me.eldodebug.flax.management.event.impl.EventRender2D;
import me.eldodebug.flax.management.event.impl.EventTick;
import me.eldodebug.flax.management.mods.Mod;
import me.eldodebug.flax.management.mods.ModCategory;
import net.minecraft.util.math.Vec3d;

import java.text.DecimalFormat;

public final class SpeedometerMod extends Mod {

	private static final DecimalFormat SPEED_FORMAT = new DecimalFormat("0.00");

	private Vec3d lastPos;
	private double speedMetersPerSecond;

	public SpeedometerMod() {
		super("speedometer", "Speedometer", "Shows horizontal movement speed", ModCategory.HUD);
	}

	@EventTarget
	public void onTick(EventTick event) {
		if (mc.player == null) {
			lastPos = null;
			speedMetersPerSecond = 0.0D;
			return;
		}

		Vec3d now = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
		if (lastPos != null) {
			double dx = now.x - lastPos.x;
			double dz = now.z - lastPos.z;
			double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
			speedMetersPerSecond = horizontalDistance * 20.0D;
		}
		lastPos = now;
	}

	@EventTarget
	public void onRender(EventRender2D event) {
		String text = "Speed: " + SPEED_FORMAT.format(speedMetersPerSecond) + " m/s";
		HudRenderUtil.drawHudLine(event.getContext(), mc, 4, 46, text, 0xFFFFA047, 0xFFFFFFFF);
	}
}
