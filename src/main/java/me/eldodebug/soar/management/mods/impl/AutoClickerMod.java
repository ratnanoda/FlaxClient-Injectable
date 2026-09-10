package me.eldodebug.soar.management.mods.impl;

import java.util.concurrent.ThreadLocalRandom;

import org.lwjgl.input.Mouse;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRenderTick;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.utils.mouse.MinecraftMouseInput;

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

		if(now >= nextClickAt) {
			// Queue the attack through Minecraft's own KeyBinding input path.
			// The normal client tick consumes this press exactly like an LWJGL
			// mouse-button press instead of us calling clickMouse() directly.
			MinecraftMouseInput.click(mc.gameSettings.keyBindAttack);
			nextClickAt = now + getNextDelay();
			comboClicks++;
		}
	}

	private boolean canAutoClick() {
		if(mc.thePlayer == null || mc.theWorld == null || !mc.inGameHasFocus || mc.currentScreen != null || mc.thePlayer.isUsingItem()) {
			return false;
		}
		return Mouse.isButtonDown(0);
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
}
