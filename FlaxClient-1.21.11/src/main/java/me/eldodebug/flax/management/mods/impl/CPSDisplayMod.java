package me.eldodebug.flax.management.mods.impl;

import me.eldodebug.flax.management.event.EventTarget;
import me.eldodebug.flax.management.event.impl.EventRender2D;
import me.eldodebug.flax.management.event.impl.EventTick;
import me.eldodebug.flax.management.mods.Mod;
import me.eldodebug.flax.management.mods.ModCategory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class CPSDisplayMod extends Mod {

	private final List<Long> leftPresses = new ArrayList<>();
	private final List<Long> rightPresses = new ArrayList<>();
	private boolean lastLeftDown;
	private boolean lastRightDown;

	public CPSDisplayMod() {
		super("cps_display", "CPS Display", "Shows left/right click rate", ModCategory.HUD);
	}

	@EventTarget
	public void onTick(EventTick event) {
		long now = System.currentTimeMillis();
		leftPresses.removeIf(time -> now - time > 1000L);
		rightPresses.removeIf(time -> now - time > 1000L);

		boolean leftDown = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean rightDown = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

		if (leftDown && !lastLeftDown) {
			leftPresses.add(now);
		}
		if (rightDown && !lastRightDown) {
			rightPresses.add(now);
		}
		lastLeftDown = leftDown;
		lastRightDown = rightDown;
	}

	@EventTarget
	public void onRender(EventRender2D event) {
		String text = leftPresses.size() + " | " + rightPresses.size() + " CPS";
		HudRenderUtil.drawHudLine(event.getContext(), mc, 4, 18, text, 0xFF00C9A7, 0xFFFFFFFF);
	}
}
