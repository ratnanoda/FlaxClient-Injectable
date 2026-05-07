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
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;

public class FastPlaceMod extends Mod {

	private final NumberSetting minCpsSetting = new NumberSetting(TranslateText.MIN_CPS, this, 10, 1, 24, true);
	private final NumberSetting maxCpsSetting = new NumberSetting(TranslateText.MAX_CPS, this, 14, 1, 24, true);

	private long nextClickAt;

	public FastPlaceMod() {
		super(TranslateText.FAST_PLACE, TranslateText.FAST_PLACE_DESCRIPTION, ModCategory.GHOST);
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
		if(!canFastPlace()) {
			resetClickState();
			return;
		}

		long now = System.currentTimeMillis();

		if(nextClickAt <= 0L || now >= nextClickAt) {
			IMixinMinecraft mixinMinecraft = (IMixinMinecraft) mc;

			mixinMinecraft.setRightClickDelayTimer(0);
			mixinMinecraft.callRightClickMouse();
			nextClickAt = now + getNextDelay();
		}

		if(nextClickAt < now - 500L) {
			nextClickAt = now + getNextDelay();
		}
	}

	private boolean canFastPlace() {
		if(mc.thePlayer == null || mc.theWorld == null || !mc.inGameHasFocus || mc.currentScreen != null) {
			return false;
		}

		if(!Mouse.isButtonDown(1)) {
			return false;
		}

		ItemStack heldItem = mc.thePlayer.getHeldItem();
		if(heldItem == null || !(heldItem.getItem() instanceof ItemBlock)) {
			return false;
		}

		return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
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
		return Math.max(1L, Math.round(1000.0D / cps));
	}

	private void resetClickState() {
		nextClickAt = 0L;
	}
}
