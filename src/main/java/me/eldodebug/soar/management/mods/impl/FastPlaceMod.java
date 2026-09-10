package me.eldodebug.soar.management.mods.impl;

import java.util.Random;

import org.lwjgl.input.Mouse;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.utils.mouse.MinecraftMouseInput;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class FastPlaceMod extends Mod {

	private final Random random = new Random();
	private int targetDelayTicks = 1;
	private int ticksUntilClick;
	private long nextDelayRefreshAt;
	private int streakUntilRefresh;

	public FastPlaceMod() {
		super(TranslateText.FAST_PLACE, TranslateText.FAST_PLACE_DESCRIPTION, ModCategory.GHOST);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		resetClickState();
		refreshDelayTarget();
	}

	@Override
	public void onDisable() {
		super.onDisable();
		resetClickState();
	}

	@EventTarget
	public void onUpdate(EventUpdate event) {
		if(!canFastPlace()) {
			resetClickState();
			return;
		}

		ItemStack heldItem = mc.thePlayer.getHeldItem();
		if(heldItem == null || !(heldItem.getItem() instanceof ItemBlock)) {
			resetClickState();
			return;
		}

		long now = System.currentTimeMillis();
		if(now >= nextDelayRefreshAt || streakUntilRefresh <= 0) {
			refreshDelayTarget();
		}

		if(ticksUntilClick <= 0) {
			// Queue a real use-item press in Minecraft's input state instead of
			// only shortening rightClickDelayTimer. The client then consumes this
			// through the same KeyBinding press queue used by mouse input.
			MinecraftMouseInput.click(mc.gameSettings.keyBindUseItem);
			ticksUntilClick = targetDelayTicks;
			streakUntilRefresh--;
		} else {
			ticksUntilClick--;
		}
	}

	private boolean canFastPlace() {
		return mc.thePlayer != null && mc.theWorld != null && mc.inGameHasFocus
				&& mc.currentScreen == null && Mouse.isButtonDown(1);
	}

	private void refreshDelayTarget() {
		double roll = random.nextDouble();
		if(roll < 0.72D) {
			targetDelayTicks = 0;
		} else if(roll < 0.96D) {
			targetDelayTicks = 1;
		} else {
			targetDelayTicks = 2;
		}

		nextDelayRefreshAt = System.currentTimeMillis() + 55L + random.nextInt(125);
		streakUntilRefresh = 4 + random.nextInt(9);
	}

	private void resetClickState() {
		ticksUntilClick = 0;
	}
}
