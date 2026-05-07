package me.eldodebug.soar.management.mods.impl;

import java.util.concurrent.ThreadLocalRandom;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventDamageEntity;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;

public class JumpResetMod extends Mod {

	private final NumberSetting successChanceSetting = new NumberSetting(TranslateText.SUCCESS_CHANCE, this, 85, 0, 100, true);
	private int queuedTicks;
	private int cooldownTicks;
	private boolean jumpQueued;

	public JumpResetMod() {
		super(TranslateText.JUMP_RESET, TranslateText.JUMP_RESET_DESCRIPTION, ModCategory.GHOST);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		queuedTicks = 0;
		cooldownTicks = 0;
		jumpQueued = false;
	}

	@Override
	public void onDisable() {
		super.onDisable();
		queuedTicks = 0;
		cooldownTicks = 0;
		jumpQueued = false;
	}

	@EventTarget
	public void onDamageEntity(EventDamageEntity event) {
		if(mc.thePlayer == null || event.getEntity() != mc.thePlayer || !canJumpReset() || cooldownTicks > 0) {
			return;
		}

		double roll = ThreadLocalRandom.current().nextDouble(100.0D);
		if(roll > successChanceSetting.getValue()) {
			return;
		}

		// Keep a short timing window so the jump can still fire on the next suitable ground tick.
		jumpQueued = true;
		queuedTicks = 6;
	}

	@EventTarget
	public void onUpdate(EventUpdate event) {
		if(mc.thePlayer == null || mc.theWorld == null) {
			queuedTicks = 0;
			cooldownTicks = 0;
			jumpQueued = false;
			return;
		}

		if(!canJumpReset()) {
			queuedTicks = 0;
			jumpQueued = false;
			return;
		}

		if(cooldownTicks > 0) {
			cooldownTicks--;
		}

		if(!jumpQueued) {
			return;
		}

		if(queuedTicks-- <= 0 || mc.thePlayer.hurtTime <= 0) {
			jumpQueued = false;
			queuedTicks = 0;
			return;
		}

		if(mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown()) {
			mc.thePlayer.jump();
			jumpQueued = false;
			queuedTicks = 0;
			cooldownTicks = 6;
		}
	}

	private boolean canJumpReset() {
		if(mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null || !mc.inGameHasFocus) {
			return false;
		}

		if(mc.thePlayer.capabilities.isFlying || mc.thePlayer.ridingEntity != null) {
			return false;
		}

		if(mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || mc.thePlayer.isOnLadder()) {
			return false;
		}

		return true;
	}
}
