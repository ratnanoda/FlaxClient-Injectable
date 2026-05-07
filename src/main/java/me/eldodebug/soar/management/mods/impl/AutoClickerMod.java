package me.eldodebug.soar.management.mods.impl;

import java.util.concurrent.ThreadLocalRandom;

import org.lwjgl.input.Mouse;

import me.eldodebug.soar.injection.interfaces.IMixinMinecraft;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRenderTick;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;

public class AutoClickerMod extends Mod {

	private final NumberSetting minCpsSetting = new NumberSetting(TranslateText.MIN_CPS, this, 8, 1, 24, true);
	private final NumberSetting maxCpsSetting = new NumberSetting(TranslateText.MAX_CPS, this, 13, 1, 24, true);

	private long nextClickAt;
	private int comboClicks;

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

		int loopGuard = 0;

		while(now >= nextClickAt && loopGuard < 4) {
			IMixinMinecraft mixinMinecraft = (IMixinMinecraft) mc;
			boolean wasSprinting = mc.thePlayer.isSprinting();
			mixinMinecraft.setLeftClickCounter(0);
			mixinMinecraft.callClickMouse();
			restoreSprintAfterClick(wasSprinting || mc.gameSettings.keyBindSprint.isKeyDown());
			nextClickAt += getNextDelay();
			comboClicks++;
			loopGuard++;
		}

		if(nextClickAt < now - 500L) {
			nextClickAt = now + getNextDelay();
		}
	}

	private boolean canAutoClick() {
		return mc.thePlayer != null && mc.theWorld != null && mc.inGameHasFocus && mc.currentScreen == null && Mouse.isButtonDown(0);
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
