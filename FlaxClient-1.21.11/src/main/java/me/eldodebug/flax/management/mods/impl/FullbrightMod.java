package me.eldodebug.flax.management.mods.impl;

import me.eldodebug.flax.management.event.EventTarget;
import me.eldodebug.flax.management.event.impl.EventTick;
import me.eldodebug.flax.management.mods.Mod;
import me.eldodebug.flax.management.mods.ModCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public final class FullbrightMod extends Mod {

	public FullbrightMod() {
		super("fullbright", "Fullbright", "Brightens the world", ModCategory.PLAYER);
	}

	@EventTarget
	public void onTick(EventTick event) {
		if (mc.player == null) {
			return;
		}
		mc.player.addStatusEffect(
				new StatusEffectInstance(StatusEffects.NIGHT_VISION, 220, 0, false, false, false));
	}

	@Override
	public void onDisable() {
		super.onDisable();
		if (mc.player != null) {
			mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
		}
	}
}
