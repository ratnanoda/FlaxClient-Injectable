package me.eldodebug.soar.management.mods.impl;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventDamageEntity;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import java.util.concurrent.ThreadLocalRandom;

public class JumpResetMod extends Mod {

	private final NumberSetting successChanceSetting = new NumberSetting(
			TranslateText.SUCCESS_CHANCE, this, 100, 0, 100, true);
	private final NumberSetting delaySetting = new NumberSetting(
			TranslateText.DELAY, this, 0, 0, 3, true);

	private static final int GRACE_TICKS = 6;
	private static final int COOLDOWN_TICKS = 6;

	private boolean armed;
	private int delayTicks;
	private int graceTicks;
	private int cooldownTicks;

	public JumpResetMod() {
		super(TranslateText.JUMP_RESET, TranslateText.JUMP_RESET_DESCRIPTION, ModCategory.GHOST);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		reset();
	}

	@Override
	public void onDisable() {
		super.onDisable();
		reset();
	}

	private void reset() {
		armed = false;
		delayTicks = 0;
		graceTicks = 0;
		cooldownTicks = 0;
	}

	@EventTarget
	public void onDamage(EventDamageEntity event) {
		if(event.getEntity() != mc.thePlayer || !canReset() || cooldownTicks > 0) {
			return;
		}
		if(ThreadLocalRandom.current().nextDouble(100.0D)
				>= successChanceSetting.getValue()) {
			return;
		}
		schedule();
	}

	@EventTarget
	public void onUpdate(EventUpdate event) {

		if(mc.thePlayer == null || mc.theWorld == null) {
			reset();
			return;
		}

		if(cooldownTicks > 0) {
			cooldownTicks--;
		}

		if(!armed) {
			return;
		}

		if(!canReset() || mc.thePlayer.hurtTime <= 0) {
			armed = false;
			return;
		}

		// Count down the configurable post-hit delay first.
		if(delayTicks > 0) {
			delayTicks--;
			return;
		}

		if(mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown()) {
			mc.thePlayer.jump();
			armed = false;
			cooldownTicks = COOLDOWN_TICKS;
		} else if(--graceTicks <= 0) {
			armed = false;
		}
	}

	private void schedule() {
		if(!canReset() || cooldownTicks > 0) {
			return;
		}
		armed = true;
		delayTicks = delaySetting.getValueInt();
		graceTicks = GRACE_TICKS;
	}

	private boolean canReset() {
		if(mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null || !mc.inGameHasFocus) {
			return false;
		}

		// Defer to flight / vertical-movement states so we don't fight other
		// movement behaviour or jump mid-air.
		if(mc.thePlayer.capabilities.isFlying || mc.thePlayer.ridingEntity != null) {
			return false;
		}

		return !mc.thePlayer.isInWater() && !mc.thePlayer.isInLava() && !mc.thePlayer.isOnLadder();
	}
}
