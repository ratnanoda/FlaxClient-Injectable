package me.eldodebug.flax.management.mods.impl;

import me.eldodebug.flax.management.event.EventTarget;
import me.eldodebug.flax.management.event.impl.EventTick;
import me.eldodebug.flax.management.mods.Mod;
import me.eldodebug.flax.management.mods.ModCategory;
import org.lwjgl.glfw.GLFW;

public final class ZoomMod extends Mod {

	private static final int ZOOM_KEY = GLFW.GLFW_KEY_C;
	private static volatile float fovMultiplier = 1.0F;

	private boolean zooming;
	private double baseSensitivity = -1.0D;
	private float visualFactor = 1.0F;

	public ZoomMod() {
		super("zoom", "Zoom", "Hold C to zoom", ModCategory.PLAYER);
	}

	@EventTarget
	public void onTick(EventTick event) {
		if (mc.player == null) {
			resetZoomState();
			return;
		}

		boolean keyDown = GLFW.glfwGetKey(mc.getWindow().getHandle(), ZOOM_KEY) == GLFW.GLFW_PRESS;
		if (keyDown) {
			if (!zooming) {
				zooming = true;
				baseSensitivity = mc.options.getMouseSensitivity().getValue();
			}

			float targetFactor = 1.0F / 4.0F;
			visualFactor += (targetFactor - visualFactor) * 0.22F;
			fovMultiplier = visualFactor;

			if (baseSensitivity > 0.0D) {
				double zoomedSensitivity = Math.max(0.01D, baseSensitivity * visualFactor * 1.45D);
				mc.options.getMouseSensitivity().setValue(zoomedSensitivity);
			}
			return;
		}

		if (zooming) {
			resetZoomState();
		}
	}

	@Override
	public void onDisable() {
		super.onDisable();
		resetZoomState();
	}

	private void resetZoomState() {
		if (zooming && baseSensitivity > 0.0D) {
			mc.options.getMouseSensitivity().setValue(baseSensitivity);
		}
		zooming = false;
		baseSensitivity = -1.0D;
		visualFactor = 1.0F;
		fovMultiplier = 1.0F;
	}

	public static float getFovMultiplier() {
		return fovMultiplier;
	}
}
