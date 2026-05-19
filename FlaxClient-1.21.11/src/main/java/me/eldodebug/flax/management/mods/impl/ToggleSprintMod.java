package me.eldodebug.flax.management.mods.impl;

import me.eldodebug.flax.management.event.EventTarget;
import me.eldodebug.flax.management.event.impl.EventTick;
import me.eldodebug.flax.management.mods.Mod;
import me.eldodebug.flax.management.mods.ModCategory;
import net.minecraft.client.option.KeyBinding;

public final class ToggleSprintMod extends Mod {

	public ToggleSprintMod() {
		super("toggle_sprint", "Toggle Sprint", "Always sprint when moving forward", ModCategory.PLAYER);
	}

	@EventTarget
	public void onTick(EventTick event) {
		if (mc.player == null) {
			return;
		}
		KeyBinding sprintKey = mc.options.sprintKey;
		if (mc.options.forwardKey.isPressed() && !mc.player.isSneaking() && !mc.player.isUsingItem()) {
			sprintKey.setPressed(true);
			mc.player.setSprinting(true);
		}
	}
}
