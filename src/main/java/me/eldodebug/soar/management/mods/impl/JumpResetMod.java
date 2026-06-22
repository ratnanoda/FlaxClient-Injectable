package me.eldodebug.soar.management.mods.impl;

import java.util.ArrayList;
import java.util.Arrays;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventAttackEntity;
import me.eldodebug.soar.management.event.impl.EventDamageEntity;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

public class JumpResetMod extends Mod {

	private final ComboSetting resetModeSetting = new ComboSetting(TranslateText.RESET_MODE, this, TranslateText.AUTO,
			new ArrayList<Option>(Arrays.asList(new Option(TranslateText.AUTO), new Option(TranslateText.ON_HIT))));
	private final NumberSetting delaySetting = new NumberSetting(TranslateText.DELAY, this, 0, 0, 5, true);

	private static final int GRACE_TICKS = 12;
	private static final int COOLDOWN_TICKS = 8;

	private Entity lastAttacked;
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
		lastAttacked = null;
		armed = false;
		delayTicks = 0;
		graceTicks = 0;
		cooldownTicks = 0;
	}

	// The player swung and connected with an entity (a real melee hit, not just a
	// swing). In Auto mode we schedule the reset straight away.
	@EventTarget
	public void onAttack(EventAttackEntity event) {
		Entity entity = event.getEntity();
		if(entity == null || entity == mc.thePlayer || !(entity instanceof EntityLivingBase)) {
			return;
		}
		lastAttacked = entity;
		if(isAuto()) {
			schedule();
		}
	}

	// The server confirmed damage on an entity (hurt status). In On Hit mode we
	// only reset once the entity we attacked actually takes damage.
	@EventTarget
	public void onDamage(EventDamageEntity event) {
		if(isAuto()) {
			return;
		}
		Entity entity = event.getEntity();
		if(entity == null || entity == mc.thePlayer || entity != lastAttacked) {
			return;
		}
		lastAttacked = null;
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

		if(!canReset()) {
			armed = false;
			return;
		}

		// Count down the configurable post-hit delay first.
		if(delayTicks > 0) {
			delayTicks--;
			return;
		}

		// Optimal moment reached: jump on the first grounded tick so sprint resets
		// as early as possible. Only one jump per hit (a cooldown blocks repeats),
		// and never while the jump key is held to avoid an unnatural double jump.
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

	private boolean isAuto() {
		return resetModeSetting.getOption().getTranslate().equals(TranslateText.AUTO);
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
