package me.eldodebug.soar.management.mods.impl;

import java.util.concurrent.ThreadLocalRandom;

import org.lwjgl.input.Mouse;

import me.eldodebug.soar.attach.MinecraftAccess;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRenderTick;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.utils.mouse.NativeMouseBridge;

public class AutoClickerMod extends Mod {

	private final NumberSetting minCpsSetting = new NumberSetting(TranslateText.MIN_CPS, this, 8, 1, 24, true);
	private final NumberSetting maxCpsSetting = new NumberSetting(TranslateText.MAX_CPS, this, 13, 1, 24, true);

	private long nextClickAt;
	private int comboClicks;
	private long lastHeldSeen;

	public AutoClickerMod() {
		super(TranslateText.AUTO_CLICKER, TranslateText.AUTO_CLICKER_DESCRIPTION, ModCategory.GHOST);
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
	public void onRenderTick(EventRenderTick event) {
		if(!canAutoClick()) {
			resetClickState();
			return;
		}

		long now = System.currentTimeMillis();

		if(nextClickAt <= 0L) {
			nextClickAt = now + getNextDelay();
		}

		if(now >= nextClickAt) {
			boolean wasSprinting = mc.thePlayer.isSprinting();
			// Keep emitting the requested real uinput pulse, but do not depend on
			// the window system turning it into another press while the physical
			// button is already held. X11/Wayland may merge those device states.
			NativeMouseBridge.click();

			// Deliver exactly one scheduled click to Minecraft as well. Resetting
			// the vanilla delay here also prevents a later queued native event from
			// producing a second attack for the same scheduled click.
			MinecraftAccess.setLeftClickCounter(mc, 0);
			MinecraftAccess.clickMouse(mc);
			restoreSprintAfterClick(wasSprinting || mc.gameSettings.keyBindSprint.isKeyDown());
			nextClickAt = now + getNextDelay();
			comboClicks++;
		}
	}

	private boolean canAutoClick() {
		if(mc.thePlayer == null || mc.theWorld == null || !mc.inGameHasFocus || mc.currentScreen != null || mc.thePlayer.isUsingItem()) {
			return false;
		}
		return isLeftButtonHeld();
	}

	/**
	 * Whether the user is still holding the physical left button. Our own virtual
	 * release events (via uinput) flip LWJGL's cached button_state to 0, so we
	 * can't rely on {@link Mouse#isButtonDown(int)} alone once we've clicked. We
	 * read the cached physical evdev state. The cache is refreshed by the native
	 * bridge's daemon poller, so this render callback never blocks on ioctl.
	 * A short hysteresis window covers scheduling jitter between samples.
	 */
	private boolean isLeftButtonHeld() {
		long now = System.currentTimeMillis();
		int physical = NativeMouseBridge.queryButton(0);
		boolean held;
		if(physical >= 0) {
			held = physical == 1;
		} else {
			held = Mouse.isButtonDown(0);
		}
		if(held || (physical < 0 && Mouse.isButtonDown(0))) {
			lastHeldSeen = now;
			return true;
		}
		return now - lastHeldSeen < 60L;
	}

	private long getNextDelay() {
		int minCps = minCpsSetting.getValueInt();
		int maxCps = maxCpsSetting.getValueInt();

		if(maxCps < minCps) {
			int temp = minCps;
			minCps = maxCps;
			maxCps = temp;
		}

		double cps = minCps == maxCps ? minCps : ThreadLocalRandom.current().nextDouble(minCps, maxCps + 0.000001D);
		long delay = Math.max(8L, Math.round(1000.0D / cps));

		double microJitter = 0.90D + ThreadLocalRandom.current().nextDouble() * 0.2D;
		delay = Math.max(8L, Math.round(delay * microJitter));

		if(ThreadLocalRandom.current().nextDouble() < 0.1D) {
			delay += ThreadLocalRandom.current().nextLong(30L, 86L);
		}

		if(comboClicks >= ThreadLocalRandom.current().nextInt(6, 11)) {
			delay += ThreadLocalRandom.current().nextLong(15L, 40L);
			comboClicks = 0;
		}

		return delay;
	}

	private void resetClickState() {
		nextClickAt = 0L;
		comboClicks = 0;
		lastHeldSeen = 0L;
	}

	private void restoreSprintAfterClick(boolean wasSprinting) {
		if(!wasSprinting || mc.thePlayer == null) {
			return;
		}

		if(mc.thePlayer.isSprinting()) {
			return;
		}

		if(mc.thePlayer.isSneaking() || mc.thePlayer.isUsingItem() || mc.thePlayer.isCollidedHorizontally) {
			return;
		}

		if(mc.thePlayer.moveForward <= 0.0F) {
			return;
		}

		if(mc.thePlayer.getFoodStats().getFoodLevel() <= 6 && !mc.thePlayer.capabilities.allowFlying) {
			return;
		}

		mc.thePlayer.setSprinting(true);
	}
}
