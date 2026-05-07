package me.eldodebug.soar.management.mods.impl;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class SafeWalkMod extends Mod {

	private final BooleanSetting blocksOnlySetting = new BooleanSetting(TranslateText.BLOCK, this, true);
	private final NumberSetting sneakDelaySetting = new NumberSetting(TranslateText.DELAY, this, 2, 0, 10, true);
	private final NumberSetting edgeMotionSetting = new NumberSetting(TranslateText.MOVEMENT, this, 1.0D, 0.5D, 1.0D, false);

	private boolean forcedSneak;
	private int unsneakDelayTicks;

	public SafeWalkMod() {
		super(TranslateText.SAFE_WALK, TranslateText.SAFE_WALK_DESCRIPTION, ModCategory.GHOST);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		forcedSneak = false;
		unsneakDelayTicks = 0;
	}

	@EventTarget
	public void onUpdate(EventUpdate event) {
		if(!canRun()) {
			releaseForcedSneak();
			unsneakDelayTicks = 0;
			return;
		}

		if(!settingsMet()) {
			releaseForcedSneak();
			unsneakDelayTicks = 0;
			return;
		}

		boolean edge = isEdgeOfBlock();
		if(edge) {
			forceSneakIfNeeded();
			unsneakDelayTicks = sneakDelaySetting.getValueInt();
			applyEdgeMotionLimit();
			return;
		}

		if(unsneakDelayTicks > 0) {
			unsneakDelayTicks--;
			return;
		}

		if(forcedSneak) {
			releaseForcedSneak();
		}
	}

	@Override
	public void onDisable() {
		super.onDisable();
		releaseForcedSneak();
		unsneakDelayTicks = 0;
	}

	private boolean canRun() {
		if(mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null || !mc.inGameHasFocus) {
			return false;
		}

		if(!mc.thePlayer.onGround || mc.thePlayer.capabilities.isFlying || mc.thePlayer.ridingEntity != null) {
			return false;
		}

		if(mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || mc.thePlayer.isOnLadder()) {
			return false;
		}

		return true;
	}

	private boolean settingsMet() {
		if(blocksOnlySetting.isToggled()) {
			ItemStack heldItem = mc.thePlayer.getHeldItem();
			if(heldItem == null || !(heldItem.getItem() instanceof ItemBlock)) {
				return false;
			}
		}

		return true;
	}

	private boolean isEdgeOfBlock() {
		if(!mc.thePlayer.onGround) {
			return false;
		}

		AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox();
		if(bb == null) {
			return false;
		}

		double y = bb.minY - 0.05D;
		double inset = 0.001D;

		boolean supportMinMin = hasSupport(bb.minX + inset, y, bb.minZ + inset);
		boolean supportMinMax = hasSupport(bb.minX + inset, y, bb.maxZ - inset);
		boolean supportMaxMin = hasSupport(bb.maxX - inset, y, bb.minZ + inset);
		boolean supportMaxMax = hasSupport(bb.maxX - inset, y, bb.maxZ - inset);

		int supportCount = 0;
		if(supportMinMin) supportCount++;
		if(supportMinMax) supportCount++;
		if(supportMaxMin) supportCount++;
		if(supportMaxMax) supportCount++;

		return supportCount > 0 && supportCount < 4;
	}

	private boolean hasSupport(double x, double y, double z) {
		BlockPos below = new BlockPos(x, y, z);
		return !mc.theWorld.isAirBlock(below);
	}

	private void applyEdgeMotionLimit() {
		double edgeMotion = edgeMotionSetting.getValue();
		if(edgeMotion >= 0.999D) {
			return;
		}

		if(!isMoving()) {
			return;
		}

		mc.thePlayer.motionX *= edgeMotion;
		mc.thePlayer.motionZ *= edgeMotion;
	}

	private boolean isMoving() {
		if(mc.thePlayer.movementInput == null) {
			return false;
		}

		return Math.abs(mc.thePlayer.movementInput.moveForward) > 0.01F || Math.abs(mc.thePlayer.movementInput.moveStrafe) > 0.01F;
	}

	private void forceSneakIfNeeded() {
		if(!isPhysicalSneakPressed()) {
			if(!forcedSneak) {
				setSneak(true);
				forcedSneak = true;
			}
		} else {
			forcedSneak = false;
		}
	}

	private boolean isPhysicalSneakPressed() {
		int keyCode = mc.gameSettings.keyBindSneak.getKeyCode();
		if(keyCode < 0) {
			return Mouse.isButtonDown(keyCode + 100);
		}
		return Keyboard.isKeyDown(keyCode);
	}

	private void setSneak(boolean state) {
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), state);
		if(state) {
			KeyBinding.onTick(mc.gameSettings.keyBindSneak.getKeyCode());
		}
	}

	private void releaseForcedSneak() {
		if(forcedSneak) {
			if(!isPhysicalSneakPressed()) {
				setSneak(false);
			}
			forcedSneak = false;
		}
	}
}
