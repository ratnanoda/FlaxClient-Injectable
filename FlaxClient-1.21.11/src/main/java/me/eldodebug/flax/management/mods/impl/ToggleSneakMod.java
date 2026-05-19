package me.eldodebug.flax.management.mods.impl;

import me.eldodebug.flax.management.event.EventTarget;
import me.eldodebug.flax.management.event.impl.EventTick;
import me.eldodebug.flax.management.mods.Mod;
import me.eldodebug.flax.management.mods.ModCategory;
import org.lwjgl.glfw.GLFW;

public final class ToggleSneakMod extends Mod {

	private boolean sneakLocked;
	private boolean lastPhysicalSneakKeyDown;

	public ToggleSneakMod() {
		super("toggle_sneak", "Toggle Sneak", "Tap shift once to keep sneaking", ModCategory.PLAYER);
	}

	@EventTarget
	public void onTick(EventTick event) {
		if (mc.player == null) {
			return;
		}

		if (mc.currentScreen != null) {
			mc.options.sneakKey.setPressed(false);
			lastPhysicalSneakKeyDown = false;
			return;
		}

		boolean physicalSneakDown = isSneakKeyDown();
		if (physicalSneakDown && !lastPhysicalSneakKeyDown) {
			sneakLocked = !sneakLocked;
		}
		lastPhysicalSneakKeyDown = physicalSneakDown;

		mc.options.sneakKey.setPressed(sneakLocked || physicalSneakDown);
	}

	@Override
	public void onDisable() {
		super.onDisable();
		sneakLocked = false;
		lastPhysicalSneakKeyDown = false;
		mc.options.sneakKey.setPressed(false);
	}

	private boolean isSneakKeyDown() {
		long window = mc.getWindow().getHandle();
		return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
				|| GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
	}
}
