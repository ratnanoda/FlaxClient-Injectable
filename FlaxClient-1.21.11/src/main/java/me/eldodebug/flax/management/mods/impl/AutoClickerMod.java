package me.eldodebug.flax.management.mods.impl;

import me.eldodebug.flax.injection.mixin.accessor.MinecraftClientInvoker;
import me.eldodebug.flax.management.event.EventTarget;
import me.eldodebug.flax.management.event.impl.EventTick;
import me.eldodebug.flax.management.mods.Mod;
import me.eldodebug.flax.management.mods.ModCategory;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ThreadLocalRandom;

public final class AutoClickerMod extends Mod {

	private static final int MIN_CPS = 8;
	private static final int MAX_CPS = 13;

	private long nextClickAt;
	private int comboClicks;

	public AutoClickerMod() {
		super("auto_clicker", "Auto Clicker", "Generates humanized left-click taps", ModCategory.GHOST);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		resetClickState();
	}

	@Override
	public void onDisable() {
		super.onDisable();
		resetClickState();
	}

	@EventTarget
	public void onTick(EventTick event) {
		if (!canAutoClick()) {
			resetClickState();
			return;
		}

		long now = System.currentTimeMillis();
		if (nextClickAt <= 0L) {
			nextClickAt = now + getNextDelayMillis();
		}

		int loopGuard = 0;
		while (now >= nextClickAt && loopGuard < 4) {
			((MinecraftClientInvoker) mc).flax$doAttack();
			nextClickAt += getNextDelayMillis();
			comboClicks++;
			loopGuard++;
		}

		if (nextClickAt < now - 500L) {
			nextClickAt = now + getNextDelayMillis();
		}
	}

	private boolean canAutoClick() {
		if (mc.player == null || mc.world == null || mc.currentScreen != null) {
			return false;
		}
		long window = mc.getWindow().getHandle();
		boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		return leftDown && !mc.player.isUsingItem();
	}

	private long getNextDelayMillis() {
		double cps = ThreadLocalRandom.current().nextDouble(MIN_CPS, MAX_CPS + 0.000001D);
		long delay = Math.max(8L, Math.round(1000.0D / cps));

		double microJitter = 0.90D + ThreadLocalRandom.current().nextDouble() * 0.2D;
		delay = Math.max(8L, Math.round(delay * microJitter));

		if (ThreadLocalRandom.current().nextDouble() < 0.1D) {
			delay += ThreadLocalRandom.current().nextLong(30L, 86L);
		}

		if (comboClicks >= ThreadLocalRandom.current().nextInt(6, 11)) {
			delay += ThreadLocalRandom.current().nextLong(15L, 40L);
			comboClicks = 0;
		}

		return delay;
	}

	private void resetClickState() {
		nextClickAt = 0L;
		comboClicks = 0;
	}
}
