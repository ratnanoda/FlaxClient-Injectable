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

	// Probe only a tiny amount beyond the player's leading edge. This avoids
	// engaging SafeWalk while the player is still approaching the lip and
	// activates it only when the bounding box has effectively reached it.
	private static final double EDGE_PROBE_EPSILON = 0.001D;

	private boolean forcedSneak;
	private int unsneakDelayTicks;
	private BlockPos edgeGapPos;

	public SafeWalkMod() {
		super(TranslateText.SAFE_WALK, TranslateText.SAFE_WALK_DESCRIPTION, ModCategory.GHOST);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		forcedSneak = false;
		unsneakDelayTicks = 0;
		edgeGapPos = null;
	}

	@EventTarget
	public void onUpdate(EventUpdate event) {
		if(!canRun() || !settingsMet()) {
			releaseForcedSneak();
			unsneakDelayTicks = 0;
			edgeGapPos = null;
			return;
		}

		// If the gap we were sneaking over just got filled (e.g. a block was
		// placed while bridging), drop the forced sneak immediately for this
		// tick - don't wait out the unsneak delay. SafeWalk re-engages on the
		// next tick once the player reaches a new edge.
		if(wasGapJustFilled()) {
			releaseForcedSneak();
			unsneakDelayTicks = 0;
			edgeGapPos = null;
			return;
		}

		boolean edge = isEdgeOfBlock();
		if(edge) {
			forceSneak();
			unsneakDelayTicks = sneakDelaySetting.getValueInt();
		} else if(unsneakDelayTicks > 0) {
			unsneakDelayTicks--;
		} else {
			releaseForcedSneak();
			edgeGapPos = null;
		}
	}

	@Override
	public void onDisable() {
		super.onDisable();
		releaseForcedSneak();
		unsneakDelayTicks = 0;
		edgeGapPos = null;
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

		double threshold = EDGE_PROBE_EPSILON;

		if(moveX > 0.001D) {
			if(checkGap(bb.maxX + threshold, y, bb.minZ + 0.01D) || checkGap(bb.maxX + threshold, y, bb.maxZ - 0.01D)) {
				return true;
			}
		} else if(moveX < -0.001D) {
			if(checkGap(bb.minX - threshold, y, bb.minZ + 0.01D) || checkGap(bb.minX - threshold, y, bb.maxZ - 0.01D)) {
				return true;
			}
		}

		if(moveZ > 0.001D) {
			if(checkGap(bb.minX + 0.01D, y, bb.maxZ + threshold) || checkGap(bb.maxX - 0.01D, y, bb.maxZ + threshold)) {
				return true;
			}
		} else if(moveZ < -0.001D) {
			if(checkGap(bb.minX + 0.01D, y, bb.minZ - threshold) || checkGap(bb.maxX - 0.01D, y, bb.minZ - threshold)) {
				return true;
			}
		}

		return false;
	}

	// Returns true when the probed point has no block under it, and remembers
	// the empty block position so we can later tell when it gets filled in.
	private boolean checkGap(double x, double y, double z) {
		BlockPos pos = new BlockPos(x, y, z);
		if(mc.theWorld.isAirBlock(pos)) {
			edgeGapPos = pos;
			return true;
		}
		return false;
	}

	// True if the gap we last forced sneak over is now solid - i.e. a block
	// was just placed there.
	private boolean wasGapJustFilled() {
		return forcedSneak && edgeGapPos != null && !mc.theWorld.isAirBlock(edgeGapPos);
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
