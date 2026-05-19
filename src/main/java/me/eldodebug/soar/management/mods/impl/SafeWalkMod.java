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
	private final NumberSetting sneakDelaySetting = new NumberSetting(TranslateText.DELAY, this, 1, 0, 10, true);
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
		if(!canRun() || !settingsMet()) {
			releaseForcedSneak();
			unsneakDelayTicks = 0;
			return;
		}

		boolean edge = isEdgeOfBlock();
		if(edge) {
			forceSneak();
			unsneakDelayTicks = sneakDelaySetting.getValueInt();
			applyEdgeMotionLimit();
		} else if(unsneakDelayTicks > 0) {
			unsneakDelayTicks--;
		} else {
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

		return !mc.thePlayer.isInWater() && !mc.thePlayer.isInLava() && !mc.thePlayer.isOnLadder();
	}

	private boolean settingsMet() {
		if(!blocksOnlySetting.isToggled()) {
			return true;
		}

		ItemStack heldItem = mc.thePlayer.getHeldItem();
		return heldItem != null && heldItem.getItem() instanceof ItemBlock;
	}

	private boolean isEdgeOfBlock() {
		AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox();
		double y = bb.minY - 0.5D;

		double moveX = mc.thePlayer.motionX;
		double moveZ = mc.thePlayer.motionZ;

		if(Math.abs(moveX) < 0.001D && Math.abs(moveZ) < 0.001D) {
			return false;
		}

		double threshold = 0.125D;

		if(moveX > 0.001D) {
			if(!hasSupport(bb.maxX + threshold, y, bb.minZ + 0.01D) || !hasSupport(bb.maxX + threshold, y, bb.maxZ - 0.01D)) {
				return true;
			}
		} else if(moveX < -0.001D) {
			if(!hasSupport(bb.minX - threshold, y, bb.minZ + 0.01D) || !hasSupport(bb.minX - threshold, y, bb.maxZ - 0.01D)) {
				return true;
			}
		}

		if(moveZ > 0.001D) {
			if(!hasSupport(bb.minX + 0.01D, y, bb.maxZ + threshold) || !hasSupport(bb.maxX - 0.01D, y, bb.maxZ + threshold)) {
				return true;
			}
		} else if(moveZ < -0.001D) {
			if(!hasSupport(bb.minX + 0.01D, y, bb.minZ - threshold) || !hasSupport(bb.maxX - 0.01D, y, bb.minZ - threshold)) {
				return true;
			}
		}

		return false;
	}

	private boolean hasSupport(double x, double y, double z) {
		return !mc.theWorld.isAirBlock(new BlockPos(x, y, z));
	}

	private void applyEdgeMotionLimit() {
		double edgeMotion = edgeMotionSetting.getValue();
		if(edgeMotion >= 0.999D) {
			return;
		}

		mc.thePlayer.motionX *= edgeMotion;
		mc.thePlayer.motionZ *= edgeMotion;
	}

	private void forceSneak() {
		if(isPhysicalSneakPressed()) {
			forcedSneak = false;
			return;
		}

		if(!forcedSneak) {
			setSneak(true);
			forcedSneak = true;
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
	}

	private void releaseForcedSneak() {
		if(!forcedSneak) {
			return;
		}

		if(!isPhysicalSneakPressed()) {
			setSneak(false);
		}
		forcedSneak = false;
	}
}
